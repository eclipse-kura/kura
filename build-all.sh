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

mvn "$@" -f target-platform/pom.xml clean install &&
mvn "$@" -f kura/manifest_pom.xml clean install -Dmaven.test.skip=true &&
mvn "$@" -f kura/pom_pom.xml clean install -Dmaven.test.skip=true -Pweb
