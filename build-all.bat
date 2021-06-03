mvn -f target-platform/pom.xml clean install "-Dmaven.test.skip=true" &&^
mvn -Pcan -f kura/pom.xml clean install "-Dmaven.test.skip=true" &&^
mvn -Pcan -f kura/examples/pom.xml clean install "-Dmaven.test.skip=true" 
rem && mvn -f kura/distrib/pom.xml clean install "-Dmaven.test.skip=true"