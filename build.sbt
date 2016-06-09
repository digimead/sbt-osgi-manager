//
// Copyright (c) 2013-2016 Alexey Aksenov ezh@ezh.msk.ru
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

ScriptedPlugin.scriptedSettings

name := "sbt-osgi-manager"

description := "OSGi development bridge based on Bndtools and Tycho."

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

organization := "org.digimead"

organizationHomepage := Some(url("http://digimead.org"))

homepage := Some(url("https://github.com/digimead/sbt-osgi-manager"))

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

// There is no "-Xfatal-warnings" because we have cross compilation against different Scala versions
scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-Xcheckinit", "-feature")

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

sbtPlugin := true

// Java then Scala for main sources
compileOrder in Compile := CompileOrder.JavaThenScala
// Java then Scala for main sources
compileOrder in Test := CompileOrder.JavaThenScala

resourceGenerators in Compile <+=
  (resourceManaged in Compile, name, version) map { (dir, n, v) =>
    val file = dir / "version-%s.properties".format(n)
    val contents = "name=%s\nversion=%s\nbuild=%s\n".format(n, v, ((System.currentTimeMillis / 1000).toInt).toString)
    IO.write(file, contents)
    Seq(file)
  }

libraryDependencies ++= {
  val aetherVersion = "1.1.0"
  val mavenVersion = "3.3.9"
  val tychoVersion = "0.25.0"
  Seq(
    "biz.aQute.bnd" % "bndlib" % "2.3.0",
    "org.apache.maven" % "maven-aether-provider" % mavenVersion,
    "org.apache.maven" % "maven-artifact" % mavenVersion,
    "org.apache.maven" % "maven-compat" % mavenVersion,
    "org.apache.maven" % "maven-core" % mavenVersion,
    "org.apache.maven" % "maven-plugin-api" % mavenVersion,
    "org.apache.maven" % "maven-embedder" % mavenVersion, // provide org.apache.maven.cli.MavenCli
    "org.digimead" %% "digi-sbt-util" % "0.2.0.0",
    "org.eclipse.tycho" % "tycho-core" % tychoVersion,
    "org.eclipse.tycho" % "tycho-p2-facade" % tychoVersion,
    "org.osgi" % "org.osgi.core" % "6.0.0",
    "org.osgi" % "org.osgi.enterprise" % "5.0.0",
    "org.osgi" % "org.osgi.annotation" % "6.0.0",
    "org.sonatype.aether" % "aether-impl" % "1.13.1", // prevents java.lang.ClassNotFoundException: org.sonatype.aether.spi.connector.RepositoryConnectorFactory
    "org.eclipse.aether" % "aether-impl" % aetherVersion,
    "org.eclipse.aether" % "aether-transport-wagon" % aetherVersion,
    "org.eclipse.aether" % "aether-connector-basic" % aetherVersion,
    "org.eclipse.aether" % "aether-transport-file" % aetherVersion,
    "org.eclipse.aether" % "aether-transport-http" % aetherVersion,
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )
}

scriptedBufferLog := false

resolvers ++= Seq(
  "sbt-osgi-mananger-digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/",
  Resolver.url("sbt-osgi-manager-typesafe-ivy-releases", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns),
  Resolver.url("sbt-osgi-manager-typesafe-ivy-snapshots", url("http://repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.defaultIvyPatterns),
  Resolver.url("sbt-osgi-manager-typesafe-repository", url("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns),
  Resolver.url("sbt-osgi-manager-typesafe-shapshots", url("http://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.defaultIvyPatterns))

sourceGenerators in Compile <+= (sbtVersion, sourceDirectory in Compile, sourceManaged in Compile) map { (v, sourceDirectory, sourceManaged) =>
  val interface = v.split("""\.""").take(2).mkString(".")
  val source = sourceDirectory / ".." / "patch" / interface
  val generated = (PathFinder(source) ***) x Path.rebase(source, sourceManaged)
  IO.copy(generated, true, false)
  generated.map(_._2).filter(_.getName endsWith ".scala")
}

//logLevel := Level.Debug
