/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2016 Alexey Aksenov ezh@ezh.msk.ru
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

import com.google.inject.{ AbstractModule, Provides, Singleton }
import com.google.inject.name.{ Named, Names }
import java.util.{ ArrayList, Collections, HashSet, List, Set }
import javax.inject.{ Inject, Provider }
import org.apache.maven.classrealm.{ ClassRealmManager, ClassRealmManagerDelegate, DefaultClassRealmManager }
import org.apache.maven.execution.{ DefaultMavenExecutionRequestPopulator, MavenExecutionRequestPopulator }
import org.apache.maven.execution.scope.internal.MojoExecutionScope
import org.apache.maven.extension.internal.{ CoreExports, CoreExtensionEntry }
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory
import org.apache.maven.session.scope.internal.SessionScope
import org.codehaus.plexus.PlexusContainer
import org.eclipse.aether.{ AbstractRepositoryListener, RepositoryListener, RepositorySystem }
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.{ ArtifactResolver, DependencyCollector, Deployer, Installer, LocalRepositoryProvider, MetadataGeneratorFactory, MetadataResolver, OfflineController, RemoteRepositoryManager, RepositoryConnectorProvider, RepositoryEventDispatcher, SyncContextFactory, UpdateCheckManager, UpdatePolicyAnalyzer }
import org.eclipse.aether.internal.impl.{ DefaultArtifactResolver, DefaultChecksumPolicyProvider, DefaultDependencyCollector, DefaultDeployer, DefaultFileProcessor, DefaultInstaller, DefaultLocalRepositoryProvider, DefaultMetadataResolver, DefaultOfflineController, DefaultRemoteRepositoryManager, DefaultRepositoryConnectorProvider, DefaultRepositoryEventDispatcher, DefaultRepositoryLayoutProvider, DefaultRepositorySystem, DefaultSyncContextFactory, DefaultTransporterProvider, DefaultUpdateCheckManager, DefaultUpdatePolicyAnalyzer, EnhancedLocalRepositoryManagerFactory, Maven2RepositoryLayoutFactory }
import org.eclipse.aether.internal.transport.wagon.{ PlexusWagonConfigurator, PlexusWagonProvider }
import org.eclipse.aether.repository.WorkspaceReader
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider
import org.eclipse.aether.spi.connector.layout.{ RepositoryLayoutFactory, RepositoryLayoutProvider }
import org.eclipse.aether.spi.connector.transport.{ TransporterFactory, TransporterProvider }
import org.eclipse.aether.spi.io.FileProcessor
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory
import org.eclipse.aether.spi.log.{ LoggerFactory, NullLoggerFactory }
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.{ WagonConfigurator, WagonProvider }
import sbt.osgi.manager.tycho.DependencyInjectionModule.{ CoreExportsProvider, IDEWorkspaceReader }

class DependencyInjectionModule extends AbstractModule {
  val singletonRepositoryLayoutFactory = new Maven2RepositoryLayoutFactory()
  val singletonRepositoryListener = new DependencyInjectionModule.StubRepositoryListener
  val singletonLocalRepositoryManagerFactory = new EnhancedLocalRepositoryManagerFactory
  val singletonMetadataGeneratorFactory = new SnapshotMetadataGeneratorFactory

