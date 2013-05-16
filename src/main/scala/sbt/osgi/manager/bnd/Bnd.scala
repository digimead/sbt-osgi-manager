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

package sbt.osgi.manager.bnd

import java.util.Properties
import java.util.jar.Manifest

import scala.collection.JavaConversions._
import scala.ref.WeakReference

import aQute.bnd.build.Workspace
import aQute.bnd.build.model.BndEditModel
import aQute.bnd.build.model.EE
import aQute.bnd.build.model.clauses.ExportedPackage
import aQute.bnd.build.model.clauses.ImportPattern
import aQute.bnd.build.model.clauses.VersionedClause
import aQute.bnd.build.model.conversions.ClauseListConverter
import aQute.bnd.build.model.conversions.Converter
import aQute.bnd.build.model.conversions.HeaderClauseListConverter
import aQute.bnd.build.model.conversions.VersionedClauseConverter
import aQute.bnd.header.Attrs
import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.{ Constants => BndConstant }
import sbt._
import sbt.Keys._
import sbt.osgi.manager.Keys._
import sbt.osgi.manager.Model
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support._

class Bnd(cnf: File) {
  /** A cnf project containing workspace-wide configuration */
  lazy val workspace: Workspace = { // getWorkspace
    val workspace = Workspace.getWorkspace(cnf.getParentFile())
    // add plugins
    //workspace.addBasicPlugin(new WorkspaceListener(workspace))
    //workspace.addBasicPlugin(Activator.instance.repoListenerTracker)
    //workspace.addBasicPlugin(getWorkspaceR5Repository())

    // Initialize projects in synchronized block
    workspace.getBuildOrder()
    workspace
  }

  def createModel()(implicit arg: Plugin.TaskArgument): BndEditModel = {
    val model = new BndEditModel()

    //bndEditModel.setBndResource(File bndResource)
    //bndEditModel.setBndResourceName(String bndResourceName)
    //bndEditModel.setBuildPackages(List< ? extends VersionedClause> paths)
    //bndEditModel.setBuildPath(List< ? extends VersionedClause> paths)
    //bndEditModel.setDSAnnotationPatterns(List< ? extends String> patterns)
    Model.getPropertyActivator foreach (model.setBundleActivator)
    Model.getPropertyCategory foreach (list => model.setBundleCategory(list.mkString(",")))
    Model.getPropertyContactAddress foreach (model.setBundleContactAddress)
    Model.getPropertyCopyright foreach (model.setBundleCopyright)
    Model.getPropertyDescription foreach (model.setBundleDescription)
    Model.getPropertyDocUrl foreach (model.setBundleDocUrl)
    Model.getPropertyLicense foreach (model.setBundleLicense)
    Model.getPropertyName foreach (model.setBundleName)
    Model.getPropertySymbolicName foreach (model.setBundleSymbolicName)
    Model.getPropertyUpdateLocation foreach (model.setBundleUpdateLocation)
    Model.getPropertyVendor foreach (model.setBundleVendor)
    Model.getPropertyVersion foreach (model.setBundleVersion)
    Model.getPropertyClassPath foreach (list => model.setClassPath(list))
    Model.getPropertyExportPackages foreach (list => model.setExportedPackages(Bnd.Converter.exportPackageConverter.convert(list.mkString(","))))
    Model.getPropertyImportPackages foreach (list => model.setImportPatterns(Bnd.Converter.importPatternConverter.convert(list.mkString(","))))
    Model.getPropertyPlugin foreach (list => model.setPlugins(Bnd.Converter.headerClauseListConverter.convert(list.mkString(","))))
    Model.getPropertyPluginPath foreach (list => model.setPluginPath(list))
    Model.getPropertyPrivatePackages foreach (list => model.setPrivatePackages(list))
    // BUG in origin, used headerClauseListConverter instead of clauseListConverter
    Model.getPropertyRunBundles foreach (list => model.setRunBundles(Bnd.Converter.clauseListConverter.convert(list.mkString(","))))
    Model.getPropertyRunEE foreach (ee => model.setEE(EE.parse(ee)))
    Model.getPropertyRunFramework foreach (model.setRunFramework)
    Model.getPropertyRunFW foreach (model.setRunFw)
    //bndEditModel.setRunProperties(Map<String,String> props)
    osgiBndRunRepos in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setRunRepos(list))
    //bndEditModel.setRunRequires(List<Requirement> requires)
    osgiBndRunVM in arg.thisOSGiScope get arg.extracted.structure.data foreach (model.setRunVMArgs)
    // BROKEN by origin
    //osgiBndSources in thisScope get extracted.structure.data foreach (b => model.setIncludeSources(Boolean.box(b)))
    //bndEditModel.setOutputFile(String name)
    //bndEditModel.setServiceComponents(List< ? extends ServiceComponent> components)
    osgiBndSub in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setSubBndFiles(list))
    // bndEditModel.setSystemPackages(List< ? extends ExportedPackage> packages)
    osgiBndTestCases in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setTestSuites(list))
    model
  }

  def calculateManifest(product: File, dependencyClasspath: Seq[File])(implicit arg: Plugin.TaskArgument): Manifest = {
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
}

