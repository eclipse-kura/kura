# Unit Testing

## Build-time testing

Build-time testing is further divided into unit testing and integration testing.

Unit testing is focused on testing separate methods or groups of methods, preferably in a single class. This way it can verify the correct operation of difficult-to-reach corner cases.

Integration testing is a more-high-level testing that tests certain functionality on a group of connected services in an approximation of the real environment. It verifies that the services can successfully register in the environment and connect to other services as well as perform their tasks.

Code coverage of the develop branch can be observed in [Jenkins](https://ci.eclipse.org/kura/) and on [SonarCloud](https://sonarcloud.io/project/overview?id=org.eclipse.kura%3Akura).

Since Kura 5.1.0 every new test must follow the Gherkin structure (Given/When/Then) described in the [contributing guide](https://github.com/eclipse-kura/kura/blob/develop/CONTRIBUTING.md#tests): one feature per test class, scenarios as `@Test` methods and steps as private `given*`/`when*`/`then*` helpers.

### Unit Testing

Unit tests should try to cover as many corner cases in the code as possible. Add them for (all) the new code you decide to contribute.

#### Test Location

Unit tests are **co-located** with the code they cover: they live in the `src/test/java` folder of the bundle module itself (for instance `kura/org.eclipse.kura.core.configuration/src/test/java`) and are run by the `maven-surefire-plugin` during the build of that module. Test-only dependencies (JUnit, Mockito, ...) are declared with the `test` scope in the bundle `pom.xml`, so they never reach the bundle manifest.

#### Code Conventions

* Use the same package for the test as the class under test. Subpackages are OK.
* Keep the test classes in `src/test/java` only: bnd packages `src/main/java` in the bundle, so nothing under `src/test` is shipped.
* Use the same coding style as [Kura](https://github.com/eclipse-kura/kura/blob/develop/CONTRIBUTING.md#making-your-changes). Try to incorporate the suggestions SonarLint may have for your tests.

#### Running the Tests

The basic flow is to build your implementation using Maven and run the unit tests of a bundle with `mvn -f kura/<bundle>/pom.xml clean test`; add `-Dtest=<ClassName>` to narrow down to a single class. `mvn clean install` from the repository root runs the unit tests of every bundle (and the integration tests too), `-DskipTests` skips them all.

### Integration Testing

These tests verify proper behavior in the OSGi environment. Some additional configuration is therefore necessary.

#### Test Location

Integration tests are located in their own modules under `test/`, one per bundle under test, named `<bundle symbolic name>.test`. Each module is an OSGi **fragment** of the bundle it tests (`Fragment-Host` in its `bnd.bnd`), so it gains access to the internal packages of the bundle. The proper folder to put the tests in is `src/main/java`, since the test classes must be packaged inside the fragment.

The tests are run by the `bnd-testing-maven-plugin` inside a real Equinox framework, described by the `integration-test.bndrun` of the module, which includes the shared `test/integration-test.bnd` (framework, `JavaSE-21` execution environment, runtime properties and the Kura emulator bundles every test needs). The `Test-Cases` header of the fragment selects the classes to run: every public, concrete class whose name ends with `Test`.

#### Code Conventions

* Use `<bundle symbolic name>.test` as the name of the test module. Add it as a module in `test/pom.xml`, which also serves as the Maven parent: the module `pom.xml` contains only the artifactId and the dependencies specific to that module.
* Use the `<package name>.test` package to put the test in. Also add the `.test` suffix to any subpackages that are also under test.
* In `integration-test.bndrun` declare only what is specific to the module: the `-runrequires.<name>` entries for the bundle under test and any extra provider. The `-runbundles` list is written by the resolver and committed: after changing the requirements, re-resolve it with `mvn -f test/pom.xml clean verify -Presolve-integration-tests`.
* Use the same coding style as [Kura](https://github.com/eclipse-kura/kura/blob/develop/CONTRIBUTING.md#making-your-changes). Try to incorporate the suggestions SonarLint may have for your tests.

#### Running the Tests

The integration tests belong to the `tests` Maven profile, active by default and disabled by `-DskipTests`, so `mvn clean install` from the repository root builds the implementation and runs them. A single module can be run with `mvn -f test/<module>/pom.xml clean verify`. The framework log of the run is streamed into the Maven output, which is the first place to look when a component does not activate.
