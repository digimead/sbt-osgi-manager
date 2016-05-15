/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2013-2016 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.osgi.manager.tycho

import java.io.{ File, FilenameFilter, IOException, InputStream }
import java.net.{ MalformedURLException, URL, URLClassLoader }
import java.util.Properties
import org.apache.maven.DefaultMaven
import org.apache.maven.artifact.{ Artifact, InvalidRepositoryException }
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.cli.MavenCli
import org.apache.maven.execution.{ DefaultMavenExecutionRequest, DefaultMavenExecutionResult, MavenExecutionRequest, MavenExecutionRequestPopulationException, MavenExecutionRequestPopulator, MavenSession }
import org.apache.maven.plugin.LegacySupport
import org.apache.maven.project.{ DefaultProjectBuildingRequest, ProjectBuilder, ProjectBuildingRequest }
import org.apache.maven.repository.RepositorySystem
import org.apache.maven.settings.Settings
import org.apache.maven.settings.building.{ DefaultSettingsBuildingRequest, SettingsBuilder, SettingsBuildingException }
import org.codehaus.plexus.{ DefaultContainerConfiguration, DefaultPlexusContainer }
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import org.codehaus.plexus.component.repository.exception.ComponentLookupException
import org.codehaus.plexus.util.IOUtil
import org.eclipse.core.runtime.adaptor.EclipseStarter
import org.eclipse.sisu.equinox.embedder.{ EquinoxLifecycleListener, EquinoxRuntimeLocator }
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator.EquinoxRuntimeDescription
import org.eclipse.sisu.equinox.embedder.internal.DefaultEquinoxEmbedder
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory
import org.osgi.framework.{ Bundle, BundleContext, BundleException }
import sbt.{ inConfig, richFile }
import sbt.osgi.manager.{ Environment, Model, OSGiManagerException, Plugin }
import sbt.osgi.manager.Keys._
import sbt.osgi.manager.Support.{ getEnvVars, logPrefix, option2rich, withClassLoaderOf }
import scala.collection.JavaConversions.{ asScalaBuffer, collectionAsScalaIterable, seqAsJavaList }
import org.apache.maven.cli.configuration.SettingsXmlConfigurationProcessor

class Maven(val plexus: DefaultPlexusContainer, val information: Maven.Information)(implicit arg: Plugin.TaskArgument) {
  val home = Maven.getHome()
  val bin = Maven.getMavenBin(home)
  val boot = Maven.getMavenBoot(home)
  val conf = Maven.getMavenConf(home)
  val lib = Maven.getMavenLib(home)
  val session = lookup(classOf[org.apache.maven.Maven]) match {
    case default: DefaultMaven ⇒
      try {
        val mavenExecutionRequest = createMavenExecutionRequest
        val repositorySession = default.newRepositorySession(mavenExecutionRequest)
        val session = new MavenSession(plexus, repositorySession, mavenExecutionRequest, new DefaultMavenExecutionResult())
        // Support for TychoOsgiRuntimeLocator. Tycho is very, very old... like a mammoth shit.
        // It is required LegacySupport (at least version 0.17 from 10 April 2013)
        lookup(classOf[LegacySupport]).setSession(session)
        val repositorySystem = lookup(classOf[RepositorySystem])
        val projectBuilder = lookup(classOf[ProjectBuilder])
        val artifact: Artifact = repositorySystem.createProjectArtifact("x", "y", "z")
        val projectBuildingRequest: ProjectBuildingRequest = new DefaultProjectBuildingRequest().
          setRepositorySession(session.getRepositorySession()).
          setPluginArtifactRepositories(List(session.getLocalRepository(), repositorySystem.createDefaultRemoteRepository()))
        val result = projectBuilder.build(artifact, true, projectBuildingRequest)
        val project = result.getProject()
        session.setProjects(List(project))
        session.setCurrentProject(project)
        session
      } catch {
        case e: ComponentLookupException ⇒
          throw new OSGiManagerException(e.getMessage(), e)
      }
    case other ⇒
      throw new OSGiManagerException("Unexpected type of Maven instance: %s. Expected: %s.".format(other.getClass(), classOf[DefaultMaven]))
  }
  lazy val settings = createMavenSettings()
  assert(lookup(classOf[LegacySupport]).getSession() != null, "Lost Maven session")
  assert(lookup(classOf[LegacySupport]).getRepositorySession() != null, "Lost Maven repository session")
  // Create our embedder with boot delegation.
  // I assume that we have absolutely flat structure with less than hundred bundles without JVM dependency intersections
  // I load whole org.osgi.* and org.eclipse.* via sbt.PluginManagement.PluginClassLoader
  /** EquinoxServiceFactory instance */
  val equinox = new Maven.EquinoxEmbedder(this)
  // val originalEquinox = lookup(classOf[EquinoxServiceFactory]) - as a history
  // initialize OSGi infrastructure via implicit 'start'
  /** Main resolve factory instance */
  val p2ResolverFactory = {
    // Negate commit 485da18aa3363e930c0129a70593987efd082133 effect
    // Override certain SecurityManager methods to avoid filesystem performance hit.
    // harrah authored on Mar 6 eed3si9n committed on Mar 22
    // SBT-0.13.2-M3 is ok
    // SBT-0.13.2-RC1 is broken
    System.setSecurityManager(null)
    // Get service and implicitly initialize OSGi
    equinox.getService(classOf[P2ResolverFactory])
  }

