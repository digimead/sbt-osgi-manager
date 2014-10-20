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

Copyright © 2013-2014 Alexey B. Aksenov/Ezh. All rights reserved.

[dm]: https://github.com/digimead/sbt-dependency-manager
[sbt]: https://github.com/sbt/sbt
[sc]: http://eed3si9n.com/testing-sbt-plugins
[sp]: https://github.com/digimead/sbt-osgi-manager/tree/master/src/sbt-test/osgi-manager/simple
