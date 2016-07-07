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

package sbt.osgi.manager.support

import java.io.File
import java.net.{ URL, URLClassLoader }
import org.codehaus.plexus.DefaultPlexusContainer
import org.digimead.sbt.util.{ StaticLoggerBinder, Util }
import org.digimead.sbt.util.SLF4JBridge
import org.digimead.sbt.util.Util.Util2implementation
import org.scalatest.{ Finders, FreeSpec, Matchers }
import sbt.{ AttributeEntry, AttributeMap, BasicCommands, Build, BuildStreams, BuildStructure, BuildUnit, BuildUtil, ConsoleOut, Def, DetectedAutoPlugin, DetectedModules, DetectedPlugins, File, GlobalLogging, KeyIndex, Keys, Load, LoadedDefinitions, LoadedPlugins, MainLogging, PartBuildUnit, Plugin, PluginData, Project, ProjectRef, Scope, SessionSettings, Settings, State, StructureIndex, This }
import sbt.osgi.manager.{ Dependency, Environment, OSGi, OSGiConf, OSGiKey, Plugin, Test }
import sbt.osgi.manager.tycho.LoggerSLF4J
import scala.language.implicitConversions

class PluginClassLoaderTest extends FreeSpec with Matchers {
  "test" in {
    val coursier = Array(
      "file:/home/user/.sbt/0.13/plugins/target/scala-2.10/sbt-0.13/classes",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/ow2/asm/asm/5.1/asm-5.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-component-annotations/1.6/plexus-component-annotations-1.6.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/scalaz/scalaz-concurrent_2.10/7.1.2/scalaz-concurrent_2.10-7.1.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/scalaz/scalaz-core_2.10/7.1.2/scalaz-core_2.10-7.1.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.10.6/scala-library-2.10.6.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/org.eclipse.osgi.compatibility.state/1.0.100.v20150402-1551/org.eclipse.osgi.compatibility.state-1.0.100.v20150402-1551.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/tycho-metadata-model/0.25.0/tycho-metadata-model-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/wagon/wagon-provider-api/2.10/wagon-provider-api-2.10.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/tycho-core/0.25.0/tycho-core-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-plugin-api/3.3.9/maven-plugin-api-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/com/google/inject/guice/4.0/guice-4.0-no_aop.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/osgi/org.osgi.enterprise/5.0.0/org.osgi.enterprise-5.0.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/javax/enterprise/cdi-api/1.0/cdi-api-1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/biz/aQute/bnd/bndlib/2.3.0/bndlib-2.3.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/commons-cli/commons-cli/1.2/commons-cli-1.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/org.eclipse.tycho.p2.tools.shared/0.25.0/org.eclipse.tycho.p2.tools.shared-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21.jar",
      "file:/home/user/.coursier/cache/v1/http/commondatastorage.googleapis.com/maven.repository.digimead.org/org/digimead/sbt-dependency-manager_2.10_0.13/0.8.0.2-SNAPSHOT/sbt-dependency-manager-0.8.0.2-SNAPSHOT.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/osgi/org.osgi.core/6.0.0/org.osgi.core-6.0.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-archiver/2.9.1/plexus-archiver-2.9.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/sisu/org.eclipse.sisu.inject/0.3.2/org.eclipse.sisu.inject-0.3.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/tycho-embedder-api/0.25.0/tycho-embedder-api-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/slf4j/jcl-over-slf4j/1.6.2/jcl-over-slf4j-1.6.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-interpolation/1.21/plexus-interpolation-1.21.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/sisu-equinox-embedder/0.25.0/sisu-equinox-embedder-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/javax/annotation/jsr250-api/1.0/jsr250-api-1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/org.eclipse.osgi/3.10.101.v20150820-1432/org.eclipse.osgi-3.10.101.v20150820-1432.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-impl/1.1.0/aether-impl-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-transport-file/1.1.0/aether-transport-file-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-settings-builder/3.3.9/maven-settings-builder-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/scalaz/scalaz-effect_2.10/7.1.2/scalaz-effect_2.10-7.1.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-connector-basic/1.1.0/aether-connector-basic-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/oss.sonatype.org/content/repositories/snapshots/io/get-coursier/sbt-coursier_2.10_0.13/1.0.0-SNAPSHOT/sbt-coursier-1.0.0-SNAPSHOT.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-transport-wagon/1.1.0/aether-transport-wagon-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/oss.sonatype.org/content/repositories/snapshots/io/get-coursier/coursier-cache_2.10/1.0.0-SNAPSHOT/coursier-cache_2.10-1.0.0-SNAPSHOT.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/sonatype/plexus/plexus-cipher/1.7/plexus-cipher-1.7.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.4/commons-lang3-3.4.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/org.eclipse.tycho.p2.resolver.shared/0.25.0/org.eclipse.tycho.p2.resolver.shared-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-classworlds/2.5.2/plexus-classworlds-2.5.2.jar",
      "file:/home/user/.coursier/cache/v1/https/oss.sonatype.org/content/repositories/snapshots/io/get-coursier/coursier_2.10/1.0.0-SNAPSHOT/coursier_2.10-1.0.0-SNAPSHOT.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-io/2.4.1/plexus-io-2.4.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/jsoup/jsoup/1.9.2/jsoup-1.9.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/osgi/org.osgi.annotation/6.0.0/org.osgi.annotation-6.0.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/sonatype/plexus/plexus-sec-dispatcher/1.3/plexus-sec-dispatcher-1.3.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/com/google/guava/guava/18.0/guava-18.0.jar",
      "file:/home/user/.coursier/cache/v1/http/commondatastorage.googleapis.com/maven.repository.digimead.org/org/digimead/digi-sbt-util_2.10/0.2.0.0/digi-sbt-util_2.10-0.2.0.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/tycho-p2-facade/0.25.0/tycho-p2-facade-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-artifact/3.3.9/maven-artifact-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/sonatype/aether/aether-spi/1.13.1/aether-spi-1.13.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/sisu-equinox-api/0.25.0/sisu-equinox-api-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-model/3.3.9/maven-model-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-api/1.1.0/aether-api-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.2/commons-io-2.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-model-builder/3.3.9/maven-model-builder-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-settings/3.3.9/maven-settings-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/aopalliance/aopalliance/1.0/aopalliance-1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-embedder/3.3.9/maven-embedder-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/ow2/asm/asm-commons/5.1/asm-commons-5.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/sonatype/aether/aether-api/1.13.1/aether-api-1.13.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-aether-provider/3.3.9/maven-aether-provider-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.3.5/httpclient-4.3.5.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/org.eclipse.tycho.core.shared/0.25.0/org.eclipse.tycho.core.shared-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-transport-http/1.1.0/aether-transport-http-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/sonatype/aether/aether-impl/1.13.1/aether-impl-1.13.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/commons-codec/commons-codec/1.6/commons-codec-1.6.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/sonatype/aether/aether-util/1.13.1/aether-util-1.13.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-core/3.3.9/maven-core-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-util/1.1.0/aether-util-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/sisu/org.eclipse.sisu.plexus/0.3.2/org.eclipse.sisu.plexus-0.3.2.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-utils/3.0.22/plexus-utils-3.0.22.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/tycho/org.eclipse.tycho.embedder.shared/0.25.0/org.eclipse.tycho.embedder.shared-0.25.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-repository-metadata/3.3.9/maven-repository-metadata-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-builder-support/3.3.9/maven-builder-support-3.3.9.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/de/pdark/decentxml/1.3/decentxml-1.3.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/eclipse/aether/aether-spi/1.1.0/aether-spi-1.1.0.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/ow2/asm/asm-tree/5.1/asm-tree-5.1.jar",
      "file:/home/user/.coursier/cache/v1/https/repo1.maven.org/maven2/org/apache/maven/maven-compat/3.3.9/maven-compat-3.3.9.jar").map(str ⇒ new URL(str))
    implicit val arg = Plugin.TaskArgument(FakeState.state, ProjectRef(FakeState.testProject.base.toURI(), FakeState.testProject.id))
    val (a, b, c) = PluginClassLoader.processClassLoaderURLs(coursier)
    val aSorted = a.map(_.toString.reverse.takeWhile(_ != '/').reverse).sorted
    val bSorted = b.map(_.toString.reverse.takeWhile(_ != '/').reverse).sorted
    val cSorted = c.map(_.toString.reverse.takeWhile(_ != '/').reverse).sorted
    val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
    val (d, e, f) = PluginClassLoader.processClassLoaderURLs(loader.getURLs)
    val dSorted = d.map(_.toString.reverse.takeWhile(_ != '/').reverse).sorted
    val eSorted = e.map(_.toString.reverse.takeWhile(_ != '/').reverse).sorted
    val fSorted = f.map(_.toString.reverse.takeWhile(_ != '/').reverse).sorted

    a.size should be(49)
    d.size should be(49)
    aSorted should be(dSorted)

    b.size should be(19)
    e.size should be(69)

    c.size should be(13)
    f.size should be(9)

    val pluginJar = getClass.getProtectionDomain.getCodeSource.getLocation
    val pluginClassLoader = new PluginClassLoader(pluginJar +: a, b, loader, Seq(classOf[PluginClassLoaderTest.A].getName))
    pluginClassLoader.getURLs.size should be(a.size + 1)
    val (a1, b1, c1, d1) =
      SLF4JBridge.withLogFactory(LoggerSLF4J.Factory) {
        Util.applyWithClassLoader[(ClassLoader, ClassLoader, AnyRef, AnyRef)](pluginClassLoader, classOf[PluginClassLoaderTest.A])
      }
    assert(a1.isInstanceOf[PluginClassLoader])
    //assert(b1.isInstanceOf[PluginClassLoader])
    c1 should not be (null)
    d1 should not be (null)
    d1 should be(LoggerSLF4J.Factory)
  }

