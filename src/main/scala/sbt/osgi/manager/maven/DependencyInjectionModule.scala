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

package sbt.osgi.manager.maven

import com.google.inject.{ AbstractModule, Provides, Singleton }
import com.google.inject.name.{ Named, Names }
import java.util.{ Collections, HashSet, Set }
import org.apache.maven.SessionScope
import org.apache.maven.execution.scope.internal.MojoExecutionScope
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory
import org.eclipse.aether.{ AbstractRepositoryListener, RepositoryListener }
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.MetadataGeneratorFactory
import org.eclipse.aether.internal.impl.{ DefaultChecksumPolicyProvider, EnhancedLocalRepositoryManagerFactory, Maven2RepositoryLayoutFactory }
import org.eclipse.aether.internal.transport.wagon.{ PlexusWagonConfigurator, PlexusWagonProvider }
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.{ WagonConfigurator, WagonProvider }

class DependencyInjectionModule extends AbstractModule {
  val singletonRepositoryLayoutFactory = new Maven2RepositoryLayoutFactory()
  val singletonRepositoryListener = new DependencyInjectionModule.StubRepositoryListener
  val singletonLocalRepositoryManagerFactory = new EnhancedLocalRepositoryManagerFactory
  val singletonMetadataGeneratorFactory = new SnapshotMetadataGeneratorFactory

  override protected def configure() {
    //install(new MavenAetherModule())
    // alternatively, use the Guice Multibindings extensions
    bind(classOf[RepositoryConnectorFactory]).annotatedWith(Names.named("basic")).to(classOf[BasicRepositoryConnectorFactory])
    bind(classOf[TransporterFactory]).annotatedWith(Names.named("file")).to(classOf[FileTransporterFactory])
    bind(classOf[TransporterFactory]).annotatedWith(Names.named("http")).to(classOf[HttpTransporterFactory])
    bind(classOf[ChecksumPolicyProvider]).to(classOf[DefaultChecksumPolicyProvider])
    bind(classOf[WagonProvider]).to(classOf[PlexusWagonProvider])
    bind(classOf[WagonConfigurator]).to(classOf[PlexusWagonConfigurator])
    bind(classOf[MojoExecutionScope]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[MojoExecutionScope])
    bind(classOf[SessionScope]).annotatedWith(Names.named("org.apache.maven.Maven")).to(classOf[SessionScope])
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
}

object DependencyInjectionModule {
  class StubRepositoryListener extends AbstractRepositoryListener
}
