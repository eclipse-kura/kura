#!/bin/bash
#
#  Copyright (c) 2025 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#   Eurotech
#
VERSION="6.0.0"
PACKAGE_FILE=$(ls ../target/kura_${VERSION}*_docker-x86_64-nn_installer.deb)
PACKAGE_FILENAME=$(basename ${PACKAGE_FILE})

rm -r target
mkdir -p target
cp $PACKAGE_FILE ./target/

docker build -t kura-debian:${VERSION} -t kura-debian-x86_64:${VERSION} \
	--build-arg PACKAGE_FILE="target/${PACKAGE_FILENAME}" .
