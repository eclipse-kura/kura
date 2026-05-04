node {
    properties([
        disableConcurrentBuilds(abortPrevious: true),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '2', daysToKeepStr: '', numToKeepStr: '5')),
        gitLabConnection('gitlab.eclipse.org'),
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
        [$class: 'JobLocalConfiguration', changeReasonComment: '']
    ])

    deleteDir()

    stage('Preparation') {
        dir("kura") {
            checkout scm
        }
    }

    stage('Build') {
        timeout(time: 2, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'adoptopenjdk-hotspot-jdk8-latest', maven: 'apache-maven-3.9.6') {
                    sh "touch /tmp/isJenkins.txt"
                    sh "mvn -f target-platform/pom.xml clean install -Pno-mirror -Pcheck-exists-plugin"
                    sh "mvn -f kura/pom.xml clean install -Pcheck-exists-plugin"
                    sh "mvn -f kura/distrib/pom.xml clean install -DbuildAll"
                    sh "mvn -f kura/examples/pom.xml clean install -Pcheck-exists-plugin"
                }
            }
        }
    }

    stage('Generate test reports') {
        dir("kura") {
            junit 'kura/test/*/target/surefire-reports/*.xml,kura/examples/test/*/target/surefire-reports/*.xml'
        }
    }

    stage('Archive .deb artifacts') {
        dir("kura") {
            archiveArtifacts artifacts: 'kura/distrib/target/*.deb', onlyIfSuccessful: true
        }
    }

    stage('Generate aggregate coverage report') {
        dir("kura") {
            withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                sh '''
                    set -eu

                    REPORT_DIR="kura/target/site/jacoco-aggregate"
                    JACOCO_CLI_DIR="kura/target/jacoco-cli"
                    JACOCO_CLI_JAR="${JACOCO_CLI_DIR}/org.jacoco.cli-nodeps.jar"
                    UNIQUE_CLASSES_DIR="${REPORT_DIR}/classes"

                    mkdir -p "${REPORT_DIR}" "${JACOCO_CLI_DIR}"

                    mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.0.0:copy \
                        -Dartifact=org.jacoco:org.jacoco.cli:0.8.8:jar:nodeps \
                        -DoutputDirectory="${JACOCO_CLI_DIR}" \
                        -Dmdep.stripVersion=true

                    find kura/test kura/examples/test -name jacoco.exec -type f | sort > "${REPORT_DIR}/jacoco-exec-files.txt"

                    if [ ! -s "${REPORT_DIR}/jacoco-exec-files.txt" ]; then
                        echo "No JaCoCo execution data found, skipping aggregate coverage report generation."
                        exit 0
                    fi

                    git ls-files 'kura/**/src/main/java/**' \
                        | sed 's#/src/main/java/.*#/src/main/java#' \
                        | grep -v '^kura/test/' \
                        | grep -v '^kura/examples/test/' \
                        | sort -u > "${REPORT_DIR}/jacoco-source-dirs.txt"

                    while IFS= read -r source_dir; do
                        class_dir="${source_dir%/src/main/java}/target/classes"
                        if [ -d "${class_dir}" ]; then
                            echo "${class_dir}"
                        fi
                    done < "${REPORT_DIR}/jacoco-source-dirs.txt" > "${REPORT_DIR}/jacoco-class-dirs.txt"

                    rm -rf "${UNIQUE_CLASSES_DIR}"
                    mkdir -p "${UNIQUE_CLASSES_DIR}"

                    while IFS= read -r class_dir; do
                        find "${class_dir}" -name '*.class' -type f | sort
                    done < "${REPORT_DIR}/jacoco-class-dirs.txt" | while IFS= read -r class_file; do
                        relative_class_file="${class_file#*/target/classes/}"
                        unique_class_file="${UNIQUE_CLASSES_DIR}/${relative_class_file}"

                        if [ ! -e "${unique_class_file}" ]; then
                            mkdir -p "$(dirname "${unique_class_file}")"
                            ln -s "$(pwd)/${class_file}" "${unique_class_file}"
                        fi
                    done

                    find kura -path '*/generated-sources/src/main/java' -type d \
                        ! -path 'kura/test/*' \
                        ! -path 'kura/examples/test/*' \
                        | sort >> "${REPORT_DIR}/jacoco-source-dirs.txt"

                    set -- report

                    while IFS= read -r exec_file; do
                        set -- "$@" "${exec_file}"
                    done < "${REPORT_DIR}/jacoco-exec-files.txt"

                    set -- "$@" --xml "${REPORT_DIR}/jacoco.xml" --html "${REPORT_DIR}/html"
                    set -- "$@" --classfiles "${UNIQUE_CLASSES_DIR}"

                    while IFS= read -r source_dir; do
                        set -- "$@" --sourcefiles "${source_dir}"
                    done < "${REPORT_DIR}/jacoco-source-dirs.txt"

                    java -jar "${JACOCO_CLI_JAR}" "$@"
                '''
            }
        }
    }

    stage('Sonar') {
        timeout(time: 2, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                    withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
                        withSonarQubeEnv {
                            sh '''
                                mvn -f kura/pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \
                                    -Dmaven.test.failure.ignore=true \
                                    -Dsonar.organization=eclipse \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.token=${SONARCLOUD_TOKEN} \
                                    -Dsonar.branch.name=${BRANCH_NAME} \
                                    -Dsonar.branch.target=${CHANGE_TARGET} \
                                    -Dsonar.java.source=8 \
                                    -Dsonar.java.binaries='target/classes' \
                                    -Dsonar.sources=src/main/java \
                                    -Dsonar.tests=src/test/java \
                                    -Dsonar.scm.exclusions.disabled=true \
                                    -Dsonar.core.codeCoveragePlugin=jacoco \
                                    -Dsonar.coverage.jacoco.xmlReportPaths="$(pwd)/kura/target/site/jacoco-aggregate/jacoco.xml" \
                                    -Dsonar.projectKey=org.eclipse.kura:kura \
                                    -Dsonar.exclusions=**/bin/**,**/node_modules/**,test/**/*.java,test-util/**/*.java,org.eclipse.kura.web2/**/*.java,org.eclipse.kura.nm/src/main/java/org/freedesktop/**/*,org.eclipse.kura.nm/src/main/java/fi/w1/**/*,org.eclipse.kura.linux.gpio.libgpiod/src/main/java/org/eclipse/kura/linux/gpio/libgpiod1/LibGpiodV1Native.java,org.eclipse.kura.linux.gpio.libgpiod/src/main/java/org/eclipse/kura/linux/gpio/libgpiod2/LibGpiodV2Native.java
                            '''
                        }
                    }
                }
            }
        }
    }

    stage('quality-gate') {
        // Sonar quality gate
        timeout(time: 30, unit: 'MINUTES') {
            withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to sonar quality gate failure: ${qg.status}"
                }
            }
        }
    }
}
