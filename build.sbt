//
// Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// PRODUCTION CONFIGURATION

ScriptedPlugin.scriptedSettings

name := "sbt-osgi-manager"

organization := "sbt.osgi.manager"

version := "0.0.1.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

sbtPlugin := true

libraryDependencies ++= {
  val mavenVersion = "3.0.5"
  val mavenWagonVersion = "2.4"
  Seq(
    "biz.aQute" % "bndlib" % "2.1.0.20130426-122213" from "https://raw.github.com/bndtools/releases/master/bnd/2.1.0.REL/biz.aQute.bndlib/biz.aQute.bndlib-2.1.0.jar",
    "org.apache.felix" % "org.apache.felix.resolver" % "1.0.0",
    "org.apache.maven" % "maven-aether-provider" % mavenVersion,
    "org.apache.maven" % "maven-artifact" % mavenVersion,
    "org.apache.maven" % "maven-compat" % mavenVersion,
    "org.apache.maven" % "maven-core" % mavenVersion,
    "org.apache.maven" % "maven-plugin-api" % mavenVersion,
    "org.apache.maven" % "maven-embedder" % mavenVersion, // provide org.apache.maven.cli.MavenCli
    "org.apache.maven.wagon" % "wagon-http" % mavenWagonVersion, // HTTP connector for remore repositories
    "org.apache.maven.wagon" % "wagon-file" % mavenWagonVersion, // File connector for local repositories
    "org.eclipse.tycho" % "tycho-core" % "0.17.0",
    "org.eclipse.tycho" % "tycho-p2-facade" % "0.17.0", // Tycho p2 Resolver Component
    "org.osgi" % "org.osgi.core" % "5.0.0",
    "org.osgi" % "org.osgi.enterprise" % "5.0.0",
    "org.sonatype.aether" % "aether-connector-wagon" % "1.13.1"
  )
}

scriptedBufferLog := false

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m")

//logLevel := Level.Debug
