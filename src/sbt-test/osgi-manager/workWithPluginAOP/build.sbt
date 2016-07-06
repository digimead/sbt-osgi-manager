import sbt.dependency.manager._

import sbt.osgi.manager._

DependencyManager

OSGiManagerWithDebug()

name := "Simple"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

OSGiKey.osgiBndBundleLicense in OSGiConf := "abc"

logLevel := Level.Info

libraryDependencies ++= Seq(
  "biz.aQute" % "bndlib" % "2.0.0.20130123-133441",
  "com.google.code.findbugs" % "jsr305" % "2.0.3" % "test")

resolvers in OSGiConf += typeP2("Eclipse P2 update site" at "http://eclipse.ialto.com/eclipse/updates/4.2/R-4.2.1-201209141800/")

libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION withSources)

mainClass in (Compile, packageBin) := Some("a.b.c")

//logLevel := Level.Debug

InputKey[Unit]("checkRoot") := {
  val args = Def.spaceDelimited().parsed
  val size = (libraryDependencies).value.size
  System.out.println("OSGi library dependencies size = " + size)
  if (size != Integer.parseInt(args.head)) error("unexpected size: " + size + ", expect " + args.head)
  ()
}
