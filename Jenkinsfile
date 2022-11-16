node {
    properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')), gitLabConnection('gitlab.eclipse.org'), [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], [$class: 'JobLocalConfiguration', changeReasonComment: '']])
    
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
                    sh "mvn -f kura/distrib/pom.xml clean install"
                    sh "mvn -f kura/examples/pom.xml clean install -Pcheck-exists-plugin"
                }
            }
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
                                -Dsonar.exclusions=test/**/*.java,test-util/**/*.java,org.eclipse.kura.web2/**/*.java'''
                        }
                    }
                }
            }
        }
    }
    
    stage('Results') {
        dir("kura") {
            junit 'kura/test/*/target/surefire-reports/*.xml,kura/examples/test/*/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'kura/distrib/target/*.deb, kura/distrib/target/*_intel-edison-nn_installer.sh', onlyIfSuccessful: true
        }
    }

    stage('Quality Gate'){
        timeout(time: 30, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                error "Pipeline aborted due to quality gate failure: ${qg.status}"
            }
        }
    }
}