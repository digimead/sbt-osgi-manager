/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.osgi.manager.maven.action

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.Properties
import java.util.jar.JarOutputStream
import java.util.jar.Pack200

import scala.Option.option2Iterable
import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.collection.mutable

import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.model.{ Dependency => MavenDependency }
import org.apache.maven.repository.RepositorySystem
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.equinox.p2.core.ProvisionException
import org.eclipse.equinox.p2.metadata.IInstallableUnit
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub
import org.eclipse.tycho.core.facade.TargetEnvironment
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult
import sbt._
import sbt.Keys._
import sbt.osgi.manager.Dependency
import sbt.osgi.manager.Dependency._
import sbt.osgi.manager.OSGiManagerException
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support
import sbt.osgi.manager.Support._
import sbt.osgi.manager.maven.Maven

// Unfortunately:
//   - from the one side SBT is too simple for handle mixed ModuleID with explicit artifacts,
//     where one of them points to already downloaded file:// and other points to http:// (we will lost http:// after ivy lookup)
//   - from the other side Tycho ALWAYS download artifacts at 'resolve' (at least 0.17). This is hard coded.
// So we have to two piece of shit from different team of developers and there is no way for sunny day.
// And I think only about my targets and my needs as anyone other do.
//   Ezh
//
// What? Submit a bug report? I had a lot already. Fuck you.
//
// Dear unusual individual, that read this funny stuff,
//   Please, submit the bug report yourself or just modify this code.
//   Your patch will be accepted. You are welcome.

/** Contain resolve action for Maven and P2 repository */
object Resolve extends Support.Resolve {
  /** Instance of Pack200, specified in JSR 200, is an HTTP compression method by Sun for faster JAR file transfer speeds over the network. */
  val unpacker = Pack200.newUnpacker()

