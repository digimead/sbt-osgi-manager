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

import org.eclipse.tycho.ArtifactKey

/** Various declarations for end user */
object OSGi {
  lazy val ECLIPSE_APPLICATION = ArtifactKey.TYPE_ECLIPSE_APPLICATION
  lazy val ECLIPSE_FEATURE = ArtifactKey.TYPE_ECLIPSE_FEATURE
  lazy val ECLIPSE_PLUGIN = ArtifactKey.TYPE_ECLIPSE_PLUGIN
  lazy val ECLIPSE_REPOSITORY = ArtifactKey.TYPE_ECLIPSE_REPOSITORY
  lazy val ECLIPSE_TEST_PLUGIN = ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN
  lazy val ECLIPSE_UPDATE_SITE = ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE
  lazy val ANY_VERSION = Dependency.ANY_VERSION
  lazy val ANY_ORGANIZATION = Dependency.ANY_ORGANIZATION
}
