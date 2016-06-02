/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2016 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.osgi.manager.tycho.action

import sbt._
import sbt.Keys._
import sbt.osgi.manager.{ Plugin }
import sbt.osgi.manager.Keys.{ OSGiConf, osgiBndPrepareHome, osgiMavenPrepareHome }
import sbt.osgi.manager.support.OSGiManagerException
import sbt.osgi.manager.support.Support.logPrefix
import sbt.osgi.manager.bnd
import scala.collection.immutable
import scala.language.{ implicitConversions, reflectiveCalls }

class Resolve {
  def apply(resolveAsRemoteArtifacts: Boolean, state: State): State = {
    val extracted = Project.extract(state)
    var actualState = state
    implicit val projectRef = extracted.currentRef
    implicit val arg = Plugin.TaskArgument(actualState, Project.current(actualState), None)
    val ivySbtForCommand = {
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
    // resolve P2

    val dependencyP2 = sbt.osgi.manager.tycho.Resolve.resolveP2Command(ivySbtForCommand, resolveAsRemoteArtifacts)
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
      import extracted._
      val append = Load.transformSettings(Load.projectScope(currentRef),
        currentRef.build, rootProject, dependencySettings.toSeq)
      val newStructure = Load.reapply(session.mergeSettings ++ append, structure)
      Project.setProject(session, newStructure, state)
    } else
      actualState
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
}

object Resolve {
  implicit def Resolve2implementation(r: Resolve.type): Resolve = r.inner
  /** Resolve implementation. */
  lazy val inner = new Resolve()
}
