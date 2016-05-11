/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2013-2014 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.osgi.manager

import java.util.{ Date, Properties }
import sbt.osgi.manager.Keys._
import sbt.osgi.manager.Support.logPrefix
import sbt.osgi.manager.bnd.Bnd
import scala.collection.immutable

import sbt.Keys._
import sbt._
import sbt.std.TaskStreams

object Plugin {
  // Please, use SBT logLevel and .options file if needed
  /** Flag that affects initialization parameters of OSGi framework/TCP port for equinox console */
  @volatile var debug: Option[Int] = None
  // This is not an ugly hairy hack. This is the decision that is based on the "simple" SBT architecture ;-)
  // SBT have no global state registry, no publish/subscribe engine, no callbacks, no events, no hooks (most of code is 'final' classes)
  // So how do we may to provide an actual mutable "something" (like log handler that may be recreated at any moment) for singleton
  // that is instantiated from the OSGi infrastructure? lol
  /** Instance of the last known State */
  @volatile private var lastKnownState: Option[TaskArgument] = None

  /** Entry point for plugin in user's project */
  lazy val defaultSettings =
    // base settings
    test.Test.settings ++
      inConfig(Keys.OSGiConf)(Seq(
        managedClasspath := Classpaths.managedJars(osgiModuleScope.value, classpathTypes.value, update.value),
        osgiDirectory <<= (target) { _ / "osgi" },
        osgiFetchPath := None,
        osgiModuleScope := OSGiConf)) ++
      // plugin settings
      bnd.Bnd.settings ++
      maven.Maven.settings ++
      // and all tasks & commands
      inConfig(Keys.OSGiConf)(Seq(
        osgiBndPrepareHome <<= Plugin.prepareBndHomeTask,
        osgiMavenPrepareHome <<= Plugin.prepareMavenHomeTask,
        osgiPluginInfo <<= osgiPluginInfoTask,
        osgiResetCache := osgiResetCacheTask)) ++
      // and global settings
      Seq(
        commands += Command.command("osgiResolve", osgiResolveCommandHelp)(osgiResolveCommand(false, _)),
        commands += Command.command("osgiResolveRemote", osgiResolveRemoteCommandHelp)(osgiResolveCommand(true, _)),
        managedClasspath in Compile <++= managedClasspath in OSGiConf,
        managedClasspath in Runtime <++= managedClasspath in OSGiConf,
        managedClasspath in sbt.Test <++= managedClasspath in OSGiConf,
        osgiCompile <<= osgiCompileTask,
        osgiFetch <<= osgiFetchTask,
        osgiShow <<= osgiShowTask,
        ivyConfigurations ++= Seq(OSGiConf, OSGiTestConf))

