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

        stage('Sonar') {
            timeout(time: 2, unit: 'HOURS') {
                dir("kura") {
                    withMaven(jdk: 'temurin-jdk21-latest', maven: 'apache-maven-3.9.9', options: [artifactsPublisher(disabled: true)]) {
                        withSonarQubeEnv(credentialsId: 'sonarcloud-token') {
                            // Check if on primary branch
                            def analysisParameters = ""
                            if (env.CHANGE_ID) {
                                analysisParameters = "-Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} -Dsonar.pullrequest.base=${env.CHANGE_TARGET} -Dsonar.pullrequest.key=${env.CHANGE_ID}"
                            } else {
                                analysisParameters = "-Dsonar.branch.name=${env.BRANCH_NAME}"
                            }

                            sh """
                                mvn -f kura/pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                    -Dmaven.test.failure.ignore=true \
                                    -Dsonar.organization=eclipse \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.java.binaries='target/' \
                                    ${analysisParameters} \
                                    -Dsonar.core.codeCoveragePlugin=jacoco \
                                    -Dsonar.projectKey=org.eclipse.kura:kura \
                                    -Dsonar.exclusions=test/**/*,**/*.xml,**/*.yml,test-util/**/* \
                                    -Dsonar.test.exclusions=**/*
                            """
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