  def lookup[T](clazz: Class[T]): T = plexus.lookup(clazz)
  def lookup[T](clazz: Class[T], hint: String): T = plexus.lookup(clazz, hint)
  def lookup[T](role: String, hint: String): T = plexus.lookup(role, hint).asInstanceOf[T]
  def lookup[T](role: String): T = plexus.lookup(role).asInstanceOf[T]
  def lookupList[T](clazz: Class[T]): java.util.List[T] = plexus.lookupList(clazz)
  def lookupList[T](role: String): java.util.List[T] = plexus.lookupList(role).asInstanceOf[java.util.List[T]]
  def lookupMap[T](clazz: Class[T]): java.util.Map[String, T] = plexus.lookupMap(clazz)
  def lookupMap[T](role: String): java.util.Map[String, T] = plexus.lookupMap(role).asInstanceOf[java.util.Map[String, T]]

  protected def createMavenSettings(): Settings = try {
    withSafeProperties {
      val settingsBuildingRequest = new DefaultSettingsBuildingRequest()
      Model.getSettingsMavenGlobalXML match {
        case Some(file) ⇒ settingsBuildingRequest.setGlobalSettingsFile(file)
        case None ⇒ SettingsXmlConfigurationProcessor.DEFAULT_GLOBAL_SETTINGS_FILE
      }
      Model.getSettingsMavenUserXML match {
        case Some(file) ⇒ settingsBuildingRequest.setUserSettingsFile(file)
        case None ⇒ SettingsXmlConfigurationProcessor.DEFAULT_USER_SETTINGS_FILE
      }
      Model.getSettingsMavenUserProperties foreach (settingsBuildingRequest.getUserProperties().putAll)
      Model.getSettingsMavenSystemProperties foreach (settingsBuildingRequest.getSystemProperties().putAll)
      lookup(classOf[SettingsBuilder]).build(settingsBuildingRequest).getEffectiveSettings()
    }
  } catch {
    case e: SettingsBuildingException ⇒
      throw new OSGiManagerException(e.getMessage(), e)
  }
  protected def createMavenExecutionRequest(): MavenExecutionRequest = {
    withSafeProperties {
      val request = new DefaultMavenExecutionRequest()
      Model.getSettingsMavenGlobalXML foreach (request.setGlobalSettingsFile(_))
      Model.getSettingsMavenUserXML foreach (request.setUserSettingsFile(_))
      try {
        lookup(classOf[MavenExecutionRequestPopulator]).populateFromSettings(request, settings)
        lookup(classOf[MavenExecutionRequestPopulator]).populateDefaults(request)
      } catch {
        case e: MavenExecutionRequestPopulationException ⇒
          throw new OSGiManagerException(e.getMessage(), e)
      }
      val localRepository = getLocalRepository()
      request.setLocalRepository(localRepository);
      request.setLocalRepositoryPath(localRepository.getBasedir())
      request.setOffline(Model.getSettingsIsOffline getOrElse false)
      request.setUpdateSnapshots(Model.getSettingsIsUpdateSnapshots getOrElse false)
      // TODO check null and create a console one ?
      //this.mavenExecutionRequest.setTransferListener( this.mavenRequest.getTransferListener() );

      //this.mavenExecutionRequest.setCacheNotFound( this.mavenRequest.isCacheNotFound() );
      //this.mavenExecutionRequest.setCacheTransferError( true );

      Model.getSettingsMavenUserProperties foreach (request.setUserProperties)
      Model.getSettingsMavenSystemProperties foreach (request.setSystemProperties)
      // FIXME
      //request.setLoggingLevel( request.LOGGING_LEVEL_DEBUG )

      /* request.setExecutionListener( request.getExecutionListener() )
            .setInteractiveMode( this.mavenRequest.isInteractive() )
            .setGlobalChecksumPolicy( this.mavenRequest.getGlobalChecksumPolicy() )
            .setGoals( this.mavenRequest.getGoals() );

        if ( this.mavenRequest.getPom() != null ) {
            this.mavenExecutionRequest.setPom( new File( this.mavenRequest.getPom() ) );
        }

        if (this.mavenRequest.getWorkspaceReader() != null) {
            this.mavenExecutionRequest.setWorkspaceReader( this.mavenRequest.getWorkspaceReader() );
        }*/
      request
    }
  }
  protected def getLocalRepository(): ArtifactRepository = try {
    val localRepositoryPath = getLocalRepositoryPath()
    if (localRepositoryPath != null)
      lookup(classOf[RepositorySystem]).createLocalRepository(new File(localRepositoryPath))
    else
      lookup(classOf[RepositorySystem]).createLocalRepository(RepositorySystem.defaultUserLocalRepository)
  } catch {
    case e: InvalidRepositoryException ⇒
      // never happened
      throw new IllegalStateException(e)
  }
  protected def getLocalRepositoryPath(): String = {
    val path = try {
      settings.getLocalRepository()
    } catch {
      case _: OSGiManagerException ⇒ null // ignore
      case _: ComponentLookupException ⇒ null // ignore
    }
    Option(path) getOrElse (RepositorySystem.defaultUserLocalRepository.getAbsolutePath())
  }
  /** Replace global system properties with plugin settings */
  protected def withSafeProperties[T](f: ⇒ T): T = {
    val systemMavenHomeValue = System.getProperty("maven.home")
    val systemMavenUserDirectoryValue = System.getProperty("user.dir")
    val systemUserHomeValue = System.getProperty("user.home")
    System.setProperty("maven.home", home.getAbsolutePath())
    System.setProperty("user.dir", "") // bind conf/settings.xml location to mavenHome
    Model.getSettingsMavenUserHome foreach (f ⇒ System.setProperty("user.home", f.getAbsolutePath()))
    try {
      f
    } finally {
      System.setProperty("maven.home", Option(systemMavenHomeValue) getOrElse "")
      System.setProperty("user.dir", Option(systemMavenUserDirectoryValue) getOrElse "")
      System.setProperty("user.home", Option(systemUserHomeValue) getOrElse "")
    }
  }
}

