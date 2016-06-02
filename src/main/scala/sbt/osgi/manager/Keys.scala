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

package sbt.osgi.manager

import aQute.bnd.osgi.Analyzer
import java.util.Properties
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration
import sbt.{ Configuration, ModuleID, SettingKey, TaskKey, config }

object Keys {
  def OSGiConf = config("osgi").hide
  def OSGiTestConf = config("osgiTest").hide

  lazy val osgiDirectory = SettingKey[java.io.File]("osgiDirectory", "Root directory with temporary OSGi data")
  lazy val osgiFetchPath = SettingKey[Option[java.io.File]]("osgiFetchPath", "Location for 'fetch' task bundles")
  lazy val osgiFetchInfo = SettingKey[(Option[ModuleID], String, Analyzer, Plugin.TaskArgument) ⇒ Unit]("osgiFetchInfo", "Fn(x) that passes infromation about the bundle to the analyzer.")

  // Tasks

  lazy val osgiCompile = TaskKey[Unit]("osgiCompile", "Compile source code and generate bundle manifest.") // global
  lazy val osgiFetch = TaskKey[Unit]("osgiFetch", "Fetch all depencencies as bundles to the specific directory.") // global
  lazy val osgiShow = TaskKey[Unit]("osgiShow", "Show the bundle properties") // global
  lazy val osgiBndPrepareHome = TaskKey[java.io.File]("osgiPrepareBndHome", "Prepare Bnd home directory")
  lazy val osgiMavenPrepareHome = TaskKey[java.io.File]("osgiPrepareMavenHome", "Prepare Maven home directory")
  lazy val osgiPluginInfo = TaskKey[Unit]("osgiPluginInfo", "Show plugin information.")

  /////////////////////////////////////
  // Bnd
  // For more information about osgiBnd arguments please look at aQute.bnd.help.Syntax

