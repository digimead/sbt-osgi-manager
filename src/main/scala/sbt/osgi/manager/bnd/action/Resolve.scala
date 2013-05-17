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
import java.net.URI

import scala.Option.option2Iterable
import scala.collection.JavaConversions._
import scala.collection.immutable

import aQute.bnd.build.Workspace
import aQute.bnd.deployer.repository.AbstractIndexedRepo
import aQute.bnd.deployer.repository.FixedIndexedRepo
import aQute.bnd.deployer.repository.api.IRepositoryContentProvider
import aQute.bnd.deployer.repository.providers.R5RepoContentProvider
import biz.aQute.resolve.internal.BndrunResolveContext
import org.apache.felix.resolver.ResolverImpl
import org.osgi.service.resolver.ResolutionException
import sbt._
import sbt.osgi.manager.Dependency
import sbt.osgi.manager.Dependency._
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support
import sbt.osgi.manager.Support._
import sbt.osgi.manager.bnd.Bnd
import sbt.osgi.manager.bnd.Logger


// This is consolidated thoughts about Bnd that was located across first version of my code.
// It may save a bit of time for someone who will choose the same way.
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
// WTF? Now I understand that Bnd spreading is suppressed by aliens, because they wrote things like this ...
//    ... _p_allsourcepath, _p_dependson, _p_output and so on. Arhh pity alien, I will decode your puzzle anyway ;-)
//
// Absolutely, Bnd code is bit more than junk.
// I must to achieve my targets with only those instruments that I have. No one to blame.
//
//    Ezh
//

object Resolve extends Support.Resolve {
  /** Predefined name for OSGi R5 repository with resolved dependencies */
  val resolvedDependenciesRepositoryName = "Internal repository with resolved dependencies"
  /** Predefined location for OSGi R5 repository with resolved dependencies */
  val resolvedDependenciesRepositoryLocation = new URI("file:/")

