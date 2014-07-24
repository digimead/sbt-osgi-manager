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

package sbt.osgi.manager.bnd.action

import aQute.bnd.osgi.{ Analyzer, Constants ⇒ BndConstant }
import java.io.File
import java.util.Properties
import java.util.jar.{ Attributes, Manifest }
import sbt.osgi.manager.{ Model, Plugin }
import sbt.osgi.manager.Support.logPrefix
import scala.collection.JavaConversions.{ asScalaSet, mapAsScalaMap, seqAsJavaList }

import sbt._

object GenerateManifest {
  def generate(product: File, dependencyClasspath: Seq[File])(implicit arg: Plugin.TaskArgument): Manifest = {
    val analyzer = new Analyzer()
    try {
      analyzer.setJar(product)
      val properties = new Properties()
      Model.getPropertyActivator.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_ACTIVATOR, value))
      Model.getPropertyActivationPolicy.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_ACTIVATIONPOLICY, value))
      Model.getPropertyCategory.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_CATEGORY, value))
      Model.getPropertyClassPath.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_CLASSPATH, value))
      Model.getPropertyContactAddress.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_CONTACTADDRESS, value))
      Model.getPropertyCopyright.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_COPYRIGHT, value))
      Model.getPropertyDescription.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_DESCRIPTION, value))
      Model.getPropertyDocUrl.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_DOCURL, value))
      Model.getPropertyDynamicImport.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.DYNAMICIMPORT_PACKAGE, value))
      Model.getPropertyFragmentHost.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.FRAGMENT_HOST, value))
      Model.getPropertyLicense.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_LICENSE, value))
      Model.getPropertyName.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_NAME, value))
      Model.getPropertySymbolicName.foreach(value ⇒ if (value.nonEmpty) {
        if (Model.getPropertySymbolicNameSingleton getOrElse false)
          properties.put(BndConstant.BUNDLE_SYMBOLICNAME, value + ";singleton:=true")
        else
          properties.put(BndConstant.BUNDLE_SYMBOLICNAME, value)
      })
      Model.getPropertyUpdateLocation.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_UPDATELOCATION, value))
      Model.getPropertyVendor.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_VENDOR, value))
      Model.getPropertyVersion.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.BUNDLE_VERSION, value))

      Model.getPropertyExportPackages match {
        case Some(exportPackages) ⇒
          // SKIP EXPORT_PACKAGE HEADER IF EMPTY
          if (exportPackages.nonEmpty)
            properties.put(BndConstant.EXPORT_PACKAGE, exportPackages.mkString(","))
        case _ ⇒
          properties.put(BndConstant.EXPORT_PACKAGE, "*")
      }
      Model.getPropertyIgnorePackages.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.IGNORE_PACKAGE, value.mkString(",")))
      Model.getPropertyImportPackages match {
        case Some(importPackages) ⇒
          // SKIP IMPORT_PACKAGE HEADER IF EMPTY
          if (importPackages.nonEmpty)
            properties.put(BndConstant.IMPORT_PACKAGE, importPackages.mkString(","))
        case _ ⇒
          properties.put(BndConstant.IMPORT_PACKAGE, "*")
      }
      Model.getPropertyPrivatePackages.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.PRIVATE_PACKAGE, value.mkString(",")))
      Model.getPropertyRequireBundle.foreach(value ⇒
        if (value.nonEmpty) properties.put(BndConstant.REQUIRE_BUNDLE, value.mkString(",")))

      Model.getPropertyNoUses.foreach(value ⇒
        if (value) properties.put(BndConstant.NOUSES, "true"))

      analyzer.setProperties(properties)
      analyzer.addClasspath(dependencyClasspath)
      analyzer.calcManifest()
    } finally {
      analyzer.close()
    }
  }
  /** Calculate manifest's content of the artifact (osgiCompile task). */
  def generateManifestTask(dependencyClasspath: Seq[Attributed[File]], options: Seq[PackageOption], products: Seq[File])(implicit arg: Plugin.TaskArgument): Manifest = {
    arg.log.info(logPrefix(arg.name) + "Calculate bundle manifest.")
    val classpath = dependencyClasspath.map(_.data)
    val unprocessedOptions = Seq[PackageOption]()
    val manifest = new Manifest
    val main = manifest.getMainAttributes
    products.foreach { product ⇒
      arg.log.debug(logPrefix(arg.name) + "Calculate manifest for " + product)
      Package.mergeManifests(manifest, generate(product, classpath))
    }
    for (option ← options) option match {
      case Package.JarManifest(mergeManifest) ⇒ Package.mergeManifests(manifest, mergeManifest)
      case Package.ManifestAttributes(attributes @ _*) ⇒ main ++= attributes
      case _ ⇒
    }
    // filter headers that generate by BND and removed by user
    val summary = main.entrySet().map(entry ⇒ (entry.getKey().toString(), entry.getValue()))
    main.clear()
    summary.foreach {
      case (name @ "Export-Package", value) ⇒
        if (Model.getPropertyExportPackages.map(_.nonEmpty) getOrElse true)
          main.put(new Attributes.Name(name), value)
      case (name @ "Import-Package", value) ⇒
        if (Model.getPropertyImportPackages.map(_.nonEmpty) getOrElse true)
          main.put(new Attributes.Name(name), value)
      case (name @ "Private-Package", value) ⇒
        if (Model.getPropertyPrivatePackages.map(_.nonEmpty) getOrElse true)
          main.put(new Attributes.Name(name), value)
      case (name, value) ⇒
        main.put(new Attributes.Name(name), value)
    }
    manifest
  }
  /** Calculate manifest's content of the artifact (packageBin task). */
  def generatePackageOptionsTask(dependencyClasspath: Seq[Attributed[File]], options: Seq[PackageOption], products: Seq[File])(implicit arg: Plugin.TaskArgument): Seq[PackageOption] = {
    arg.log.info(logPrefix(arg.name) + "Calculate bundle package options.")
    val classpath = dependencyClasspath.map(_.data)
    val unprocessedOptions = Seq[PackageOption]()
    val manifest = new Manifest
    val main = manifest.getMainAttributes
    products.foreach { product ⇒
      arg.log.debug(logPrefix(arg.name) + "Calculate manifest for " + product)
      Package.mergeManifests(manifest, generate(product, classpath))
    }
    for (option ← options) option match {
      case Package.JarManifest(mergeManifest) ⇒ Package.mergeManifests(manifest, mergeManifest)
      case Package.ManifestAttributes(attributes @ _*) ⇒ main ++= attributes
      case _ ⇒
    }
    val attributes = main.entrySet().map(entry ⇒ (entry.getKey().toString, entry.getValue().toString)).filter {
      case ("Export-Package", _) ⇒
        Model.getPropertyExportPackages.map(_.nonEmpty) getOrElse true
      case ("Import-Package", _) ⇒
        Model.getPropertyImportPackages.map(_.nonEmpty) getOrElse true
      case ("Private-Package", _) ⇒
        Model.getPropertyPrivatePackages.map(_.nonEmpty) getOrElse true
      case _ ⇒ true
    }
    // Manifest of the artifact is unsorted anyway due the Java design
    Package.ManifestAttributes(attributes.toSeq: _*) +: unprocessedOptions
  }
}
