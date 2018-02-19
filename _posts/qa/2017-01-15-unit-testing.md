---
layout: page
title:  "Unit Testing"
categories: [qa]
---

Build-time Testing
------------

Build-time testing is further divided into unit testing and integration testing.

Unit testing is focused on testing separate methods or groups of methods, preferably in a single class. This way it can verify the correct operation of difficult-to-reach corner cases.

Integration testing is a more-high-level testing that tests certain functionality on a group of connected services in an approximation of the real environment. It verifies that the services can successfully register in the environment and connect to other services as well as perform their tasks.

Code coverage of the develop branch can be observed in [Jenkins](https://ci.eclipse.org/kura/job/kura-develop/lastBuild/jacoco/).

For some tips on running the tests also check [Kura GitHub Wiki](https://github.com/eclipse/kura/wiki/Development-Environment-Tips-and-Tricks).

### Unit Testing

Unit tests should try to cover as many corner cases in the code as possible. Add them for (all) the new code you decide to contribute.

#### Test Location

Kura discourages to introduce test-only dependencies on the implementation level, so all tests are located in their own projects under test/. The proper folder to put the tests in is src/test/java.

#### Code Conventions

* Preferably use &lt;package name>.test as the name of the test project. Add it as a module in test/pom.xml that also serves as maven artifact's parent.
* Only add `src/main/java` to build.properties' `source..`.
* Make the bundle a fragment of the class-under-test's bundle so that you gain access to its internal packages.
* Use the same package for the test as the class under test. Subpackages are OK.
* Use the same coding style as [Kura](https://github.com/eclipse/kura/blob/develop/CONTRIBUTING.md#making-your-changes). Try to incorporate the suggestions SonarLint may have for your tests.

#### Running the Tests

The basic flow is to build your implementation using maven and then also build and run the unit tests using `mvn clean test` (or some other phase e.g. install, which also runs integration tests).

Advanced test running and running them in IDE is described in [Kura GitHub Wiki](https://github.com/eclipse/kura/wiki/Development-Environment-Tips-and-Tricks).

### Integration Testing

These test proper behavior in the OSGi environment. Some additional configuration is therefore necessary.

#### Test Location

We don't want to mess with the implementation code here, either, so all tests are again located in the test projects under test/. It can be the same project as for unit tests. The proper folder to put the tests in is src/main/java.

#### Code Conventions

* Preferably use &lt;package name>.test as the name of the test project. Add it as a module in test/pom.xml that also serves as maven artifact's parent.
* Only add `src/main/java` to build.properties' `source..`.
* Make the bundle a fragment of the class-under-test's bundle so that you gain access to its internal packages.
* Use the <package name>.test as the package to put the test in. Also add .test suffix to any subpackages that are also under test.
* Use the same coding style as [Kura](https://github.com/eclipse/kura/blob/develop/CONTRIBUTING.md#making-your-changes). Try to incorporate the suggestions SonarLint may have for your tests.

#### Running the Tests

The basic flow is to build your implementation using maven and then also build and run the integration tests using `mvn clean install`.

Advanced test running and running them in IDE is described in [Kura GitHub Wiki](https://github.com/eclipse/kura/wiki/Development-Environment-Tips-and-Tricks).
