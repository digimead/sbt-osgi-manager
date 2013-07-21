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

import java.util.Properties
import sbt._
import aQute.bnd.osgi.Analyzer

object Keys {
  def OSGiConf = config("osgi") hide
  def OSGiTestConf = config("osgi-test") hide

  lazy val osgiDirectory = SettingKey[java.io.File]("osgi-directory", "Root directory with temporary OSGi data")
  lazy val osgiFetchPath = SettingKey[Option[java.io.File]]("osgi-fetch-path", "Location for 'fetch' task bundles")
  lazy val osgiFetchInfo = SettingKey[(Option[ModuleID], String, Analyzer, Plugin.TaskArgument) => Unit]("osgi-fetch-info", "Fn(x) that passes infromation about the bundle to the analyzer.")

  // Tasks

  lazy val osgiCompile = TaskKey[Unit]("osgi-compile", "Compile source code and generate bundle manifest.") // global
  lazy val osgiFetch = TaskKey[Unit]("osgi-fetch", "Fetch all depencencies as bundles to the specific directory.") // global
  lazy val osgiShow = TaskKey[Unit]("osgi-show", "Show the bundle properties") // global
  lazy val osgiBndPrepareHome = TaskKey[java.io.File]("osgi-prepare-bnd-home", "Prepare Bnd home directory")
  lazy val osgiMavenPrepareHome = TaskKey[java.io.File]("osgi-prepare-maven-home", "Prepare Maven home directory")
  lazy val osgiPluginInfo = TaskKey[Unit]("osgi-plugin-info", "Show plugin information.")
  lazy val osgiResetCache = TaskKey[Unit]("osgi-reset-cache", "Reset plugin cache(s)")

  /////////////////////////////////////
  // Bnd
  // For more information about osgi-bnd- arguments please look at aQute.bnd.help.Syntax

