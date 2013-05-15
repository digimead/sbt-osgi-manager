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

package sbt.osgi

import java.net.URI
import java.net.URL

import org.apache.maven.model.Dependency
import org.eclipse.equinox.p2.metadata.Version

import sbt._
import sbt.osgi.manager.Dependency
import sbt.osgi.manager.Keys
import sbt.osgi.manager.Plugin

package object manager {
  /** Entry point for plugin in user's project */
  def activateOSGiManager = Plugin.defaultSettings
  /** Entry point for plugin in user's project */
  def activateOSGiManagerWithDebug(tcpPortForEquinoxConsole: Int = 12345) = {
    debug(true)
    Plugin.debug = Some(tcpPortForEquinoxConsole)
    Plugin.defaultSettings
  }

  // export declarations for end user
  lazy val OSGiKey = Keys
  lazy val OSGiConf = Keys.OSGiConf

  implicit def moduleID2Dependency(dependencies: Seq[ModuleID]): Seq[Dependency] =
    Dependency.moduleID2Dependency(dependencies)
  implicit def tuplesWithString2repositories(repositories: Seq[(String, String)]): Seq[(String, URI)] =
    Dependency.tuplesWithString2repositories(repositories)
  implicit def tuplesWithURL2repositories(repositories: Seq[(String, URL)]): Seq[(String, URI)] =
    Dependency.tuplesWithURL2repositories(repositories)
  implicit def version2string(version: Version): String =
    Dependency.version2string(version)

  def typeOBR(moduleID: ModuleID): ModuleID = Plugin.markDependencyAsOBR(moduleID)
  def typeP2(moduleID: ModuleID): ModuleID = Plugin.markDependencyAsP2(moduleID)

  def typeOBR(resolver: Resolver): Resolver = Plugin.markResolverAsOBR(resolver)
  def typeP2(resolver: Resolver): Resolver = Plugin.markResolverAsP2(resolver)

  def typeP2(module1: ModuleID, module2: ModuleID, modulen: ModuleID*): Seq[ModuleID] =
    (module1 +: module2 +: modulen).map(typeP2)
  def typeP2(moduleID: String): ModuleID =
    typeP2(OSGi.ECLIPSE_PLUGIN % moduleID % OSGi.ANY_VERSION)
  def typeP2WithSources(module1: String, module2: String, modulen: String*): Seq[ModuleID] =
    (module1 +: module2 +: modulen).map(name => typeP2((OSGi.ECLIPSE_PLUGIN % name % OSGi.ANY_VERSION).withSources))
  def typeP2(module1: String, module2: String, modulen: String*): Seq[ModuleID] =
    (module1 +: module2 +: modulen).map(name => typeP2(OSGi.ECLIPSE_PLUGIN % name % OSGi.ANY_VERSION))

  protected def debug(enable: Boolean) {
    System.setProperty("org.sonatype.inject.debug", enable.toString)
  }
}
