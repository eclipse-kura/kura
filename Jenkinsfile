def boolean onlyDocumentationFilesChangedIn(String workDirectory) {
    if (!env.CHANGE_TARGET) {
        echo "CHANGE_TARGET not set. Skipping check"
        return false
    }

    def changedFiles = sh(script: "cd ${workDirectory} && git diff --name-only origin/${env.CHANGE_TARGET} origin/${env.BRANCH_NAME}", returnStdout: true).trim().split("\n")

    echo "Changed files: ${changedFiles}" // Debug

    return changedFiles && changedFiles.every { it.endsWith(".md") || it.endsWith(".txt") }
}

podTemplate(inheritFrom: 'basic', yaml: '''
spec:
  containers:
  - name: "jnlp"
    resources:
      limits:
        cpu: "2000m"
        memory: "5Gi"
      requests:
        cpu: "1000m"
        memory: "3Gi"
''') {
    node(POD_LABEL) {

        properties([
            disableConcurrentBuilds(abortPrevious: true),
            buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '1', daysToKeepStr: '', numToKeepStr: '3')),
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

        stage('Build target-platform') {
            timeout(time: 1, unit: 'HOURS') {
                dir("kura") {
                    withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6', options: [artifactsPublisher(disabled: true)]) {
                        sh "mvn -f target-platform/pom.xml clean install -Pno-mirror -Pcheck-exists-plugin"
                    }
                }
            }
        }

        stage('Build core') {
            timeout(time: 2, unit: 'HOURS') {
                dir("kura") {
                    withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6', options: [artifactsPublisher(disabled: true)]) {
                        sh "mvn -f kura/pom.xml -Dsurefire.rerunFailingTestsCount=3 clean install -Pcheck-exists-plugin"
                    }
                }
            }
        }

        stage('Build distrib') {
            timeout(time: 1, unit: 'HOURS') {
                dir("kura") {
                    withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6', options: [artifactsPublisher(disabled: true)]) {
                        sh "mvn -f kura/distrib/pom.xml clean install -DbuildAll"
                    }
                }
            }
        }

        stage('Generate test reports') {
            dir("kura") {
                junit 'kura/test/*/target/surefire-reports/*.xml'
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
                    withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6', options: [artifactsPublisher(disabled: true)]) {
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
}