  object FakeState {
    lazy val settings: Seq[Def.Setting[_]] = Seq.empty

    val base = new File("").getAbsoluteFile
    val testProject = Project("test-project", base)

    val currentProject = Map(testProject.base.toURI -> testProject.id)
    val currentEval: () ⇒ sbt.compiler.Eval = () ⇒ Load.mkEval(Nil, base, Nil)
    val sessionSettings = SessionSettings(base.toURI, currentProject, Nil, Map.empty, Nil, currentEval)

    val delegates: (Scope) ⇒ Seq[Scope] = scope ⇒ Seq(scope, Scope(This, scope.config, This, This))
    val scopeLocal: Def.ScopeLocal = _ ⇒ Nil

    val data: Settings[Scope] = Def.make(settings)(delegates, scopeLocal, Def.showFullKey)
    val extra: KeyIndex ⇒ BuildUtil[_] = (keyIndex) ⇒ BuildUtil(base.toURI, Map.empty, keyIndex, data)
    val structureIndex: StructureIndex = Load.structureIndex(data, settings, extra, Map.empty)
    val streams: (State) ⇒ BuildStreams.Streams = null

    val loadedDefinitions: LoadedDefinitions = new LoadedDefinitions(
      base, Nil, ClassLoader.getSystemClassLoader, Nil, Seq(testProject), Nil)

