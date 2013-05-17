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

package sbt.osgi.manager

import java.util.Locale
import java.util.Properties

import scala.collection.JavaConversions._
import scala.collection.mutable

import org.codehaus.plexus.util.Os
import sbt._
import sbt.Keys._

object Support {
  implicit def option2rich[T](option: Option[T]): RichOption[T] = new RichOption(option)

  /** Collect environment variables to Properties container */
  def getEnvVars(): Properties = {
    val envVars = new Properties()
    val caseSensitive = !Os.isFamily(Os.FAMILY_WINDOWS)
    for (entry <- System.getenv().entrySet()) {
      val key = "env." + (if (caseSensitive) entry.getKey() else entry.getKey().toUpperCase(Locale.ENGLISH))
      envVars.setProperty(key, entry.getValue())
    }
    envVars
  }
  /** Returns dependencies */
  def getDependencies(dependencyType: Dependency.Type, scope: Scope)(implicit arg: Plugin.TaskArgument): Seq[ModuleID] = {
    arg.log.debug("Collect dependencies for " + scope.project)
    ((libraryDependencies in scope get arg.extracted.structure.data): Option[Seq[ModuleID]]).
      getOrElse(Seq[ModuleID]()).filter(_ match {
        case dependency if dependency.extraAttributes.get(dependencyType.key) == Some(dependencyType.name) =>
          arg.log.debug(logPrefix(arg.name) + "Add %s dependency \"%s\"".format(dependencyType.name, dependency.copy(extraAttributes = Map())))
          true
        case otherDependency =>
          arg.log.debug(logPrefix(arg.name) + "Skip dependency " + otherDependency)
          false
      })
  }
  /** Returns resolvers as Seq[(id, url)] */
  def getResolvers(dependencyType: Dependency.Type, scope: Scope)(implicit arg: Plugin.TaskArgument): Seq[(String, String)] = {
    arg.log.debug("Collect resolvers for " + scope.project)
    ((sbt.Keys.resolvers in scope get arg.extracted.structure.data): Option[Seq[Resolver]]).
      getOrElse(Seq[Resolver]()).filter(_ match {
        case resolver: URLRepository if resolver.patterns.artifactPatterns == Seq(dependencyType.name) =>
          val repo = resolver.patterns.ivyPatterns.head // always one element, look at markResolverAsP2
          arg.log.debug(logPrefix(arg.name) + "Add %s resolver \"%s\" at %s".format(dependencyType.name, resolver.name, repo))
          true
        case otherResolver =>
          arg.log.debug(logPrefix(arg.name) + "Skip resolver " + otherResolver)
          false
      }).map {
        case resolver: URLRepository => (resolver.name, resolver.patterns.ivyPatterns.head)
        case resolver => throw new OSGiManagerException("Unknown resolver " + resolver)
      }
  }
  /** Default sbt-osgi-manager log prefix */
  def logPrefix(name: String) = "[OSGi manager:%s] ".format(name)
  /**
   * Executes the function f within the ContextClassLoader of 'classOf'.
   * After execution the original ClassLoader will be restored.
   */
  def withClassLoaderOf[T](classOf: Class[_])(f: => T): T = {
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
  def withClassLoaderOf[T](loader: ClassLoader)(f: => T): T = {
    val thread = Thread.currentThread
    val oldContext = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(loader)
      f
    } finally {
      thread.setContextClassLoader(oldContext)
    }
  }

  class RichOption[T](option: Option[T]) {
    def getOrThrow(onError: String) = option getOrElse { throw new NoSuchElementException(onError) }
  }
  trait Resolve {
    /** Simple cache that holds per project resolvers + dependencies */
    private val cache = new mutable.HashMap[CacheKey, Seq[Int]] with mutable.SynchronizedMap[CacheKey, Seq[Int]]
    /** Reset resolution cache */
    def resetCache() = cache.clear
    /** Check if there are settings which is already cached for the cacheKey */
    def isCached(cacheKey: CacheKey, dependencies: Seq[ModuleID], resolvers: Seq[(String, String)]): Boolean = cache.get(cacheKey) match {
      case Some(cached) => cached.sameElements((dependencies.map(_.hashCode) ++ resolvers.map(_.hashCode)).sorted)
      case None => false
    }
    /** Update P2 cache value */
    def updateCache(cacheKey: CacheKey, dependencies: Seq[ModuleID], resolvers: Seq[(String, String)])(implicit arg: Plugin.TaskArgument) = {
      arg.log.debug(logPrefix(arg.name) + "Update cache for " + cacheKey)
      cache(cacheKey) = (dependencies.map(_.hashCode) ++ resolvers.map(_.hashCode)).sorted
    }
  }
  sealed trait CacheKey {
    val projectId: String
  }
  case class CacheOBRKey(projectId: String) extends CacheKey
  case class CacheP2Key(projectId: String) extends CacheKey
}