object Maven {
  private val POM_PROPERTIES_PATH = "META-INF/maven/org.apache.maven/maven-core/pom.properties"
  @volatile private var singleton: Option[Maven] = None
  lazy val settings = inConfig(OSGiConf)(Seq(
    osgiMavenDirectory <<= (osgiDirectory) { _ / "maven" },
    osgiMavenIsOffline := false,
    osgiMavenIsUpdateSnapshots := false,
    osgiMavenPlexusXML := getClass.getClassLoader().getResource("plexus.xml"),
    osgiMavenGlobalSettingsXML := None,
    osgiMavenSystemProperties := {
      val properties = new Properties
      properties.putAll(System.getProperties())
      properties.putAll(getEnvVars)
      properties
    },
    osgiMavenUserHome := new File(System.getProperty("user.home")),
    osgiMavenUserProperties := new Properties(),
    osgiMavenUserSettingsXML := None,
    osgiTychoExecutionEnvironmentConfiguration := Environment.Execution.JavaSE6,
    osgiTychoTarget := Environment.current))

  /** Create new Maven singleton */
  def apply()(implicit arg: Plugin.TaskArgument): Maven = singleton getOrElse {
    // Prevents DefaultEquinoxEmbedder to throw IllegalStateException if project is reloaded
    // If Mark Harrah do everything right then Maven infrastructure must be unloaded with project class loader.
    System.setProperty("org.osgi.framework.vendor", "")
    // Create Maven infrastructure
    val mavenHome = getHome
    val world = new ClassWorld()
    val realm = buildClassRealm(mavenHome, Some(world))
    Maven.getMavenVersion(mavenHome, Some(realm)) match {
      case Some(information) ⇒
        _root_.sbt.osgi.manager.tycho.Logger.
          info("Initialize Maven core. Maven version: " + information.version, null)
        val plexus = buildPlexusContainer(realm, getPlexusOverridingComponentsXml)
        val instance = new Maven(plexus, information)
        singleton = Some(instance)
        instance
      case None ⇒
        throw new OSGiManagerException("Maven version not found.")
    }
  }
  /** Path to Maven home directory */
  def getHome()(implicit arg: Plugin.TaskArgument) =
    Model.getSettingsMavenDirectory getOrThrow "osgiMavenDirectory is undefined"
  /** Path to Maven bin directory that usually contains binary utilities */
  def getMavenBin(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, "bin") else new File(getHome, "bin")
  /** Path to Maven boot directory that usually contains a classwords jar */
  def getMavenBoot(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, "boot") else new File(getHome, "boot")
  /** Path to Maven conf directory that usually contains a global settings.xml */
  def getMavenConf(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, "conf") else new File(getHome, "conf")
  /** Path to Maven lib directory that usually contains core jars */
  def getMavenLib(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, "lib") else new File(getHome, "lib")
  def getPlexusOverridingComponentsXml()(implicit arg: Plugin.TaskArgument) =
    Model.getSettingsMavenPlexusXML getOrElse getClass.getClassLoader().getResource("plexus.xml")
  /** Prepare Maven home directory */
  def prepareHome()(implicit arg: Plugin.TaskArgument): File = {
    arg.log.debug(logPrefix(arg.name) + "Prepare Maven home directory.")
    val mavenHome = getHome
    //IO.delete(mavenHome)
    if (!mavenHome.exists())
      if (!mavenHome.mkdirs())
        throw new OSGiManagerException("Unable to create osgiMavenDirectory: " + mavenHome.getAbsolutePath())
    val mavenBin = getMavenBin(mavenHome)
    if (!mavenBin.exists())
      if (!mavenBin.mkdirs())
        throw new OSGiManagerException("Unable to create osgiMavenDirectory / bin: " + mavenBin.getAbsolutePath())
    val mavenBoot = getMavenBoot(mavenHome)
    if (!mavenBoot.exists())
      if (!mavenBoot.mkdirs())
        throw new OSGiManagerException("Unable to create osgiMavenDirectory / boot: " + mavenBoot.getAbsolutePath())
    val mavenConf = getMavenConf(mavenHome)
    if (!mavenConf.exists())
      if (!mavenConf.mkdirs())
        throw new OSGiManagerException("Unable to create osgiMavenDirectory / conf: " + mavenConf.getAbsolutePath())
    val mavenLib = getMavenLib(mavenHome)
    if (!mavenLib.exists())
      if (!mavenLib.mkdirs())
        throw new OSGiManagerException("Unable to create osgiMavenDirectory / lib: " + mavenLib.getAbsolutePath())
    mavenHome
  }