object Bnd {
  @volatile private var cnfProjects = Seq[WeakReference[Project]]()
  lazy val settings = inConfig(OSGiConf)(Seq[sbt.Project.Setting[_]](
    osgiCnfPath <<= (osgiDirectory) { file =>
      val cnf = file / "cnf"
      cnf.mkdirs()
      assert(cnf.isDirectory(), cnf + " is not directory")
      assert(cnf.canWrite(), cnf + " is not writable")
      cnf
    },
    osgiBndtoolsDirectory <<= (osgiDirectory) { _ / "bnd" },
    osgiBndBuildPath := List[String](),
    osgiBndBundleActivator := "",
    osgiBndBundleActivationPolicy := "",
    osgiBndBundleCategory := List[String](), // http://www.osgi.org/Specifications/Reference#categories
    osgiBndBundleContactAddress := "",
    osgiBndBundleCopyright := "",
    osgiBndBundleDescription <<= description in This,
    osgiBndBundleDocURL :== "",
    osgiBndBundleIcon :== List[String](),
    osgiBndBundleUpdateLocation := "",
    osgiBndBundleSymbolicName := "",
    osgiBndBundleSymbolicNameSingleton := false,
    osgiBndBundleName <<= name in This,
    osgiBndBundleLicense := "",
    osgiBndBundleVendor <<= organizationName in This,
    osgiBndBundleVersion <<= version in This,
    osgiBndBundleFragmentHost := "",
    osgiBndClassPath := List[String](),
    osgiBndExportPackage := List[String](),
    osgiBndImportPackage := List[String](),
    osgiBndPlugin := List[String](),
    osgiBndPluginPath := List[String](),
    osgiBndPrivatePackage := List[String](),
    osgiBndRunBundles := List[String](),
    osgiBndRunEE := "OSGi/Minimum-1.0",
    osgiBndRunFramework := "",
    osgiBndRunFW := "org.apache.felix.framework",
    osgiBndRunProperties := "",
    osgiBndRunRepos := List[String](),
    osgiBndRunRequires := "",
    osgiBndRunVM := "",
    osgiBndSub := List[String](),
    osgiBndServiceComponent := "",
    osgiBndSources := false,
    osgiBndTestCases := List[String]()))

