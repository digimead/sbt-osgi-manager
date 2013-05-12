libraryDependencies ++= {
  Seq(
    "biz.aQute" % "bndlib" % "2.0.0.20130123-133441",
    "biz.aQute" % "bndrepository" % "2.1.0.122819_210REL" from "http://bndtools-updates.s3.amazonaws.com/plugins/biz.aQute.repository_2.1.0.122819_210REL.jar",
    "biz.aQute" % "bndresolve" % "0.0.3.201304261229_210REL" from "http://bndtools-updates.s3.amazonaws.com/plugins/biz.aQute.resolve_0.0.3.201304261229_210REL.jar",
    "org.apache.felix" % "org.apache.felix.framework" % "4.2.1",
    "org.osgi" % "org.osgi.enterprise" % "5.0.0"
  )
}

addSbtPlugin("sbt.osgi.manager" % "sbt-osgi-manager" % "0.0.1.0-SNAPSHOT")

addSbtPlugin("sbt.dependency.manager" % "sbt-dependency-manager" % "0.6-SNAPSHOT")
