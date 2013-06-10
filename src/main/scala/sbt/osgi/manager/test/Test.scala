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

package sbt.osgi.manager.test

import sbt._
import sbt.Keys._
import sbt.osgi.manager.Keys._

object Test {
  lazy val settings = inConfig(OSGiTestConf)(Defaults.testSettings ++ Seq(
    externalDependencyClasspath <<= Classpaths.concatDistinct(externalDependencyClasspath, externalDependencyClasspath in Compile),
    internalDependencyClasspath <<= Classpaths.concatDistinct(internalDependencyClasspath, internalDependencyClasspath in sbt.Test)))
}
