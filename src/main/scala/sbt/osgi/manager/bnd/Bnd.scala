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

package sbt.osgi.manager.bnd

import aQute.bnd.build.Workspace
import aQute.bnd.build.model.{ BndEditModel, EE }
import aQute.bnd.build.model.clauses.{ ExportedPackage, ImportPattern, VersionedClause }
import aQute.bnd.build.model.conversions.{ ClauseListConverter, Converter, HeaderClauseListConverter, VersionedClauseConverter }
import aQute.bnd.header.Attrs
import aQute.bnd.service.{ Plugin ⇒ BndPlugin }
import sbt.osgi.manager.{ Model, OSGiManagerException, Plugin }
import sbt.osgi.manager.Keys._
import sbt.osgi.manager.Support.{ logPrefix, option2rich }
import scala.collection.JavaConversions.seqAsJavaList
import scala.ref.WeakReference

import sbt.Keys._
import sbt._

class Bnd(home: File) {
  def createModel()(implicit arg: Plugin.TaskArgument): BndEditModel = {
    val model = new BndEditModel()

    //bndEditModel.setBndResource(File bndResource)
    //bndEditModel.setBndResourceName(String bndResourceName)
    //bndEditModel.setBuildPackages(List< ? extends VersionedClause> paths)
    //bndEditModel.setBuildPath(List< ? extends VersionedClause> paths)
    //bndEditModel.setDSAnnotationPatterns(List< ? extends String> patterns)
    Model.getPropertyActivator foreach (model.setBundleActivator)
    Model.getPropertyCategory foreach (list ⇒ model.setBundleCategory(list.mkString(",")))
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
    Model.getPropertyClassPath foreach (list ⇒ model.setClassPath(list))
    Model.getPropertyExportPackages foreach (list ⇒ model.setExportedPackages(Bnd.Converter.exportPackageConverter.convert(list.mkString(","))))
    Model.getPropertyImportPackages foreach (list ⇒ model.setImportPatterns(Bnd.Converter.importPatternConverter.convert(list.mkString(","))))
    Model.getPropertyPlugin foreach (list ⇒ model.setPlugins(Bnd.Converter.headerClauseListConverter.convert(list.mkString(","))))
    Model.getPropertyPluginPath foreach (list ⇒ model.setPluginPath(list))
    Model.getPropertyPrivatePackages foreach (list ⇒ model.setPrivatePackages(list))
    // BUG in origin, used headerClauseListConverter instead of clauseListConverter
    Model.getPropertyRunBundles foreach (list ⇒ model.setRunBundles(Bnd.Converter.clauseListConverter.convert(list.mkString(","))))
    Model.getPropertyRunEE foreach (ee ⇒ model.setEE(EE.parse(ee)))
    Model.getPropertyRunFramework foreach (model.setRunFramework)
    Model.getPropertyRunFW foreach (model.setRunFw)
    //bndEditModel.setRunProperties(Map<String,String> props)

    // DANGER: if runRepos is List() than BndrunResolveContext drop all known repos, if null - accept all known repos. Funny.
    // DON'T DO IT: osgiBndRunRepos in arg.thisOSGiScope get arg.extracted.structure.data foreach (list => model.setRunRepos(list))

    //bndEditModel.setRunRequires(List<Requirement> requires)
    osgiBndRunVM in arg.thisOSGiScope get arg.extracted.structure.data foreach (model.setRunVMArgs)
    // BROKEN by origin
    //osgiBndSources in thisScope get extracted.structure.data foreach (b => model.setIncludeSources(Boolean.box(b)))
    //bndEditModel.setOutputFile(String name)
    //bndEditModel.setServiceComponents(List< ? extends ServiceComponent> components)
    osgiBndSub in arg.thisOSGiScope get arg.extracted.structure.data foreach (list ⇒ model.setSubBndFiles(list))
    // bndEditModel.setSystemPackages(List< ? extends ExportedPackage> packages)
    osgiBndTestCases in arg.thisOSGiScope get arg.extracted.structure.data foreach (list ⇒ model.setTestSuites(list))
    model
  }
  /** Create Bnd workspace */
  def createWorkspace(plugins: Seq[Workspace ⇒ BndPlugin]): Workspace = { // getWorkspace
    val workspace = new Workspace(home)
    val project = workspace.getProject(Bnd.defaultProjectName)
    if (project == null)
      throw new OSGiManagerException("Unable to create Bnd project")
    // add plugins
    plugins.foreach(f ⇒ workspace.addBasicPlugin(f(workspace)))
    // Initialize projects in synchronized block
    workspace.getBuildOrder()
    workspace
  }
}