  lazy val osgiBndBuildPath = SettingKey[List[String]]("osgiBndBuildPath",
    "Bnd BUILDPATH parameter. Provides the class path for building the jar. The entries are references to the repository.")
  lazy val osgiBndBundleActivator = SettingKey[String]("osgiBndBundleActivator",
    "Bnd BUNDLE_ACTIVATOR parameter. Bundle-Activator is a value which specifies a class, implementing the org.osgi.framework.BundleActivator interface, which will be called at bundle activation time and deactivation time.")
  lazy val osgiBndBundleActivationPolicy = SettingKey[String]("osgiBndBundleActivationPolicy",
    "Bnd BUNDLE_ACTIVATIONPOLICY parameter. The Bundle-ActivationPolicy is a marker to tell the OSGi runtime whether this bundle should be activated (i.e. run its Bundle-Activator).")
  lazy val osgiBndBundleCategory = SettingKey[List[String]]("osgiBndBundleCategory",
    "Bnd BUNDLE_CATEGORY parameter. The purpose of the Bundle-Category is to allow bundles to be listed in different categories.")
  lazy val osgiBndBundleContactAddress = SettingKey[String]("osgiBndBundleContactAddress",
    "Bnd BUNDLE_CONTACTADDRESS parameter. The Bundle-ContactAddress header is used to store a humanReadable physical address of where to find out more information about the bundle.")
  lazy val osgiBndBundleCopyright = SettingKey[String]("osgiBndBundleCopyright",
    "Bnd BUNDLE_COPYRIGHT parameter. The Bundle-Copyright may be used to store some text indicating the copyright owner of the bundle.")
  lazy val osgiBndBundleDescription = SettingKey[String]("osgiBndBundleDescription",
    "Bnd BUNDLE_DESCRIPTION parameter. The Bundle-Description header allows you to provide a humanReadable textual description of the bundle.")
  lazy val osgiBndBundleDocURL = SettingKey[String]("osgiBndBundleDocURL",
    "Bnd BUNDLE_DOCURL parameter. The Bundle-DocURL is a textual header, which can contain a URL (typically a website) that the user can find more information about the bundle.")
  lazy val osgiBndBundleDynamicImport = SettingKey[String]("osgiBndBundleDynamicImport",
    "Bnd DYNAMICIMPORT_PACKAGE parameter. DynamicImport-Package is not widely used. Its purpose is to allow a bundle to be wired up to packages that may not be known about in advance.")
  lazy val osgiBndBundleFragmentHost = SettingKey[String]("osgiBndBundleFragmentHost",
    "Bnd FRAGMENT_HOST parameter. Declares this bundle to be a Fragment, and specifies which parent bundle to attach to.")
  lazy val osgiBndBundleIcon = SettingKey[List[String]]("osgiBndBundleIcon",
    "Bnd BUNDLE_ICON parameter. The Bundle-Icon is a list of URLs which contain icons to be used as the bundle's representation.")
  lazy val osgiBndBundleLicense = SettingKey[String]("osgiBndBundleLicense",
    "Bnd BUNDLE_LICENSE parameter. The Bundle-License is an identifier which can record which license(s) the bundle is made available under.")
  lazy val osgiBndBundleName = SettingKey[String]("osgiBndBundleName",
    "Bnd BUNDLE_NAME parameter. The Bundle-Name is a textual identifier for the bundle.")
  lazy val osgiBndBundleSymbolicName = SettingKey[String]("osgiBndBundleSymbolicName",
    "Bnd BUNDLE_SYMBOLICNAME parameter. The Bundle-SymbolicName header is used together with Bundle-Version to uniquely identify a bundle in an OSGi runtime.")
  lazy val osgiBndBundleSymbolicNameSingleton = SettingKey[Boolean]("osgiBndBundleSymbolicNameSingleton",
    "Bnd BUNDLE_SYMBOLICNAME parameter extension. The Directive indicating whether this bundle is a singleton, and there should be only one bundle with this name in the framework at once..")
  lazy val osgiBndBundleUpdateLocation = SettingKey[String]("osgiBndBundleUpdateLocation",
    "Bnd BUNDLE_UPDATELOCATION parameter. The Bundle-UpdateLocation specifies where any updates to this bundle should be loaded from.")
  lazy val osgiBndBundleVendor = SettingKey[String]("osgiBndBundleVendor",
    "Bnd BUNDLE_VENDOR parameter. Bundle-Vendor contains a humanReadable textual description string which identifies the vendor of the bundle.")
  lazy val osgiBndBundleVersion = SettingKey[String]("osgiBndBundleVersion",
    "Bnd BUNDLE_VERSION parameter. The Bundle-Version specifies the version of this bundle, in major.minor.micro.qualifier format.")
  lazy val osgiBndClassPath = SettingKey[List[String]]("osgiBndClasspath",
    "Bnd CLASSPATH parameter. The BUNDLE_CLASSPATH header defines a commaSeparated list of JAR file path names or directories (inside the bundle) containing classes and resources. The period (’.’) specifies the root directory of the bundle’s JAR. The period is also the default.")
  lazy val osgiBndExportPackage = SettingKey[List[String]]("osgiBndExportPackage",
    "Bnd EXPORT_PACKAGE parameter. Bundles may export zero or more packages from the JAR to be consumable by other bundles.")
  lazy val osgiBndIgnorePackage = SettingKey[List[String]]("osgiBndIgnorePackage",
    "Bnd IGNORE_PACKAGE parameter. Tells which Java packages will ignored. To ignore packages you should use the negated syntax i.e. !com.foo, com.*. The negated package must come before its wildcard.")
  lazy val osgiBndImportPackage = SettingKey[List[String]]("osgiBndImportPackage",
    "Bnd IMPORT_PACKAGE parameter. The Import-Package header is used to declare dependencies at a package level from the bundle.")
  lazy val osgiBndNoUses = SettingKey[Boolean]("osgiBndNoUses",
    "Bnd NOUSES parameter extension. Do not calculate the uses: directive.")
  lazy val osgiBndPlugin = SettingKey[List[String]]("osgiBndPlugin",
    "Bnd PLUGIN parameter. Define the plugins.")
  lazy val osgiBndPluginPath = SettingKey[List[String]]("osgiBndPluginPath",
    "Bnd PLUGINPATH parameter. Path to plugins jar.")
  lazy val osgiBndPrivatePackage = SettingKey[List[String]]("osgiBndPrivatePackage",
    "Bnd PRIVATE_PACKAGE parameter. Private-Package defines Java packages to be included in the bundle but not exported.")
  lazy val osgiBndRequireBundle = SettingKey[List[String]]("osgiBndRequireBundle",
    "Bnd REQUIRE_BUNDLE parameter. The Require-Bundle header is used to express a dependency on a bundle's exports by reference to its symbolic name instead of via specific packages.")
  lazy val osgiBndRequireCapability = SettingKey[String]("osgiBndRequireCapability",
    "Bnd REQUIRE_CAPABILITY parameter. Manifest header identifying the capabilities on which the bundle depends.")
  lazy val osgiBndRunBundles = SettingKey[List[String]]("osgiBndRunBundles",
    "Bnd RUNBUNDLES parameter. Add additional bundles, specified with their bsn and version like in BUILDPATH, that are started before the project is run.")
  lazy val osgiBndRunEE = SettingKey[String]("osgiBndRunEE",
    "Bnd RUNEE parameter. ??? JavaSE-1.6")
  lazy val osgiBndRunFramework = SettingKey[String]("osgiBndRunFramework",
    "Bnd RUNFRAMEWORK parameter. ??? deprecated ???")
  lazy val osgiBndRunFW = SettingKey[String]("osgiBndRunFW",
    "Bnd RUNFW parameter. The OSGi framework identifier like org.eclipse.osgi;version='[3.9.0,3.9.0]' or org.apache.felix.framework;version='[4,5)'.")
  lazy val osgiBndRunProperties = SettingKey[String]("osgiBndRunProperties",
    "Bnd RUNPROPERTIES parameter. Properties that are set as system properties before the framework is started.")
  // DANGER: if RUNREPOS is List() than BndrunResolveContext drop all known repos, if null - accept all known repos. Funny.
  // SKIP RUNREPOS parameter. It provides a filter for active Bnd repositories.
  // SKIP RUNREQUIRES parameter. It provides a list of requirements for resolution process.
  lazy val osgiBndRunVM = SettingKey[String]("osgiBndRunVM",
    "Bnd RUNVM parameter. Additional arguments for the VM invokation. Keys that start with a - are added as options, otherwise they are treated as -D properties for the VM.")
  lazy val osgiBndSub = SettingKey[List[String]]("osgiBndSub",
    "Bnd SUB parameter. Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.")
  lazy val osgiBndServiceComponent = SettingKey[String]("osgiBndServiceComponent",
    "Bnd SERVICE_COMPONENT parameter. The header for Declarative Services.")
  lazy val osgiBndSources = SettingKey[Boolean]("osgiBndSources",
    "Bnd SOURCES parameter. Include sources in the jar.")
  lazy val osgiBndTestCases = SettingKey[List[String]]("osgiBndTestCases",
    "Bnd TESTCASES parameter. Declare the tests in the manifest with the Test-Cases header")

