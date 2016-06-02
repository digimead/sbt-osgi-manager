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

import java.net.{ URL, URLClassLoader }
import org.digimead.sbt.util.SLF4JBridge
import scala.language.implicitConversions

class PluginClassLoader(internal: Array[URL], external: Array[URL], parent: ClassLoader, reloadPrefix: Seq[String])
    extends URLClassLoader(internal, parent) with SLF4JBridge.Loader {
  protected val externalURLClassLoader = new URLClassLoader(external)

  override def getResource(name: String): URL =
    Option(findResource(name)).getOrElse(parent.getResource(name))
  @throws(classOf[ClassNotFoundException])
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    name match {
      case this.SLF4JBinderTargetName ⇒
        // transform with ASM
        val bytecode = loadSLF4JBinder(name)
        defineClass(name, bytecode, 0, bytecode.length)
      case name if reloadPrefix.find(name.startsWith).nonEmpty ⇒
        Option(findLoadedClass(name)) getOrElse {
          try findClass(name) catch {
            case e: ClassNotFoundException ⇒
              // reload with this class loader
              loadClass(parent.loadClass(name))
          }
        }
      case name if PluginClassLoader.passToParent.find(name.startsWith).nonEmpty ⇒
        // pass to parent
        parent.loadClass(name)
      case name ⇒
        // try to load or pass to parent if we found a name in the externalURLClassLoader
        Option(findLoadedClass(name)) getOrElse {
          try {
            val clazz = findClass(name)
            // a bit of debug shit
            // if (name.startsWith("org.osgi"))
            //   println(clazz.getProtectionDomain.getCodeSource.getLocation)
            clazz
          } catch {
            case e: ClassNotFoundException ⇒
              // Filter class loading with ClassNotFoundException
              externalURLClassLoader.loadClass(name)
              parent.loadClass(name)
            case e: Throwable ⇒
              // println("PluginClassLoader exception: " + e)
              parent.loadClass(name)
          }
        }
    }
  }
  def loadClass(clazz: Class[_]): Class[_] = {
    val is = clazz.getResourceAsStream('/' + clazz.getName().replace('.', '/') + ".class")
    val bytes = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
    defineClass(clazz.getName, bytes, 0, bytes.length)
  }
}