  /**
   * Build a {@link ClassRealm} with all jars in mavenHome / lib / *.jar
   * the {@link ClassRealm} is ChildFirst with the current classLoader as parent.
   */
  protected def buildClassRealm(mavenHome: File, argWorld: Option[ClassWorld] = None, argParentClassLoader: Option[ClassLoader] = None): ClassRealm = {
    val mavenLibraries = new File(mavenHome, "lib")
    assert(mavenHome != null, "mavenHome cannot be null")
    assert(mavenHome.exists(), "mavenHome must exists")
    assert(mavenHome.exists(), "mavenHome/lib must exists")
    val jarFiles = mavenLibraries.listFiles(new FilenameFilter { def accept(dir: File, name: String) = name.endsWith(".jar") })
    val world = argWorld getOrElse new ClassWorld()
    val parentClassLoader = argParentClassLoader getOrElse classOf[org.apache.maven.Maven].getClassLoader()
    val classRealm = new ClassRealm(world, "plexus.core", parentClassLoader)
    classRealm.setParentRealm(new ClassRealm(world, "maven-parent", Thread.currentThread().getContextClassLoader()))
    // add jars from mavenLibraries
    for (jarFile ← jarFiles) try {
      classRealm.addURL(jarFile.toURI().toURL())
    } catch {
      case e: MalformedURLException ⇒
        throw new OSGiManagerException(e.getMessage(), e)
    }
    // add jars from current classOf[org.apache.maven.Maven] loader
    classOf[org.apache.maven.Maven].getClassLoader() match {
      case loader: URLClassLoader ⇒ loader.getURLs().foreach(classRealm.addURL)
      case _ ⇒
    }
    classRealm
  }
  protected def buildPlexusContainer(realm: ClassRealm, configurationURL: URL): DefaultPlexusContainer = {
    withClassLoaderOf(realm) {
      // Remove plexus-container-default...jar
      // Remove sisu-inject-plexus...jar
      val configuration = new DefaultContainerConfiguration().
        //setAutoWiring(true).
        setClassWorld(realm.getWorld()).
        setContainerConfigurationURL(configurationURL).
        setName("sbt.osgi.manager").
        //setComponentVisibility(PlexusConstants.GLOBAL_VISIBILITY).
        setRealm(realm)
      // classes from plexus-container-default come with the maven installation
      // BUT groupId/artifactId was changed from
      //    plexus-container-default (maven 2.0.x)
      //    sisu-inject-plexus (maven 3.0.x)
      //    org.eclipse.sisu.plexus (maven 3.1.x)
      val container = new DefaultPlexusContainer(configuration, new DependencyInjectionModule)
      container.getLogger().debug("Initialize Plexus container.")
      container.discoverComponents(container.getContainerRealm())
      container
    }
  }
  protected def getMavenVersion(mavenHome: File, argRealm: Option[ClassRealm] = None): Option[Information] = {
    val realm = argRealm getOrElse buildClassRealm(mavenHome)
    withClassLoaderOf(realm) {
      var inputStream: InputStream = null
      try {
        Thread.currentThread().setContextClassLoader(realm)
        val resource = realm.findResource(POM_PROPERTIES_PATH)
        if (resource == null)
          return None
        inputStream = resource.openStream()
        val properties = new Properties()
        properties.load(inputStream)
        Some(Information(properties.getProperty("version"), resource.toExternalForm()))
      } catch {
        case e: IOException ⇒
          throw new OSGiManagerException(e.getMessage(), e)
      } finally {
        IOUtil.close(inputStream)
      }
    }
  }
  case class Information(version: String, versionResourcePath: String)
  // doStart is protected, equinoxLocator is private... fucking designers, lol ;-)
  // poor Sonatype... at least they try to make things
  class EquinoxEmbedder(maven: Maven) extends DefaultEquinoxEmbedder {
    val bootClasses = Seq("org.eclipse.*", "org.osgi.*")

