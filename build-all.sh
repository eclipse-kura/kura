#!/usr/bin/env bash

#
#  Copyright (c) 2016, 2026 Red Hat and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#     Red Hat
#     Eurotech
#

# activate batch mode by default

MAVEN_PROPS="-B"

# Tests run via the `tests` profile (kura/pom.xml), which is active unless -DskipTests
# is set. It pulls in the kura/test integration-test modules (bnd-testing) in addition
# to the unit tests relocated into the bundles. Skip them by default; set RUN_TESTS=1
# to run the full unit + integration suite. Use -DskipTests (not -Dmaven.test.skip)
# so the integration-test modules are excluded from the reactor when skipping.

[ -z "$RUN_TESTS" ] && MAVEN_PROPS="$MAVEN_PROPS -DskipTests"

mvn "$@" --color=always -f target-platform/pom.xml clean install $MAVEN_PROPS &&
mvn "$@" --color=always -f kura/pom.xml clean install $MAVEN_PROPS &&
mvn "$@" --color=always -f kura/distrib/pom.xml clean install $MAVEN_PROPS

