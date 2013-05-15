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

object Keys {
  def OSGiConf = config("osgi-manager") extend (Compile)

  lazy val osgiDirectory = SettingKey[java.io.File]("directory", "Root directory with temporary OSGi data")

  // Tasks

  lazy val osgiShow = TaskKey[Unit]("osgi-show-properies", "Show the project bundle properties") // global
  lazy val osgiMavenPrepareHome = TaskKey[java.io.File]("prepare-maven-home", "Prepare maven home directory")
  lazy val osgiResetCache = TaskKey[Unit]("reset-cache", "Reset plugin cache(s)")

  /////////////////////////////////////
  // Bndtool
  //

  // Bunch of Bndtool settings that is available for BndEditModel
  // For more information about osgi-bnd- arguments please look at aQute.bnd.help.Syntax

  lazy val osgiBndBuildPath = SettingKey[List[String]]("bnd-buildpath",
    "Bnd BUILDPATH parameter. Provides the class path for building the jar. The entries are references to the repository.")
  lazy val osgiBndBundleActivator = SettingKey[String]("bnd-bundle-activator",
    "Bnd BUNDLE_ACTIVATOR parameter. Bundle-Activator is a which specifies a class, implementing the org.osgi.framework.BundleActivator interface, which will be called at bundle activation time and deactivation time.")
  lazy val osgiBndBundleCategory = SettingKey[List[String]]("bnd-bundle-category",
    "Bnd BUNDLE_CATEGORY parameter. The purpose of the Bundle-Category is to allow bundles to be listed in different categories.")
  lazy val osgiBndBundleContactAddress = SettingKey[String]("bnd-bundle-contactaddress",
    "Bnd BUNDLE_CONTACTADDRESS parameter. The Bundle-ContactAddress header is used to store a human-readable physical address of where to find out more information about the bundle.")
  lazy val osgiBndBundleCopyright = SettingKey[String]("bnd-bundle-copyright",
    "Bnd BUNDLE_COPYRIGHT parameter. The Bundle-Copyright may be used to store some text indicating the copyright owner of the bundle.")
  lazy val osgiBndBundleDescription = SettingKey[String]("bnd-bundle-description",
    "Bnd BUNDLE_DESCRIPTION parameter. The Bundle-Description header allows you to provide a human-readable textual description of the bundle.")
  lazy val osgiBndBundleDocURL = SettingKey[String]("bnd-bundle-docurl",
    "Bnd BUNDLE_DOCURL parameter. The Bundle-DocURL is a textual header, which can contain a URL (typically a website) that the user can find more information about the bundle.")
  lazy val osgiBndBundleLicense = SettingKey[String]("bnd-bundle-license",
    "Bnd BUNDLE_LICENSE parameter. The Bundle-License is an identifier which can record which license(s) the bundle is made available under.")
  lazy val osgiBndBundleName = SettingKey[String]("bnd-bundle-name",
    "Bnd BUNDLE_NAME parameter. The Bundle-Name is a textual identifier for the bundle.")
  lazy val osgiBndBundleSymbolicName = SettingKey[String]("bnd-bundle-symbolicname",
    "Bnd BUNDLE_SYMBOLICNAME parameter. The Bundle-SymbolicName header is used together with Bundle-Version to uniquely identify a bundle in an OSGi runtime.")
  lazy val osgiBndBundleUpdateLocation = SettingKey[String]("bnd-bundle-updatelocation",
    "Bnd BUNDLE_UPDATELOCATION parameter. The Bundle-UpdateLocation specifies where any updates to this bundle should be loaded from.")
  lazy val osgiBndBundleVendor = SettingKey[String]("bnd-bundle-vendor",
    "Bnd BUNDLE_VENDOR parameter. Bundle-Vendor contains a human-readable textual description string which identifies the vendor of the bundle.")
  lazy val osgiBndBundleVersion = SettingKey[String]("bnd-bundle-version",
    "Bnd BUNDLE_VERSION parameter. The Bundle-Version specifies the version of this bundle, in major.minor.micro.qualifier format.")
  lazy val osgiBndClassPath = SettingKey[List[String]]("bnd-classpath",
    "Bnd CLASSPATH parameter. The BUNDLE_CLASSPATH header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The period (’.’) specifies the root directory of the bundle’s JAR. The period is also the default.")
  lazy val osgiBndExportPackage = SettingKey[List[String]]("bnd-export-package",
    "Bnd EXPORT_PACKAGE parameter. Bundles may export zero or more packages from the JAR to be consumable by other bundles.")
  lazy val osgiBndImportPackage = SettingKey[List[String]]("bnd-import-package",
    "Bnd IMPORT_PACKAGE parameter. The Import-Package header is used to declare dependencies at a package level from the bundle.")
  lazy val osgiBndPlugin = SettingKey[List[String]]("bnd-plugin",
    "Bnd PLUGIN parameter. Define the plugins.")
  lazy val osgiBndPluginPath = SettingKey[List[String]]("bnd-pluginpath",
    "Bnd PLUGINPATH parameter. Path to plugins jar.")
  lazy val osgiBndPrivatePackage = SettingKey[List[String]]("bnd-private-package",
    "Bnd PRIVATE_PACKAGE parameter. Private-Package defines Java packages to be included in the bundle but not exported.")
  lazy val osgiBndRunBundles = SettingKey[List[String]]("bnd-runbundles",
    "Bnd RUNBUNDLES parameter. Add additional bundles, specified with their bsn and version like in BUILDPATH, that are started before the project is run.")
  lazy val osgiBndRunEE = SettingKey[String]("bnd-runee",
    "Bnd RUNEE parameter. ??? JavaSE-1.6")
  lazy val osgiBndRunFramework = SettingKey[String]("bnd-runframework",
    "Bnd RUNFRAMEWORK parameter. ??? deprecated ???")
  lazy val osgiBndRunFW = SettingKey[String]("bnd-runfw",
    "Bnd RUNFW parameter. The OSGi framework identifier like org.eclipse.osgi;version='[3.9.0,3.9.0]' or org.apache.felix.framework;version='[4,5)'.")
  lazy val osgiBndRunProperties = SettingKey[String]("bnd-runproperties",
    "Bnd RUNPROPERTIES parameter. Properties that are set as system properties before the framework is started.")
  lazy val osgiBndRunRepos = SettingKey[List[String]]("bnd-runrepos",
    "Bnd RUNREPOS parameter. ???")
  lazy val osgiBndRunRequires = SettingKey[String]("bnd-runrequires",
    "Bnd RUNREQUIRES parameter. ???")
  lazy val osgiBndRunVM = SettingKey[String]("bnd-runvm",
    "Bnd RUNVM parameter. Additional arguments for the VM invokation. Keys that start with a - are added as options, otherwise they are treated as -D properties for the VM.")
  lazy val osgiBndSub = SettingKey[List[String]]("bnd-sub",
    "Bnd SUB parameter. Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.")
  lazy val osgiBndServiceComponent = SettingKey[String]("bnd-service-component",
    "Bnd SERVICE_COMPONENT parameter. The header for Declarative Services.")
  lazy val osgiBndSources = SettingKey[Boolean]("bnd-sources",
    "Bnd SOURCES parameter. Include sources in the jar.")
  lazy val osgiBndTestCases = SettingKey[List[String]]("bnd-testcases",
    "Bnd TESTCASES parameter. Declare the tests in the manifest with the Test-Cases header")

