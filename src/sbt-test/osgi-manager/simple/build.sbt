import sbt.dependency.manager._

import sbt.osgi.manager._

activateDependencyManager

activateOSGiManagerWithDebug()

name := "Simple"

version := "0.1"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Info

libraryDependencies += "biz.aQute" % "bndlib" % "2.0.0.20130123-133441"

resolvers in OSGiConf += typeP2("Eclipse P2 update site" at "http://eclipse.ialto.com/eclipse/updates/4.2/R-4.2.1-201209141800/")

libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION withSources)
