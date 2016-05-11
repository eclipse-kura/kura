#!/usr/bin/env bash

mvn clean install -f target-platform/pom.xml -Dmaven.test.skip=true &&

mvn clean install -f kura/org.eclipse.kura.api/pom.xml -Dmaven.test.skip=true &&
mvn clean install -f kura/org.eclipse.kura.core/pom.xml -Dmaven.test.skip=true &&
mvn clean install -f kura/org.eclipse.kura.core.certificates/pom.xml -Dmaven.test.skip=true &&
mvn clean install -f kura/org.eclipse.kura.camel/pom.xml &&
mvn clean install -f kura/org.eclipse.kura.linux.position/pom.xml -Dmaven.test.skip=true &&

mvn clean install -f kura/pom_pom.xml -Pweb -Dmaven.test.skip=true