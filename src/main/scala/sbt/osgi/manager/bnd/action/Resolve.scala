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

package sbt.osgi.manager.bnd.action

import sbt._
import sbt.osgi.manager.Plugin

object Resolve {
  /** Resolve the dependency for the specific project against OBR repository */
  def resolveOBR(projectRef: ProjectRef)(implicit arg: Plugin.TaskArgument): Seq[Project.Setting[_]] = {
    Seq()
  }
  /** Command that populates libraryDependencies with required bundles */
  def resolveOBRCommand()(implicit arg: Plugin.TaskArgument): Seq[Project.Setting[_]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    // Check if we already processed our dependencies with same values
    /*    val cached = for (id <- build.defined.keys) yield {
      val projectRef = ProjectRef(uri, id)
      val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
      isCached(CacheP2Key(id), getP2Dependencies(scope), getP2Resolvers(scope))
    }
    if (cached.forall(_ == true)) {
      arg.log.info("Pass P2 resolution: already resolved")
      Seq()
    } else {*/
    (for (id <- build.defined.keys) yield resolveOBR(ProjectRef(uri, id))).toSeq.flatten
    //}
  }
}
