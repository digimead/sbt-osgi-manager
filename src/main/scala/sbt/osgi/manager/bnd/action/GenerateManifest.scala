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
import java.util.Properties
import java.util.jar.Manifest

import scala.collection.JavaConversions._

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.{ Constants => BndConstant }
import sbt._
import sbt.osgi.manager.Model
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support._

object GenerateManifest {
  def generate(product: File, dependencyClasspath: Seq[File])(implicit arg: Plugin.TaskArgument): Manifest = {
    val analyzer = new Analyzer()
    try {
      analyzer.setJar(product)
      val properties = new Properties()
      Model.getPropertyActivator.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_ACTIVATOR, value))
      Model.getPropertyActivationPolicy.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_ACTIVATIONPOLICY, value))
      Model.getPropertyCategory.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_CATEGORY, value))
      Model.getPropertyClassPath.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_CLASSPATH, value))
      Model.getPropertyContactAddress.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_CONTACTADDRESS, value))
      Model.getPropertyCopyright.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_COPYRIGHT, value))
      Model.getPropertyDescription.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_DESCRIPTION, value))
      Model.getPropertyDocUrl.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_DOCURL, value))
      Model.getPropertyDynamicImport.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.DYNAMICIMPORT_PACKAGE, value))
      Model.getPropertyFragmentHost.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.FRAGMENT_HOST, value))
      Model.getPropertyLicense.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_LICENSE, value))
      Model.getPropertyName.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_NAME, value))
      Model.getPropertySymbolicName.foreach(value => if (value.nonEmpty) {
        if (Model.getPropertySymbolicNameSingleton getOrElse false)
          properties.put(BndConstant.BUNDLE_SYMBOLICNAME, value + ";singleton:=true")
        else
          properties.put(BndConstant.BUNDLE_SYMBOLICNAME, value)
      })
      Model.getPropertyUpdateLocation.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_UPDATELOCATION, value))
      Model.getPropertyVendor.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_VENDOR, value))
      Model.getPropertyVersion.foreach(value =>
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_VERSION, value))

      Model.getPropertyImportPackages match {
        case Some(importPackages) if importPackages.nonEmpty =>
          properties.put(BndConstant.IMPORT_PACKAGE, importPackages.mkString(","))
        case _ =>
          properties.put(BndConstant.IMPORT_PACKAGE, "*")
      }
      Model.getPropertyExportPackages match {
        case Some(exportPackages) if exportPackages.nonEmpty =>
          properties.put(BndConstant.EXPORT_PACKAGE, exportPackages.mkString(","))
        case _ =>
          properties.put(BndConstant.IMPORT_PACKAGE, "*")
      }
      analyzer.setProperties(properties)
      analyzer.addClasspath(dependencyClasspath)
      analyzer.calcManifest()
    } finally {
      analyzer.close()
    }
  }
  /** Calculate manifest's content of the artifact */
  def generateTask(dependencyClasspath: Seq[Attributed[File]], options: Seq[PackageOption], products: Seq[File])(implicit arg: Plugin.TaskArgument): Seq[PackageOption] = {
    arg.log.info(logPrefix(arg.name) + "Calculate bundle manifest.")
    val classpath = dependencyClasspath.map(_.data)
    val unprocessedOptions = Seq[PackageOption]()
    val manifest = new Manifest
    val main = manifest.getMainAttributes
    products.foreach { product =>
      arg.log.debug(logPrefix(arg.name) + "Calculate manifest for " + product)
      Package.mergeManifests(manifest, generate(product, classpath))
    }
    // sort option by name if possible
    for (option <- options) option match {
      case Package.JarManifest(mergeManifest) => Package.mergeManifests(manifest, mergeManifest)
      case Package.ManifestAttributes(attributes @ _*) => main ++= attributes
      case _ =>
    }
    val attributes = main.entrySet().map(entry => (entry.getKey().toString, entry.getValue().toString)).toSeq.sortBy(_.toString())
    // Manifest of the artifact is unsorted anyway due the Java design
    Package.ManifestAttributes(attributes: _*) +: unprocessedOptions
  }
}