  // Various Bndtool settings

  lazy val osgiCnfPath = SettingKey[java.io.File]("bnd-cnf-path", "Path to the cnf project that contains a workspace-wide configuration")
  lazy val osgiBndtoolsDirectory = SettingKey[java.io.File]("bnd-directory", "Bndtools directory")

  /////////////////////////////////////
  // Maven
  //

  lazy val osgiMavenDirectory = SettingKey[java.io.File]("maven-directory", "MAVEN")
  lazy val osgiMavenGlobalSettingsXML = SettingKey[Option[java.io.File]]("maven-global-xml", "MAVEN")
  lazy val osgiMavenIsOffline = SettingKey[Boolean]("maven-is-offline", "MAVEN")
  lazy val osgiMavenIsUpdateSnapshots = SettingKey[Boolean]("maven-is-updatesnapshots", "MAVEN")
  lazy val osgiMavenPlexusXML = SettingKey[java.net.URL]("maven-plexus-xml", "MAVEN")
  lazy val osgiMavenSystemProperties = SettingKey[Properties]("maven-user-properties", "Maven system properties")
  lazy val osgiMavenUserSettingsXML = SettingKey[Option[java.io.File]]("maven-user-xml", "MAVEN")
  lazy val osgiMavenUserHome = SettingKey[java.io.File]("maven-user-directory", "Directory that contains '.m2'")
  lazy val osgiMavenUserProperties = SettingKey[Properties]("maven-user-properties", "Maven user properties")
}
