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

import biz.aQute.resolve.internal.BndrunResolveContext
import sbt._
import sbt.Keys._
import sbt.osgi.manager.Dependency
import sbt.osgi.manager.Dependency._
import sbt.osgi.manager.Plugin
import sbt.osgi.manager.Support
import sbt.osgi.manager.Support._
import sbt.osgi.manager.bnd.Bnd
import sbt.osgi.manager.bnd.Logger
import org.osgi.service.resolver.ResolutionException
import org.apache.felix.resolver.ResolverImpl
import aQute.bnd.deployer.repository.FixedIndexedRepo
import aQute.bnd.build.Workspace

object Resolve extends Support.Resolve {
  /** Resolve the dependency for the specific project against OBR repository */
  def resolveOBR(projectRef: ProjectRef)(implicit arg: Plugin.TaskArgument): Seq[Project.Setting[_]] = {
    val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
    val name = sbt.Keys.name in arg.thisScope.copy(project = Select(projectRef)) get arg.extracted.structure.data getOrElse projectRef.project
    // get resolvers as Seq[(id, url)]
    val resolvers = getResolvers(Dependency.OBR, scope)
    val dependencies = getDependencies(Dependency.OBR, scope)
    if (resolvers.nonEmpty && dependencies.nonEmpty) {
      arg.log.info(logPrefix(arg.name) + "Resolve OBR dependencies for project [%s]".format(name))
      val bridge = Bnd.get()
      val modules = resolveOBR(dependencies, resolvers, bridge)
      updateCache(CacheOBRKey(projectRef.project), dependencies, resolvers)
      Seq(libraryDependencies in projectRef ++= modules)
    } else {
      arg.log.info(logPrefix(arg.name) + "No OBR dependencies for project [%s] found".format(name))
      updateCache(CacheOBRKey(projectRef.project), Seq(), Seq())
      Seq()
    }
  }
  /** Resolve the dependency against OBR repository */
  def resolveOBR(dependencies: Seq[ModuleID], repositories: Seq[(String, URI)], bnd: Bnd)(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    val model = bnd.createModel()
    val log = new Logger(arg.log)
    (model.getRunFw() != null, "The OSGi Framework and Execution Environment must be specified for resolution.")
    (model.getEE() != null, "The OSGi Framework and Execution Environment must be specified for resolution.")
    try {
      //      val repo = createRepo(new File("testdata/repo1.index.xml"))
      val repo = new FixedIndexedRepo()
      val props = new java.util.HashMap[String, String]()
      props.put("locations", repositories.map(_._2).mkString(","))
      repo.setProperties(props)
      // I want to here repository here.
      // LOL! init() is proptected, but getIndexLocations() is public. OK call init via getIndexLocations
      repo.getIndexLocations()

      val plugins = Seq((w: Workspace) => repo)
      val workspace = bnd.createWorkspace(Bnd.getHome, plugins)
      val resolver = new ResolverImpl(log)
      val resolveContext = new BndrunResolveContext(model, workspace, log)
      val result = try {
        val result = resolver.resolve(resolveContext)

        // Find required resources
        /*val requiredResourceSet = new HashSet<Resource>(result.size());
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
        arg.log.error("Exception during resolution. " + e)
        Seq()
    }
  }
  def createFixedIndexedRepo {
    /* val repo = new FixedIndexedRepo();

        Map<String,String> props = new HashMap<String,String>();
        props.put(FixedIndexedRepo.PROP_LOCATIONS, index.toURI().toString());
        if (name != null)
            props.put(AbstractIndexedRepo.PROP_NAME, name);
        repo.setProperties(props);

        return repo;*/
  }
  /** Command that populates libraryDependencies with required bundles */
  def resolveOBRCommand()(implicit arg: Plugin.TaskArgument): Seq[Project.Setting[_]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    // Check if we already processed our dependencies with same values
    val cached = for (id <- build.defined.keys) yield {
      val projectRef = ProjectRef(uri, id)
      val scope = arg.thisOSGiScope.copy(project = Select(projectRef))
      isCached(CacheOBRKey(id), getDependencies(Dependency.OBR, scope), getResolvers(Dependency.OBR, scope))
    }
    if (cached.forall(_ == true) && false) {
      arg.log.info("Pass OBR resolution: already resolved")
      Seq()
    } else {
      (for (id <- build.defined.keys) yield resolveOBR(ProjectRef(uri, id))).toSeq.flatten
    }
  }
}
