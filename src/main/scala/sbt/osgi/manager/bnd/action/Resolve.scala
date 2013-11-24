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

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.MalformedURLException
import java.net.URI

import scala.collection.JavaConversions._
import scala.collection.immutable

import org.apache.felix.resolver.ResolverImpl
import org.eclipse.equinox.internal.p2.metadata.VersionParser
import org.osgi.framework.namespace.IdentityNamespace
import org.osgi.service.resolver.ResolutionException

import aQute.bnd.build.Workspace
import aQute.bnd.deployer.repository.AbstractIndexedRepo
import aQute.bnd.deployer.repository.FixedIndexedRepo
import aQute.bnd.deployer.repository.api.IRepositoryContentProvider
import aQute.bnd.deployer.repository.providers.R5RepoContentProvider
import aQute.bnd.osgi.resource.CapReqBuilder
import aQute.bnd.service.ResourceHandle
import aQute.bnd.service.Strategy

import biz.aQute.resolve.internal.BndrunResolveContext

import sbt.{ Keys ⇒ skey }
import sbt.osgi.manager.Dependency
import sbt.osgi.manager.Dependency._
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support
import sbt.osgi.manager.Support._
import sbt.osgi.manager.bnd.Bnd
import sbt.osgi.manager.bnd.Logger
import sbt.osgi.manager.Support
import sbt.osgi.manager.bnd.Bnd

import sbt._

// This is consolidated thoughts about Bnd that was located across first version of my code.
// It may save a bit of time for someone who will choose the same way.
//
// http://wiki.osgi.org/wiki/Modularity_Maturity_Model
//   I am absolutely sure, that Bnd originator _don't know anything_ about 3rd level.
//   Read Bnd source code for proof. :-/
//
// Also I suspect that Bnd team has very little understanding of http://en.wikipedia.org/wiki/Extreme_Programming and http://en.wikipedia.org/wiki/Continuous_integration :-(
//
// NOTE 1
//  Unable to extends with FixedIndexedRepo.
//  Scala compiler lost public function that already defined:
//  class FixedIndexedRepository needs to be abstract, since method get in trait RepositoryPlugin of type (x$1: java.lang.String,
//  x$2: aQute.bnd.version.Version, x$3: java.util.Map[java.lang.String,java.lang.String], x$4: <repeated...>[aQute.bnd.service.RepositoryPlugin.DownloadListener])java.io.File is not defined
//
// NOTE 2
//  There are a lot of hairy code in FixedIndexedRepo, AbstractIndexedRepo and so on,
//  I have no reason to break the tradition of Bnd developers.
//  And I remember that my target is the only significant entity
//
// NOTE 3
//  AbstractIndexedRepo.parseLocations(locations) ??? Hahaha, lol... this guy was really addicted. It is a static function... :/
//
// NOTE 4
// I really like public functions that hasn't any descriptions: project._p_dependson(x$1)
// WTF? Now I understand that Bnd spreading was suppressed by aliens, because they wrote things like this ...
//    ... _p_allsourcepath, _p_dependson, _p_output and so on. Arhh pity aliens, I will decode your puzzle anyway ;-)
//
// Absolutely, Bnd code is bit more than junk.
// I must achieve my targets with only those instruments that I have. No one to blame.
//
//    Ezh

/** Resolve interface for SBT via Bnd API */
object Resolve extends Support.Resolve {
  /** Predefined name for OSGi R5 repository with resolved dependencies */
  val internalRepositoryName = "Internal repository with resolved dependencies"
  /** Predefined location for OSGi R5 repository with resolved dependencies */
  val localRepositoryLocation = new URI("file:/")

