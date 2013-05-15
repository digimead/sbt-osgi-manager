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

import org.codehaus.plexus.util.Os

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
}