  /** Returns last known State. It is a complex helper for Simple Build Tool simple architecture. lol */
  def getLastKnownState(): Option[TaskArgument] = lastKnownState
  // Mark Harrah mark sbt.Resolver as sealed and all other classes as final ;-) It is so funny.
  // We will support him in his beginning.
  // Let's eat a shit that we have.
  /** Mark OSGi dependency as OBR */
  def markDependencyAsOBR(moduleId: ModuleID): ModuleID = moduleId.extra((Dependency.OBR.key, Dependency.OBR.name))
  /** Mark OSGi dependency as P2 */
  def markDependencyAsP2(moduleId: ModuleID): ModuleID = moduleId.extra((Dependency.P2.key, Dependency.P2.name))
  /** Mark OSGi resolver as P2 */
  def markResolverAsOBR(resolver: Resolver): Resolver = resolver match {
    case mavenResolver: MavenRepository ⇒
      import mavenResolver._
      new URLRepository(name, Patterns(Seq[String](root), Seq[String](Dependency.OBR.name), false))
    case unsupported ⇒
      throw new UnsupportedOperationException("Unknown resolver type %s for %s".format(resolver.getClass(), resolver))
  }
  /** Mark OSGi resolver as P2 */
  def markResolverAsP2(resolver: Resolver): Resolver = resolver match {
    case mavenResolver: MavenRepository ⇒
      import mavenResolver._
      new URLRepository(name, Patterns(Seq[String](root), Seq[String](Dependency.P2.name), false))
    case unsupported ⇒
      throw new UnsupportedOperationException("Unknown resolver type %s for %s".format(resolver.getClass(), resolver))
  }
  /** Generate bundle manifest for compiled code. */
  def osgiCompileTask = (dependencyClasspath in Compile, packageOptions in (Compile, packageBin), products in Compile, state, streams, thisProjectRef) map {
    (dependencyClasspath, packageOptions, products, state, streams, thisProjectRef) ⇒
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      val manifest = bnd.action.GenerateManifest.generateManifestTask(dependencyClasspath, packageOptions, products)
      products.foreach { p ⇒
        val manifestPath = p / "META-INF"
        if (!manifestPath.exists())
          manifestPath.mkdirs()
        val manifestFile = manifestPath / "MANIFEST.MF"
        streams.log.debug(logPrefix(arg.name) + "Write manifest to " + manifestFile.getAbsolutePath())
        Using.fileOutputStream()(manifestFile) { outputStream ⇒ manifest.write(outputStream) }
      }
  }
  /** Fetch all project dependencies as bundles */
  def osgiFetchTask = (dependencyClasspath in Compile, osgiFetchPath in Keys.OSGiConf, state, streams, thisProjectRef) map {
    (dependencyClasspath, osgiFetchPath, state, streams, thisProjectRef) ⇒
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      osgiFetchPath match {
        case Some(osgiFetchPath) ⇒
          bnd.action.Fetch.fetchTask(osgiFetchPath, dependencyClasspath.map(cp ⇒
            bnd.action.Fetch.Item(cp.get(moduleID.key), cp.data)).toSet)
        case None ⇒
          streams.log.info(logPrefix(arg.name) + "Fetch task disabled.")
      }
      () // Project/Def.Initialize[Task[Unit]]
  }
  /** Show plugin information */
  def osgiPluginInfoTask = (state, streams, thisProjectRef) map { (state, streams, thisProjectRef) ⇒
    implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
    Option(getClass().getClassLoader().getResourceAsStream("version-sbt-osgi-manager.properties")) match {
      case Some(stream) ⇒
        val properties = new Properties()
        properties.load(stream)
        val date = new Date(properties.getProperty("build").toLong * 1000)
        streams.log.info(logPrefix(arg.name) + "Name: " + properties.getProperty("name"))
        streams.log.info(logPrefix(arg.name) + "Version: " + properties.getProperty("version"))
        streams.log.info(logPrefix(arg.name) + "Build: " + date + " (" + properties.getProperty("build") + ")")
      case None ⇒
        streams.log.error(logPrefix(arg.name) + "OSGi Mananger plugin information not found.")
    }
  }
  /** Command that populates libraryDependencies with required bundles */
  def osgiResolveCommand(resolveAsRemoteArtifacts: Boolean, state: State): State = {
    val extracted = Project.extract(state)
    val uri = extracted.currentRef.build
    val build = extracted.structure.units(uri)
    var actualState: State = state
    val ivySbtSeq = for (id ← build.defined.keys) yield {
      implicit val projectRef = ProjectRef(uri, id)
      // This selects the 'osgi-maven-prepare' task for the current project.
      // The value produced by 'osgi-maven-prepare' is of type File
      val taskMavenPrepareHomeKey = osgiMavenPrepareHome in Compile in OSGiConf
      EvaluateTask(extracted.structure, taskMavenPrepareHomeKey, actualState, projectRef) match {
        case Some((state, result)) ⇒
          actualState = state
        case None ⇒
          throw new OSGiManagerException("Unable to prepare Maven home for project %s.".format(projectRef.project))
      }
      // This selects the 'osgi-bnd-prepare' task for the current project.
      // The value produced by 'osgi-bnd-prepare' is of type File
      val taskBndPrepareHomeKey = osgiBndPrepareHome in Compile in OSGiConf
      EvaluateTask(extracted.structure, taskBndPrepareHomeKey, actualState, projectRef) match {
        case Some((state, result)) ⇒
          actualState = state
        case None ⇒
          throw new OSGiManagerException("Unable to prepare Bnd home for project %s.".format(projectRef.project))
      }
      // Get ivySbt
      EvaluateTask(extracted.structure, ivySbt in Compile, actualState, projectRef) match {
        case Some((state, result)) ⇒
          result.toEither match {
            case Left(incomplete) ⇒
              throw new OSGiManagerException("Unable to get IvySbt for project %s.".format(projectRef.project))
            case Right(ivySbt) ⇒
              actualState = state
              ivySbt
          }
        case None ⇒
          throw new OSGiManagerException("Unable to get IvySbt for project %s.".format(projectRef.project))
      }
    }
    implicit val arg = TaskArgument(actualState, Project.current(actualState), None)
    // resolve P2
    val dependencyP2 = maven.action.Resolve.resolveP2Command(ivySbtSeq.head, resolveAsRemoteArtifacts)
    val dependencySettingsP2 =
      for (projectRef ← dependencyP2.keys)
        yield if (dependencyP2(projectRef).nonEmpty) Seq(libraryDependencies in projectRef ++= dependencyP2(projectRef)) else Seq()
    // resolve OBR
    val resolvedDependencies = collectResolvedDependencies(dependencyP2)
    val dependencyOBR = bnd.action.Resolve.resolveOBRCommand(resolvedDependencies)
    val dependencySettingsOBR = for (projectRef ← dependencyOBR.keys) yield if (dependencyOBR(projectRef).nonEmpty)
      Seq(libraryDependencies in projectRef ++= dependencyOBR(projectRef))
    else
      Seq()
    val dependencySettings = dependencySettingsP2.flatten ++ dependencySettingsOBR.flatten
    arg.log.debug(logPrefix("*") + "Add  settings: " + dependencySettings)
    if (dependencySettings.nonEmpty) {
      arg.log.info(logPrefix(arg.name) + "Update library dependencies")
      val newStructure = {
        import arg.extracted._
        val append = Load.transformSettings(Load.projectScope(currentRef), currentRef.build, rootProject, dependencySettings.toSeq)
        Load.reapply(session.original ++ append, structure)
      }
      Project.setProject(arg.extracted.session, newStructure, actualState)
    } else
      actualState
  }
  /** Reset all plugin caches */
  def osgiResetCacheTask = (state, streams, thisProjectRef) map { (state, streams, thisProjectRef) ⇒
    implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
    // It is only one now
    maven.action.Resolve.resetCache()
  }
  /** Generate help for osgiResolveCommand */
  def osgiResolveCommandHelp = {
    val osgiResolveCommand = "osgiResolve"
    val osgiResolveCommandBrief = (osgiResolveCommand, "Add OSGi dependencies that points to local artifacts.")
    val osgiResolveCommandDetailed = "Add OSGi dependencies to libraryDependencies setting per user project."
    Help(osgiResolveCommand, osgiResolveCommandBrief, osgiResolveCommandDetailed)
  }
  /** Generate help for osgiResolveCommand */
  def osgiResolveRemoteCommandHelp = {
    val osgiResolveCommand = "osgiResolveRemote"
    val osgiResolveCommandBrief = (osgiResolveCommand, "Add OSGi dependencies that points to remote artifacts.")
    val osgiResolveCommandDetailed = "Add OSGi dependencies to libraryDependencies setting per user project."
    Help(osgiResolveCommand, osgiResolveCommandBrief, osgiResolveCommandDetailed)
  }
  def osgiShowTask = (state, streams, thisProjectRef) map { (state, streams, thisProjectRef) ⇒
    implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
    Bnd.show()
    () // Project/Def.Initialize[Task[Unit]]
  }
  def packageOptionsTask =
    (dependencyClasspath in Compile, state, streams, thisProjectRef, packageOptions in (Compile, packageBin), products in Compile) map {
      (dependencyClasspath, state, streams, thisProjectRef, packageOptions, products) ⇒
        implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
        bnd.action.GenerateManifest.generatePackageOptionsTask(dependencyClasspath, packageOptions, products)
    }
  /** Prepare Bnd home directory. Returns the home location. */
  def prepareBndHomeTask =
    (state, streams, thisProjectRef) map { (state, streams, thisProjectRef) ⇒
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      bnd.Bnd.prepareHome()
    }
  /** Prepare Maven home directory. Returns the home location. */
  def prepareMavenHomeTask =
    (state, streams, thisProjectRef) map { (state, streams, thisProjectRef) ⇒
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      maven.Maven.prepareHome()
    }
  /** Collects resolved artifacts per project */
  protected def collectResolvedDependencies(resolvedDependencies: immutable.HashMap[ProjectRef, Seq[ModuleID]])(implicit arg: Plugin.TaskArgument): immutable.HashMap[ProjectRef, Seq[File]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    val result = for (id ← build.defined.keys) yield {
      implicit val projectRef = ProjectRef(uri, id)
      val scope = arg.thisScope.copy(project = Select(projectRef))
      val taskExternalDependencyClasspath = externalDependencyClasspath in scope in Compile
      val localArg = arg.copy(thisProjectRef = projectRef)
      arg.log.debug(logPrefix(localArg.name) + "Collect external-dependency-classpath")
      val projectDependencies = Project.runTask(taskExternalDependencyClasspath, localArg.state) match {
        case None ⇒
          None // Key wasn't defined.
        case Some((state, Inc(inc))) ⇒
          Incomplete.show(inc.tpe); None // Error detail, inc is of type Incomplete
        case Some((state, Value(classpath))) ⇒
          Some(classpath)
      }
      val additionalDependencies = resolvedDependencies.get(projectRef) getOrElse Seq() map { moduleId ⇒
        moduleId.explicitArtifacts.flatMap { artifact ⇒
          if (artifact.classifier == None || artifact.classifier == Some(""))
            artifact.url.flatMap(url ⇒ if (url.getProtocol() == "file") Some(new File(url.toURI())) else None)
          else
            None
        }
      }
      (projectRef, ((projectDependencies.map(_.map(_.data)) getOrElse Seq()) ++ additionalDependencies.flatten).distinct)
    }
    immutable.HashMap(result.toSeq: _*)
  }

