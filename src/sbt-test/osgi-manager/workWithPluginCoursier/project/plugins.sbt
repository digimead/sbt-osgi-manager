resolvers ++= Seq(
  Classpaths.typesafeResolver,
  Resolver.sonatypeRepo("snapshots"),
  "oss sonatype" at "https://oss.sonatype.org/content/groups/public/",
  "digimead-maven" at "http://commondatastorage.googleapis.com/maven.repository.digimead.org/"
)

addSbtPlugin("org.digimead" % "sbt-dependency-manager" % "0.8.0.2-SNAPSHOT")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-SNAPSHOT")

libraryDependencies <+= (sbtBinaryVersion in update, scalaBinaryVersion in update, baseDirectory) { (sbtV, scalaV, base) =>
  Defaults.sbtPluginExtra("org.digimead" % "sbt-osgi-manager" %
    scala.io.Source.fromFile(base / Seq("..", "version").mkString(java.io.File.separator)).mkString.trim, sbtV, scalaV) }
