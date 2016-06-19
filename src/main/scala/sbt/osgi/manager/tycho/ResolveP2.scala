/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2014-2016 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.osgi.manager.tycho

import java.io.{ BufferedOutputStream, FileOutputStream, OutputStream }
import java.net.{ URI, URISyntaxException }
import java.util.{ ArrayList, Collections, Properties }
import java.util.jar.{ JarOutputStream, Pack200 }
import org.apache.maven.model.{ Dependency ⇒ MavenDependency }
import org.eclipse.core.runtime.{ IProgressMonitor, IStatus }
import org.eclipse.equinox.p2.core.ProvisionException
import org.eclipse.equinox.p2.metadata.{ IArtifactKey, IInstallableUnit }
import org.eclipse.equinox.p2.repository.artifact.{ IArtifactDescriptor, IArtifactRepository, IArtifactRepositoryManager }
import org.eclipse.tycho.artifacts.TargetPlatform
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration
import org.eclipse.tycho.core.resolver.shared.{ MavenRepositoryLocation, PlatformPropertiesUtils }
import org.eclipse.tycho.core.shared.TargetEnvironment
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub
import sbt.{ File, IO, IvySbt, ModuleID, moduleIDConfigurable }
import sbt.osgi.manager.{ Environment, Keys, Plugin }
import sbt.osgi.manager.Dependency.getOrigin
import sbt.osgi.manager.support.Model
import sbt.osgi.manager.support.Support.logPrefix
import sbt.toGroupID
import scala.annotation.tailrec
import scala.collection.{ breakOut, immutable, mutable }
import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, collectionAsScalaIterable }
import scala.language.{ implicitConversions, reflectiveCalls }

class ResolveP2 {
  /** Instance of Pack200, specified in JSR 200, is an HTTP compression method by Sun for faster JAR file transfer speeds over the network. */
  val unpacker = Pack200.newUnpacker()

