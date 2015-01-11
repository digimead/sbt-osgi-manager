SBT OSGi Manager [![Build Status](https://travis-ci.org/digimead/sbt-osgi-manager.png)](https://travis-ci.org/digimead/sbt-osgi-manager)
================

OSGi development bridge based on Bnd and Tycho.

There is a [sample project][sp]. Please, overview `test` file which contains interactive example in [Scripted format][sc].

What is it? [SBT][sbt] Plugin for solid integration OSGi infrastructure with your favorite IDE.

Plugin provides such abilities as:

* resolving OSGi dependencies and dependencies with source code at Eclipse P2 update site
* resolving OSGi dependencies via OSGi R5 repositories
* generating bundle manifest

Resolved bundles are added to project to 'library-dependencies' settings key. Resolved bundles and their source code (if any) may be fetched with [sbt-dependency-manager][dm] or may be processed with your favorite tool that uses SBT dependency information (for example, SBT command `deliver-local`).

[See SBT OSGi Manager documentation](http://digimead.github.io/sbt-osgi-manager/).

__Required Java 6 or higher__

Few tips
--------

Use [local](https://github.com/digimead/sbt-osgi-manager/tree/master/src/sbt-test/osgi-manager/local) test as example if you wish to store OSGi dependencies on your local storage

Use [remote](https://github.com/digimead/sbt-osgi-manager/tree/master/src/sbt-test/osgi-manager/remote) test as example if you wish to use OSGi dependencies directly from remote resource

Use [subproject](https://github.com/digimead/sbt-osgi-manager/tree/master/src/sbt-test/osgi-manager/subproject) test as example with nested projects

Checkout plugin and run ```sbt-0.13 'set scriptedLaunchOpts := Seq("-Xms384m", "-Xmx384m", "-XX:MaxPermSize=128m")' scripted```. Compare tests output and your project debug messages. You may look for tests output at Travis (click on build status)

```osgiResolve``` and ```osgiResolveLocal``` are input tasks. You must run it from SBT console or from SBT hooks like onLoad before project tasks.

Example session:
```
sbt> show osgi:libraryDependencies
  ... empty list ...
sbt> osgiResolve
  ... resolution process ...
sbt> show osgi:libraryDependencies
  ... list with OSGi dependencies ...
```

Use ```osgiShow``` task.

AUTHORS
-------

* Alexey Aksenov

LICENSE
-------

SBT OSGi Manager project is licensed to you under the terms of
the Apache License, version 2.0, a copy of which has been
included in the LICENSE file.
Please check the individual source files for details.

Copyright
---------

Copyright © 2013-2015 Alexey B. Aksenov/Ezh. All rights reserved.

[dm]: https://github.com/digimead/sbt-dependency-manager
[sbt]: https://github.com/sbt/sbt
[sc]: http://eed3si9n.com/testing-sbt-plugins
[sp]: https://github.com/digimead/sbt-osgi-manager/tree/master/src/sbt-test/osgi-manager/local
