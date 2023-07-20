node {
    properties([disableConcurrentBuilds(abortPrevious: true), buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), gitLabConnection('gitlab.eclipse.org'), [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], [$class: 'JobLocalConfiguration', changeReasonComment: '']])

    deleteDir()

    stage('Preparation') { 
        dir("kura") {
            checkout scm
        }
    }

    stage('Build') {
        timeout(time: 2, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'adoptopenjdk-hotspot-jdk8-latest', maven: 'apache-maven-3.6.3') {
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
                withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.6.3') {
                    withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
                        withSonarQubeEnv {
                            sh '''mvn -f kura/pom.xml sonar:sonar \
                                -Dmaven.test.failure.ignore=true \
                                -Dsonar.organization=eclipse \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.login=${SONARCLOUD_TOKEN} \
                                -Dsonar.branch.name=${BRANCH_NAME} \
                                -Dsonar.junit.reportPaths='target/surefire-reports' \
                                -Dsonar.jacoco.reportPaths='target/jacoco/' \
                                -Dsonar.java.binaries='target/' \
                                -Dsonar.core.codeCoveragePlugin=jacoco \
                                -Dsonar.projectKey=org.eclipse.kura:kura \
                                -Dsonar.exclusions=test/**/*.java,test-util/**/*.java,org.eclipse.kura.web2/**/*.java,org.eclipse.kura.nm/src/main/java/org/freedesktop/**/*,org.eclipse.kura.nm/src/main/java/fi/w1/**/*'''
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
