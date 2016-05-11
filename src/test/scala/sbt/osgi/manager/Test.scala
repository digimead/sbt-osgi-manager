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

package sbt.osgi.manager

import java.io.File
import scala.language.reflectiveCalls

object Test {
  /** Create temporary folder. */
  def createTempFolder(name: String = "sbt-osgi-manager"): Option[File] = {
    val tempFolder = System.getProperty("java.io.tmpdir")
    var folder: File = null
    folder = new File(tempFolder, name)
    var successful = false
    if (folder.isDirectory())
      return Some(folder)
    for (i ← 0 until 1000 if !successful)
      successful = folder.mkdir()
    if (successful)
      Some(folder)
    else
      None
  }
  def removeAll(path: File) = {
    def getRecursively(f: File): Seq[File] =
      Option(f.listFiles).getOrElse(Array.empty).filter(_.isDirectory).flatMap(getRecursively) ++ f.listFiles
    getRecursively(path).foreach { f ⇒
      f.delete()
    }
  }
  /** Substitute singleton implementation. */
  def withImplementation[A, B](singleton: { val inner: B }, implementation: B)(f: ⇒ A): A = {
    val inner = singleton.inner
    val innerField = singleton.getClass.getDeclaredField("inner")
    innerField.setAccessible(true)
    innerField.set(singleton, implementation)
    try f
    finally innerField.set(singleton, inner)
  }
}
