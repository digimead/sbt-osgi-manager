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

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration
import sbt.ModuleID
import sbt.osgi.manager.{ Environment, Plugin }
import sbt.osgi.manager.support.Support.logPrefix
import scala.collection.mutable
import scala.language.implicitConversions

trait Resolve {
  /** Simple cache that holds per project already processed: resolvers + dependencies */
  private lazy val cache = new mutable.HashMap[CacheKey, Seq[Int]]

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