object Bnd {
  @volatile private var cnfProjects = Seq[WeakReference[Project]]()
  /** Name of the default Bnd project */
  val defaultProjectName = "default"
  /** Name of the default Bnd properties file */
  val defaultPropertiesFileName = aQute.bnd.build.Project.BNDFILE
  lazy val settings = inConfig(OSGiConf)(Seq(
    osgiFetchInfo := action.Fetch.defaultFetchInfo,
    osgiBndDirectory <<= (osgiDirectory) { _ / "bnd" },
    osgiBndBuildPath := List[String](),
    osgiBndBundleActivator := "",
    osgiBndBundleActivationPolicy := "",
    osgiBndBundleCategory := List[String](), // http://www.osgi.org/Specifications/Reference#categories
    osgiBndBundleContactAddress := "",
    osgiBndBundleCopyright := "",
    osgiBndBundleDescription <<= description in This,
    osgiBndBundleDocURL := "",
    osgiBndBundleIcon := List[String](),
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
    osgiBndNoUses := false,
    osgiBndPluginPath := List[String](),
    osgiBndPrivatePackage := List[String](),
    osgiBndRequireBundle := List[String](),
    osgiBndRunBundles := List[String](),
    osgiBndRunEE := "JavaSE-1.6",
    osgiBndRunFramework := "",
    osgiBndRunFW := "org.eclipse.osgi", // "org.apache.felix.framework"
    osgiBndRunProperties := "",
    osgiBndRunVM := "",
    osgiBndSub := List[String](),
    osgiBndServiceComponent := "",
    osgiBndSources := false,
    osgiBndTestCases := List[String]()))
  /*
   *  For example to resolve for Win32/x86 set the following in your bndrun:
   *
   * -runsystemcapabilities: osgi.native; osgi.native.osname=Win32;
   * osgi.native.processor=x86
   *
   * Alternatively if you want to resolve for the same platform that you
   * are currently running Bndtools on, use the ${native_capability} macro:
   *
   * -runsystemcapabilities: ${native_capability}
   *
   */

