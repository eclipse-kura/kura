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

[ -z "$RUN_TESTS" ] && MAVEN_PROPS="$MAVEN_PROPS -DskipTests"
mvn "$@" --color=always clean install $MAVEN_PROPS

