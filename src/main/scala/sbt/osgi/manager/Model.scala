/**
 * sbt-osgi-manager - OSGi development bridge based on Bndtools and Tycho.
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

import scala.collection.JavaConversions._

import org.apache.maven.project.MavenProject

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
import sbt._
import sbt.osgi.manager.Keys._
import sbt.osgi.manager.Support._

object Model {
  private lazy val exportPackageConverter: Converter[java.util.List[ExportedPackage], String] =
    new ClauseListConverter[ExportedPackage](
      new Converter[ExportedPackage, aQute.libg.tuple.Pair[String, Attrs]]() {
        def convert(input: aQute.libg.tuple.Pair[String, Attrs]) = new ExportedPackage(input.getFirst(), input.getSecond())
      })
  private lazy val headerClauseListConverter =
    new HeaderClauseListConverter()
  private lazy val importPatternConverter: Converter[java.util.List[ImportPattern], String] =
    new ClauseListConverter[ImportPattern](
      new Converter[ImportPattern, aQute.libg.tuple.Pair[String, Attrs]]() {
        def convert(input: aQute.libg.tuple.Pair[String, Attrs]) = new ImportPattern(input.getFirst(), input.getSecond())
      })
  private lazy val clauseListConverter: Converter[java.util.List[VersionedClause], String] =
    new ClauseListConverter[VersionedClause](new VersionedClauseConverter())

  /** Get an BndEditModel view */
  def getBndEditModel()(implicit arg: Plugin.TaskArgument): BndEditModel = synchronized {
    val model = new BndEditModel()
    //model.loadFrom(inputStream)

    //bndEditModel.setBndResource(File bndResource)
    //bndEditModel.setBndResourceName(String bndResourceName)
    //bndEditModel.setBuildPackages(List< ? extends VersionedClause> paths)
    //bndEditModel.setBuildPath(List< ? extends VersionedClause> paths)
    //bndEditModel.setDSAnnotationPatterns(List< ? extends String> patterns)
    getPropertyBundleActivator foreach (model.setBundleActivator)
    getPropertyBundleCategory foreach (list => model.setBundleCategory(list.mkString(",")))
    getPropertyContactAddress foreach (model.setBundleContactAddress)
    getPropertyCopyright foreach (model.setBundleCopyright)
    getPropertyBundleDescription foreach (model.setBundleDescription)
    getPropertyDocUrl foreach (model.setBundleDocUrl)
    getPropertyLicense foreach (model.setBundleLicense)
    getPropertyName foreach (model.setBundleName)
    getPropertySymbolicName foreach (model.setBundleSymbolicName)
    getPropertyUpdateLocation foreach (model.setBundleUpdateLocation)
    getPropertyVendor foreach (model.setBundleVendor)
    getPropertyVersion foreach (model.setBundleVersion)
    getPropertyClassPath foreach (list => model.setClassPath(list))
    getPropertyExportedPackages foreach (list => model.setExportedPackages(Model.exportPackageConverter.convert(list.mkString(","))))
    getPropertyImportPackages foreach (list => model.setImportPatterns(Model.importPatternConverter.convert(list.mkString(","))))
    getPropertyPlugin foreach (list => model.setPlugins(Model.headerClauseListConverter.convert(list.mkString(","))))
    getPropertyPluginPath foreach (list => model.setPluginPath(list))
    getPropertyPrivatePackages foreach (list => model.setPrivatePackages(list))
    // BUG in origin, used headerClauseListConverter instead of clauseListConverter
    getPropertyRunBundles foreach (list => model.setRunBundles(Model.clauseListConverter.convert(list.mkString(","))))
    getPropertyRunEE foreach (ee => model.setEE(EE.parse(ee)))
    getPropertyRunFramework foreach (model.setRunFramework)
    getPropertyRunFW foreach (model.setRunFw)
    //bndEditModel.setRunProperties(Map<String,String> props)
    osgiBndRunRepos in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setRunRepos(list))
    //bndEditModel.setRunRequires(List<Requirement> requires)
    osgiBndRunVM in arg.thisOSGiScope get arg.extracted.structure.data foreach (model.setRunVMArgs)
    // BROKEN by origin
    //osgiBndSources in thisScope get extracted.structure.data foreach (b => model.setIncludeSources(Boolean.box(b)))
    //bndEditModel.setOutputFile(String name)
    //bndEditModel.setServiceComponents(List< ? extends ServiceComponent> components)
    osgiBndSub in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setSubBndFiles(list))
    //        bndEditModel.setSystemPackages(List< ? extends ExportedPackage> packages)
    osgiBndTestCases in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setTestSuites(list))
    model
  }
  def getMavenProject(groupId: String, artifactId: String, version: String)(implicit arg: Plugin.TaskArgument): MavenProject = synchronized {
    val project = Some(new MavenProject)
    //val realm = plexus.getContainerRealm()
    //withClassLoaderOf(realm) {
    //System.err.println("!!!" + plexus.lookup(classOf[PluginRealmHelper].toString))
    //System.err.println("!!!" + plexus.lookup(classOf[RepositorySystem].toString))

    //}

    /*    val project = for {
    } yield {
      val project = new MavenProject(organization, name, version)
      val repositorySystem =
      MavenProject( RepositorySystem , ProjectBuilder mavenProjectBuilder,
                  ProjectBuildingRequest projectBuilderConfiguration, Logger logger )
      project
    }*/
    project getOrElse {
      if (getPropertyVersion.isEmpty) throw new RuntimeException("BUNDLE_VERSION is undefined")
      throw new RuntimeException("Unknown error")
    }
  }

  /////////////////////////////////////
  // Bndtools properties
  //
  /** aQute.bnd.osgi.Constants.BUNDLE_ACTIVATOR */
  def getPropertyBundleActivator(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleActivator in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_CATEGORY */
  def getPropertyBundleCategory(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleCategory in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_CONTACTADDRESS */
  def getPropertyContactAddress(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleContactAddress in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_COPYRIGHT */
  def getPropertyCopyright(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleCopyright in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_DESCRIPTION */
  def getPropertyBundleDescription(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleDescription in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_DOCURL */
  def getPropertyDocUrl(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleDocURL in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_LICENSE */
  def getPropertyLicense(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleLicense in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_NAME */
  def getPropertyName(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleName in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_SYMBOLICNAME */
  def getPropertySymbolicName(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleSymbolicName in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_UPDATELOCATION */
  def getPropertyUpdateLocation(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleUpdateLocation in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_VENDOR */
  def getPropertyVendor(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleVendor in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_VERSION */
  def getPropertyVersion(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleVersion in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.CLASSPATH */
  def getPropertyClassPath(implicit arg: Plugin.TaskArgument) =
    osgiBndClassPath in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.EXPORT_PACKAGE */
  def getPropertyExportedPackages(implicit arg: Plugin.TaskArgument) =
    osgiBndExportPackage in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.IMPORT_PACKAGE */
  def getPropertyImportPackages(implicit arg: Plugin.TaskArgument) =
    osgiBndImportPackage in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.PLUGIN */
  def getPropertyPlugin(implicit arg: Plugin.TaskArgument) =
    osgiBndPlugin in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.PLUGINPATH */
  def getPropertyPluginPath(implicit arg: Plugin.TaskArgument) =
    osgiBndPluginPath in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.PRIVATE_PACKAGE */
  def getPropertyPrivatePackages(implicit arg: Plugin.TaskArgument) =
    osgiBndPrivatePackage in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.RUNBUNDLES */
  def getPropertyRunBundles(implicit arg: Plugin.TaskArgument) =
    osgiBndRunBundles in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.RUNEE */
  def getPropertyRunEE(implicit arg: Plugin.TaskArgument) =
    osgiBndRunEE in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.RUNFRAMEWORK */
  def getPropertyRunFramework(implicit arg: Plugin.TaskArgument) =
    osgiBndRunFramework in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.RUNFW */
  def getPropertyRunFW(implicit arg: Plugin.TaskArgument) =
    osgiBndRunFW in arg.thisOSGiScope get arg.extracted.structure.data

  def getSettingsBndtoolsDirectory(implicit arg: Plugin.TaskArgument) =
    osgiBndtoolsDirectory in arg.thisOSGiScope get arg.extracted.structure.data

  /////////////////////////////////////
  // Various plugin settings
  //

  def getSettingsDirectory(implicit arg: Plugin.TaskArgument) =
    osgiDirectory in arg.thisOSGiScope get arg.extracted.structure.data

  /////////////////////////////////////
  // Maven
  //

  def getSettingsMavenDirectory(implicit arg: Plugin.TaskArgument) =
    osgiMavenDirectory in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsMavenGlobalXML(implicit arg: Plugin.TaskArgument) =
    osgiMavenGlobalSettingsXML in arg.thisOSGiScope get arg.extracted.structure.data getOrElse None
  def getSettingsIsOffline(implicit arg: Plugin.TaskArgument) =
    osgiMavenIsOffline in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsIsUpdateSnapshots(implicit arg: Plugin.TaskArgument) =
    osgiMavenIsUpdateSnapshots in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsMavenPlexusXML(implicit arg: Plugin.TaskArgument) =
    osgiMavenPlexusXML in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsMavenSystemProperties(implicit arg: Plugin.TaskArgument) =
    osgiMavenSystemProperties in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsMavenUserHome(implicit arg: Plugin.TaskArgument) =
    osgiMavenUserHome in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsMavenUserProperties(implicit arg: Plugin.TaskArgument) =
    osgiMavenUserProperties in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsMavenUserXML(implicit arg: Plugin.TaskArgument) =
    osgiMavenUserSettingsXML in arg.thisOSGiScope get arg.extracted.structure.data getOrElse None

  /** */
  def show()(implicit arg: Plugin.TaskArgument) {
    show("BUILDPACKAGES", "") // UNUSED by origin
    show("BUILDPATH", "")
    show("BUNDLE_ACTIVATOR", getPropertyBundleActivator getOrElse "", "-", scala.Console.WHITE)
    show("BUNDLE_CATEGORY", getPropertyBundleCategory map (_.mkString(",")) getOrElse "")
    show("BUNDLE_CONTACTADDRESS", getPropertyContactAddress getOrElse "")
    show("BUNDLE_COPYRIGHT", getPropertyCopyright getOrElse "", "UNKNOWN", scala.Console.YELLOW)
    show("BUNDLE_DESCRIPTION", getPropertyBundleDescription getOrElse "", "UNKNOWN", scala.Console.YELLOW)
    show("BUNDLE_DOCURL", getPropertyDocUrl getOrElse "")
    show("BUNDLE_LICENSE", getPropertyLicense getOrElse "", "UNKNOWN", scala.Console.YELLOW)
    show("BUNDLE_NAME", getPropertyName getOrElse "", "UNKNOWN", scala.Console.RED)
    show("BUNDLE_SYMBOLICNAME", getPropertySymbolicName getOrElse "", "UNKNOWN", scala.Console.RED)
    show("BUNDLE_UPDATELOCATION", getPropertyUpdateLocation getOrElse "")
    show("BUNDLE_VENDOR", getPropertyVendor getOrElse "")
    show("BUNDLE_VERSION", getPropertyVersion getOrElse "", "UNKNOWN", scala.Console.RED)
    show("CLASSPATH", getPropertyClassPath map (_.mkString(",")) getOrElse "")
    show("EXPORT_PACKAGE", getPropertyExportedPackages map (_.mkString(",")) getOrElse "")
    show("IMPORT_PACKAGE", getPropertyImportPackages map (_.mkString(",")) getOrElse "")
    show("PLUGIN", getPropertyPlugin map (_.mkString(",")) getOrElse "")
    show("PLUGINPATH", getPropertyPluginPath map (_.mkString(",")) getOrElse "")
    show("PRIVATE_PACKAGE", getPropertyPrivatePackages map (_.mkString(",")) getOrElse "")
    show("RUNBUNDLES", getPropertyRunBundles map (_.mkString(",")) getOrElse "")
    show("RUNEE", getPropertyRunEE getOrElse "", "UNKNOWN", scala.Console.RED)
    show("RUNFRAMEWORK", getPropertyRunFramework getOrElse "")
    show("RUNFW", getPropertyRunFW getOrElse "", "UNKNOWN", scala.Console.RED)
    /*show("RUNPROPERTIES", Option(model.getRunProperties()).map(_.mkString(",")).getOrElse(""))
    show("RUNREPOS", Option(model.getRunRepos()).map(_.mkString(",")).getOrElse(""))
    show("RUNREQUIRES", Option(model.getRunRequires()).map(_.mkString(",")).getOrElse(""))
    show("RUNVM", model.getRunVMArgs())
    show("SUB", Option(model.getSubBndFiles()).map(_.mkString(",")).getOrElse(""))
    show("SOURCES", Option(model.isIncludeSources()).map(Boolean.box).getOrElse(""))
    show("TESTCASES", Option(model.getTestSuites()).map(_.mkString(",")).getOrElse(""))*/
  }
  protected def show(parameter: String, value: AnyRef, onEmpty: String)(implicit arg: Plugin.TaskArgument): Unit = show(parameter, value, Some("", onEmpty))(arg)
  protected def show(parameter: String, value: AnyRef, onEmpty: String, color: String)(implicit arg: Plugin.TaskArgument): Unit = show(parameter, value, Some(color, onEmpty))(arg)
  protected def show(parameter: String, value: AnyRef)(implicit arg: Plugin.TaskArgument): Unit = show(parameter, value, None)(arg)
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
}