  /** Get an exists or create a new instance for the cnf project */
  def get(cnf: File = null)(implicit arg: Plugin.TaskArgument): Bnd = {
    val home = if (cnf != null) cnf else getHome()
    cnfProjects find (_.get.exists(_.cnfLocation == home)) flatMap (_.get.map(_.bnd)) getOrElse {
      val bndInstance = Project(home, new Bnd(home))
      cnfProjects = cnfProjects :+ new WeakReference(bndInstance)
      bndInstance.bnd
    }
  }
  /** Path to Bnd cnf directory */
  def getHome()(implicit arg: Plugin.TaskArgument) =
    Model.getSettingsBndDirectory getOrThrow "osgiBndDirectory is undefined"
  /** Path to Bnd build directory */
  def getBndBuild(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, Workspace.BNDDIR) else new File(getHome, Workspace.BNDDIR)
  /** Path to Bnd cnf directory */
  def getBndCnf(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, Workspace.CNFDIR) else new File(getHome, Workspace.CNFDIR)
  /** Path to Bnd cache directory */
  def getBndCache(buildDirectory: File = null)(implicit arg: Plugin.TaskArgument) =
    if (buildDirectory != null) new File(buildDirectory, Workspace.CACHEDIR) else new File(getBndBuild(), Workspace.CACHEDIR)
  /** Path to Bnd build.bnd file */
  def getBndBuildFile(buildDirectory: File = null)(implicit arg: Plugin.TaskArgument) =
    if (buildDirectory != null) new File(buildDirectory, Workspace.BUILDFILE) else new File(getBndBuild(), Workspace.BUILDFILE)
  /** Path to Bnd directory with default project */
  def getBndDefaultProjectLocation(home: File = null)(implicit arg: Plugin.TaskArgument) =
    if (home != null) new File(home, defaultProjectName) else new File(getHome(), defaultProjectName)
  def getBndDefaultPropertiesFile(projectLocation: File = null)(implicit arg: Plugin.TaskArgument) =
    if (projectLocation != null)
      new File(projectLocation, defaultPropertiesFileName)
    else
      new File(getBndDefaultProjectLocation(), defaultPropertiesFileName)
  /** Prepare Bnd home(workspace) directory */
  def prepareHome()(implicit arg: Plugin.TaskArgument): File = {
    arg.log.debug(logPrefix(arg.name) + "Prepare Bnd home directory.")
    val bndHome = getHome
    //IO.delete(bndHome) I don't want to delete everything
    if (!bndHome.exists())
      if (!bndHome.mkdirs())
        throw new OSGiManagerException("Unable to create osgiBndDirectory: " + bndHome.getAbsolutePath())
    val bndBuild = getBndBuild(bndHome)
    if (!bndBuild.exists())
      if (!bndBuild.mkdirs())
        throw new OSGiManagerException("Unable to create osgiBndDirectory / bnd: " + bndBuild.getAbsolutePath())
    val bndCnf = getBndCnf(bndHome)
    if (!bndCnf.exists())
      if (!bndCnf.mkdirs())
        throw new OSGiManagerException("Unable to create osgiBndDirectory / cnf: " + bndCnf.getAbsolutePath())
    val bndCache = getBndCache(bndBuild)
    if (!bndCache.exists())
      if (!bndCache.mkdirs())
        throw new OSGiManagerException("Unable to create osgiBndDirectory / bnd / cache: " + bndCache.getAbsolutePath())
    val bndBuildFile = getBndBuildFile(bndBuild)
    bndBuildFile.delete // only this
    if (!bndBuildFile.exists())
      if (!bndBuildFile.createNewFile())
        throw new OSGiManagerException("Unable to create osgiBndDirectory / bnd / build.bnd: " + bndBuildFile.getAbsolutePath())
    val bndDefaultProjectLocation = getBndDefaultProjectLocation(bndHome)
    if (!bndDefaultProjectLocation.exists())
      if (!bndDefaultProjectLocation.mkdirs())
        throw new OSGiManagerException("Unable to create osgiBndDirectory / bnd / default: " + bndDefaultProjectLocation.getAbsolutePath())
    val bndDefaultPropertiesFile = getBndDefaultPropertiesFile(bndDefaultProjectLocation)
    bndDefaultPropertiesFile.delete // and that
    if (!bndDefaultPropertiesFile.exists())
      if (!bndDefaultPropertiesFile.createNewFile())
        throw new OSGiManagerException("Unable to create osgiBndDirectory / bnd / default / bnd.bnd: " +
          bndDefaultPropertiesFile.getAbsolutePath())
    bndHome
  }
  /** Show the project bundle properties */
  def show()(implicit arg: Plugin.TaskArgument): Unit = synchronized {
    arg.log.info("OSGi properties of %s/%s".format(arg.name, arg.thisProjectRef.project))
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
    show("BUNDLE_SYMBOLICNAME singleton", Model.getPropertySymbolicNameSingleton.map(_.toString) getOrElse "false", "false", scala.Console.WHITE)
    show("BUNDLE_UPDATELOCATION", Model.getPropertyUpdateLocation getOrElse "")
    show("BUNDLE_VENDOR", Model.getPropertyVendor getOrElse "")
    show("BUNDLE_VERSION", Model.getPropertyVersion getOrElse "", "UNKNOWN", scala.Console.RED)
    show("FRAGMENT_HOST", Model.getPropertyFragmentHost getOrElse "")
    show("CLASSPATH", Model.getPropertyClassPath map (_.mkString(",")) getOrElse "")
    show("DYNAMICIMPORT_PACKAGE", Model.getPropertyDynamicImport getOrElse "")
    show("EXPORT_PACKAGE", Model.getPropertyExportPackages map (_.mkString(",")) getOrElse "")
    show("IMPORT_PACKAGE", Model.getPropertyImportPackages map (_.mkString(",")) getOrElse "")
    show("NOUSES", Model.getPropertyNoUses.map(_.toString) getOrElse "false", "false")
    show("PLUGIN", Model.getPropertyPlugin map (_.mkString(",")) getOrElse "")
    show("PLUGINPATH", Model.getPropertyPluginPath map (_.mkString(",")) getOrElse "")
    show("PRIVATE_PACKAGE", Model.getPropertyPrivatePackages map (_.mkString(",")) getOrElse "")
    show("REQUIRE_BUNDLE", Model.getPropertyRequireBundle map (_.mkString(",")) getOrElse "")
    show("RUNBUNDLES", Model.getPropertyRunBundles map (_.mkString(",")) getOrElse "")
    show("RUNEE", Model.getPropertyRunEE getOrElse "", "UNKNOWN", scala.Console.RED)
    show("RUNFRAMEWORK", Model.getPropertyRunFramework getOrElse "")
    show("RUNFW", Model.getPropertyRunFW getOrElse "", "UNKNOWN", scala.Console.RED)
    /*show("RUNPROPERTIES", Option(model.getRunProperties()).map(_.mkString(",")).getOrElse(""))
    show("RUNVM", model.getRunVMArgs())
    show("SUB", Option(model.getSubBndFiles()).map(_.mkString(",")).getOrElse(""))
    show("SOURCES", Option(model.isIncludeSources()).map(Boolean.box).getOrElse(""))
    show("TESTCASES", Option(model.getTestSuites()).map(_.mkString(",")).getOrElse(""))*/
    Thread.sleep(25) // synchronized and sleep are prevent from mix output with multiple projects
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
      case Some(onEmpty) ⇒
        if (Option(value).isEmpty || value.toString.trim.isEmpty) {
          if (arg.log.ansiCodesSupported)
            key + ": " + onEmpty._1 + onEmpty._2 + scala.Console.RESET
          else
            key + ": " + onEmpty._2
        } else
          key + ": " + value.toString()
      case None ⇒
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