  /** Resolve the dependency for the specific project against OBR repository */
  def resolveOBR(resolvedDependencies: Seq[File])(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    // get resolvers as Seq[(id, url)]
    val resolvers = getResolvers(Dependency.OBR, arg.thisOSGiScope)
    val dependencies = getDependencies(Dependency.OBR, arg.thisOSGiScope)
    if (resolvers.nonEmpty && dependencies.nonEmpty) {
      arg.log.info(logPrefix(arg.name) + "Resolve OBR dependencies")
      val bridge = Bnd.get()
      val modules = resolveOBR(dependencies, resolvers, bridge, resolvedDependencies)
      val resolved = skey.libraryDependencies in arg.thisScope get arg.extracted.structure.data getOrElse Seq()
      updateCache(CacheOBRKey(arg.thisProjectRef.project), dependencies, resolvers)
      modules.filterNot { m =>
        val alreadyInLibraryDependencies = resolved.exists(_ == m)
        if (alreadyInLibraryDependencies)
          arg.log.debug(logPrefix(arg.name) + "Skip, already in libraryDependencies: " + m)
        alreadyInLibraryDependencies
      }
      modules
    } else {
      arg.log.info(logPrefix(arg.name) + "No OBR dependencies found")
      updateCache(CacheOBRKey(arg.thisProjectRef.project), dependencies, resolvers)
      Seq()
    }
  }
  /** Resolve the dependency against OBR repository */
  def resolveOBR(dependencies: Seq[ModuleID], repositories: Seq[(String, URI)], bnd: Bnd,
    resolvedDependencies: Seq[File])(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    val model = bnd.createModel()
    val log = new Logger(arg.log)
    if (model.getRunFw() == null || model.getRunFw().isEmpty()) {
      arg.log.error(logPrefix(arg.name) + "The OSGi Framework and Execution Environment must be specified for resolution.")
      return Seq()
    }
    if (model.getEE() == null) {
      arg.log.error(logPrefix(arg.name) + "The OSGi Framework and Execution Environment must be specified for resolution.")
      return Seq()
    }
    val requirements = dependencies.map { moduleId =>
      val version = VersionParser.parse(moduleId.revision, 0, moduleId.revision.length())
      CapReqBuilder.createPackageRequirement(moduleId.name, if (version.compareTo(ANY_VERSION) != 0) version else null).
        buildSyntheticRequirement()
    }
    model.setRunRequires(requirements)
    try {
      val workspace = bnd.createWorkspace(Seq())
      val workspaceRepositories = (repositories.map { case (id, location) => aquireRepositoryIndex(id, location, workspace) } :+
        getResolvedDependenciesIndex(resolvedDependencies, workspace)).flatten
      val plugins = workspaceRepositories.foreach(repo => workspace.addBasicPlugin(repo))
      val resolver = new ResolverImpl(log)
      val context = new BndrunResolveContext(model, workspace, log)
      try {
        // Get required bundles
        val resolved = resolver.resolve(context)
        val required = immutable.HashSet({
          for (resource <- resolved.keySet) yield {
            if (!context.isInputRequirementsResource(resource) && !context.isFrameworkResource(resource))
              Some(resource)
            else
              None
          }
        }.toSeq.flatten: _*)
        // Iterate over repositories and looking for matches, collect resources Tuple3(Bundle-SymbolicName, Bundle-Version, File, Repository)
        val resources = for (resource <- required.toSeq) yield workspaceRepositories.flatMap(repository => try {
          Option(resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)).map(_.toList) match {
            case Some(List(identityCapability)) =>
              val bsn = identityCapability.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString
              val version = identityCapability.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE).toString
              // Bundle-SymbolicName and Bundle-Version allow us to find required bundle
              // getHandle method implemented at AbstractIndexedRepo (abstraction level) and don't use properties argument at all.
              // +1 to other Bnd garbage :-/ Brrrr... Slopwork.
              val properties = null
              val handle = try { Option(repository.getHandle(bsn, version, Strategy.EXACT, properties)) } catch { case _: Throwable => None }
              val file = handle.flatMap(handle => try {
                Option(handle.request()) match {
                  // Bnd is required that you set rootURI at index creation and... and...
                  // after that this broken thing set the root to parent directory of the index!!!??? hahaha. fucking developers
                  // There are billion reasons why we need to create an index in different location than original bundles.
                  case Some(file) if file.exists() && file.isFile() =>
                    Option(file)
                  case Some(file) =>
                    repository.getIndexLocations().toList.headOption.flatMap(index => {
                      val artifactFile = file.getCanonicalPath()
                      val indexFile = new File(index).getParentFile().getCanonicalPath()
                      if (artifactFile.startsWith(indexFile)) {
                        val file = new File(artifactFile.substring(indexFile.length()))
                        findFileViaURL(file.toURI().toURL)
                      } else
                        None
                    }) orElse findFileViaResourceHandle(handle)
                  case _ =>
                    findFileViaResourceHandle(handle)
                }
              } catch {
                case e: Throwable =>
                  arg.log.warn(logPrefix(arg.name) + "Unable create ModuleID for %s %s at %s: %s".format(bsn, version, handle.getName(), e))
                  None
              })
              file.map((bsn, version, _, repository))
            case _ =>
              None
          }
        } catch {
          case _: Throwable =>
            arg.log.error(logPrefix(arg.name) + "Unable to process OBR resource %s from repository %s".format(resource, repository.getName()))
            None
        })
        resources.flatten.filter {
          // save only resources which isn't located on our file system previously
          case (bsn, version, file, repository) => !resolvedDependencies.exists(_.getAbsolutePath() == file.getAbsolutePath())
        }.map {
          case (bsn, version, file, repository) =>
            arg.log.info(logPrefix(arg.name) + "Collect OBR bundle %s %s".format(bsn, version))
            arg.log.debug(logPrefix(arg.name) + "%s %s -> [%s] from %s".format(bsn, version, "?", repository.getName()))
            Some(bsn % bsn % version from file.getAbsoluteFile.toURI.toURL.toString)
        }.flatten
      } catch {
        case e: ResolutionException =>
          arg.log.warn(logPrefix(arg.name) + "Unable to resolve: " + e)
          Seq()
      }
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(arg.name) + "Exception during resolution. " + e)
        Seq()
    }
  }
  /** Command that populates libraryDependencies with required bundles */
  def resolveOBRCommand(resolvedDependencies: immutable.HashMap[ProjectRef, Seq[File]] = immutable.HashMap())(implicit arg: Plugin.TaskArgument): immutable.HashMap[ProjectRef, Seq[ModuleID]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    // Check if we already processed our dependencies with same values
    val cached = for (id <- build.defined.keys) yield {
      implicit val projectRef = ProjectRef(uri, id)
      val localArg = arg.copy(thisProjectRef = projectRef)
      isCached(CacheOBRKey(id), getDependencies(Dependency.OBR, localArg.thisOSGiScope)(localArg),
        getResolvers(Dependency.OBR, localArg.thisOSGiScope)(localArg))(localArg)
    }
    if (cached.forall(_ == true)) {
      arg.log.info(logPrefix(arg.name) + "Pass OBR resolution: already resolved")
      immutable.HashMap((for (id <- build.defined.keys) yield {
        val projectRef = ProjectRef(uri, id)
        (projectRef, Seq())
      }).toSeq: _*)
    } else {
      immutable.HashMap((for (id <- build.defined.keys) yield {
        implicit val projectRef = ProjectRef(uri, id)
        (projectRef, resolveOBR(resolvedDependencies.get(projectRef) getOrElse Seq())(arg.copy(thisProjectRef = projectRef)))
      }).toSeq: _*)
    }
  }
  /** Get exists or create new R5 index if user provides file:/directory URI */
  protected def aquireRepositoryIndex(id: String, location: URI, workspace: Workspace)(implicit arg: Plugin.TaskArgument): Option[AbstractIndexedRepo] = {
    val repository = if (location.getScheme() == "file") {
      val local = new File(location)
      if (!local.exists) {
        arg.log.warn(logPrefix(arg.name) + "OBR %s with URI %s not found".format(id, location))
        return None
      }
      if (local.isDirectory()) {
        val jars = IO.listFiles(new FileFilter { def accept(file: File) = file.isDirectory() || file.getName.endsWith(".jar") })(local).filter(!_.isDirectory()).toList
        val project = workspace.getProject(Bnd.defaultProjectName)
        val indexName = "%010X_index.xml".format(jars.map(_.getAbsolutePath()).sorted.hashCode) // Hashcodes for List produce a value from the hashcodes of all the elements of the list
        val index = new File(project.getBase(), indexName).getAbsoluteFile()
        val repository = createR5FixedIndexedRepository(id, index.toURI)
        if (!index.exists() || index.length() == 0)
          repository.getGeneratingProviders.find(_.getName() == R5RepoContentProvider.NAME) match {
            case Some(generatingProviderR5) if generatingProviderR5.supportsGeneration() =>
              generateR5Index(jars.toSet, id, localRepositoryLocation, index, generatingProviderR5, workspace)
            case _ =>
              arg.log.error(logPrefix(arg.name) + "Unable to find R5 generating provider for '%s' with URI %s".format(id, location))
              return None
          }
        else
          arg.log.debug(logPrefix(arg.name) + "Use pregenerated OSGi R5 index '%s' with root URI '%s' at %s".format(id, location, index))
        repository
      } else
        createR5FixedIndexedRepository(id, location) // URI points to file
    } else {
      createR5FixedIndexedRepository(id, location) // unknown schema, we couldn't do much about
    }
    try {
      // LOL! I want to validate repository right now, but... The old story...
      // init() is protected, but getIndexLocations() is public. OK call init via getIndexLocations
      // Bnd has very weak design. The quality of the Bnd source code is much more worth than Scala, SBT, or Eclipse.
      // It is oddly. I expected that OSGi developers must be more accurate than others. Negative.
      repository.getIndexLocations()
      Some(repository)
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(arg.name) + e.getMessage)
        None
    }
  }
  /** Create OSGi R5 fixed indexed repository, but not initialize it */
  protected def createR5FixedIndexedRepository(id: String, index: URI): FixedIndexedRepo = {
    val repository = new FixedIndexedRepo()
    val props = new java.util.HashMap[String, String]()
    props.put(FixedIndexedRepo.PROP_LOCATIONS, index.toString())
    props.put(AbstractIndexedRepo.PROP_NAME, id)
    repository.setProperties(props)
    repository
  }
  /** Returns local file from handle if any */
  protected def findFileViaResourceHandle(handle: ResourceHandle): Option[File] = {
    val url = handle.getName() // Lol. getName returns URL, shity designers...
    try {
      findFileViaURL(new URL(url))
    } catch {
      case e: MalformedURLException if e.getMessage() == "no protocol" =>
        (try { findFileViaURL(new URL("file:" + url)) } catch { case _: Throwable => None }) orElse
          (try { findFileViaURL(new URL("file:/" + url)) } catch { case _: Throwable => None })
      case _: Throwable =>
        return None
    }
  }
  /** Convert URL to the local file and validate */
  protected def findFileViaURL(url: URL): Option[File] = {
    if (url.getProtocol() != "file")
      return None
    val file = new File(url.toURI)
    if (file.exists() && file.isFile())
      return Some(file)
    else if (!file.isAbsolute()) {
      // try to convert it to absolute
      val file = new File(new URI("/" + url.toURI.toString))
      if (file.exists() && file.isFile())
        return Some(file)
      else
        None
    } else {
      None
    }
  }
  /** Generate R5 repository index */
  protected def generateR5Index(jarFiles: Set[File], repoName: String, rootUri: URI, index: File, generatingProviderR5: IRepositoryContentProvider,
    workspace: Workspace, pretty: Boolean = true)(implicit arg: Plugin.TaskArgument): Option[File] = {
    arg.log.debug(logPrefix(arg.name) + "Generate new OSGi R5 index '%s' with root URI '%s' at %s".format(repoName, rootUri, index))
    var output: OutputStream = null
    try {
      output = new BufferedOutputStream(new FileOutputStream(index))
      generatingProviderR5.generateIndex(jarFiles, output, repoName, rootUri, pretty, workspace, new Logger(arg.log))
      Some(index)
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(arg.name) + "Unable to generate repository index: " + e)
        None
    } finally {
      try { Option(output).foreach(_.close) } catch { case _: Throwable => }
    }
  }
  /** Return R5 repository index for resolved dependencies */
  protected def getResolvedDependenciesIndex(resolvedDependencies: Seq[File], workspace: Workspace)(implicit arg: Plugin.TaskArgument): Option[FixedIndexedRepo] =
    if (resolvedDependencies.isEmpty)
      None
    else {
      val jars = resolvedDependencies.filter(f => f.exists() && !f.isDirectory() && f.getName().endsWith(".jar")).toList
      if (jars.isEmpty) {
        arg.log.warn(logPrefix(arg.name) + "Resolved dependencies has no artifacts on local file system")
        return None
      }
      val project = workspace.getProject(Bnd.defaultProjectName)
      val indexName = "%010X_index.xml".format(jars.map(_.getAbsolutePath()).sorted.hashCode) // Hashcodes for List produce a value from the hashcodes of all the elements of the list
      val index = new File(project.getBase(), indexName).getAbsoluteFile()
      val repository = createR5FixedIndexedRepository(internalRepositoryName, index.toURI)
      if (!index.exists() || index.length() == 0)
        repository.getGeneratingProviders.find(_.getName() == R5RepoContentProvider.NAME) match {
          case Some(generatingProviderR5) if generatingProviderR5.supportsGeneration() =>
            generateR5Index(jars.toSet, internalRepositoryName, localRepositoryLocation,
              index, generatingProviderR5, workspace)
          case _ =>
            arg.log.error(logPrefix(arg.name) + "Unable to find R5 generating provider for resolved dependencies repository.")
            return None
        }
      else
        arg.log.debug(logPrefix(arg.name) + "Use pregenerated OSGi R5 index '%s' with root URI '%s' at %s".format(internalRepositoryName, localRepositoryLocation, index))
      Option(repository)
    }
}
