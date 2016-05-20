import sbt.dependency.manager._

import sbt.osgi.manager._

DependencyManager

OSGiManagerWithDebug()

name := "Sub"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Info

mainClass in (Compile, packageBin) := Some("a.b.c")

//logLevel := Level.Debug

lazy val base_project = Project(id = "local", base = file("local"))

val sub_project = Project(id = "root", base = file(".")).dependsOn(base_project)

libraryDependencies ++= (libraryDependencies in base_project).value
