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

import java.net.URI
import java.net.URL

import scala.collection.mutable

import org.apache.maven.model.{ Dependency => MavenDependency }
import org.eclipse.equinox.internal.p2.metadata.OSGiVersion
import org.eclipse.equinox.internal.p2.metadata.VersionParser
import org.eclipse.equinox.p2.metadata.Version

import sbt.Artifact
import sbt.ModuleID

/**
 * Dependency concentrator that interacts with SBT and OSGi dependencies
 * It is also contains plugin weak hash map of Maven.Dependency -> SBT.ModuleID
 */
object Dependency {
  implicit def moduleID2Dependency(dependencies: Seq[ModuleID]): Seq[MavenDependency] =
    dependencies.map(convertDependency)
  implicit def tuplesWithString2repositories(repositories: Seq[(String, String)]): Seq[(String, URI)] =
    repositories.map { case (id, url) => (id, new URL(url).toURI) }
  implicit def tuplesWithURL2repositories(repositories: Seq[(String, URL)]): Seq[(String, URI)] =
    repositories.map { case (id, url) => (id, url.toURI) }
  implicit def version2string(version: Version): String = version.getOriginal
  private val dependencyOrigin = mutable.WeakHashMap[MavenDependency, Origin]()
  val ANY_VERSION = new OSGiVersion(0, 0, 0, "qualifier") // SBT is unable to handle "" or null version

  /** Convert SBT model id to Maven dependency */
  def convertDependency(moduleId: ModuleID): MavenDependency = {
    val version = VersionParser.parse(moduleId.revision, 0, moduleId.revision.length())
    val dependency = new MavenDependency()
    dependency.setArtifactId(moduleId.name)
    dependency.setType(moduleId.organization)
    if (version.compareTo(ANY_VERSION) != 0)
      dependency.setVersion(moduleId.revision)
    val withSources = moduleId.explicitArtifacts.exists(_.classifier == Some(Artifact.SourceClassifier))
    dependencyOrigin(dependency) = Origin(withSources, moduleId)
    dependency
  }
  def getOrigin(dependency: MavenDependency) = dependencyOrigin.get(dependency)

  case class Origin(val withSources: Boolean, moduleId: ModuleID)
}