  override protected def configure() {
    bind(classOf[ArtifactResolver]).to(classOf[DefaultArtifactResolver])
    bind(classOf[ChecksumPolicyProvider]).to(classOf[DefaultChecksumPolicyProvider])
    bind(classOf[ClassRealmManager]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[DefaultClassRealmManager])
    bind(classOf[CoreExports]).toProvider(classOf[DependencyInjectionModule.CoreExportsProvider])
    bind(classOf[DefaultLocalRepositoryProvider]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[DefaultLocalRepositoryProvider])
    bind(classOf[DefaultRepositorySystemSessionFactory]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[DefaultRepositorySystemSessionFactory])
    bind(classOf[DependencyCollector]).to(classOf[DefaultDependencyCollector])
    bind(classOf[Deployer]).to(classOf[DefaultDeployer])
    bind(classOf[FileProcessor]).to(classOf[DefaultFileProcessor])
    bind(classOf[Installer]).to(classOf[DefaultInstaller])
    bind(classOf[LocalRepositoryManagerFactory]).annotatedWith(Names.named("simple")).toInstance(singletonLocalRepositoryManagerFactory)
    bind(classOf[LocalRepositoryProvider]).to(classOf[DefaultLocalRepositoryProvider])
    bind(classOf[MavenExecutionRequestPopulator]).to(classOf[DefaultMavenExecutionRequestPopulator])
    bind(classOf[MetadataResolver]).to(classOf[DefaultMetadataResolver])
    bind(classOf[MojoExecutionScope]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[MojoExecutionScope])
    bind(classOf[OfflineController]).to(classOf[DefaultOfflineController])
    bind(classOf[RemoteRepositoryManager]).to(classOf[DefaultRemoteRepositoryManager])
    bind(classOf[RepositoryConnectorFactory]).annotatedWith(Names.named("basic")).to(classOf[BasicRepositoryConnectorFactory])
    bind(classOf[RepositoryConnectorProvider]).to(classOf[DefaultRepositoryConnectorProvider])
    bind(classOf[RepositoryEventDispatcher]).to(classOf[DefaultRepositoryEventDispatcher])
    bind(classOf[RepositoryLayoutProvider]).to(classOf[DefaultRepositoryLayoutProvider])
    bind(classOf[RepositorySystem]).to(classOf[DefaultRepositorySystem])
    bind(classOf[SessionScope]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[SessionScope])
    bind(classOf[SyncContextFactory]).to(classOf[DefaultSyncContextFactory])
    bind(classOf[TransporterFactory]).annotatedWith(Names.named("file")).to(classOf[FileTransporterFactory])
    bind(classOf[TransporterFactory]).annotatedWith(Names.named("http")).to(classOf[HttpTransporterFactory])
    bind(classOf[TransporterProvider]).to(classOf[DefaultTransporterProvider])
    bind(classOf[UpdateCheckManager]).to(classOf[DefaultUpdateCheckManager])
    bind(classOf[UpdatePolicyAnalyzer]).to(classOf[DefaultUpdatePolicyAnalyzer])
    bind(classOf[WagonConfigurator]).to(classOf[PlexusWagonConfigurator])
    bind(classOf[WagonProvider]).to(classOf[PlexusWagonProvider])
    bind(classOf[WorkspaceReader]).annotatedWith(Names.named("ide")).to(classOf[DependencyInjectionModule.IDEWorkspaceReader])
  }

  @Provides
  @Singleton
  def provideRepositoryConnectorFactories(@Named("basic") basic: RepositoryConnectorFactory): Set[RepositoryConnectorFactory] = {
    val factories = new HashSet[RepositoryConnectorFactory]()
    factories.add(basic)
    Collections.unmodifiableSet(factories)
  }

  @Provides
  @Singleton
  def provideTransporterFactories(@Named("file") file: TransporterFactory,
    @Named("http") http: TransporterFactory): Set[TransporterFactory] = {
    val factories = new HashSet[TransporterFactory]()
    factories.add(file)
    factories.add(http)
    Collections.unmodifiableSet(factories)
  }

  @Provides
  @Singleton
  def provideRepositoryLayoutFactory(): Set[RepositoryLayoutFactory] = {
    val factories = new HashSet[RepositoryLayoutFactory]()
    factories.add(singletonRepositoryLayoutFactory)
    Collections.unmodifiableSet(factories)
  }

  @Provides
  @Singleton
  def provideRepositoryListener(): Set[RepositoryListener] = {
    val factories = new HashSet[RepositoryListener]()
    factories.add(singletonRepositoryListener)
    Collections.unmodifiableSet(factories)
  }

  @Provides
  @Singleton
  def provideLocalRepositoryManagerFactory(): Set[LocalRepositoryManagerFactory] = {
    val factories = new HashSet[LocalRepositoryManagerFactory]()
    factories.add(singletonLocalRepositoryManagerFactory)
    Collections.unmodifiableSet(factories)
  }

  @Provides
  @Singleton
  def provideMetadataGeneratorFactory(): Set[MetadataGeneratorFactory] = {
    val factories = new HashSet[MetadataGeneratorFactory]()
    factories.add(singletonMetadataGeneratorFactory)
    Collections.unmodifiableSet(factories)
  }
  @Provides
  @Singleton
  def provideLoggerFactory(): LoggerFactory = {
    NullLoggerFactory.INSTANCE
  }
  @Provides
  @Singleton
  def provideClassRealmManagerDelegate(): List[ClassRealmManagerDelegate] = {
    new ArrayList[ClassRealmManagerDelegate]()
  }
}

object DependencyInjectionModule {
  class StubRepositoryListener extends AbstractRepositoryListener
  class CoreExportsProvider @Inject() (container: PlexusContainer) extends Provider[CoreExports] {
    val _exports: CoreExports = new CoreExports(CoreExtensionEntry.discoverFrom(container.getContainerRealm()))
    def get(): CoreExports = _exports
  }
  class IDEWorkspaceReader extends WorkspaceReader {
    def getRepository() = null
    def findArtifact(artifact: Artifact) = null
    def findVersions(artifact: Artifact): List[String] = new ArrayList()
  }
}
