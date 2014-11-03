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

package sbt.osgi.manager.maven.action

import java.io.{ BufferedOutputStream, FileOutputStream, OutputStream }
import java.net.{ URI, URISyntaxException }
import java.util.Properties
import java.util.jar.{ JarOutputStream, Pack200 }
import org.apache.maven.artifact.Artifact
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.model.{ Dependency ⇒ MavenDependency }
import org.apache.maven.repository.RepositorySystem
import org.eclipse.core.runtime.{ IProgressMonitor, IStatus }
import org.eclipse.equinox.p2.core.ProvisionException
import org.eclipse.equinox.p2.metadata.IInstallableUnit
import org.eclipse.equinox.p2.repository.artifact.{ IArtifactDescriptor, IArtifactRepository, IArtifactRepositoryManager }
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub
import org.eclipse.tycho.core.facade.TargetEnvironment
import org.eclipse.tycho.core.resolver.shared.{ MavenRepositoryLocation, PlatformPropertiesUtils }
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult
import sbt.osgi.manager.Dependency.{ getOrigin, moduleId2Dependency, tuplesWithString2repositories }
import sbt.osgi.manager.Support.{ CacheP2Key, getDependencies, getResolvers, logPrefix }
import sbt.osgi.manager.maven.Maven
import sbt.osgi.manager.{ Dependency, Model }
import sbt.osgi.manager.{ Plugin, Support }
import sbt.{ Keys ⇒ skey, _ }
import scala.annotation.tailrec
import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, collectionAsScalaIterable, seqAsJavaList }
import scala.collection.{ immutable, mutable }
import scala.language.reflectiveCalls

// Unfortunately:
//   - from the one side SBT is too simple for handle mixed ModuleID with explicit artifacts,
//     where one of them points to already downloaded file:// and other points to http:// (we will lost http:// after ivy lookup)
//   - from the other side Tycho ALWAYS download artifacts at 'resolve' (at least 0.17). This is hard coded.
// So we have to two piece of shit from different team of developers and there is no way for sunny day.
// And I think only about my targets and my needs as anyone other do.
//   Ezh
//
// What? Submit a bug report? I had a lot already. Fuck you.
//
// Dear unusual individual, that read this funny stuff,
//   Please, submit the bug report yourself or just modify this code.
//   Your patch will be accepted. You are welcome.

/** Contain resolve action for Maven and P2 repository */
object Resolve extends Support.Resolve {
  val environmentJRE1_6 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.6")
  val environmentJRE1_7 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.7")
  val environmentJRE1_8 = new ExecutionEnvironmentConfigurationStub("JavaSE-1.8")