  lazy val osgiBndBuildPath = SettingKey[List[String]]("osgi-bnd-buildpath",
    "Bnd BUILDPATH parameter. Provides the class path for building the jar. The entries are references to the repository.")
  lazy val osgiBndBundleActivator = SettingKey[String]("osgi-bnd-bundle-activator",
    "Bnd BUNDLE_ACTIVATOR parameter. Bundle-Activator is a value which specifies a class, implementing the org.osgi.framework.BundleActivator interface, which will be called at bundle activation time and deactivation time.")
  lazy val osgiBndBundleActivationPolicy = SettingKey[String]("osgi-bnd-bundle-activation-policy",
    "Bnd BUNDLE_ACTIVATIONPOLICY parameter. The Bundle-ActivationPolicy is a marker to tell the OSGi runtime whether this bundle should be activated (i.e. run its Bundle-Activator).")
  lazy val osgiBndBundleCategory = SettingKey[List[String]]("osgi-bnd-bundle-category",
    "Bnd BUNDLE_CATEGORY parameter. The purpose of the Bundle-Category is to allow bundles to be listed in different categories.")
  lazy val osgiBndBundleContactAddress = SettingKey[String]("osgi-bnd-bundle-contactaddress",
    "Bnd BUNDLE_CONTACTADDRESS parameter. The Bundle-ContactAddress header is used to store a human-readable physical address of where to find out more information about the bundle.")
  lazy val osgiBndBundleCopyright = SettingKey[String]("osgi-bnd-bundle-copyright",
    "Bnd BUNDLE_COPYRIGHT parameter. The Bundle-Copyright may be used to store some text indicating the copyright owner of the bundle.")
  lazy val osgiBndBundleDescription = SettingKey[String]("osgi-bnd-bundle-description",
    "Bnd BUNDLE_DESCRIPTION parameter. The Bundle-Description header allows you to provide a human-readable textual description of the bundle.")
  lazy val osgiBndBundleDocURL = SettingKey[String]("osgi-bnd-bundle-docurl",
    "Bnd BUNDLE_DOCURL parameter. The Bundle-DocURL is a textual header, which can contain a URL (typically a website) that the user can find more information about the bundle.")
  lazy val osgiBndBundleDynamicImport = SettingKey[String]("osgi-bnd-bundle-dynamicimport",
    "Bnd DYNAMICIMPORT_PACKAGE parameter. DynamicImport-Package is not widely used. Its purpose is to allow a bundle to be wired up to packages that may not be known about in advance.")
  lazy val osgiBndBundleFragmentHost = SettingKey[String]("osgi-bnd-bundle-fragmenthost",
    "Bnd FRAGMENT_HOST parameter. Declares this bundle to be a Fragment, and specifies which parent bundle to attach to.")
  lazy val osgiBndBundleIcon = SettingKey[List[String]]("osgi-bnd-bundle-icon",
    "Bnd BUNDLE_ICON parameter. The Bundle-Icon is a list of URLs which contain icons to be used as the bundle's representation.")
  lazy val osgiBndBundleLicense = SettingKey[String]("osgi-bnd-bundle-license",
    "Bnd BUNDLE_LICENSE parameter. The Bundle-License is an identifier which can record which license(s) the bundle is made available under.")
  lazy val osgiBndBundleName = SettingKey[String]("osgi-bnd-bundle-name",
    "Bnd BUNDLE_NAME parameter. The Bundle-Name is a textual identifier for the bundle.")
  lazy val osgiBndBundleSymbolicName = SettingKey[String]("osgi-bnd-bundle-symbolicname",
    "Bnd BUNDLE_SYMBOLICNAME parameter. The Bundle-SymbolicName header is used together with Bundle-Version to uniquely identify a bundle in an OSGi runtime.")
  lazy val osgiBndBundleSymbolicNameSingleton = SettingKey[Boolean]("osgi-bnd-bundle-symbolicname-singleton",
    "Bnd BUNDLE_SYMBOLICNAME parameter extension. The Directive indicating whether this bundle is a singleton, and there should be only one bundle with this name in the framework at once..")
  lazy val osgiBndBundleUpdateLocation = SettingKey[String]("osgi-bnd-bundle-updatelocation",
    "Bnd BUNDLE_UPDATELOCATION parameter. The Bundle-UpdateLocation specifies where any updates to this bundle should be loaded from.")
  lazy val osgiBndBundleVendor = SettingKey[String]("osgi-bnd-bundle-vendor",
    "Bnd BUNDLE_VENDOR parameter. Bundle-Vendor contains a human-readable textual description string which identifies the vendor of the bundle.")
  lazy val osgiBndBundleVersion = SettingKey[String]("osgi-bnd-bundle-version",
    "Bnd BUNDLE_VERSION parameter. The Bundle-Version specifies the version of this bundle, in major.minor.micro.qualifier format.")
  lazy val osgiBndClassPath = SettingKey[List[String]]("osgi-bnd-classpath",
    "Bnd CLASSPATH parameter. The BUNDLE_CLASSPATH header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The period (’.’) specifies the root directory of the bundle’s JAR. The period is also the default.")
  lazy val osgiBndExportPackage = SettingKey[List[String]]("osgi-bnd-export-package",
    "Bnd EXPORT_PACKAGE parameter. Bundles may export zero or more packages from the JAR to be consumable by other bundles.")
  lazy val osgiBndIgnorePackage = SettingKey[List[String]]("osgi-bnd-ignore-package",
    "Bnd IGNORE_PACKAGE parameter. Tells which Java packages will ignored. To ignore packages you should use the negated syntax i.e. !com.foo, com.*. The negated package must come before its wildcard.")
  lazy val osgiBndImportPackage = SettingKey[List[String]]("osgi-bnd-import-package",
    "Bnd IMPORT_PACKAGE parameter. The Import-Package header is used to declare dependencies at a package level from the bundle.")
  lazy val osgiBndPlugin = SettingKey[List[String]]("osgi-bnd-plugin",
    "Bnd PLUGIN parameter. Define the plugins.")
  lazy val osgiBndPluginPath = SettingKey[List[String]]("osgi-bnd-pluginpath",
    "Bnd PLUGINPATH parameter. Path to plugins jar.")
  lazy val osgiBndPrivatePackage = SettingKey[List[String]]("osgi-bnd-private-package",
    "Bnd PRIVATE_PACKAGE parameter. Private-Package defines Java packages to be included in the bundle but not exported.")
  lazy val osgiBndRequireBundle = SettingKey[List[String]]("osgi-bnd-require-bundle",
    "Bnd REQUIRE_BUNDLE parameter. The Require-Bundle header is used to express a dependency on a bundle's exports by reference to its symbolic name instead of via specific packages.")
  lazy val osgiBndRunBundles = SettingKey[List[String]]("osgi-bnd-runbundles",
    "Bnd RUNBUNDLES parameter. Add additional bundles, specified with their bsn and version like in BUILDPATH, that are started before the project is run.")
  lazy val osgiBndRunEE = SettingKey[String]("osgi-bnd-runee",
    "Bnd RUNEE parameter. ??? JavaSE-1.6")
  lazy val osgiBndRunFramework = SettingKey[String]("osgi-bnd-runframework",
    "Bnd RUNFRAMEWORK parameter. ??? deprecated ???")
  lazy val osgiBndRunFW = SettingKey[String]("osgi-bnd-runfw",
    "Bnd RUNFW parameter. The OSGi framework identifier like org.eclipse.osgi;version='[3.9.0,3.9.0]' or org.apache.felix.framework;version='[4,5)'.")
  lazy val osgiBndRunProperties = SettingKey[String]("osgi-bnd-runproperties",
    "Bnd RUNPROPERTIES parameter. Properties that are set as system properties before the framework is started.")
  // DANGER: if RUNREPOS is List() than BndrunResolveContext drop all known repos, if null - accept all known repos. Funny.
  // SKIP RUNREPOS parameter. It provides a filter for active Bnd repositories.
  // SKIP RUNREQUIRES parameter. It provides a list of requirements for resolution process.
  lazy val osgiBndRunVM = SettingKey[String]("osgi-bnd-runvm",
    "Bnd RUNVM parameter. Additional arguments for the VM invokation. Keys that start with a - are added as options, otherwise they are treated as -D properties for the VM.")
  lazy val osgiBndSub = SettingKey[List[String]]("osgi-bnd-sub",
    "Bnd SUB parameter. Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.")
  lazy val osgiBndServiceComponent = SettingKey[String]("osgi-bnd-service-component",
    "Bnd SERVICE_COMPONENT parameter. The header for Declarative Services.")
  lazy val osgiBndSources = SettingKey[Boolean]("osgi-bnd-sources",
    "Bnd SOURCES parameter. Include sources in the jar.")
  lazy val osgiBndTestCases = SettingKey[List[String]]("osgi-bnd-testcases",
    "Bnd TESTCASES parameter. Declare the tests in the manifest with the Test-Cases header")

