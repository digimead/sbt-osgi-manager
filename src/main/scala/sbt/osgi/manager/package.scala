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

package sbt.osgi

import java.net.{ URI, URL }
import org.apache.maven.model.Dependency
import org.eclipse.equinox.p2.metadata.Version
import sbt.osgi.manager.{ Dependency, Keys, OSGi, Plugin }
import scala.language.implicitConversions

import sbt.Keys._
import sbt._

package object manager {
  /** Entry point for the plugin in user's project */
  def OSGiManager = Plugin.defaultSettings ++ Seq( // modify global SBT tasks
    packageOptions in (Compile, packageBin) in This <<= Plugin.packageOptionsTask)
  /** Entry point for the plugin in user's project */
  def OSGiManagerWithDebug(tcpPortForEquinoxConsole: Int = 12345) = {
    debug(true)
    Plugin.debug = Some(tcpPortForEquinoxConsole)
    Plugin.defaultSettings
  } ++ Seq( // modify global SBT tasks
    packageOptions in (Compile, packageBin) in This <<= Plugin.packageOptionsTask)

  // export declarations for end user
  lazy val OSGiKey = Keys
  lazy val OSGiConf = Keys.OSGiConf
  lazy val OSGiTestConf = Keys.OSGiTestConf

  implicit def moduleId2Dependency(dependencies: Seq[ModuleID]): Seq[Dependency] =
    Dependency.moduleId2Dependency(dependencies)
  implicit def tuplesWithString2repositories(repositories: Seq[(String, String)]): Seq[(String, URI)] =
    Dependency.tuplesWithString2repositories(repositories)
  implicit def tuplesWithURL2repositories(repositories: Seq[(String, URL)]): Seq[(String, URI)] =
    Dependency.tuplesWithURL2repositories(repositories)
  implicit def version2string(version: Version): String =
    Dependency.version2string(version)

  def typeOBR(moduleId: ModuleID): ModuleID = Plugin.markDependencyAsOBR(moduleId)
  def typeP2(moduleId: ModuleID): ModuleID = Plugin.markDependencyAsP2(moduleId)

  def typeOBR(resolver: Resolver): Resolver = Plugin.markResolverAsOBR(resolver)
  def typeP2(resolver: Resolver): Resolver = Plugin.markResolverAsP2(resolver)

  def typeP2(module1: ModuleID, module2: ModuleID, modulen: ModuleID*): Seq[ModuleID] =
    (module1 +: module2 +: modulen).map(typeP2)
  def typeP2(moduleId: String): ModuleID =
    typeP2(OSGi.ECLIPSE_PLUGIN % moduleId % OSGi.ANY_VERSION)
  def typeP2WithSources(module1: String, module2: String, modulen: String*): Seq[ModuleID] =
    (module1 +: module2 +: modulen).map(name ⇒ typeP2((OSGi.ECLIPSE_PLUGIN % name % OSGi.ANY_VERSION).withSources))
  def typeP2(module1: String, module2: String, modulen: String*): Seq[ModuleID] =
    (module1 +: module2 +: modulen).map(name ⇒ typeP2(OSGi.ECLIPSE_PLUGIN % name % OSGi.ANY_VERSION))

  protected def debug(enable: Boolean) {
    System.setProperty("org.sonatype.inject.debug", enable.toString)
  }
}
