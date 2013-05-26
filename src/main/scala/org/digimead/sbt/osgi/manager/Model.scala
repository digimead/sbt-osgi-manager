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

package org.digimead.sbt.osgi.manager

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
import org.digimead.sbt.osgi.manager.Keys._
import org.digimead.sbt.osgi.manager.Support._

object Model {
  /////////////////////////////////////
  // Bnd properties
  //
  /** aQute.bnd.osgi.Constants.BUNDLE_ACTIVATOR */
  def getPropertyActivator(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleActivator in arg.thisOSGiScope get arg.extracted.structure.data
  def getPropertyActivationPolicy(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleActivationPolicy in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_CATEGORY */
  def getPropertyCategory(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleCategory in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_CONTACTADDRESS */
  def getPropertyContactAddress(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleContactAddress in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_COPYRIGHT */
  def getPropertyCopyright(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleCopyright in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_DESCRIPTION */
  def getPropertyDescription(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleDescription in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_DOCURL */
  def getPropertyDocUrl(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleDocURL in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.DYNAMICIMPORT_PACKAGE */
  def getPropertyDynamicImport(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleDynamicImport in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.FRAGMENT_HOST */
  def getPropertyFragmentHost(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleFragmentHost in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_ICON */
  def getPropertyIcon(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleIcon in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_LICENSE */
  def getPropertyLicense(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleLicense in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_NAME */
  def getPropertyName(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleName in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_SYMBOLICNAME */
  def getPropertySymbolicName(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleSymbolicName in arg.thisOSGiScope get arg.extracted.structure.data
  /** aQute.bnd.osgi.Constants.BUNDLE_SYMBOLICNAME extension */
  def getPropertySymbolicNameSingleton(implicit arg: Plugin.TaskArgument) =
    osgiBndBundleSymbolicNameSingleton in arg.thisOSGiScope get arg.extracted.structure.data
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
  def getPropertyExportPackages(implicit arg: Plugin.TaskArgument) =
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

  def getSettingsBndDirectory(implicit arg: Plugin.TaskArgument) =
    osgiBndDirectory in arg.thisOSGiScope get arg.extracted.structure.data

  /////////////////////////////////////
  // Various plugin settings
  //

  def getSettingsDirectory(implicit arg: Plugin.TaskArgument) =
    osgiDirectory in arg.thisOSGiScope get arg.extracted.structure.data
  def getSettingsFetchInfo(implicit arg: Plugin.TaskArgument) =
    osgiFetchInfo in arg.thisOSGiScope get arg.extracted.structure.data

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
}
