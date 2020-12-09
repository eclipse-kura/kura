#!/usr/bin/env bash

#
#  Copyright (c) 2016, 2020 Red Hat and others
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

# allow running tests

[ -z "$RUN_TESTS" ] && MAVEN_PROPS="$MAVEN_PROPS -Dmaven.test.skip=true"

mvn "$@" -f target-platform/pom.xml clean install $MAVEN_PROPS &&
mvn "$@" -f kura/pom.xml clean install $MAVEN_PROPS &&
mvn "$@" -f kura/examples/pom.xml clean install $MAVEN_PROPS &&
mvn "$@" -f kura/distrib/pom.xml clean install $MAVEN_PROPS 

