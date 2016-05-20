InputKey[Unit]("check") := {
  val args = Def.spaceDelimited().parsed
  val size = (libraryDependencies in sbt.osgi.manager.OSGiConf).value.size
  System.out.println("OSGi library dependencies size = " + size)
  if (size != Integer.parseInt(args.head)) error("unexpected size: " + size)
  ()
}
