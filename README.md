sbt-osgi-manager [![Build Status](https://travis-ci.org/digimead/sbt-osgi-manager.png)](https://travis-ci.org/digimead/sbt-osgi-manager)
================

OSGi development bridge based on Bnd and Tycho.

TODO demo

What is it? You may use OSGi infrastructure via [SBT](https://github.com/sbt/sbt "Simple Build Tool") project and your favorite IDE.

Plugin can provide such abilities as:

* resolving OSGi dependencies and dependencies source code via Eclipse P2 update site
* resolving OSGi dependencies via OSGi R5 repositories
* generating bundle manifest

Resolved bundles added to project 'library-dependencies'. Resolved bundles and their source code (if any) may be fetched with [sbt-dependency-manager](https://github.com/digimead/sbt-dependency-manager) or processed with your favorite tool that uses SBT dependency information - for example, SBT command `deliver-local`.

If you want to improve plugin, please send mail to sbt-android-mill at digimead.org. You will be granted write access. Please, feel free to add yourself to authors.

SBT source code is really simple to read and simple to extend :-)

This readme cover all plugin functionality, even if it is written in broken english (would you have preferred well written russian :-) Please, correct it, if you find something inappropriate.

Table of contents
-----------------

- [Adding to your project](#adding-to-your-project)
    - [Via interactive build](#via-interactive-build)
    - [As published jar artifact](#as-published-jar-artifact)
    - [As local build](#as-local-build)
    - [Activate in your project](#activate-in-your-project)
- [Usage](#usage)
    - [Generate bundle manifest](#generate-bundle-manifest)
    - [Resolve OSGi dependencies](#resolve-osgi-dependencies)
- [Internals](#internals)
    - [Options](#options)
    - [Tasks](#tasks)
- [Demonstration](#demonstration)
- [FAQ](#faq)
- [Participate in the development](#participate-in-the-development)
- [Authors](#authors)
- [License](#license)
- [Copyright](#copyright)

## Adding to your project

You may find sample project at [src/sbt-test/osgi-manager/simple](src/sbt-test/osgi-manager/simple)

### Via interactive build

Supported SBT versions: 0.11.x, 0.12.x.

Create a

 * _project/plugins/project/Build.scala_ - for older simple-build-tool
 * _project/project/Build.scala_ - for newer simple-build-tool

file that looks like the following:

``` scala
    import sbt._
    object PluginDef extends Build {
      override def projects = Seq(root)
      lazy val root = Project("plugins", file(".")) dependsOn(osgi)
      lazy val dm = uri("git://github.com/digimead/sbt-osgi-manager.git#0.0.1.4")
    }
```

You may find sample project at [src/sbt-test/osgi-manager/simple](src/sbt-test/osgi-manager/simple)

### As published jar artifact

Supported SBT versions: 0.11.3, 0.12.x. Add to your _project/plugins.sbt_

    addSbtPlugin("org.digimead" % "sbt-osgi-manager" % "0.0.1.4")

Maven repository:

    resolvers += "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/"

Ivy repository:

    resolvers += Resolver.url("digimead-ivy", url("http://storage.googleapis.com/ivy.repository.digimead.org/"))(Resolver.defaultIvyPatterns)

### As local build

Clone this repository to your development system then do `sbt publish-local`

### Activate in your project

For _build.sbt_, simply add:

``` scala
    import sbt.osgi.manager._

    OSGiManager
```

For _Build.scala_:

``` scala
    import sbt.dependency.manager._

    ... yourProjectSettings ++ OSGiManager
```

If you want to enable extra run-time debugging use `OSGiManagerWithDebug(Equinox TCP port)` instead of `OSGiManager`. Also put [.options](src/sbt-test/osgi-manager/simple/.options.no) file to your project directory.

[Imported package](src/main/scala/sbt/osgi/manager/package.scala) contains public declarations.

## Usage ##

*You may find plugin usage examples at [https://github.com/ezh/](https://github.com/ezh/). Look at `build.sbt` of Digi- libraries.*

Please note, that OSGi infrastructure has no dependency `organization` field as Ivy or Maven has. The bundle symbolic name and bundle version identify a unique artifact.

TODO Keys

TODO doc

[API (latest version)](http://ezh.github.com/?)

### Generate bundle manifest

To generate bundle manifest:

1. Add necessary information your project. Look at [Modify bundle properties](#modify-bundle-properties)
2. Check bundle settings. Look at [List actual properties per project](#list-actual-properties-per-project)
3. Create your artifact as usual. The plugin will intercept `packageOptions in (Compile, packageBin)` and will inject OSGi headers to the generated manifest.

#### Modify bundle properties

You may alter bundle properties via complex block

```scala
inConfig(OSGiConf)({
  import OSGiKey._
  Seq[Project.Setting[_]](
    osgiBndBundleActivator := "org.example.Activator",
    osgiBndBundleSymbolicName := "org.example",
    osgiBndBundleCopyright := "Copyright © 19xx-23xx N. All rights reserved.",
    osgiBndExportPackage := List("org.example.*"),
    osgiBndImportPackage := List("!org.aspectj.*", "*"),
    osgiBndBundleLicense := Seq("http://www.gnu.org/licenses/agpl.html;description=GNU Affero General Public License",
      "http://example.org/CommercialLicense.txt;description=Commercial License").mkString(","),
    resolvers += typeP2("Eclipse P2 update site" at "http://eclipse.nn.nn"),
    resolvers += typeOBR("Local OBR repository" at sys.env("OBR_REPOSITORY"))
  )
})
```

You may alter bundle properties as single line SBT settings.

```scala
OSGiKey.osgiBndBundleActivator in OSGiConf := "org.example.Activator"
```

#### List actual properties per project

You may inspect OSGi properties with SBT `show` command or to use `osgi-show` report.

### Resolve OSGi dependencies

TODO doc

#### Resolve OSGi dependencies against P2 update site

TODO doc

    inConfig(OSGiConf)({
    import OSGiKey._
      Seq[Project.Setting[_]](
        resolvers += typeP2("Eclipse P2 update site" at "http://eclipse.ialto.com/eclipse/updates/4.2/R-4.2.1-201209141800/"),
        libraryDependencies += typeP2((OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION).withSources),
        libraryDependencies += typeP2((OSGi.ECLIPSE_PLUGIN % "org.eclipse.core.runtime" % OSGi.ANY_VERSION).withSources))
    })

> `osgi-resolve`

#### Resolver OSGi dependencies against OBR R5 repository

TODO doc

    inConfig(OSGiConf)({
      import OSGiKey._
      Seq[Project.Setting[_]](
        resolvers += typeOBR("Local OBR repository" at "file:/path/to/obr"),
        libraryDependencies += typeOBR((OSGi.ANY_ORGANIZATION % "org.digimead.digi.lib" % OSGi.ANY_VERSION).withSources))
    })

> `osgi-resolve`

## Participate in the development ##

Branches:

* origin/master reflects a production-ready state
* origin/release-* support preparation of a new production release. Allow for last-minute dotting of i’s and crossing t’s
* origin/hotfix-* support preparation of a new unplanned production release
* origin/develop reflects a state with the latest delivered development changes for the next release (nightly builds)
* origin/feature-* new features for the upcoming or a distant future release

Structure of branches follow strategy of http://nvie.com/posts/a-successful-git-branching-model/

If you will create new origin/feature-* please open feature request for yourself.

* Anyone may comment you feature here.
* We will have a history for feature and ground for documentation
* If week passed and there wasn't any activity + all tests passed = release a new version ;-)

AUTHORS
-------

* Alexey Aksenov

LICENSE
-------

The sbt-osgi-manager project is licensed to you under the terms of
the Apache License, version 2.0, a copy of which has been
included in the LICENSE file.
Please check the individual source files for details.

Copyright
---------

Copyright © 2013 Alexey B. Aksenov/Ezh. All rights reserved.
