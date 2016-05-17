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

package sbt.osgi.manager

import java.util.{ Locale, Properties }
import org.codehaus.plexus.util.Os
import org.eclipse.equinox.internal.p2.metadata.VersionParser
import org.eclipse.equinox.p2.metadata.Version
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration
import sbt.{ Keys ⇒ skey, ModuleID, Resolver, Scope, URLRepository }
import scala.collection.JavaConversions.asScalaSet
import scala.collection.mutable
import scala.language.implicitConversions

object Support {
  implicit def option2rich[T](option: Option[T]): RichOption[T] = new RichOption(option)

  /** Collect environment variables to Properties container */
  def getEnvVars(): Properties = {
    val envVars = new Properties()
    val caseSensitive = !Os.isFamily(Os.FAMILY_WINDOWS)
    for (entry ← System.getenv().entrySet()) {
      val key = "env." + (if (caseSensitive) entry.getKey() else entry.getKey().toUpperCase(Locale.ENGLISH))
      envVars.setProperty(key, entry.getValue())
    }
    envVars
  }
  /** Returns dependencies */
  def getDependencies(dependencyType: Dependency.Type, scope: Scope)(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    ((skey.libraryDependencies in scope get arg.extracted.structure.data): Option[Seq[ModuleID]]).
      getOrElse(Seq[ModuleID]()).filter(_ match {
        case dependency if dependency.extraAttributes.get(dependencyType.key) == Some(dependencyType.name) ⇒
          arg.log.debug(logPrefix(arg.name) + "Add %s dependency \"%s\"".format(dependencyType.name, dependency.copy(extraAttributes = Map())))
          true
        case otherDependency ⇒
          //arg.log.debug(logPrefix(name) + "Skip dependency " + otherDependency)
          false
      })
  }
  /** Returns resolvers as Seq[(id, url)] */
  def getResolvers(dependencyType: Dependency.Type, scope: Scope)(implicit arg: Plugin.TaskArgument): Seq[(String, String)] = {
    ((skey.resolvers in scope get arg.extracted.structure.data): Option[Seq[Resolver]]).
      getOrElse(Seq[Resolver]()).filter(_ match {
        case resolver: URLRepository if resolver.patterns.artifactPatterns == Seq(dependencyType.name) ⇒
          val repo = resolver.patterns.ivyPatterns.head // always one element, look at markResolverAsP2
          arg.log.debug(logPrefix(arg.name) + "Add %s resolver \"%s\" at %s".format(dependencyType.name, resolver.name, repo))
          true
        case otherResolver ⇒
          //arg.log.debug(logPrefix(name) + "Skip resolver " + otherResolver)
          false
      }).map {
        case resolver: URLRepository ⇒ (resolver.name, resolver.patterns.ivyPatterns.head)
        case resolver ⇒ throw new OSGiManagerException("Unknown resolver " + resolver)
      }
  }
  /** Default sbt-osgi-manager log prefix */
  def logPrefix(name: String) = "[OSGi manager:%s] ".format(name)
  /**
   * Executes the function f within the ContextClassLoader of 'classOf'.
   * After execution the original ClassLoader will be restored.
   */
  def withClassLoaderOf[T](classOf: Class[_])(f: ⇒ T): T = {
    val thread = Thread.currentThread
    val oldContext = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(classOf.getClassLoader)
      f
    } finally {
      thread.setContextClassLoader(oldContext)
    }
  }
  /**
   * Executes the function f within the ClassLoader.
   * After execution the original ClassLoader will be restored.
   */
  def withClassLoaderOf[T](loader: ClassLoader)(f: ⇒ T): T = {
    val thread = Thread.currentThread
    val oldContext = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(loader)
      f
    } finally {
      thread.setContextClassLoader(oldContext)
    }
  }
  // Some bright ideas on https://github.com/inventage/version-tiger/blob/master/com.inventage.tools.versiontiger/
  //   .../src/main/java/com/inventage/tools/versiontiger/internal/impl/OsgiVersionImpl.java
  /**
   * Convert string to valid OSGi version
   */
  def toOSGiVersion(version: String)(implicit arg: Plugin.TaskArgument): Version = {
    def filter(s: String) = s.trim match {
      case s if s.nonEmpty ⇒ Some(s)
      case s ⇒ None
    }
    val qualiferPattern = """(\d*)(.+)""".r.pattern
    val parts = version.split("""[.-]""")
    val (major, minor, micro, qualifier) = if (parts.length > 1) {
      val qualifier = parts.lastOption.flatMap { string ⇒
        val m = qualiferPattern.matcher(string)
        if (string.forall(_.isDigit) && parts.length < 3) {
          None // It is minor or micro
        } else {
          if (m.matches()) Some(m.group(1)) else None
        }
      }
      val major = parts.headOption.flatMap(string ⇒ """\d+""".r.findFirstIn(string))
      val minor = parts.drop(1).headOption.flatMap(string ⇒ """^\d+""".r.findFirstIn(string))
      val micro = parts.drop(2).headOption.flatMap(string ⇒ """^\d+""".r.findFirstIn(string))
      (major, minor, micro, qualifier)
    } else if (parts.length > 0) {
      val qualifier = parts.lastOption.flatMap { string ⇒
        val m = qualiferPattern.matcher(string)
        if (string.forall(_.isDigit)) {
          None // It is major
        } else {
          if (m.matches()) Some(m.group(1)) else None
        }
      }
      val major = parts.headOption.flatMap(string ⇒ """\d+""".r.findFirstIn(string))
      (major, None, None, qualifier)
    } else
      (None, None, None, None)
    val majorVersion = major.flatMap(filter) getOrElse "0"
    val minorVersion = minor.flatMap(filter) getOrElse "0"
    val microVersion = micro.flatMap(filter) getOrElse "0"
    val qualifierVersion = qualifier.flatMap(filter).map { string ⇒
      // add leading 0- if qualifier not beginning from digit
      if (string.head.isDigit) string else "0-" + string
    } getOrElse ""
    val converted = if (majorVersion == "0" && minorVersion == "0" && microVersion == "0" && qualifierVersion.isEmpty)
      "0.0.1"
    else if (qualifierVersion.nonEmpty)
      Seq(majorVersion, minorVersion, microVersion, qualifierVersion).mkString(".")
    else
      Seq(majorVersion, minorVersion, microVersion).mkString(".")
    arg.log.debug(logPrefix(arg.name) + "Convert version %s -> %s".format(version, converted))
    VersionParser.parse(converted, 0, converted.length())
  }

  class RichOption[T](option: Option[T]) {
    def getOrThrow(onError: String) = option getOrElse { throw new NoSuchElementException(onError) }
  }
  trait Resolve {
    /** Simple cache that holds per project already processed: resolvers + dependencies */
    private val cache = new mutable.HashMap[CacheKey, Seq[Int]]

    /** Reset resolution cache */
    def resetCache()(implicit arg: Plugin.TaskArgument) = synchronized {
      arg.log.debug(logPrefix(arg.name) + "Clear cache.")
      cache.clear
    }
    /** Check if there are settings which is already cached for the cacheKey */
    def isCached(cacheKey: CacheKey, eeConfiguration: ExecutionEnvironmentConfiguration,
      target: Seq[(Environment.OS, Environment.WS, Environment.ARCH)],
      dependencies: Seq[ModuleID], resolvers: Seq[(String, String)])(implicit arg: Plugin.TaskArgument): Boolean = synchronized {
      cache.get(cacheKey) match {
        case Some(cached) ⇒
          val value = (dependencies.map(_.hashCode) ++ resolvers.map(_.hashCode) ++
            target.map(_.hashCode()) :+ eeConfiguration.getProfileName.hashCode()).sorted
          arg.log.debug(logPrefix(arg.name) + "Check cache for " + cacheKey + " with value " + cached + " against value: " + value)
          val result = cached.sameElements(value)
          if (result)
            arg.log.debug(logPrefix(arg.name) + "Cache HIT.")
          else
            arg.log.debug(logPrefix(arg.name) + "Cache MISS.")
          result
        case None ⇒
          arg.log.debug(logPrefix(arg.name) + "Cache is empty.")
          false
      }
    }
    /** Update P2 cache value */
    def updateCache(cacheKey: CacheKey, eeConfiguration: ExecutionEnvironmentConfiguration,
      target: Seq[(Environment.OS, Environment.WS, Environment.ARCH)],
      dependencies: Seq[ModuleID], resolvers: Seq[(String, String)])(implicit arg: Plugin.TaskArgument) = synchronized {
      val value = (dependencies.map(_.hashCode) ++ resolvers.map(_.hashCode) ++
        target.map(_.hashCode()) :+ eeConfiguration.getProfileName.hashCode()).sorted
      arg.log.debug(logPrefix(arg.name) + "Update cache for " + cacheKey + " with value: " + value)
      cache(cacheKey) = value
    }
  }
  sealed trait CacheKey {
    val projectId: String
  }
  case class CacheOBRKey(projectId: String) extends CacheKey
  case class CacheP2Key(projectId: String) extends CacheKey
}