  /** Calculate manifest's content of the artifact */
  def calculateManifest(cnf: File, dependencyClasspath: Seq[Attributed[File]], options: Seq[PackageOption], products: Seq[File])(implicit arg: Plugin.TaskArgument): Seq[PackageOption] = {
    arg.log.info(logPrefix(arg.name) + "Calculate bundle manifest.")
    val bnd = get(cnf)
    val classpath = dependencyClasspath.map(_.data)
    val unprocessedOptions = Seq[PackageOption]()
    val manifest = new Manifest
    val main = manifest.getMainAttributes
    products.foreach { product =>
      arg.log.debug(logPrefix("Calculate manifest for " + product))
      Package.mergeManifests(manifest, bnd.calculateManifest(product, classpath))
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
  /** Get an exists or create a new instance for the cnf project */
  def get(cnf: File): Bnd =
    cnfProjects find (_.get.exists(_.cnfLocation == cnf)) flatMap (_.get.map(_.bnd)) getOrElse {
      val bndtoolInstance = Project(cnf, new Bnd(cnf))
      cnfProjects = cnfProjects :+ new WeakReference(bndtoolInstance)
      bndtoolInstance.bnd
    }
  /** Show the project bundle properties */
  def show()(implicit arg: Plugin.TaskArgument) {
    show("BUILDPACKAGES", "") // UNUSED by origin
    show("BUILDPATH", "")
    show("BUNDLE_ACTIVATOR", Model.getPropertyActivator getOrElse "", "-", scala.Console.WHITE)
    show("BUNDLE_ACTIVATIONPOLICY", Model.getPropertyActivationPolicy getOrElse "")
    show("BUNDLE_CATEGORY", Model.getPropertyCategory map (_.mkString(",")) getOrElse "")
    show("BUNDLE_CONTACTADDRESS", Model.getPropertyContactAddress getOrElse "")
    show("BUNDLE_COPYRIGHT", Model.getPropertyCopyright getOrElse "", "UNKNOWN", scala.Console.YELLOW)
    show("BUNDLE_DESCRIPTION", Model.getPropertyDescription getOrElse "", "UNKNOWN", scala.Console.YELLOW)
    show("BUNDLE_DOCURL", Model.getPropertyDocUrl getOrElse "")
    show("BUNDLE_ICON", Model.getPropertyIcon map (_.mkString(",")) getOrElse "")
    show("BUNDLE_LICENSE", Model.getPropertyLicense getOrElse "", "UNKNOWN", scala.Console.YELLOW)
    show("BUNDLE_NAME", Model.getPropertyName getOrElse "", "UNKNOWN", scala.Console.RED)
    show("BUNDLE_SYMBOLICNAME", Model.getPropertySymbolicName getOrElse "", "UNKNOWN", scala.Console.RED)
    show("BUNDLE_SYMBOLICNAME singleton", Model.getPropertySymbolicNameSingleton.map(_.toString), "false", scala.Console.WHITE)
    show("BUNDLE_UPDATELOCATION", Model.getPropertyUpdateLocation getOrElse "")
    show("BUNDLE_VENDOR", Model.getPropertyVendor getOrElse "")
    show("BUNDLE_VERSION", Model.getPropertyVersion getOrElse "", "UNKNOWN", scala.Console.RED)
    show("FRAGMENT_HOST", Model.getPropertyFragmentHost getOrElse "")
    show("CLASSPATH", Model.getPropertyClassPath map (_.mkString(",")) getOrElse "")
    show("DYNAMICIMPORT_PACKAGE", Model.getPropertyDynamicImport getOrElse "")
    show("EXPORT_PACKAGE", Model.getPropertyExportPackages map (_.mkString(",")) getOrElse "")
    show("IMPORT_PACKAGE", Model.getPropertyImportPackages map (_.mkString(",")) getOrElse "")
    show("PLUGIN", Model.getPropertyPlugin map (_.mkString(",")) getOrElse "")
    show("PLUGINPATH", Model.getPropertyPluginPath map (_.mkString(",")) getOrElse "")
    show("PRIVATE_PACKAGE", Model.getPropertyPrivatePackages map (_.mkString(",")) getOrElse "")
    show("RUNBUNDLES", Model.getPropertyRunBundles map (_.mkString(",")) getOrElse "")
    show("RUNEE", Model.getPropertyRunEE getOrElse "", "UNKNOWN", scala.Console.RED)
    show("RUNFRAMEWORK", Model.getPropertyRunFramework getOrElse "")
    show("RUNFW", Model.getPropertyRunFW getOrElse "", "UNKNOWN", scala.Console.RED)
    /*show("RUNPROPERTIES", Option(model.getRunProperties()).map(_.mkString(",")).getOrElse(""))
    show("RUNREPOS", Option(model.getRunRepos()).map(_.mkString(",")).getOrElse(""))
    show("RUNREQUIRES", Option(model.getRunRequires()).map(_.mkString(",")).getOrElse(""))
    show("RUNVM", model.getRunVMArgs())
    show("SUB", Option(model.getSubBndFiles()).map(_.mkString(",")).getOrElse(""))
    show("SOURCES", Option(model.isIncludeSources()).map(Boolean.box).getOrElse(""))
    show("TESTCASES", Option(model.getTestSuites()).map(_.mkString(",")).getOrElse(""))*/
  }
  /** Display the single property */
  protected def show(parameter: String, value: AnyRef, onEmpty: String)(implicit arg: Plugin.TaskArgument): Unit = show(parameter, value, Some("", onEmpty))(arg)
  /** Display the single property */
  protected def show(parameter: String, value: AnyRef, onEmpty: String, color: String)(implicit arg: Plugin.TaskArgument): Unit = show(parameter, value, Some(color, onEmpty))(arg)
  /** Display the single property */
  protected def show(parameter: String, value: AnyRef)(implicit arg: Plugin.TaskArgument): Unit = show(parameter, value, None)(arg)
  /** Display the single property */
  protected def show(parameter: String, value: AnyRef, onEmpty: Option[(String, String)])(implicit arg: Plugin.TaskArgument) {
    val key = if (arg.log.ansiCodesSupported) scala.Console.GREEN + parameter + scala.Console.RESET else parameter
    val message = onEmpty match {
      case Some(onEmpty) =>
        if (Option(value).isEmpty || value.toString.trim.isEmpty) {
          if (arg.log.ansiCodesSupported)
            key + ": " + onEmpty._1 + onEmpty._2 + scala.Console.RESET
          else
            key + ": " + onEmpty._2
        } else
          key + ": " + value.toString()
      case None =>
        if (Option(value).isEmpty || value.toString.trim.isEmpty) return
        key + ": " + value.toString()
    }
    arg.log.info(logPrefix(arg.name) + message)
  }

  case class Project(cnfLocation: File, bnd: Bnd)
  object Converter {
    private[Bnd] lazy val exportPackageConverter: Converter[java.util.List[ExportedPackage], String] =
      new ClauseListConverter[ExportedPackage](
        new Converter[ExportedPackage, aQute.libg.tuple.Pair[String, Attrs]]() {
          def convert(input: aQute.libg.tuple.Pair[String, Attrs]) = new ExportedPackage(input.getFirst(), input.getSecond())
        })
    private[Bnd] lazy val headerClauseListConverter =
      new HeaderClauseListConverter()
    private[Bnd] lazy val importPatternConverter: Converter[java.util.List[ImportPattern], String] =
      new ClauseListConverter[ImportPattern](
        new Converter[ImportPattern, aQute.libg.tuple.Pair[String, Attrs]]() {
          def convert(input: aQute.libg.tuple.Pair[String, Attrs]) = new ImportPattern(input.getFirst(), input.getSecond())
        })
    private[Bnd] lazy val clauseListConverter: Converter[java.util.List[VersionedClause], String] =
      new ClauseListConverter[VersionedClause](new VersionedClauseConverter())
  }
}
