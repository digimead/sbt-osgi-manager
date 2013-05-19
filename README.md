sbt-osgi-manager
================
[![Build Status](https://travis-ci.org/digimead/sbt-osgi-manager.png)](https://travis-ci.org/digimead/sbt-osgi-manager)

OSGi development bridge based on Bnd and Tycho.

TODO demo

What is it? You may use OSGi infrastructure via [SBT](https://github.com/sbt/sbt "Simple Build Tool") project and your favorite IDE.

It is provide an ability:

* resolve OSGi dependencies and dependencies source code via Eclipse P2 update site / with Tycho API
* resolve OSGi dependencies via OSGi R5 repositories / with Bnd API, only local repository tested, but remote maybe worked too
* generate bundle manifest with Bnd API

Resolved bundles added to project 'library-dependencies' property.

TODO index

DOCUMENTATION
-------------

Please note, that OSGi infrastructure has no dependency `organization` field as Ivy or Maven has. The bundle symbolic name and bundle version identify a unique artifact.

TODO doc

[API (latest version)](http://ezh.github.com/?)

### Generate bundle manifest

TODO doc

#### Modify bundle properties

TODO doc

#### List actual properties per project

TODO doc

`osgi-show`

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

### How to install

TODO doc

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
