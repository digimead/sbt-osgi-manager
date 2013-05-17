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

package sbt.osgi.manager

import java.util.jar.JarFile

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.Jar
import aQute.bnd.osgi.{ Constants => BndConstant }
import biz.aQute.resolve.internal.BndrunResolveContext
import org.apache.felix.resolver.ResolverImpl
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.eclipse.tycho.ArtifactKey
import sbt.Keys._
import sbt._
import sbt.osgi.manager.Dependency._
import sbt.osgi.manager.Keys._
import sbt.osgi.manager.Support.logPrefix
import sbt.osgi.manager.bnd.Bnd
import sbt.osgi.manager.maven.Maven
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
  @volatile private var lastKnownState: Option[State] = None

  /** Entry point for plugin in user's project */
  lazy val defaultSettings =
    // base settings
    inConfig(Keys.OSGiConf)(Seq(
      osgiDirectory <<= (target) { _ / "osgi" })) ++
      // plugin settings
      Bnd.settings ++
      Maven.settings ++
      // and all tasks & commands
      inConfig(Keys.OSGiConf)(Seq(
        osgiBndPrepareHome <<= Plugin.prepareBndHomeTask,
        osgiMavenPrepareHome <<= Plugin.prepareMavenHomeTask,
        osgiResetCache := osgiResetCacheTask)) ++
      // and global settings
      Seq(
        commands += Command.command("osgi-resolve", osgiResolveCommandHelp)(osgiResolveCommand),
        osgiShow <<= Plugin.osgiShowTask)

  /** Returns last known State. It is a complex helper for Simple Build Tool simple architecture. lol */
  def getLastKnownState(): Option[State] = lastKnownState
  // Mark Harrah mark sbt.Resolver as sealed and all other classes as final ;-) It is so funny.
  // We will support him in his beginning.
  // Let's eat a shit that we have.
  /** Mark OSGi dependency as OBR */
  def markDependencyAsOBR(moduleID: ModuleID): ModuleID = moduleID.extra((Dependency.OBR.key, Dependency.OBR.name))
  /** Mark OSGi dependency as P2 */
  def markDependencyAsP2(moduleID: ModuleID): ModuleID = moduleID.extra((Dependency.P2.key, Dependency.P2.name))
  /** Mark OSGi resolver as P2 */
  def markResolverAsOBR(resolver: Resolver): Resolver = resolver match {
    case mavenResolver: MavenRepository =>
      import mavenResolver._
      new URLRepository(name, new Patterns(Seq[String](root), Seq[String](Dependency.OBR.name), false))
    case unsupported =>
      throw new UnsupportedOperationException("Unknown resolver type %s for %s".format(resolver.getClass(), resolver))
  }
  /** Mark OSGi resolver as P2 */
  def markResolverAsP2(resolver: Resolver): Resolver = resolver match {
    case mavenResolver: MavenRepository =>
      import mavenResolver._
      new URLRepository(name, new Patterns(Seq[String](root), Seq[String](Dependency.P2.name), false))
    case unsupported =>
      throw new UnsupportedOperationException("Unknown resolver type %s for %s".format(resolver.getClass(), resolver))
  }

  /** Command that populates libraryDependencies with required bundles */
  def osgiResolveCommand(state: State): State = {
    implicit val arg = TaskArgument(state, None)
    // This selects the 'osgi-maven-prepare' task for the current project.
    // The value produced by 'osgi-maven-prepare' is of type File
    val taskMavenPrepareHomeKey = osgiMavenPrepareHome in Compile in OSGiConf
    // This selects the 'osgi-bnd-prepare' task for the current project.
    // The value produced by 'osgi-bnd-prepare' is of type File
    val taskBndPrepareHomeKey = osgiBndPrepareHome in Compile in OSGiConf
    // Evaluate the tasks
    // None if the key is not defined
    // Some(Inc) if the task does not complete successfully (Inc for incomplete)
    // Some(Value(v)) with the resulting value
    val mavenPrepareTaskResult = Project.runTask(taskMavenPrepareHomeKey, state) match {
      case None =>
        None // Key wasn't defined.
      case Some((state, Inc(inc))) =>
        Incomplete.show(inc.tpe); None // Error detail, inc is of type Incomplete
      case Some((state, Value(mavenHome))) =>
        Some(state)
    }
    val bndPrepareTaskResult = mavenPrepareTaskResult.flatMap(Project.runTask(taskBndPrepareHomeKey, _) match {
      case None =>
        None // Key wasn't defined.
      case Some((state, Inc(inc))) =>
        Incomplete.show(inc.tpe); None // Error detail, inc is of type Incomplete
      case Some((state, Value(bndHome))) =>
        Some(state)
    })
    bndPrepareTaskResult match {
      case Some(state) =>
        val dependencySettingsP2 = maven.action.Resolve.resolveP2Command()
        val dependencySettingsOBR = bnd.action.Resolve.resolveOBRCommand()
        val dependencySettings = dependencySettingsP2 ++ dependencySettingsOBR
        if (dependencySettings.nonEmpty) {
          arg.log.info(logPrefix(arg.name) + "Update library dependencies")
          val newStructure = {
            import arg.extracted._
            val append = Load.transformSettings(Load.projectScope(currentRef), currentRef.build, rootProject, dependencySettings)
            Load.reapply(session.original ++ append, structure)
          }
          Project.setProject(arg.extracted.session, newStructure, state)
        } else
          state
      case None =>
        state
    }
  }
  /** Reset all plugin caches */
  def osgiResetCacheTask {
    // It is only one now
    maven.action.Resolve.resetCache()
  }
  /** Generate help for osgiResolveCommand */
  def osgiResolveCommandHelp = {
    val osgiResolveCommand = "osgi-resolve"
    val osgiResolveCommandBrief = (osgiResolveCommand, "Add OSGi dependencies to projects.")
    val osgiResolveCommandDetailed = "Add OSGi dependencies to libraryDependencies setting per user project."
    Help(osgiResolveCommand, osgiResolveCommandBrief, osgiResolveCommandDetailed)
  }
  def osgiShowTask: Project.Initialize[Task[Unit]] = (name, thisProjectRef, state, streams) map { (name, thisProjectRef, state, streams) =>
    implicit val arg = TaskArgument(state, Some(streams))
    Bnd.show()
  }
  def packageOptionsTask: Project.Initialize[Task[Seq[PackageOption]]] =
    (dependencyClasspath in Compile, state, streams, packageOptions in (Compile, packageBin), products in Compile) map {
      (dependencyClasspath, state, streams, packageOptions, products) =>
        implicit val arg = TaskArgument(state, Some(streams))
        bnd.action.GenerateManifest.generateTask(dependencyClasspath, packageOptions, products)
    }
  /** Prepare Bnd home directory. Returns the home location. */
  def prepareBndHomeTask: Project.Initialize[Task[File]] =
    (state, streams) map { (state, streams) =>
      implicit val arg = TaskArgument(state, Some(streams))
      bnd.Bnd.prepareHome()
    }
  /** Prepare Maven home directory. Returns the home location. */
  def prepareMavenHomeTask: Project.Initialize[Task[File]] =
    (state, streams) map { (state, streams) =>
      implicit val arg = TaskArgument(state, Some(streams))
      maven.Maven.prepareHome()
    }

  /** Consolidated argument with all required information */
  case class TaskArgument(
    /** Data structure representing all command execution information. */
    state: State,
    streams: Option[TaskStreams[ScopedKey[_]]] = None) {
    /** Extracted state projection */
    lazy val extracted = Project.extract(state)
    /** SBT logger */
    val log = streams.map(_.log) getOrElse {
      // Heh, another feature not bug?
      // MultiLogger and project level is debug, but ConsoleLogger is still info...
      // Don't care about CPU time
      state.globalLogging.full match {
        case logger: AbstractLogger =>
          val level = logLevel in thisScope get extracted.structure.data
          level.foreach(logger.setLevel(_)) // force level
          logger
        case logger =>
          logger
      }
    }
    /** Current project name */
    val name: String = (sbt.Keys.name in thisScope get extracted.structure.data) getOrElse thisProjectRef.project
    /** Reference to current current project */
    lazy val thisProjectRef = Project.current(state)
    /** Scope of current project */
    lazy val thisScope = Load.projectScope(thisProjectRef)
    /** Scope of current project withing plugin configuration */
    lazy val thisOSGiScope = thisScope.copy(config = Select(OSGiConf))

    updateLastKnownState()

    /** Update last known state */
    def updateLastKnownState() = synchronized {
      if (!lastKnownState.exists(_.eq(state)))
        lastKnownState = Some(state)
    }
  }
}
