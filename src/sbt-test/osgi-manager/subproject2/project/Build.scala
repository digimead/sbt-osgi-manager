import java.io.File
import sbt.Keys._
import sbt._
import sbt.dependency.manager._
import sbt.osgi.manager._

object TestPluginBuild extends Build {

  lazy val commonsSettings = super.settings

  lazy val plugin = Project("plugin", file("plugin")) settings((commonsSettings ++ DependencyManager ++ OSGiManagerWithDebug()): _*) settings (
    resolvers in OSGiConf += typeP2("Eclipse P2 update site" at "http://eclipse.ialto.com/eclipse/updates/4.2/R-4.2.1-201209141800/"),
    libraryDependencies ++= Seq(
      "biz.aQute" % "bndlib" % "2.0.0.20130123-133441",
      "com.google.code.findbugs" % "jsr305" % "2.0.3" % "test"),
//    logLevel := Level.Debug,

 libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION withSources))
}