  /** Add repositories to target platform parameters. */
  def addRepositoriesToTargetPlatformConfiguration(tpParameters: TargetPlatformConfigurationStub, p2Pepositories: Seq[(String, URI)], maven: Maven)(implicit arg: Plugin.TaskArgument): Seq[IArtifactRepository] = {
    arg.log.info(logPrefix(arg.name) + "Initialize repositories. It may HANGS or FREEZES if one of repositories is not responding.")
    val provisioningAgentInterface = maven.equinox.getClass.getClassLoader.loadClass("org.eclipse.equinox.p2.core.IProvisioningAgent")
    val remoteAgent = maven.equinox.getService(provisioningAgentInterface).asInstanceOf[{ def getService(serviceName: String): AnyRef }]
    val remoteArtifactRepositoryManager = remoteAgent.getService(IArtifactRepositoryManager.SERVICE_NAME).asInstanceOf[IArtifactRepositoryManager]
    val repositories = {
      for (repo ← p2Pepositories) yield {
        val id = repo._1
        val location = repo._2
        try {
          arg.log.info(logPrefix(arg.name) + s"Append ${repo._2} to resolution configuration.")
          tpParameters.addP2Repository(new MavenRepositoryLocation(id, location))
          Option(remoteArtifactRepositoryManager.loadRepository(location, null)) match {
            case repository @ Some(_) ⇒
              arg.log.info(logPrefix(arg.name) + s"${repo._2} added.")
              repository
            case None ⇒
              arg.log.info(logPrefix(arg.name) + s"${repo._2} skipped. Unable to load.")
              None
          }
        } catch {
          case e: URISyntaxException ⇒
            arg.log.warn(logPrefix(arg.name) + "Unable to resolve repository URI : " + location)
            None
          case e: ProvisionException ⇒
            arg.log.warn(logPrefix(arg.name) + e.getMessage())
            None
          case e: Throwable ⇒
            arg.log.warn(logPrefix(arg.name) + e.getMessage())
            None
        }
      }
    }.flatten
    // Get remote repositories
    if (repositories.isEmpty)
      arg.log.info(logPrefix(arg.name) + "There are no any usable repositories")
    repositories
  }
  /** Resolve the dependency against Eclipse P2 repository */
  // For more information about metadata and artifact repository manager, look at
  // http://eclipsesource.com/blogs/tutorials/eclipse-p2-tutorial-managing-metadata/
  def apply(dependencies: Seq[MavenDependency], rawRepositories: Seq[(String, URI)],
    eeConfiguration: ExecutionEnvironmentConfiguration, target: Seq[(Environment.OS, Environment.WS, Environment.ARCH)],
    maven: Maven, ivySbt: IvySbt, resolveAsRemoteArtifacts: Boolean, includeLocalMavenRepo: Boolean)(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    val targetPlatformConfiguration = new TargetPlatformConfigurationStub()
    val repositories = addRepositoriesToTargetPlatformConfiguration(targetPlatformConfiguration, rawRepositories, maven)
    val targetPlatform = createTargetPlatform(targetPlatformConfiguration, eeConfiguration, target, !includeLocalMavenRepo, maven)
    val resolver = maven.p2ResolverFactory.createResolver(new MavenLoggerAdapter(maven.plexus.getLogger, true))
    dependencies.foreach(d ⇒ resolver.addDependency(d.getType(), d.getArtifactId(), d.getVersion()))

    arg.log.info(logPrefix(arg.name) + "Resolve P2 resources.")
    val resolutionResults: Seq[P2ResolutionResult] = ivySbt.withIvy(arg.log) { ivy ⇒
      // Set reactor project location to null
      (if (target.isEmpty) Environment.all else target).flatMap {
        case (tOS, tWS, tARCH) ⇒
          try {
            val environmentList = new ArrayList[TargetEnvironment]()
            environmentList.add(new TargetEnvironment(tOS.value, tWS.value, tARCH.value))
            resolver.setEnvironments(environmentList)
            resolver.resolveDependencies(targetPlatform, null).toSeq
          } catch {
            case e: RuntimeException ⇒
              Option(e.getMessage).foreach(message ⇒ arg.log.debug(logPrefix(arg.name) + message))
              Seq(ResolveP2.EmptyP2ResolutionResult: P2ResolutionResult)
          }
      }
    }

    val artifacts = resolutionResults.map(_.getArtifacts).flatten.groupBy(_.getId).map(_._2.head)(breakOut).sortBy(_.getId)
    if (artifacts.isEmpty) {
      arg.log.info(logPrefix(arg.name) + "There are no any resolved entries")
      return Seq()
    }
    // Process results
    val rePerDependencyMap = collectArtifactsPerDependency(dependencies, artifacts)
    // FYI:
    //   single P2ResolutionResult.Entry may have any number of InstallableUnits that point to the same artifact
    //   single InstallableUnit may be bound to multiple originModuleIds. Some of ModuleIDs may require source code, some not
    artifacts.map { entry ⇒
      val originModuleIds = rePerDependencyMap.get(entry).map(dependencies ⇒ dependencies.flatMap(getOrigin)) getOrElse Seq()
      entry.getInstallableUnits().map(_ match {
        case riu: IInstallableUnit if originModuleIds.nonEmpty && originModuleIds.exists(_.withSources) ⇒
          arg.log.info(logPrefix(arg.name) + "Collect P2 IU %s with source code".format(riu))
          arg.log.debug(logPrefix(arg.name) + "%s -> [%s]".format(riu, originModuleIds.map(_.moduleId.copy(extraAttributes = Map())).mkString(",")))
          Some(getModuleId(entry, riu, resolveAsRemoteArtifacts, repositories, true))
        case riu: IInstallableUnit if originModuleIds.nonEmpty ⇒
          arg.log.info(logPrefix(arg.name) + "Collect P2 IU %s".format(riu))
          arg.log.debug(logPrefix(arg.name) + "%s -> [%s]".format(riu, originModuleIds.map(_.moduleId.copy(extraAttributes = Map())).mkString(",")))
          Some(getModuleId(entry, riu, resolveAsRemoteArtifacts, repositories, false))
        case riu: IInstallableUnit ⇒
          arg.log.warn(logPrefix(arg.name) + "Collect an unbound installable unit: " + riu)
          Some(getModuleId(entry, riu, resolveAsRemoteArtifacts, repositories, false))
        case ru ⇒
          arg.log.warn(logPrefix(arg.name) + "Skip an unknown reactor unit: " + ru)
          None
      }).flatten
    }.flatten
  }
  /** Create target platform context. */
  def createTargetPlatform(tpParameters: TargetPlatformConfigurationStub, eeConfiguration: ExecutionEnvironmentConfiguration,
    target: Seq[(Environment.OS, Environment.WS, Environment.ARCH)], forceIgnoreLocalArtifacts: Boolean,
    maven: Maven)(implicit arg: Plugin.TaskArgument): TargetPlatform = {
    // actually targetPlatformBuilder is a resolution context
    val pomDependencies = maven.p2ResolverFactory.newPomDependencyCollector()
    val tpFactory = maven.p2ResolverFactory.getTargetPlatformFactory()
    val environmentList = new ArrayList[TargetEnvironment]()
    if (target.isEmpty)
      Environment.all.foreach { case (tOS, tWS, tARCH) ⇒ environmentList.add(new TargetEnvironment(tOS.value, tWS.value, tARCH.value)) }
    else
      target.foreach { case (tOS, tWS, tARCH) ⇒ environmentList.add(new TargetEnvironment(tOS.value, tWS.value, tARCH.value)) }
    tpParameters.setEnvironments(environmentList)
    tpParameters.setForceIgnoreLocalArtifacts(forceIgnoreLocalArtifacts)
    tpFactory.createTargetPlatform(tpParameters, eeConfiguration, new ArrayList(), pomDependencies)
  }

