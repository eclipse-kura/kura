#!/usr/bin/env bash

#
# Copyright (c) 2016 Red Hat and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
#  Contributors:
#     Red Hat - Initial API and implementation
#

# call this script with KURA_BUILD_CACHE set to an existing directory
# in order to use a local cache for downloaded dependencies
# Call it like:
#   KURA_BUILD_CACHE=/tmp/kura.build ./build-all.sh

test -z "$KURA_BUILD_CACHE" && echo "Consider setting KURA_BUILD_CACHE to enable the local build cache"

mvn "$@" -f target-platform/pom.xml clean install &&
mvn "$@" -f kura/manifest_pom.xml clean install -Dmaven.test.skip=true -Pcan &&
mvn "$@" -f kura/pom_pom.xml clean install -Dmaven.test.skip=true -Pweb

