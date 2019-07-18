mvn  -f target-platform/pom.xml clean install -B -Dmaven.test.skip=true
mvn  -f kura/pom.xml clean install -B -Dmaven.test.skip=true
mvn  -f kura/examples/pom.xml clean install -B -Dmaven.test.skip=true