  /** Unpack packedAndGzipped to target */
  protected def aquireGzippedPack200Artifact(packedAndGzipped: File, target: File)(implicit arg: Plugin.TaskArgument): Option[Throwable] = {
    val out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(target)))
    val tmpFile = java.io.File.createTempFile("sbt-osgi-manager-", "-pack")
    tmpFile.deleteOnExit()
    try {
      IO.gunzip(packedAndGzipped, tmpFile)
      unpacker.unpack(tmpFile, out)
      None
    } catch {
      case e: Throwable ⇒
        arg.log.error(logPrefix(arg.name) + "Unable to unpack artifact: " + e)
        Some(e)
    } finally {
      try { out.close() } catch { case _: Throwable ⇒ }
      try { tmpFile.delete() } catch { case _: Throwable ⇒ }
    }
  }
  /** Bind P2ResolutionResult.Entry(s) to SBT dependencies */
  protected def collectArtifactsPerDependency(dependencies: Seq[MavenDependency],
    artifacts: Seq[P2ResolutionResult.Entry])(implicit arg: Plugin.TaskArgument): immutable.HashMap[P2ResolutionResult.Entry, Seq[MavenDependency]] = {
    // map id -> dependency
    val dependencyMap = immutable.HashMap(dependencies.map(d ⇒ d.getArtifactId() -> d): _*)
    // map parent IU -requires-> Seq(child IU) which represents the IU requirements
    val iuRequirements = mutable.HashMap(artifacts.map(_.getInstallableUnits().map {
      case iu: IInstallableUnit ⇒
        Some(iu -> Seq[IInstallableUnit]())
      case iu ⇒
        arg.log.error(logPrefix(arg.name) + "Unknown type %s of installable unit '%s'".format(iu.getClass(), iu))
        None
    }.flatten.toSeq).flatten: _*)
    // map child IU -used by-> Seq(parent IU)
    val iuContainers = mutable.HashMap[IInstallableUnit, Seq[IInstallableUnit]]()

    // populate iuRequirements
    iuRequirements.keys.foreach(iu ⇒
      iuRequirements(iu) = iu.getRequirements().map { requirement ⇒
        iuRequirements.keys.filter(requirement.isMatch)
      }.toSeq.flatten)

    // populate iuContainers
    iuRequirements.foreach {
      case (parent, children) ⇒
        children.foreach { child ⇒
          val saved = iuContainers.get(child) getOrElse Seq()
          if (!saved.contains(parent) && child != parent)
            iuContainers(child) = saved :+ parent
        }
    }

    @tailrec
    def getIUParents(iu: Seq[IInstallableUnit], acc: Seq[IInstallableUnit] = Seq()): Seq[IInstallableUnit] = {
      val parents = iu.flatMap(iuContainers.get).flatten
      val filtered = parents.filterNot(parent ⇒
        acc.exists(_.getId() == parent.getId()) || iu.exists(_.getId() == parent.getId()))
      if (filtered.isEmpty) return acc
      getIUParents(filtered, acc ++ filtered)
    }

    immutable.HashMap(artifacts.map { artifact ⇒
      val mavenDependencies = artifact.getInstallableUnits().map {
        case iu: IInstallableUnit ⇒
          Some(iu)
        case iu ⇒
          None // we already notify a user
      }.flatten.map { iu ⇒
        // search for all SBT dependencies against the IU
        dependencyMap.get(iu.getId()).toSeq ++ getIUParents(Seq(iu)).map(iu ⇒ dependencyMap.get(iu.getId())).flatten
      }.toSeq.flatten
      (artifact -> mavenDependencies)
    }: _*)
  }
  /** Create sbt.ModuleID from P2ResolutionResult.Entry and IInstallableUnit */
  protected def getModuleId(resolutionEntry: P2ResolutionResult.Entry, iu: IInstallableUnit, resolveAsRemoteArtifacts: Boolean,
    repositories: Seq[IArtifactRepository], withSourceCode: Boolean)(implicit arg: Plugin.TaskArgument): ModuleID = {
    val location = if (resolveAsRemoteArtifacts) {
      var location = Option.empty[URI]
      // Get descriptors with code
      for (r ← repositories.map(P2SimpleRepository.apply).flatten if location.isEmpty) {
        val rDescriptors = r.getDescriptors()
        val descriptors = (iu.getArtifacts() map { artifact ⇒
          rDescriptors.filter { descriptor ⇒
            val key = descriptor.getArtifactKey()
            key.getClassifier() == artifact.getClassifier() &&
              key.getId() == artifact.getId() &&
              key.getVersion() == artifact.getVersion()
          }
        }).flatten.toSeq.
          // first is unpacked then all others
          sortBy(_.getProperty(IArtifactDescriptor.FORMAT) == IArtifactDescriptor.FORMAT_PACKED)
        descriptors match {
          case Seq(descriptor) ⇒
            location = Some(r.getLocation(descriptor))
          case Nil ⇒
            arg.log.debug(logPrefix(arg.name) + "Unable to find artifact for " + iu + " in " + r)
          case seq ⇒
            arg.log.debug(logPrefix(arg.name) + "There are more than one artifacts: " + seq + " (keep first available)")
            for (descriptor ← seq if location.isEmpty)
              location = Some(r.getLocation(descriptor))
        }
      }
      location match {
        case Some(uri) ⇒
          uri.toASCIIString()
        case None ⇒
          arg.log.warn(logPrefix(arg.name) + "Unable to find source code for " + iu)
          resolutionEntry.getLocation.getAbsoluteFile.toURI.toASCIIString()
      }
    } else {
      resolutionEntry.getLocation.getAbsoluteFile.toURI.toASCIIString()
    }
    val moduleId = iu.getId() % resolutionEntry.getId() % iu.getVersion().getOriginal() % Keys.OSGiConf from location
    if (withSourceCode) {
      getModuleSourceCode(resolutionEntry, iu, resolveAsRemoteArtifacts, repositories) match {
        case Some(uri) ⇒
          val artifactWithSourceCode = _root_.sbt.Artifact.classified(moduleId.name, _root_.sbt.Artifact.SourceClassifier).copy(url = Some(uri.toURL()))
          moduleId.copy(explicitArtifacts = moduleId.explicitArtifacts :+ artifactWithSourceCode)
        case None ⇒
          arg.log.warn(logPrefix(arg.name) + "Unable to find source code for " + iu)
          moduleId
      }
    } else
      moduleId
  }
  /** Download source code artifact if needed and get its location. */
  protected def getModuleSourceCode(resolutionEntry: P2ResolutionResult.Entry, iu: IInstallableUnit, resolveAsRemoteArtifacts: Boolean,
    repositories: Seq[IArtifactRepository])(implicit arg: Plugin.TaskArgument): Option[URI] = {
    var sourceCode = Option.empty[URI]
    // Get descriptors with source code
    for (r ← repositories.map(P2SimpleRepository.apply).flatten if sourceCode.isEmpty) {
      val rDescriptors = r.getDescriptors()
      val sourceCodeDescriptors = (iu.getArtifacts() map { artifact ⇒
        rDescriptors.filter { descriptor ⇒
          val key = descriptor.getArtifactKey()
          key.getClassifier() == artifact.getClassifier() &&
            key.getId() == artifact.getId() + ".source" &&
            key.getVersion() == artifact.getVersion()
        }
      }).flatten.toSeq.
        // first is unpacked then all others
        sortBy(_.getProperty(IArtifactDescriptor.FORMAT) == IArtifactDescriptor.FORMAT_PACKED)
      sourceCodeDescriptors match {
        case Seq(descriptor) ⇒
          if (resolveAsRemoteArtifacts)
            sourceCode = Some(r.getLocation(descriptor))
          else {
            val directory = resolutionEntry.getLocation().getParent()
            val key = descriptor.getArtifactKey()
            val name = key.getId() + "_" + key.getVersion() + ".jar"
            val target = new File(directory, name)
            if (P2SimpleRepository.download(descriptor, target, r))
              sourceCode = Some(target.toURI())
          }
        case Nil ⇒
          arg.log.debug(logPrefix(arg.name) + "Unable to find source code for " + iu + " in " + r)
        case seq ⇒
          arg.log.debug(logPrefix(arg.name) + "There are more than one source code artifacts: " + seq + " (keep first available)")
          for (descriptor ← seq if sourceCode.isEmpty)
            if (resolveAsRemoteArtifacts)
              sourceCode = Some(r.getLocation(descriptor))
            else {
              val directory = resolutionEntry.getLocation().getParent()
              val key = descriptor.getArtifactKey()
              val name = key.getId() + "_" + key.getVersion() + ".jar"
              val target = new File(directory, name)
              if (P2SimpleRepository.download(descriptor, target, r))
                sourceCode = Some(target.toURI())
            }
      }
    }
    sourceCode
  }
  protected def getRunningEnvironment(): TargetEnvironment = {
    val properties = new Properties()
    properties.put(PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS(properties))
    properties.put(PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS(properties))
    properties.put(PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch(properties))

    new TargetEnvironment(properties.getProperty(PlatformPropertiesUtils.OSGI_OS),
      properties.getProperty(PlatformPropertiesUtils.OSGI_WS),
      properties.getProperty(PlatformPropertiesUtils.OSGI_ARCH))
  }

  /**
   * Convert IArtifactRepository -> Seq[P2SimpleRepository.Interface]
   */
  object P2SimpleRepository {
    def apply(repository: IArtifactRepository)(implicit arg: Plugin.TaskArgument): Seq[Interface] = {
      val clazz = repository.getClass()
      clazz.getName() match {
        case "org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository" ⇒
          // process CompositeArtifactRepository children
          val methodGetLoadedChildren = clazz.getDeclaredMethod("getLoadedChildren")
          val result = methodGetLoadedChildren.invoke(repository).asInstanceOf[java.util.List[IArtifactRepository]].
            map(apply)
          //      val directory = resolutionEntry.getLocation().getParent()
          result.flatten.toSeq.distinct
        case "org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository" ⇒
          Seq(repository.asInstanceOf[Interface])
        case unknown ⇒
          arg.log.debug(logPrefix(arg.name) + "Unknown repository type " + unknown)
          Seq.empty
      }
    }
    /** Download artifact for the specific P2 installable unit */
    def download(descriptor: IArtifactDescriptor, target: java.io.File, repository: Interface)(implicit arg: Plugin.TaskArgument): Boolean = try {
      val key = descriptor.getArtifactKey()
      arg.log.debug(logPrefix(arg.name) + "Downloading artifact '" + key + "' to " + target)
      val methodGetRawArtifact = repository.getClass.getDeclaredMethod("getRawArtifact", classOf[IArtifactDescriptor], classOf[OutputStream], classOf[IProgressMonitor])
      val monitor = new IProgressMonitor {
        @volatile var cancelled = false
        def beginTask(name: String, totalWork: Int) {}
        def done() {}
        def internalWorked(work: Double) {}
        def isCanceled(): Boolean = cancelled
        def setCanceled(value: Boolean) { cancelled = value }
        def setTaskName(name: String) {}
        def subTask(name: String) {}
        def worked(work: Int) {}
      }
      if (!target.exists()) {
        val status = if (descriptor.getProperty(IArtifactDescriptor.FORMAT) == IArtifactDescriptor.FORMAT_PACKED) {
          val tmpFile = java.io.File.createTempFile("sbt-osgi-manager-", "-pack.gz")
          tmpFile.deleteOnExit()
          val out = new BufferedOutputStream(new FileOutputStream(tmpFile))
          val downloadStatus = methodGetRawArtifact.invoke(repository, descriptor, out, monitor).asInstanceOf[IStatus]
          try { out.close() } catch { case _: Throwable ⇒ }
          val status = if (downloadStatus.getCode() == IStatus.OK)
            aquireGzippedPack200Artifact(tmpFile, target) map (throwable ⇒
              new IStatus {
                def getChildren(): Array[org.eclipse.core.runtime.IStatus] = downloadStatus.getChildren()
                def getCode(): Int = IStatus.ERROR
                def getException() = throwable
                def getMessage() = throwable.getMessage()
                def getPlugin() = downloadStatus.getPlugin()
                def getSeverity(): Int = IStatus.ERROR
                def isMultiStatus() = downloadStatus.isMultiStatus()
                def isOK() = false
                def matches(severityMask: Int) = false // unused
              }) getOrElse downloadStatus
          else
            downloadStatus
          try { tmpFile.delete() } catch { case _: Throwable ⇒ }
          status
        } else {
          val out = new BufferedOutputStream(new FileOutputStream(target))
          val status = methodGetRawArtifact.invoke(repository, descriptor, out, monitor).asInstanceOf[IStatus]
          try { out.close() } catch { case _: Throwable ⇒ }
          status
        }
        if (status.getCode() == IStatus.OK) {
          arg.log.info(logPrefix(arg.name) + "Artifact " + key + " downloaded")
          true
        } else {
          arg.log.warn(logPrefix(arg.name) + "Unable to download artifact %s: %s".format(key, status))
          try { target.delete() } catch { case _: Throwable ⇒ }
          false
        }
      } else {
        arg.log.debug(logPrefix(arg.name) + "Get cached artifact " + key + " from " + target)
        true
      }
    } catch {
      case e: Throwable ⇒
        arg.log.debug(logPrefix(arg.name) + "Unable to download artifact: " + e)
        false
    }

    /**
     * Interface to org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository
     */
    type Interface = {
      def getDescriptors(): java.util.HashSet[IArtifactDescriptor]
      def contains(descriptor: IArtifactDescriptor): Boolean
      def contains(key: IArtifactKey): Boolean
      def getLocation(descriptor: IArtifactDescriptor): URI
    }
  }
}

object ResolveP2 {
  implicit def resolveP22implementation(r: ResolveP2.type): ResolveP2 = r.inner
  /** Resolve P2 implementation. */
  lazy val inner = new ResolveP2()

  object EmptyP2ResolutionResult extends P2ResolutionResult {
    val artifacts = Collections.emptyList[P2ResolutionResult.Entry]()
    val reactorUnits = Collections.emptySet()
    def getArtifacts() = artifacts
    def getNonReactorUnits() = reactorUnits
  }
}