  /** Resolve the dependency against the standard Maven repository */
  def resolveBasic(maven: Maven)(implicit arg: Plugin.TaskArgument) {
    val groupId = "org.apache.maven"
    val artifactId = "maven-core"
    val version = "3.0"
    val repositorySystem = maven.lookup(classOf[RepositorySystem])
    val repo = repositorySystem.createDefaultRemoteRepository()
    val dependency = new MavenDependency()
    dependency.setGroupId(groupId)
    dependency.setArtifactId(artifactId)
    dependency.setVersion(version)
    dependency.setScope(Artifact.SCOPE_COMPILE)
    val artifact = repositorySystem.createDependencyArtifact(dependency)
    val request = new ArtifactResolutionRequest().
      setArtifact(artifact).
      setResolveRoot(true).
      setResolveTransitively(true).
      setRemoteRepositories(List(repo)).
      setLocalRepository(maven.session.getLocalRepository()).
      setListeners(null) // reset an empty list to null for org.apache.maven.artifact.resolver.DefaultArtifactResolver.resolve

    //if (scope != null) {
    //  io.debug("Using scope: {}", scope);
    //  request.setCollectionFilter(new ScopeArtifactFilter(scope));
    //}

    for (rr <- request.getRemoteRepositories())
      arg.log.debug(logPrefix(arg.name) + "Add remote repository:\n" + rr)
    arg.log.info(logPrefix(arg.name) + "Resolve artifact: " + artifact)
    val result = repositorySystem.resolve(request)
    for (exception <- result.getErrorArtifactExceptions())
      arg.log.error(exception.toString())
    for (exception <- result.getCircularDependencyExceptions())
      arg.log.error(exception.toString())
    if (result.getMissingArtifacts().nonEmpty)
      arg.log.warn("Unable to locate " + result.getMissingArtifacts().mkString(","))

    val artifacts = result.getArtifacts()
    arg.log.info(logPrefix(arg.name) + "Resolved artifacts:\n" + result)

    //    val origin = maven.session.getCurrentProject()
    //            val model = origin.getModel().clone();
    /*      val build = model.getBuild()
         val tpmp = new Plugin()
         tpmp.setGroupId()
         tpmp.setArtifactId(artifactId)
         tpmp.setVersion(version)

         build.getPlugins().add(tpmp)
         build.flushPluginMap()*/

    //  System.err.println("!!!" + result)
    //    repositorySystem.createArtifactRepository(id, url, repositoryLayout, snapshots, releases)(repository)
    //val repo = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/")
    //repositorySystem.resolve()
    //    val request = new ArtifactRequest()
    //    request.setArtifact(new DefaultArtifact(":maven-model:3.0"))
    //    request.setRepositories(List(repo))
    /*getLog().info("Resolving artifact " + artifact +
      " from " + remoteRepos);

    ArtifactResult result;*/

    /*getLog().info("Resolved artifact " + artifact + " to " +
      result.getArtifact().getFile() + " from "
      + result.getRepository());*/
  }
  /** Resolve the dependency for the specific project against Eclipse P2 repository */
  def resolveP2()(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Seq[ModuleID] = {
    val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
    // get resolvers as Seq[(id, url)]
    val resolvers = getResolvers(Dependency.P2, scope)
    val dependencies = getDependencies(Dependency.P2, scope)
    if (resolvers.nonEmpty && dependencies.nonEmpty) {
      arg.log.info(logPrefix(name) + "Resolve P2 dependencies")
      val bridge = Maven()
      val modules = resolveP2(dependencies, resolvers, bridge)
      updateCache(Support.CacheP2Key(projectRef.project), dependencies, resolvers)
      modules
    } else {
      arg.log.info(logPrefix(name) + "No P2 dependencies found")
      updateCache(Support.CacheP2Key(projectRef.project), Seq(), Seq())
      Seq()
    }
  }
  /** Resolve the dependency against Eclipse P2 repository */
  // For more information about metadata and artifact repository manager, look at
  // http://eclipsesource.com/blogs/tutorials/eclipse-p2-tutorial-managing-metadata/
  def resolveP2(dependencies: Seq[MavenDependency], p2Pepositories: Seq[(String, URI)],
    maven: Maven, includeLocalMavenRepo: Boolean = true)(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Seq[ModuleID] = {
    val provisioningAgentInterface = maven.equinox.getClass.getClassLoader.loadClass("org.eclipse.equinox.p2.core.IProvisioningAgent")
    val remoteAgent = maven.equinox.getService(provisioningAgentInterface).asInstanceOf[{ def getService(serviceName: String): AnyRef }]
    val remoteArtifactRepositoryManager = remoteAgent.getService(IArtifactRepositoryManager.SERVICE_NAME).asInstanceOf[IArtifactRepositoryManager]

    // actually targetPlatformBuilder is a resolution context
    val targetPlatformBuilder = maven.p2ResolverFactory.createTargetPlatformBuilder(
      new ExecutionEnvironmentConfigurationStub("JavaSE-1.6"))
    targetPlatformBuilder.setIncludeLocalMavenRepo(includeLocalMavenRepo)
    val loadedPepositories = {
      for (repo <- p2Pepositories) yield {
        val id = repo._1
        val location = repo._2
        try {
          targetPlatformBuilder.addP2Repository(new MavenRepositoryLocation(id, location))
          Some(location)
        } catch {
          case e: URISyntaxException =>
            arg.log.warn(logPrefix(name) + "Unable to resolve repository URI : " + location)
            None
          case e: ProvisionException =>
            arg.log.warn(logPrefix(name) + e.getMessage())
            None
          case e: Throwable =>
            arg.log.warn(logPrefix(name) + e.getMessage())
            None
        }
      }
    }.flatten
    if (loadedPepositories.isEmpty) {
      arg.log.info(logPrefix(name) + "There are no any usable repositories")
      return Seq()
    }
    val targetPlatform = targetPlatformBuilder.buildTargetPlatform()
    val resolver = maven.p2ResolverFactory.createResolver(new MavenLoggerAdapter(maven.plexus.getLogger, true))
    dependencies.foreach(d => resolver.addDependency(d.getType(), d.getArtifactId(), d.getVersion()))

    val resolutionResult = resolver.resolveDependencies(targetPlatform, null) // set reactor project location to null
    val nonReactorUnits = (for (r <- resolutionResult) yield r.getNonReactorUnits().toArray()).flatten
    val artifacts = (for (r <- resolutionResult) yield r.getArtifacts()).flatten
    if (nonReactorUnits.isEmpty && artifacts.isEmpty) {
      arg.log.info(logPrefix(name) + "There are no any resolved entries")
      return Seq()
    }

    if (artifacts.isEmpty)
      return Seq() // nothing found

    val actualRepositories = loadedPepositories.map(uri => Option(remoteArtifactRepositoryManager.loadRepository(uri, null))).flatten // set monitors to null

    val rePerDependencyMap = collectArtifactsPerDependency(dependencies, artifacts)
    // associate results with repository records
    // nonReactorUnits is most probably a p2 internal InstallableUnit (type not accessible from Tycho).
    nonReactorUnits.foreach(_ match {
      case nriu: IInstallableUnit =>
        // check if IInstallableUnit not exists in artifacts
        if (!artifacts.exists(_.getInstallableUnits().exists(_ == nriu)))
          arg.log.info(logPrefix(name) + "Skip non reactor installable unit: " + nriu)
      case nru =>
        arg.log.info(logPrefix(name) + "Skip non reactor unit: " + nru)
    })

    // FYI:
    //   single P2ResolutionResult.Entry may have any number of InstallableUnits that point to the same artifact
    //   single InstallableUnit may be bound to multiple originModuleIds. Some of ModuleIDs may require source code, some not
    artifacts.map { entry =>
      val originModuleIds = rePerDependencyMap.get(entry).map(dependencies => dependencies.flatMap(getOrigin)) getOrElse Seq()
      entry.getInstallableUnits().map(_ match {
        case riu: IInstallableUnit =>
          if (originModuleIds.nonEmpty) {
            if (originModuleIds.exists(_.withSources)) {
              val sources = actualRepositories.map(aquireP2SourceCodeArtifacts(entry, riu, _)).flatten.distinct
              if (sources.isEmpty) {
                arg.log.info(logPrefix(name) + "Collect P2 IU %s".format(riu))
                arg.log.warn(logPrefix(name) + "Unable to find source code for " + riu)
                arg.log.debug("%s -> [%s]".format(riu, originModuleIds.map(_.moduleId.copy(extraAttributes = Map())).mkString(",")))
                Some(riu.getId() % entry.getId() % riu.getVersion().getOriginal()
                  from entry.getLocation.getAbsoluteFile.toURI.toURL.toString)
              } else {
                arg.log.info(logPrefix(name) + "Collect P2 IU %s with source code".format(riu))
                arg.log.debug("%s -> [%s]".format(riu, originModuleIds.map(_.moduleId.copy(extraAttributes = Map())).mkString(",")))
                val moduleId = riu.getId() % entry.getId() % riu.getVersion().getOriginal() from
                  entry.getLocation.getAbsoluteFile.toURI.toURL.toString
                val artifactsWithSourceCode = sources.map(file =>
                  sbt.Artifact.classified(moduleId.name, sbt.Artifact.SourceClassifier).copy(url = Some(file.toURI.toURL())))
                val moduleIdWithSourceCode = moduleId.copy(explicitArtifacts = moduleId.explicitArtifacts ++ artifactsWithSourceCode)
                Some(moduleIdWithSourceCode)
              }
            } else {
              arg.log.info(logPrefix(name) + "Collect P2 IU %s".format(riu))
              arg.log.debug("%s -> [%s]".format(riu, originModuleIds.map(_.moduleId.copy(extraAttributes = Map())).mkString(",")))
              Some(riu.getId() % entry.getId() % riu.getVersion().getOriginal()
                from entry.getLocation.getAbsoluteFile.toURI.toURL.toString)
            }
          } else {
            arg.log.warn(logPrefix(name) + "Collect an unbound installable unit: " + riu)
            Some(riu.getId() % entry.getId() % riu.getVersion().getOriginal()
              from entry.getLocation.getAbsoluteFile.toURI.toURL.toString)
          }
        case ru =>
          arg.log.warn(logPrefix(name) + "Skip an unknown reactor unit: " + ru)
          None
      }).flatten
    }.flatten
  }
  /** Command that populates libraryDependencies with required bundles */
  def resolveP2Command()(implicit arg: Plugin.TaskArgument): immutable.HashMap[ProjectRef, Seq[ModuleID]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    // Check if we already processed our dependencies with same values
    val cached = for (id <- build.defined.keys) yield {
      implicit val projectRef = ProjectRef(uri, id)
      val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
      arg.log.debug(logPrefix(name) + "Check is settings cached.")
      isCached(Support.CacheP2Key(id), getDependencies(Dependency.P2, scope), getResolvers(Dependency.P2, scope))
    }
    if (cached.forall(_ == true)) {
      arg.log.info("Pass P2 resolution: already resolved")
      immutable.HashMap((for (id <- build.defined.keys) yield {
        val projectRef = ProjectRef(uri, id)
        (projectRef, Seq())
      }).toSeq: _*)
    } else {
      immutable.HashMap((for (id <- build.defined.keys) yield {
        implicit val projectRef = ProjectRef(uri, id)
        (projectRef, resolveP2())
      }).toSeq: _*)
    }
  }

  /** Unpack packedAndGzipped to target */
  protected def aquireGzippedPack200Artifact(packedAndGzipped: File, target: File)(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Option[Throwable] = {
    val out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(target)))
    val tmpFile = java.io.File.createTempFile("sbt-osgi-manager-", "-pack")
    tmpFile.deleteOnExit()
    try {
      IO.gunzip(packedAndGzipped, tmpFile)
      unpacker.unpack(tmpFile, out)
      None
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(name) + "Unable to unpack artifact: " + e)
        Some(e)
    } finally {
      try { out.close() } catch { case _: Throwable => }
      try { tmpFile.delete() } catch { case _: Throwable => }
    }
  }
  /** Download source code for P2 installable unit */
  protected def aquireP2SourceCodeArtifacts(entry: P2ResolutionResult.Entry, iu: IInstallableUnit, repository: IArtifactRepository)(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Seq[java.io.File] = try {
    // Assume that we have SimpleArtifactRepository
    val clazz = repository.getClass()
    val methodGetDescriptors = clazz.getDeclaredMethod("getDescriptors")
    val methodGetRawArtifact = clazz.getDeclaredMethod("getRawArtifact", classOf[IArtifactDescriptor], classOf[OutputStream], classOf[IProgressMonitor])
    val allAvaiableDescriptors = methodGetDescriptors.invoke(repository).asInstanceOf[java.util.HashSet[IArtifactDescriptor]]
    // Get descriptors with source code
    val sourceCodeDescriptors = (iu.getArtifacts() map { artifact =>
      allAvaiableDescriptors.find { descriptor =>
        val key = descriptor.getArtifactKey()
        key.getClassifier() == artifact.getClassifier() &&
          key.getId() == artifact.getId() + ".source" &&
          key.getVersion() == artifact.getVersion()
      }
    }).flatten
    val directory = entry.getLocation().getParent()
    val monitor = new IProgressMonitor {
      @volatile var cancelled = false
      def beginTask(name: String, totalWork: Int) {}
      def done() {}
      def internalWorked(work: Double) {}
      def isCanceled(): Boolean = cancelled
      def setCanceled(value: Boolean) { cancelled = value }
      def setTaskName(name: String) {}
      def subTask(name: String) {}
      def worked(work: Int) {}
    }
    val files = sourceCodeDescriptors.map { descriptor =>
      val properties = descriptor.getProperties()
      val repository = descriptor.getRepository()
      val key = descriptor.getArtifactKey()
      val name = key.getId() + "_" + key.getVersion() + ".jar"
      val targetFile = new File(directory, name)
      if (!targetFile.exists()) {
        arg.log.debug(logPrefix(name) + "Fetching source code of '" + key + "' to " + targetFile)
        val status = if (descriptor.getProperty(IArtifactDescriptor.FORMAT) == IArtifactDescriptor.FORMAT_PACKED) {
          val tmpFile = java.io.File.createTempFile("sbt-osgi-manager-", "-pack.gz")
          tmpFile.deleteOnExit()
          val out = new BufferedOutputStream(new FileOutputStream(tmpFile))
          val downloadStatus = methodGetRawArtifact.invoke(repository, descriptor, out, monitor).asInstanceOf[IStatus]
          try { out.close() } catch { case _: Throwable => }
          val status = if (downloadStatus.getCode() == IStatus.OK)
            aquireGzippedPack200Artifact(tmpFile, targetFile) map (throwable =>
              new IStatus {
                def getChildren(): Array[org.eclipse.core.runtime.IStatus] = downloadStatus.getChildren()
                def getCode(): Int = IStatus.ERROR
                def getException() = throwable
                def getMessage() = throwable.getMessage()
                def getPlugin() = downloadStatus.getPlugin()
                def getSeverity(): Int = IStatus.ERROR
                def isMultiStatus() = downloadStatus.isMultiStatus()
                def isOK() = false
                def matches(severityMask: Int) = false // unused
              }) getOrElse downloadStatus
          else
            downloadStatus
          try { tmpFile.delete() } catch { case _: Throwable => }
          status
        } else {
          val out = new BufferedOutputStream(new FileOutputStream(targetFile))
          val status = methodGetRawArtifact.invoke(repository, descriptor, out, monitor).asInstanceOf[IStatus]
          try { out.close() } catch { case _: Throwable => }
          status
        }
        if (status.getCode() == IStatus.OK) {
          arg.log.info(logPrefix(name) + "Aquire source code artifact: " + key)
          Some(targetFile)
        } else {
          arg.log.warn(logPrefix(name) + "Unable to download source code artifact %s: %s".format(key, status))
          try { targetFile.delete() } catch { case _: Throwable => }
          None
        }
      } else {
        arg.log.debug(logPrefix(name) + "Get cached source code artifact " + key + " from " + targetFile)
        Some(targetFile)
      }
    }.flatten
    files.toSeq
  } catch {
    case e: Throwable =>
      arg.log.debug(logPrefix(name) + "Unable to get source code dependency: " + e)
      Seq()
  }
  /** Bind P2ResolutionResult.Entry(s) to SBT dependencies */
  protected def collectArtifactsPerDependency(dependencies: Seq[MavenDependency],
    artifacts: Seq[P2ResolutionResult.Entry])(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): immutable.HashMap[P2ResolutionResult.Entry, Seq[MavenDependency]] = {
    // map id -> dependency
    val dependencyMap = immutable.HashMap(dependencies.map(d => d.getArtifactId() -> d): _*)
    // map parent IU -requires-> Seq(child IU) which represents the IU requirements
    val iuRequirements = mutable.HashMap(artifacts.map(_.getInstallableUnits().map {
      case iu: IInstallableUnit =>
        Some(iu -> Seq[IInstallableUnit]())
      case iu =>
        arg.log.error(logPrefix(name) + "Unknown type %s of installable unit '%s'".format(iu.getClass(), iu))
        None
    }.flatten.toSeq).flatten: _*)
    // map child IU -used by-> Seq(parent IU)
    val iuContainers = mutable.HashMap[IInstallableUnit, Seq[IInstallableUnit]]()

    // populate iuRequirements
    iuRequirements.keys.foreach(iu =>
      iuRequirements(iu) = iu.getRequirements().map { requirement =>
        iuRequirements.keys.filter(requirement.isMatch)
      }.toSeq.flatten)

    // populate iuContainers
    iuRequirements.foreach {
      case (parent, children) =>
        children.foreach { child =>
          val saved = iuContainers.get(child) getOrElse Seq()
          if (!saved.contains(parent) && child != parent)
            iuContainers(child) = saved :+ parent
        }
    }

    @tailrec def getIUParents(iu: Seq[IInstallableUnit], acc: Seq[IInstallableUnit] = Seq()): Seq[IInstallableUnit] = {
      val parents = iu.flatMap(iuContainers.get).flatten
      val filtered = parents.filterNot(parent =>
        acc.exists(_.getId() == parent.getId()) || iu.exists(_.getId() == parent.getId()))
      if (filtered.isEmpty) return acc
      getIUParents(filtered, acc ++ filtered)
    }
    immutable.HashMap(artifacts.map { artifact =>
      val mavenDependencies = artifact.getInstallableUnits().map {
        case iu: IInstallableUnit =>
          Some(iu)
        case iu =>
          None // we already notify a user
      }.flatten.map { iu =>
        // search for all SBT dependencies against the IU
        dependencyMap.get(iu.getId()).toSeq ++ getIUParents(Seq(iu)).map(iu => dependencyMap.get(iu.getId())).flatten
      }.toSeq.flatten
      (artifact -> mavenDependencies)
    }: _*)
  }
  protected def getRunningEnvironment(): TargetEnvironment = {
    val properties = new Properties()
    properties.put(PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS(properties))
    properties.put(PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS(properties))
    properties.put(PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch(properties))

    new TargetEnvironment(properties.getProperty(PlatformPropertiesUtils.OSGI_OS),
      properties.getProperty(PlatformPropertiesUtils.OSGI_WS),
      properties.getProperty(PlatformPropertiesUtils.OSGI_ARCH))
  }
}
