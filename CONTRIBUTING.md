# How to contribute to Eclipse Kura

First of all, thanks for considering to contribute to Eclipse Kura. We really appreciate the time and effort you want to
spend helping to improve things around here. And help we can use :-)

Here is a (non-exclusive, non-prioritized) list of things you might be able to help us with:

* bug reports
* bug fixes
* improvements regarding code quality e.g. improving readability, performance, modularity etc.
* documentation (Getting Started guide, Examples, Deployment instructions in cloud environments)
* features (both ideas and code are welcome)
* tests

In order to get you started as fast as possible we need to go through some organizational issues first.

## Legal Requirements

Kura is an [Eclipse IoT](http://iot.eclipse.org) project and as such is governed by the Eclipse Development process.
This process helps us in creating great open source software within a safe legal framework.

### First Steps
For you as a contributor, the following preliminary steps are required in order for us to be able to accept your contribution:

* Sign the [Eclipse Foundation Contributor License Agreement](http://www.eclipse.org/legal/CLA.php).
In order to do so:

  * Obtain an Eclipse Foundation user ID. Anyone who currently uses Eclipse Bugzilla or Gerrit systems already has one of those.
If you don't already have an account simply [register on the Eclipse web site](https://dev.eclipse.org/site_login/createaccount.php).
  * Once you have your account, log in to the [projects portal](https://projects.eclipse.org/), select *My Account* and then the *Contributor License Agreement* tab.

* Add your GiHub username to your Eclipse Foundation account. Log in to Eclipse and go to [Edit my account](https://dev.eclipse.org/site_login/myaccount.php).

### File Headers
A proper header must be in place for any file contributed to Eclipse Kura. For a new contribution, please add the below header:

```
/*******************************************************************************
 * Copyright (c) <year> <legal entity> and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  <legal entity>
 *******************************************************************************/
```

 Please ensure \<year\> is replaced with the current year or range (e.g. 2017 or 2015, 2017).
 Please ensure \<legal entity\> is replaced with the relevant information. If you are editing an existing contribution, feel free
 to create or add your legal entity to the contributors section

### How to Contribute
The easiest way to contribute code/patches/whatever is by creating a GitHub pull request (PR). When you do make sure that you *Sign-off* your commit records using the same email address used for your Eclipse account.

You do this by adding the `-s` flag when you make the commit(s), e.g.

    $> git commit -s -m "Shave the yak some more"

You can find all the details in the [Contributing via Git](http://wiki.eclipse.org/Development_Resources/Contributing_via_Git) document on the Eclipse web site.

## Making your Changes

* Fork the repository on GitHub
* Create a new branch for your changes
* Configure your IDE installing:
  * The formatter profile available in kura/setup/formatting/KuraFormatter*.xml
  * The cleanup profile available in kura/setup/formatting/KuraCleanupProfile*.xml
  * [SonarLint](http://www.sonarlint.org/eclipse/index.html)
* Make your changes
* Make sure you include test cases for non-trivial features
* Make sure the test suite passes after your changes
* Make sure copyright headers are included in (all) files including updated year(s)
* Make sure build plugins and dependencies and their versions have (approved) CQs
* Make sure proper headers are in place for each file (see above Legal Requirements)
* Commit your changes into that branch
* Use descriptive and meaningful commit messages
* If you have a lot of commits squash them into a single commit
* Make sure you use the `-s` flag when committing as explained above
* Push your changes to your branch in your forked repository
* Make a full clean build (locally and/or using [Travis CI](http://travis-ci.org)) before you submit a pull request

## Tests

Eclipse Kura since 5.1.0 version only accepts tests made following Gherkin format.

Extend the old tests files is still allowed but not creating new ones.

### Gherkin Tests Guidelines

Gherkin is a particular language that originates from Behavioral-Driven Development (BDD). In BDD, the developer defines tests that will guide the development and constitute a requirement. The tests are written by first defining the **feature** that is going to be tested, then defining the most relevant execution paths for that feature, called **scenarios**, and, finally, detailing the scenarios into simple **steps** using Gherkin language.

### Features

Every Gherkin test should test a specific feature. Every tested feature is contained in a single test class.

An example of feature for the H2DbWireStoreComponent can be *“Store wire envelopes“* and the corresponding test class will be StoreWireEnvelopeTest.java.

Guidelines for defining features:

- group features for a specific component in the same test package
- if it is not easy to define a simple name that describes the feature, then maybe it is too complicated and needs to be split into multiple features
- if the tested component is simple, then use the existing naming convention: ComponentNameTest

### Scenarios

Scenarios break down the single feature into steps. Each scenario covers a path of execution for that feature and is identified by a method inside the feature class.

As an example for the H2DbStoreComponent, one can think of a scenario *“Successful store data“* which will correspond to the method successfulStore(), but also of a scenario that should cause an error like *“Store in a DB table that does not exist“*.

Guidelines for defining scenarios:

- try to define also the unhappy paths, i.e. paths that cause errors
- methods that define scenarios do not return values, do not have parameters, and should be annotated with JUnit’s @Test annotation
- the method name should describe the scenario as better as possible; no comments should be needed to understand it; else, break it down into multiple ones
- no nested scenarios
- scenarios should be placed on top of the class, it is the first and only thing that a developer should read to understand the test

### Steps

Steps detail scenarios further. Scenarios contain multiple steps that follow a specific pattern. Steps can be defined using one of these keywords:

***given - when - then***

- **given** defines the test preconditions, an initial context
- **when** defines the actions performed, or events
- **then** describes the effects, the expected outcome

Guidelines for defining steps:

- step methods must be private and separated from the scenarios: public scenarios on top, private steps on the bottom
- steps inside a scenario always follow the *given-when-then* order
- step methods names must start with one of the keywords
- there can be multiple steps of the same kind inside a single scenario, i.e. 3 given, 1 when, 2 then; to improve readability separate them with a blank line to form a group for the *given* steps, one for the *when* steps, and one for the *then* steps
- steps can take arguments
- steps do not return values
- step methods contain asserts

Example of a scenario with steps:

```java
@Test
public void storeOnNotExistentTable() {
  givenDatabaseConnection();
  givenConfigurationWithWrongTableName();
  
  whenWireEnvelopeArrives();

  thenThrowException();
  thenDataNotStored();
}
```

### Internal State and Helpers

Since step and scenario methods are not allowed to return any value, it is sometimes necessary to maintain an **internal state** inside the feature class. Where the tested features require complex dependencies, it might be useful to define an **abstract helper class** which will be inherited by all the feature classes.

The Gherkin reference: https://cucumber.io/docs/gherkin/reference/ 

### Manual Tests

Before you submit a Pull Request, ensure that your changes have been tested and are functional in a live environment. If you require additional validation, kindly request your reviewer to perform the necessary tests. It is strongly recommended that you personally verify your changes before submission and only seek further testing when absolutely necessary.

## Submitting the Changes

Submit a pull request via the normal GitHub UI.

We are using the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) convention for our pull request titles. This convention dovetails with [SemVer](https://semver.org/), by describing the features, fixes, and breaking changes made in commit messages.

Every PR which is not in draft mode should follow conventional commit convention for PR title. Therefore the title should be in the format:

```
<type>[optional scope]: <description>
```

Where:
- `<type>` - see [_Type_ section](#type)
- `[optional scope]` - see [_Scope_ section](#scope)
- `<description>` - description of the PR

**BREAKING CHANGE:** a commit that appends a `!` after the type/scope, introduces a breaking API change. A BREAKING CHANGE can be part of commits of any _type_.

Once merged and squashed in the main development branch, the resulting commit message should match the PR title.

For details see the [full specification](https://www.conventionalcommits.org/en/v1.0.0/#specification).

### Type

1.  The type `feat` MUST be used when a commit adds a new feature to your application or library.
2.  The type `fix` MUST be used when a commit represents a bug fix for your application.

Types other than `feat` and `fix` MAY be used in your commit messages and must be one of the following:

- `docs`: Documentation only changes
- `refactor`: A code change that neither fixes a bug nor adds a feature (removing redundant code, code semplification, renaming variables, removing features, etc)
- `test`: Adding missing tests or correcting existing tests
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- `chore`: Tool changes, configuration changes, and changes to things that do not actually go into production at all (upticks/bumps, manual update of release notes...)
- `build`: Changes that affect the build system or external dependencies (dependencies update)
- `ci`: Changes to our CI configuration files and scripts
- `revert`: Reverts a previous commit
- `perf`: A code change that improves performance
- `deprecate`: An API/component deprecation. Use the summary field to provide the API/component which was deprecated (eg: `deprecate: org.eclipse.kura.position.PositionService`)

### Scope

A scope MAY be provided after a type. A scope MUST consist of a noun describing a section of the codebase surrounded by parenthesis, e.g., `fix(parser):`

The scope should describe the section of the code affected by the changes as perceived by the person reading the changelog generated from commit messages.

As a general rule use the last part of the bundle name as the scope (i.e. without `org.eclipse.kura`). If the PR spans across multiple bundles use the main one or do not fill the scope at all.

Example: if a PR adds a feature to the `org.eclipse.kura.core.keystore` bundle, the PR title shoud be `feat(core.keystore): new awesome feature`.

## After Submitting

* Do not use your branch for any other development, otherwise further changes that you make will be visible in the PR.
