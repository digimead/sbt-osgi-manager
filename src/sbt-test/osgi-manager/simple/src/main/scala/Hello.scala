object HelloWorld {
  def main(args: Array[String]) {
     assert(org.eclipse.core.runtime.adaptor.EclipseStarter.PROP_BUNDLES != null)
     println("Hello, world!")
  }
}