    val pluginData = PluginData(Nil, Nil, None, None, Nil)
    val detectedModules: DetectedModules[Plugin] = new DetectedModules(Nil)
    val builds: DetectedModules[Build] = new DetectedModules[Build](Nil)

    val detectedAutoPlugins: Seq[DetectedAutoPlugin] = Seq.empty
    val detectedPlugins = new DetectedPlugins(detectedModules, detectedAutoPlugins, builds)
    val loadedPlugins = new LoadedPlugins(base, pluginData, ClassLoader.getSystemClassLoader, detectedPlugins)
    val buildUnit = new BuildUnit(base.toURI, base, loadedDefinitions, loadedPlugins)

    val (partBuildUnit: PartBuildUnit, _) = Load.loaded(buildUnit)
    val loadedBuildUnit = Load.resolveProjects(base.toURI, partBuildUnit, _ ⇒ testProject.id)

    val units = Map(base.toURI -> loadedBuildUnit)
    val buildStructure = new BuildStructure(units, base.toURI, settings, data, structureIndex, streams, delegates, scopeLocal)

    val attributes = AttributeMap.empty ++ AttributeMap(
      AttributeEntry(Keys.sessionSettings, sessionSettings),
      AttributeEntry(Keys.stateBuildStructure, buildStructure))

    val initialGlobalLogging = GlobalLogging.initial(MainLogging.globalDefault(ConsoleOut.systemOut), File.createTempFile("sbt", ".log"), ConsoleOut.systemOut)
    val commandDefinitions = BasicCommands.allBasicCommands
    val state = State(null, commandDefinitions, Set.empty, None, Seq.empty, State.newHistory,
      attributes, initialGlobalLogging, State.Continue)
  }
}

object PluginClassLoaderTest {
  class A {
    def apply() = {
      val a = getClass.getClassLoader
      val b = Class.forName("org.codehaus.plexus.DefaultPlexusContainer")
      val c = new DefaultPlexusContainer()
      Class.forName("org.osgi.framework.Bundle")
      (a, b.getClassLoader, c, StaticLoggerBinder.getSingleton.getLoggerFactory)
    }
  }
}