  // Various Bndtool settings

  lazy val osgiBndDirectory = SettingKey[java.io.File]("osgi-bnd-directory", "Bnd directory")

  /////////////////////////////////////
  // Maven
  //

  lazy val osgiMavenDirectory = SettingKey[java.io.File]("osgi-maven-directory", "Maven home directory")
  lazy val osgiMavenGlobalSettingsXML = SettingKey[Option[java.io.File]]("osgi-maven-global-xml", "MAVEN")
  lazy val osgiMavenIsOffline = SettingKey[Boolean]("osgi-maven-is-offline", "MAVEN")
  lazy val osgiMavenIsUpdateSnapshots = SettingKey[Boolean]("osgi-maven-is-updatesnapshots", "MAVEN")
  lazy val osgiMavenPlexusXML = SettingKey[java.net.URL]("osgi-maven-plexus-xml", "MAVEN")
  lazy val osgiMavenSystemProperties = SettingKey[Properties]("osgi-maven-user-properties", "Maven system properties")
  lazy val osgiMavenUserSettingsXML = SettingKey[Option[java.io.File]]("osgi-maven-user-xml", "MAVEN")
  lazy val osgiMavenUserHome = SettingKey[java.io.File]("osgi-maven-user-directory", "Directory that contains '.m2'")
  lazy val osgiMavenUserProperties = SettingKey[Properties]("osgi-maven-user-properties", "Maven user properties")
}