  /** Resolve the dependency against the standard Maven repository */
  def resolveBasic(maven: Maven)(implicit arg: Plugin.TaskArgument) {
    val groupId = "org.apache.maven"
    val artifactId = "maven-core"
    val version = "3.0"
    val repositorySystem = maven.lookup(classOf[RepositorySystem])
    val repo = repositorySystem.createDefaultRemoteRepository()
    val dependency = new MavenDependency()
    dependency.setGroupId(groupId)
    dependency.setArtifactId(artifactId)
    dependency.setVersion(version)
    dependency.setScope(Artifact.SCOPE_COMPILE)
    val artifact = repositorySystem.createDependencyArtifact(dependency)
    val request = new ArtifactResolutionRequest().
      setArtifact(artifact).
      setResolveRoot(true).
      setResolveTransitively(true).
      setRemoteRepositories(List(repo)).
      setLocalRepository(maven.session.getLocalRepository()).
      setListeners(null) // reset an empty list to null for org.apache.maven.artifact.resolver.DefaultArtifactResolver.resolve

    //if (scope != null) {
    //  io.debug("Using scope: {}", scope);
    //  request.setCollectionFilter(new ScopeArtifactFilter(scope));
    //}

    for (rr ← request.getRemoteRepositories())
      arg.log.debug(logPrefix(arg.name) + "Add remote repository:\n" + rr)
    arg.log.info(logPrefix(arg.name) + "Resolve artifact: " + artifact)
    val result = repositorySystem.resolve(request)
    for (exception ← result.getErrorArtifactExceptions())
      arg.log.error(logPrefix(arg.name) + exception.toString())
    for (exception ← result.getCircularDependencyExceptions())
      arg.log.error(logPrefix(arg.name) + exception.toString())
    if (result.getMissingArtifacts().nonEmpty)
      arg.log.warn(logPrefix(arg.name) + "Unable to locate " + result.getMissingArtifacts().mkString(","))

    val artifacts = result.getArtifacts()
    arg.log.info(logPrefix(arg.name) + "Resolved artifacts:\n" + result)

    //    val origin = maven.session.getCurrentProject()
    //            val model = origin.getModel().clone();
    /*      val build = model.getBuild()
         val tpmp = new Plugin()
         tpmp.setGroupId()
         tpmp.setArtifactId(artifactId)
         tpmp.setVersion(version)

         build.getPlugins().add(tpmp)
         build.flushPluginMap()*/

    //  System.err.println("!!!" + result)
    //    repositorySystem.createArtifactRepository(id, url, repositoryLayout, snapshots, releases)(repository)
    //val repo = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/")
    //repositorySystem.resolve()
    //    val request = new ArtifactRequest()
    //    request.setArtifact(new DefaultArtifact(":maven-model:3.0"))
    //    request.setRepositories(List(repo))
    /*getLog().info("Resolving artifact " + artifact +
      " from " + remoteRepos);

    ArtifactResult result;*/

    /*getLog().info("Resolved artifact " + artifact + " to " +
      result.getArtifact().getFile() + " from "
      + result.getRepository());*/
  }
  /** Resolve the dependency for the specific project against Eclipse P2 repository */
  def resolveP2(resolveAsRemoteArtifacts: Boolean)(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    // get resolvers as Seq[(id, url)]
    val resolvers = getResolvers(Dependency.P2, arg.thisOSGiScope)
    val dependencies = getDependencies(Dependency.P2, arg.thisOSGiScope)
    if (resolvers.nonEmpty && dependencies.nonEmpty) {
      arg.log.info(logPrefix(arg.name) + "Resolve P2 dependencies")
      val bridge = Maven()
      val modules = ResolveP2(dependencies, resolvers, environmentJRE1_6, bridge, resolveAsRemoteArtifacts, true)
      val resolved = skey.libraryDependencies in arg.thisScope get arg.extracted.structure.data getOrElse Seq()
      updateCache(CacheP2Key(arg.thisProjectRef.project), dependencies, resolvers)
      modules.filterNot { m ⇒
        val alreadyInLibraryDependencies = resolved.exists(_ == m)
        if (alreadyInLibraryDependencies)
          arg.log.debug(logPrefix(arg.name) + "Skip, already in libraryDependencies: " + m)
        alreadyInLibraryDependencies
      }
    } else {
      arg.log.info(logPrefix(arg.name) + "No P2 dependencies found")
      updateCache(CacheP2Key(arg.thisProjectRef.project), dependencies, resolvers)
      Seq()
    }
  }
  /** Command that populates libraryDependencies with required bundles */
  def resolveP2Command(resolveAsRemoteArtifacts: Boolean)(implicit arg: Plugin.TaskArgument): immutable.HashMap[ProjectRef, Seq[ModuleID]] = {
    val uri = arg.extracted.currentRef.build
    val build = arg.extracted.structure.units(uri)
    // Check if we already processed our dependencies with same values
    val cached = for (id ← build.defined.keys) yield {
      implicit val projectRef = ProjectRef(uri, id)
      val localArg = arg.copy(thisProjectRef = projectRef)
      isCached(Support.CacheP2Key(id), getDependencies(Dependency.P2, localArg.thisOSGiScope)(localArg),
        getResolvers(Dependency.P2, localArg.thisOSGiScope)(localArg))(localArg)
    }
    if (cached.forall(_ == true) && !resolveAsRemoteArtifacts) {
      arg.log.info(logPrefix(arg.name) + "Pass P2 resolution: already resolved")
      immutable.HashMap((for (id ← build.defined.keys) yield {
        val projectRef = ProjectRef(uri, id)
        (projectRef, Seq())
      }).toSeq: _*)
    } else {
      immutable.HashMap((for (id ← build.defined.keys) yield {
        implicit val projectRef = ProjectRef(uri, id)
        (projectRef, resolveP2(resolveAsRemoteArtifacts)(arg.copy(thisProjectRef = projectRef)))
      }).toSeq: _*)
    }
  }
}
