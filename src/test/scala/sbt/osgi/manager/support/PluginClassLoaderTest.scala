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

import java.net.URLClassLoader
import org.codehaus.plexus.DefaultPlexusContainer
import org.digimead.sbt.util.{ StaticLoggerBinder, Util }
import org.digimead.sbt.util.SLF4JBridge
import org.scalatest.{ Finders, FreeSpec, Matchers }
import sbt.osgi.manager.tycho.LoggerSLF4J
import scala.language.implicitConversions

class PluginClassLoaderTest extends FreeSpec with Matchers {
  "test" in {
    val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]
    val allDependencies = loader.getURLs.sortBy { _.toString() }.filter { _.toString().contains("/.ivy2/cache/") }.
      map(_.getFile.replaceFirst(""".*/\.ivy2/cache/""", "").
        replaceFirst("""/jars/.*""", "").replaceFirst("""/eclipse-plugins/.*""", "").replaceFirst("""/bundles/.*""", ""))
    val (internal, external) = allDependencies.partition { s ⇒
      s.startsWith("org.apache.maven") ||
        s.startsWith("org.codehaus") ||
        s.startsWith("org.eclipse") ||
        s.startsWith("org.sonatype") ||
        s.contains("org.slf4j")
    }
    println("Internal:")
    println("\"" + internal.mkString("\",\n\"") + "\"")
    println("External:")
    println("\"" + external.mkString("\",\n\"") + "\"")
    val internalURLs = loader.getURLs().filter { url ⇒ PluginClassLoader.internalLibraries.find { url.toString().contains }.nonEmpty }
    val externalURLs = loader.getURLs().filter { url ⇒ PluginClassLoader.externalLibraries.find { url.toString().contains }.nonEmpty }
    val pluginClassLoader = new PluginClassLoader(internalURLs, externalURLs, loader, Seq(classOf[PluginClassLoaderTest.A].getName))
    pluginClassLoader.getURLs.size should be(internal.size)
    val (a, b, c, d) =
      SLF4JBridge.withLogFactory(LoggerSLF4J.Factory) {
        Util.applyWithClassLoader[(ClassLoader, ClassLoader, AnyRef, AnyRef)](pluginClassLoader, classOf[PluginClassLoaderTest.A])
      }
    assert(a.isInstanceOf[PluginClassLoader])
    assert(b.isInstanceOf[PluginClassLoader])
    c should not be (null)
    d should not be (null)
    d should be(LoggerSLF4J.Factory)
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