    protected def activateBundlesInWorkingOrderR() {
      val method = classOf[DefaultEquinoxEmbedder].getDeclaredMethod("activateBundlesInWorkingOrder")
      method.setAccessible(true)
      method.invoke(this).asInstanceOf[Array[String]]
    }
    protected def addBundlesDirR(bundles: java.lang.StringBuilder, files: Array[File], absolute: java.lang.Boolean) {
      val method = classOf[DefaultEquinoxEmbedder].getDeclaredMethod("addBundlesDir", classOf[java.lang.StringBuilder], classOf[Array[File]], java.lang.Boolean.TYPE)
      method.setAccessible(true)
      method.invoke(this, bundles, files, Boolean.box(absolute))
    }
    protected def copyToTempFolderR(configDir: File): String = {
      val method = classOf[DefaultEquinoxEmbedder].getDeclaredMethod("copyToTempFolder", classOf[File])
      method.setAccessible(true)
      method.invoke(this, configDir).asInstanceOf[String]
    }
    override protected def doStart() {
      var installationLocations = new java.util.ArrayList[File]()
      var bundleLocations = new java.util.ArrayList[File]()
      var extraSystemPackages = new java.util.ArrayList[String]()
      var platformProperties = new java.util.LinkedHashMap[String, String]()

      equinoxLocatorR.locateRuntime(new EquinoxRuntimeDescription() {
        def addExtraSystemPackage(systemPackage: String) {
          if (systemPackage == null || systemPackage.length() == 0)
            throw new IllegalArgumentException()
          extraSystemPackages.add(systemPackage)
        }

        def addPlatformProperty(property: String, value: String) {
          if (property == null || property.length() == 0)
            throw new IllegalArgumentException()
          platformProperties.put(property, value);
        }

        def addInstallation(location: File) {
          if (location == null || !location.isDirectory() || !new File(location, "plugins").isDirectory())
            throw new IllegalArgumentException()
          if (!installationLocations.isEmpty())
            // allow only one installation for now
            throw new IllegalStateException()
          installationLocations.add(location)
        }

        def addBundle(location: File) {
          if (location == null || !location.exists())
            throw new IllegalArgumentException()
          if (!isFrameworkBundle(location))
            bundleLocations.add(location)
        }

        def addBundleStartLevel(id: String, level: Int, autostart: Boolean) {
          // TODO do we need to autostart?
        }
      })

      if (installationLocations.isEmpty() && !platformProperties.containsKey("osgi.install.area"))
        throw new RuntimeException("Equinox runtime location is missing or invalid")

      System.setProperty("osgi.framework.useSystemProperties", "false") //$NON-NLS-1$ //$NON-NLS-2$

      val bundles = new java.lang.StringBuilder()

      if (!installationLocations.isEmpty()) {
        val frameworkDir = installationLocations.get(0)
        val frameworkLocation = frameworkDir.getAbsolutePath()

        platformProperties.put("osgi.install.area", frameworkLocation)
        platformProperties.put("osgi.syspath", frameworkLocation + "/plugins")
        platformProperties.put("osgi.configuration.area", copyToTempFolderR(new File(frameworkDir, "configuration")))

        addBundlesDirR(bundles, new File(frameworkDir, "plugins").listFiles(), false)
      }

      for (location ← bundleLocations) {
        if (bundles.length() > 0)
          bundles.append(',')
        bundles.append(getReferenceUrlR(location))
      }
      platformProperties.put("osgi.bundles", bundles.toString())

      // This tells framework to use our classloader as parent, so it can see classes that we see
      platformProperties.put("osgi.parentClassloader", "fwk")

      if (extraSystemPackages.size() > 0) {
        val sb = new StringBuilder()
        for (pkg ← extraSystemPackages) {
          if (sb.length() > 0)
            sb.append(',')
          sb.append(pkg)
        }
        // Make the system bundle export the given packages and load them from the parent class loader
        platformProperties.put("org.osgi.framework.system.packages.extra", sb.toString())
      }

      // Debug
      if (Plugin.debug.nonEmpty) {
        platformProperties.put("osgi.console.enable.builtin", "true")
        platformProperties.put("osgi.console", Plugin.debug.get.toString)
        platformProperties.put("osgi.debug", "")
        platformProperties.put("eclipse.consoleLog", "true")
      }

      platformProperties.put("org.osgi.framework.bootdelegation", bootClasses.mkString(","))

      // TODO switch to org.eclipse.osgi.launch.Equinox
      // EclipseStarter is not helping here

      EclipseStarter.setInitialProperties(platformProperties)

      val args = (getNonFrameworkArgsR().filterNot(arg ⇒ arg == "-console" || arg == "-consoleLog" || arg == "-debug")).distinct
      EclipseStarter.startup(args, null)

      frameworkContextR = EclipseStarter.getSystemBundleContext()
      activateBundlesInWorkingOrder()

      for (listener ← lifecycleListenersR.values())
        listener.afterFrameworkStarted(this)
    }
    protected def equinoxLocatorR: EquinoxRuntimeLocator = {
      val field = classOf[DefaultEquinoxEmbedder].getDeclaredField("equinoxLocator")
      field.setAccessible(true)
      Option(field.get(this)) match {
        case Some(locator) ⇒ locator.asInstanceOf[EquinoxRuntimeLocator]
        case None ⇒
          val locator = maven.lookup(classOf[EquinoxRuntimeLocator])
          field.set(this, locator)
          locator
      }
    }
    protected def frameworkContextR: BundleContext = {
      val field = classOf[DefaultEquinoxEmbedder].getDeclaredField("frameworkContext")
      field.setAccessible(true)
      field.get(this).asInstanceOf[BundleContext]
    }
    protected def frameworkContextR_=(arg: BundleContext) = {
      val field = classOf[DefaultEquinoxEmbedder].getDeclaredField("frameworkContext")
      field.setAccessible(true)
      field.set(this, arg)
    }
    override protected def getLogger() = maven.plexus.getLogger()
    protected def getNonFrameworkArgsR(): Array[String] = {
      val method = classOf[DefaultEquinoxEmbedder].getDeclaredMethod("getNonFrameworkArgs")
      method.setAccessible(true)
      method.invoke(this).asInstanceOf[Array[String]]
    }
    protected def getReferenceUrlR(file: File): String = {
      val method = classOf[DefaultEquinoxEmbedder].getDeclaredMethod("getReferenceUrl", classOf[File])
      method.setAccessible(true)
      method.invoke(this, file).asInstanceOf[String]
    }
    protected def lifecycleListenersR: java.util.Map[String, EquinoxLifecycleListener] = {
      val field = classOf[DefaultEquinoxEmbedder].getDeclaredField("lifecycleListeners")
      field.setAccessible(true)
      Option(field.get(this)) match {
        case Some(listeners) ⇒ listeners.asInstanceOf[java.util.Map[String, EquinoxLifecycleListener]]
        case None ⇒
          val listeners = maven.lookupMap(classOf[EquinoxLifecycleListener])
          field.set(this, listeners)
          listeners
      }
    }
    protected def activateBundlesInWorkingOrder() {
      // our custom addition: in the original code IStatus lookup trigger
      // org.eclipse.equinox.common start, but now IStatus is inlined
      tryActivateBundle("org.eclipse.equinox.common")
      // activate bundles which need to do work in their respective activator; stick to a working order (cf. bug 359787)
      // TODO this order should come from the EquinoxRuntimeLocator
      tryActivateBundle("org.eclipse.equinox.ds")
      tryActivateBundle("org.eclipse.equinox.registry")
      tryActivateBundle("org.eclipse.core.net")
    }
    protected def tryActivateBundle(symbolicName: String) =
      for (bundle ← frameworkContextR.getBundles()) {
        if (symbolicName.equals(bundle.getSymbolicName())) try {
          // don't have OSGi remember the autostart setting; want to start these bundles manually to control the start order
          bundle.start(Bundle.START_TRANSIENT)
        } catch {
          case e: BundleException ⇒
            getLogger().warn("Could not start bundle " + bundle.getSymbolicName(), e)
        }
      }
  }
}