object PluginClassLoader {
  implicit def PluginClassLoader2implementation(p: PluginClassLoader.type): PluginClassLoader = p.inner
  /** PluginClassLoader implementation. */
  lazy val inner = {
    getClass.getClassLoader() match {
      case loader: URLClassLoader ⇒
        val internalURLs = loader.getURLs().filter { url ⇒ PluginClassLoader.internalLibraries.find { url.toString().contains }.nonEmpty }
        val externalURLs = loader.getURLs().filter { url ⇒ PluginClassLoader.externalLibraries.find { url.toString().contains }.nonEmpty }
        new PluginClassLoader(internalURLs, externalURLs, loader, Seq("sbt.osgi.manager.tycho", "sbt.osgi.manager.bnd"))
      case classLoader ⇒
        throw new IllegalStateException("Unable to create PluginClassLoader with unexpected parent class loader " + classLoader.getClass)
    }
  }
  /** List of URL parts of libraries that PluginClassLoader must load itself. */
  val internalLibraries = Seq(
    "org.apache.maven.wagon/wagon-provider-api",
    "org.apache.maven/maven-aether-provider",
    "org.apache.maven/maven-artifact",
    "org.apache.maven/maven-builder-support",
    "org.apache.maven/maven-compat",
    "org.apache.maven/maven-core",
    "org.apache.maven/maven-embedder",
    "org.apache.maven/maven-model",
    "org.apache.maven/maven-model-builder",
    "org.apache.maven/maven-plugin-api",
    "org.apache.maven/maven-repository-metadata",
    "org.apache.maven/maven-settings",
    "org.apache.maven/maven-settings-builder",
    "org.codehaus.plexus/plexus-archiver",
    "org.codehaus.plexus/plexus-classworlds",
    "org.codehaus.plexus/plexus-component-annotations",
    "org.codehaus.plexus/plexus-interpolation",
    "org.codehaus.plexus/plexus-io",
    "org.codehaus.plexus/plexus-utils",
    "org.digimead/sbt-osgi-manager",
    "org.eclipse.aether/aether-api",
    "org.eclipse.aether/aether-connector-basic",
    "org.eclipse.aether/aether-impl",
    "org.eclipse.aether/aether-spi",
    "org.eclipse.aether/aether-transport-file",
    "org.eclipse.aether/aether-transport-http",
    "org.eclipse.aether/aether-transport-wagon",
    "org.eclipse.aether/aether-util",
    "org.eclipse.sisu/org.eclipse.sisu.inject",
    "org.eclipse.sisu/org.eclipse.sisu.plexus",
    "org.eclipse.tycho/org.eclipse.osgi",
    "org.eclipse.tycho/org.eclipse.osgi.compatibility.state",
    "org.eclipse.tycho/org.eclipse.tycho.core.shared",
    "org.eclipse.tycho/org.eclipse.tycho.embedder.shared",
    "org.eclipse.tycho/org.eclipse.tycho.p2.resolver.shared",
    "org.eclipse.tycho/org.eclipse.tycho.p2.tools.shared",
    "org.eclipse.tycho/sisu-equinox-api",
    "org.eclipse.tycho/sisu-equinox-embedder",
    "org.eclipse.tycho/tycho-core",
    "org.eclipse.tycho/tycho-embedder-api",
    "org.eclipse.tycho/tycho-metadata-model",
    "org.eclipse.tycho/tycho-p2-facade",
    "org.slf4j/jcl-over-slf4j",
    "org.slf4j/slf4j-api",
    "org.sonatype.aether/aether-api",
    "org.sonatype.aether/aether-impl",
    "org.sonatype.aether/aether-spi",
    "org.sonatype.aether/aether-util",
    "org.sonatype.plexus/plexus-cipher",
    "org.sonatype.plexus/plexus-sec-dispatcher")
  /** List of URL parts of libraries that PluginClassLoader must delegate to parent. */
  val externalLibraries = Seq(
    "aopalliance/aopalliance",
    "biz.aQute.bnd/bndlib",
    "com.google.guava/guava",
    "com.google.inject/guice",
    "com.jcraft/jsch",
    "com.thoughtworks.paranamer/paranamer",
    "commons-cli/commons-cli",
    "commons-codec/commons-codec",
    "commons-io/commons-io",
    "de.pdark/decentxml",
    "javax.annotation/jsr250-api",
    "javax.enterprise/cdi-api",
    "javax.inject/javax.inject",
    "jline/jline",
    "org.apache.commons/commons-compress",
    "org.apache.commons/commons-lang3",
    "org.apache.httpcomponents/httpclient",
    "org.apache.httpcomponents/httpcore",
    "org.digimead/digi-sbt-util",
    "org.fusesource.jansi/jansi",
    "org.json4s/json4s-ast",
    "org.json4s/json4s-core",
    //"org.osgi/org.osgi.annotation",
    //"org.osgi/org.osgi.core",
    //"org.osgi/org.osgi.enterprise",
    "org.ow2.asm/asm",
    "org.ow2.asm/asm-commons",
    "org.ow2.asm/asm-tree",
    "org.scala-lang.modules/scala-pickling",
    "org.scala-sbt.ivy/ivy",
    "org.scala-sbt/actions",
    "org.scala-sbt/api",
    "org.scala-sbt/apply-macro",
    "org.scala-sbt/cache",
    "org.scala-sbt/classfile",
    "org.scala-sbt/classpath",
    "org.scala-sbt/collections",
    "org.scala-sbt/command",
    "org.scala-sbt/compile",
    "org.scala-sbt/compiler-integration",
    "org.scala-sbt/compiler-interface",
    "org.scala-sbt/compiler-ivy-integration",
    "org.scala-sbt/completion",
    "org.scala-sbt/control",
    "org.scala-sbt/cross",
    "org.scala-sbt/incremental-compiler",
    "org.scala-sbt/interface",
    "org.scala-sbt/io",
    "org.scala-sbt/ivy",
    "org.scala-sbt/launcher-interface",
    "org.scala-sbt/logging",
    "org.scala-sbt/logic",
    "org.scala-sbt/main-settings",
    "org.scala-sbt/main",
    "org.scala-sbt/persist",
    "org.scala-sbt/process",
    "org.scala-sbt/relation",
    "org.scala-sbt/run",
    "org.scala-sbt/sbt",
    "org.scala-sbt/serialization",
    "org.scala-sbt/task-system",
    "org.scala-sbt/tasks",
    "org.scala-sbt/test-agent",
    "org.scala-sbt/test-interface",
    "org.scala-sbt/testing",
    "org.scala-sbt/tracking",
    "org.scala-tools.sbinary/sbinary",
    "org.scalamacros/quasiquotes",
    "org.spire-math/jawn-parser",
    "org.spire-math/json4s-support")
  /** List of class names than passes to parent class loader. */
  val passToParent = Seq(
    "java.",
    "org.apache.maven.model.Dependency",
    "org.digimead.sbt.util.StaticLoggerBinder",
    "org.slf4j.ILoggerFactory",
    "org.slf4j.spi.LoggerFactoryBinder",
    "org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration",
    "sbt.",
    "scala.")
}
