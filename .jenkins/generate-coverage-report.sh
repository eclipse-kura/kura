#!/usr/bin/env bash

#
#  Copyright (c) 2026 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#     Eurotech
#

# Generates a consolidated JaCoCo coverage report for all Kura bundles.
# Must be run from the repository root, after the build and tests have run.

set -euo pipefail

if [ ! -f kura/pom.xml ] || [ ! -d .jenkins ]; then
    echo "ERROR: $(basename "$0") must be run from the repository root" >&2
    exit 1
fi

JACOCO_CLI="${JACOCO_CLI:-kura/target/coverage/org.jacoco.cli-nodeps.jar}"

if [ ! -f "$JACOCO_CLI" ]; then
    echo "ERROR: JaCoCo CLI not found at '$JACOCO_CLI' (set JACOCO_CLI to override)" >&2
    exit 1
fi

COVERAGE_DIR=kura/target/coverage
CLASSES="$COVERAGE_DIR/classes"
REPORT="$COVERAGE_DIR/report"

mkdir -p "$CLASSES" "$REPORT"

for classes in kura/org.eclipse.kura.*/target/classes; do
    cp -R "$classes"/. "$CLASSES"
done
find "$CLASSES" -type f ! -name '*.class' -delete

# exclude generated code, keep aligned with -Dsonar.exclusions in the Jenkinsfile
rm -rf "$CLASSES/org/eclipse/kura/web" "$CLASSES/org/freedesktop" "$CLASSES/fi/w1" "$CLASSES/inference" "$CLASSES/org/eclipse/tahu/protobuf"
find "$CLASSES" -name 'LibGpiodV[12]Native.class' -delete -o -name 'LibGpiodV[12]Native$*.class' -delete

SOURCES=()
for sources in kura/org.eclipse.kura.*/src/main/java; do
    SOURCES+=(--sourcefiles "$sources")
done

java -jar "$JACOCO_CLI" report kura/test/*/target/jacoco.exec \
    --classfiles "$CLASSES" "${SOURCES[@]}" --encoding UTF-8 --name "Eclipse Kura" \
    --html "$REPORT/html" --xml "$REPORT/jacoco.xml" --csv "$REPORT/jacoco.csv"
