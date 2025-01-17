def boolean onlyDocumentationFilesChangedIn(String workDirectory) {
    if (!env.CHANGE_TARGET) {
        echo "CHANGE_TARGET not set. Skipping check"
        return false
    }

    def changedFiles = sh(script: "cd ${workDirectory} && git diff --name-only origin/${env.CHANGE_TARGET} origin/${env.BRANCH_NAME}", returnStdout: true).trim().split("\n")

    echo "Changed files: ${changedFiles}" // Debug

    return changedFiles && changedFiles.every { it.endsWith(".md") || it.endsWith(".txt") }
}

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
            sh "touch /tmp/isJenkins.txt"
        }
    }

    // Skip build if only documentation files (i.e. *.md and *.txt) have changed
    if (onlyDocumentationFilesChangedIn("kura")) {
        echo "Skipping build for documentation changes"
        currentBuild.result = 'SUCCESS'
        return
    }

    stage('Check copyright headers date') {
        dir("kura") {
            def changedFiles = sh(script: "git diff --name-only origin/${env.CHANGE_TARGET} origin/${env.BRANCH_NAME}", returnStdout: true).trim().split("\n")
            def year = new Date().format("yyyy")
            def atLeastOneFileWasNotValid = false

            for (String file : changedFiles) {
                if (!file.endsWith(".java") ) {
                    continue
                }

                // Only grep the second line of the file (which contains the year) as per our checkstyle rules
                String command = "cat ${file} | sed -n 2p | grep -o -e '\\(\\d\\{4\\}\\)[^,]' || true"
                // Debug
                echo "Command: ${command}"

                def out = sh(script: command , returnStdout: true).trim()

                // Debug
                echo "File: ${file}"
                echo "Output: ${out}"

                if(out != year) {
                    echo "File ${file} does not have the current year in the header"
                    atLeastOneFileWasNotValid = true
                }
            }

            if (atLeastOneFileWasNotValid) {
                error "At least one file does not have the correct year in the header. See console output for details"
            }
        }
    }

    stage('Build target-platform') {
        timeout(time: 1, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                    sh "mvn -f target-platform/pom.xml clean install -Pno-mirror -Pcheck-exists-plugin"
                }
            }
        }
    }

    stage('Build core') {
        timeout(time: 2, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                    sh "mvn -f kura/pom.xml -Dsurefire.rerunFailingTestsCount=3 clean install -Pcheck-exists-plugin"
                }
            }
        }
    }

    stage('Build distrib') {
        timeout(time: 1, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                    sh "mvn -f kura/distrib/pom.xml clean install -DbuildAll"
                }
            }
        }
    }

    stage('Build examples') {
        timeout(time: 1, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
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

    stage('Sonar') {
        timeout(time: 2, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
                    withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
                        withSonarQubeEnv {
                            sh '''
                                mvn -f kura/pom.xml sonar:sonar \
                                    -Dmaven.test.failure.ignore=true \
                                    -Dsonar.organization=eclipse \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.token=${SONARCLOUD_TOKEN} \
                                    -Dsonar.branch.name=${BRANCH_NAME} \
                                    -Dsonar.branch.target=${CHANGE_TARGET} \
                                    -Dsonar.java.source=8 \
                                    -Dsonar.java.binaries='target/' \
                                    -Dsonar.core.codeCoveragePlugin=jacoco \
                                    -Dsonar.projectKey=org.eclipse.kura:kura \
                                    -Dsonar.exclusions=test/**/*.java,test-util/**/*.java,org.eclipse.kura.web2/**/*.java,org.eclipse.kura.nm/src/main/java/org/freedesktop/**/*,org.eclipse.kura.nm/src/main/java/fi/w1/**/*
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