  /** Resolve the dependency for the specific project against OBR repository */
  def resolveOBR(resolvedDependencies: Seq[ModuleID])(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Seq[ModuleID] = {
    val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
    // get resolvers as Seq[(id, url)]
    val resolvers = getResolvers(Dependency.OBR, scope)
    val dependencies = getDependencies(Dependency.OBR, scope)
    if (resolvers.nonEmpty && dependencies.nonEmpty) {
      arg.log.info(logPrefix(name) + "Resolve OBR dependencies")
      val bridge = Bnd.get()
      val modules = resolveOBR(dependencies, resolvers, bridge, resolvedDependencies)
      updateCache(CacheOBRKey(projectRef.project), dependencies, resolvers)
      modules
    } else {
      arg.log.info(logPrefix(name) + "No OBR dependencies found")
      updateCache(CacheOBRKey(projectRef.project), Seq(), Seq())
      Seq()
    }
  }
  /** Resolve the dependency against OBR repository */
  def resolveOBR(dependencies: Seq[ModuleID], repositories: Seq[(String, URI)], bnd: Bnd,
    resolvedDependencies: Seq[ModuleID])(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Seq[ModuleID] = {
    val model = bnd.createModel()
    val log = new Logger(arg.log)
    if (model.getRunFw() == null || model.getRunFw().isEmpty()) {
      arg.log.error(logPrefix(name) + "The OSGi Framework and Execution Environment must be specified for resolution.")
      return Seq()
    }
    if (model.getEE() == null) {
      arg.log.error(logPrefix(name) + "The OSGi Framework and Execution Environment must be specified for resolution.")
      return Seq()
    }
    try {
      val workspace = bnd.createWorkspace(Seq())
      val repositoryPlugins = (repositories.map { case (id, location) => aquireRepositoryIndex(id, location, workspace) } :+
        getResolvedDependenciesIndex(resolvedDependencies, workspace)).flatten
      val plugins = repositoryPlugins.foreach(repo => workspace.addBasicPlugin(repo))
      val resolver = new ResolverImpl(log)
      val resolveContext = new BndrunResolveContext(model, workspace, log)
      val result = try {
        val result = resolver.resolve(resolveContext)

        // Find required resources
       /* val requiredResourceSet = new HashSet<Resource>(result.size());
            for (Resource resource : result.keySet()) {
                if (!resolveContext.isInputRequirementsResource(resource) && !resolveContext.isFrameworkResource(resource)) {
                    requiredResourceSet.add(resource);
                }
            }*/

        // Process the mandatory requirements and save them as reasons against the required resources
        /*requiredResources = new HashMap<Resource,Collection<Requirement>>(requiredResourceSet.size());
            for (Entry<Requirement,List<Capability>> entry : resolveContext.getMandatoryRequirements().entrySet()) {
                Requirement req = entry.getKey();
                Resource requirer = req.getResource();
                if (requiredResourceSet.contains(requirer)) {
                    List<Capability> caps = entry.getValue();

                    for (Capability cap : caps) {
                        Resource requiredResource = cap.getResource();
                        if (requiredResourceSet.remove(requiredResource)) {
                            Collection<Requirement> reasons = requiredResources.get(requiredResource);
                            if (reasons == null) {
                                reasons = new LinkedList<Requirement>();
                                requiredResources.put(requiredResource, reasons);
                            }
                            reasons.add(req);
                        }
                    }
                }
            }*/
        // Add the remaining resources in the requiredResourceSet (these come from initial requirements)
        //for (Resource resource : requiredResourceSet)
        //    requiredResources.put(resource, Collections.<Requirement> emptyList());

        // Find optional resources
        //processOptionalRequirements(resolveContext);

        true
      } catch {
        case e: ResolutionException =>
          arg.log.warn("Unable to resolve: " + e)
          //resolutionException = e;
          false
      }
      arg.log.error("!!!" + resolveContext)
      /*      val felixResolver = new ResolverImpl(log)
      val resolved = resolve.resolve(model, Bndtools.get(osgiCnfPath).workspace, felixResolver, log)
      if (resolved) {
        streams.log.info("resolved")
      } else {
        val exception = resolve.getResolutionException()
        if (exception != null)
          streams.log.error(exception.getLocalizedMessage())
        else
          streams.log.error("Resolution failed, reason unknown")
      }*/
      Seq()
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(name) + "Exception during resolution. " + e)
        Seq()
    }
  }
  /** Command that populates libraryDependencies with required bundles */
  def resolveOBRCommand(resolvedDependencies: immutable.HashMap[ProjectRef, Seq[ModuleID]] = immutable.HashMap())(implicit arg: Plugin.TaskArgument): immutable.HashMap[ProjectRef, Seq[ModuleID]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    // Check if we already processed our dependencies with same values
    val cached = for (id <- build.defined.keys) yield {
      implicit val projectRef = ProjectRef(uri, id)
      val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
      arg.log.debug(logPrefix(name) + "Check is settings cached.")
      isCached(CacheOBRKey(id), getDependencies(Dependency.OBR, scope), getResolvers(Dependency.OBR, scope))
    }
    if (cached.forall(_ == true) && false) {
      arg.log.info("Pass OBR resolution: already resolved")
      immutable.HashMap((for (id <- build.defined.keys) yield {
        val projectRef = ProjectRef(uri, id)
        (projectRef, Seq())
      }).toSeq: _*)
    } else {
      immutable.HashMap((for (id <- build.defined.keys) yield {
        implicit val projectRef = ProjectRef(uri, id)
        (projectRef, resolveOBR(resolvedDependencies.get(projectRef) getOrElse Seq()))
      }).toSeq: _*)
    }
  }
  /** Get exists or create new R5 index if user provides file:/directory URI */
  protected def aquireRepositoryIndex(id: String, location: URI, workspace: Workspace)(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Option[AbstractIndexedRepo] = {
    val repository = if (location.getScheme() == "file") {
      val local = new File(location)
      if (!local.exists) {
        arg.log.warn("OBR %s with URI %s not found".format(id, location))
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
              generateR5Index(jars.toSet, id, location, index, generatingProviderR5, workspace)
            case _ =>
              arg.log.error("Unable to find R5 generating provider for '%s' with URI %s".format(id, location))
              return None
          }
        else
          arg.log.debug(logPrefix(name) + "Use pregenerated OSGi R5 index '%s' with root URI '%s' at %s".format(id, location, index))
        repository
      } else
        createR5FixedIndexedRepository(id, location) // URI points to file
    } else {
      createR5FixedIndexedRepository(id, location) // unknown schema, we couldn't do much about
    }
    try {
      // LOL! I want to validate repository right now, but... The old story...
      // init() is protected, but getIndexLocations() is public. OK call init via getIndexLocations
      // Bnd has very weak design. The quality of the Bnd source code is much more worth than scala, sbt, or eclipse.
      // It is oddly. I expected that OSGi developers must be more accurate than others. Negative.
      repository.getIndexLocations()
      Some(repository)
    } catch {
      case e: Throwable =>
        arg.log.error(logPrefix(name) + e.getMessage)
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
  /** Generate R5 repository index */
  protected def generateR5Index(jarFiles: Set[File], repoName: String, rootUri: URI, index: File, generatingProviderR5: IRepositoryContentProvider,
    workspace: Workspace, pretty: Boolean = true)(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Option[File] = {
    arg.log.debug(logPrefix(name) + "Generate new OSGi R5 index '%s' with root URI '%s' at %s".format(repoName, rootUri, index))
    var output: OutputStream = null
    try {
      output = new BufferedOutputStream(new FileOutputStream(index))
      generatingProviderR5.generateIndex(jarFiles, output, repoName, rootUri, pretty, workspace, new Logger(arg.log))
      Some(index)
    } catch {
      case e: Throwable =>
        arg.log.error("Unable to generate repository index: " + e)
        None
    } finally {
      try { Option(output).foreach(_.close) } catch { case _: Throwable => }
    }
  }
  /** Return R5 repository index for resolved dependencies */
  protected def getResolvedDependenciesIndex(resolvedDependencies: Seq[ModuleID], workspace: Workspace)(implicit arg: Plugin.TaskArgument, projectRef: ProjectRef): Option[FixedIndexedRepo] =
    if (resolvedDependencies.isEmpty)
      None
    else {
      var artifacts = Seq[File]()
      resolvedDependencies.foreach { moduleId =>
        moduleId.explicitArtifacts.foreach { artifact =>
          if (artifact.classifier == None || artifact.classifier == Some(""))
            artifact.url.foreach(url => if (url.getProtocol() == "file") artifacts = artifacts :+ new File(url.toURI()))
        }
      }
      val jars = artifacts.filter(f => f.exists() && !f.isDirectory()).toList
      if (jars.isEmpty) {
        arg.log.warn(logPrefix(name) + "Resolved dependencies has no artifacts on local file system")
        return None
      }
      val project = workspace.getProject(Bnd.defaultProjectName)
      val indexName = "%010X_index.xml".format(jars.map(_.getAbsolutePath()).sorted.hashCode) // Hashcodes for List produce a value from the hashcodes of all the elements of the list
      val index = new File(project.getBase(), indexName).getAbsoluteFile()
      val repository = createR5FixedIndexedRepository(resolvedDependenciesRepositoryName, index.toURI)
      if (!index.exists() || index.length() == 0)
        repository.getGeneratingProviders.find(_.getName() == R5RepoContentProvider.NAME) match {
          case Some(generatingProviderR5) if generatingProviderR5.supportsGeneration() =>
            generateR5Index(jars.toSet, resolvedDependenciesRepositoryName, resolvedDependenciesRepositoryLocation,
              index, generatingProviderR5, workspace)
          case _ =>
            arg.log.error("Unable to find R5 generating provider for resolved dependencies repository.")
            return None
        }
      else
        arg.log.debug(logPrefix(name) + "Use pregenerated OSGi R5 index '%s' with root URI '%s' at %s".format(resolvedDependenciesRepositoryName, resolvedDependenciesRepositoryLocation, index))
      Option(repository)
    }
}