  /** Consolidated argument with all required information */
  case class TaskArgument(
      /** The data structure representing all command execution information. */
      state: State,
      // It is more reasonable to pass it from SBT than of fetch it directly.
      /** The reference to the current project. */
      thisProjectRef: ProjectRef,
      /** The structure that contains reference to log facilities. */
      streams: Option[TaskStreams[ScopedKey[_]]] = None) {
    /** Extracted state projection */
    lazy val extracted = Project.extract(state)
    /** SBT logger */
    val log = streams.map(_.log) getOrElse {
      // Heh, another feature not bug? SBT 0.12.3
      // MultiLogger and project level is debug, but ConsoleLogger is still info...
      // Don't care about CPU time
      val globalLoggin = _root_.sbt.osgi.manager.patch.Patch.getGlobalLogging(state)
      import globalLoggin._
      full match {
        case logger: AbstractLogger ⇒
          val level = logLevel in thisScope get extracted.structure.data
          level.foreach(logger.setLevel(_)) // force level
          logger
        case logger ⇒
          logger
      }
    }
    /** Current project name */
    val name: String = (_root_.sbt.Keys.name in thisScope get extracted.structure.data) getOrElse thisProjectRef.project.toString()
    /** Scope of current project */
    lazy val thisScope = Load.projectScope(thisProjectRef)
    /** Scope of current project withing plugin configuration */
    lazy val thisOSGiScope = thisScope.copy(config = Select(OSGiConf))

    updateLastKnownState()

    /** Update last known state */
    def updateLastKnownState() = synchronized {
      if (!lastKnownState.exists(_.eq(this)))
        lastKnownState = Some(this)
    }
  }
}