  // Various Bndtool settings

  lazy val osgiBndDirectory = SettingKey[java.io.File]("osgiBndDirectory", "Bnd directory")

  /////////////////////////////////////
  // Maven
  //

  lazy val osgiMavenDirectory = SettingKey[java.io.File]("osgiMavenDirectory", "Maven home directory")
  lazy val osgiMavenGlobalSettingsXML = SettingKey[Option[java.io.File]]("osgiMavenGlobalXML", "MAVEN")
  lazy val osgiMavenIsOffline = SettingKey[Boolean]("osgiMavenIsOffline", "MAVEN")
  lazy val osgiMavenIsUpdateSnapshots = SettingKey[Boolean]("osgiMavenIsUpdateSnapshots", "MAVEN")
  lazy val osgiMavenPlexusXML = SettingKey[java.net.URL]("osgiMavenPlexusXML", "MAVEN")
  lazy val osgiMavenSystemProperties = SettingKey[Properties]("osgiMavenUserProperties", "Maven system properties")
  lazy val osgiMavenUserSettingsXML = SettingKey[Option[java.io.File]]("osgiMavenUserXML", "MAVEN")
  lazy val osgiMavenUserHome = SettingKey[java.io.File]("osgiMavenUserDirectory", "Directory that contains '.m2'")
  lazy val osgiMavenUserProperties = SettingKey[Properties]("osgiMavenUserProperties", "Maven user properties")
  lazy val osgiTychoExecutionEnvironmentConfiguration = SettingKey[ExecutionEnvironmentConfiguration]("osgiTychoExecutionEnvironmentConfiguration", "Tycho execution environment configuration. There are few predefined at sbt.osgi.manager.Environment.Execution")
  lazy val osgiTychoTarget = SettingKey[Seq[(Environment.OS, Environment.WS, Environment.ARCH)]]("osgiTychoTarget", "Tycho resolution target")
}
