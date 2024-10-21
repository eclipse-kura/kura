node {
   properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')), gitLabConnection('gitlab.eclipse.org'), [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false], [$class: 'JobLocalConfiguration', changeReasonComment: '']])
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
                  sh "mvn -f kura/distrib/pom.xml clean install"
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

    // Sonar checking commented to allow the build to finish
    // It needed to be commented as older versions of Eclipse Kura were meant to be compiled with Java 8. Now Sonar requires
    // Java 17, posing a challengo for a successful build.
    // We have decided to disable the sonar part to allow a successful build process but giving the chance to users that want to leverage such old 
    // maintenance versions of Kura to re-enable them if needed.

    // stage('Sonar') {
    //     timeout(time: 2, unit: 'HOURS') {
    //         dir("kura") {
    //             withMaven(jdk: 'temurin-jdk17-latest', maven: 'apache-maven-3.9.6') {
    //                 withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
    //                     withSonarQubeEnv {
    //                         sh '''
    //                             mvn -f kura/pom.xml sonar:sonar \
    //                                 -Dmaven.test.failure.ignore=true \
    //                                 -Dmaven.compiler.target=17 \
    //                                 -Dsonar.organization=eclipse \
    //                                 -Dsonar.host.url=${SONAR_HOST_URL} \
    //                                 -Dsonar.token=${SONARCLOUD_TOKEN} \
    //                                 -Dsonar.branch.name=${BRANCH_NAME} \
    //                                 -Dsonar.branch.target=${CHANGE_TARGET} \
    //                                 -Dsonar.java.source=8 \
    //                                 -Dsonar.java.binaries='target/' \
    //                                 -Dsonar.core.codeCoveragePlugin=jacoco \
    //                                 -Dsonar.projectKey=org.eclipse.kura:kura \
    //                                 -Dsonar.exclusions=test/**/*.java,test-util/**/*.java,org.eclipse.kura.web2/**/*.java,org.eclipse.kura.nm/src/main/java/org/freedesktop/**/*,org.eclipse.kura.nm/src/main/java/fi/w1/**/*
    //                         '''
    //                     }
    //                 }
    //             }
    //         }
    //     }
    // }
    // stage('quality-gate') {
    //     // Sonar quality gate
    //     timeout(time: 30, unit: 'MINUTES') {
    //         withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
    //             def qg = waitForQualityGate()
    //             if (qg.status != 'OK') {
    //                 error "Pipeline aborted due to sonar quality gate failure: ${qg.status}"
    //             }
    //         }
    //     }
    // }
}
