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

import java.io.File

import scala.collection.JavaConversions._

import sbt.osgi.manager.Model
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support._

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.Jar
import sbt.IO
import sbt.ModuleID

/** Fetch project dependencies as bundles */
object Fetch {
  /*
   * fetchInfo is Fn(ModuleID, JarName, Analyzer) that adds information about bundle to analyzer
   * It looks like:
   *   analyzer.setImportPackage("*;resolution:=optional")
   *   analyzer.setExportPackage("*")
   *   analyzer.setBundleSymbolicName("my.bundle")
   *   analyzer.setBundleVersion("1.0.0")
   */
  /** Default Fn that adds information about bundle to analyzer */
  def defaultFetchInfo(module: Option[ModuleID], name: String, analyzer: Analyzer) {
    analyzer.setImportPackage("*;resolution:=optional")
    analyzer.setExportPackage("*")
    module match {
      case Some(module) =>
        analyzer.setBundleSymbolicName(module.organization + "." + module.name)
        analyzer.setBundleVersion("0.0.1.0-SNAPSHOT")
      case None =>
        analyzer.setBundleSymbolicName(name.replaceAll("[^a-zA-Z0-9]", "-"))
        analyzer.setBundleVersion("0.0.1.0-SNAPSHOT")
    }
  }
  /** Fetch project dependencies as bundles */
  def fetchTask(output: File, classpath: Set[Item])(implicit arg: Plugin.TaskArgument) {
    arg.log.info(logPrefix(arg.name) + "Fetch bundles to " + output)
    classpath.foreach {
      case Item(moduleId, artifact) if artifact.isFile =>
        arg.log.debug(logPrefix(arg.name) + "process " + artifact.getCanonicalPath)
        var jar: Jar = null
        try {
          jar = new Jar(artifact)
          val target = new File(output, artifact.getName)
          val manifest = jar.getManifest()
          val entries = manifest.getMainAttributes().entrySet()
          entries.find { _.getKey().toString == "Bundle-SymbolicName" } match {
            case Some(e) =>
              val bundleName = e.getValue().toString.split(";").head
              arg.log.info("Fetch bundle %s: %s".format(bundleName, artifact.getName))
              try {
                IO.copyFile(artifact, target)
              } catch {
                case e: Throwable =>
                  arg.log.error(logPrefix(arg.name) + "unable to fetch bundle %s: %s".format(bundleName, e))
              }
            case None =>
              arg.log.info("Convert plain jar %s to bundle".format(artifact.getName))
              val fetchInfo = Model.getSettingsFetchInfo getOrThrow "osgiFetchInfo not defined"
              convert(moduleId, jar, target, fetchInfo)
          }
        } finally {
          try { if (jar != null) jar.close } catch { case _: Throwable => }
        }
      case Item(module, artifact) =>
        arg.log.warn(logPrefix(arg.name) + "Skip " + artifact.getCanonicalPath)
    }
  }
  /** Convert jar to bundle */
  protected def convert(moduleId: Option[ModuleID], from: Jar, to: File, fetchInfo: (Option[ModuleID], String, Analyzer) => Unit)(implicit arg: Plugin.TaskArgument) {
    var analyzer: Analyzer = null
    try {
      analyzer = new Analyzer()
      // analyzer.use(null) // WTF? Processor? Not today.
      analyzer.setJar(from)
      // add information about bundle via fetchInfo
      fetchInfo(moduleId, to.getName, analyzer)
      val newManifest = analyzer.calcManifest()
      if (analyzer.isOk()) {
        analyzer.getJar().setManifest(newManifest)
        analyzer.save(to, true)
      } else {
        arg.log.error(logPrefix(arg.name) + "Unable to process " + to.getName)
        analyzer.getErrors.foreach(error => arg.log.error(logPrefix(arg.name) + error))
      }
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(arg.name) + "unable to convert %s to bundle: %s".format(to.getName, e))
    } finally {
      try { if (analyzer != null) analyzer.close() } catch { case _: Throwable => }
    }
  }
  case class Item(module: Option[ModuleID], jar: File)
}
