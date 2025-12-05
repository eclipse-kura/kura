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
        dir('kura') {
            checkout scm
        }
    }

    stage('Build') {
        timeout(time: 2, unit: 'HOURS') {
            dir('kura') {
                withMaven(jdk: 'adoptopenjdk-hotspot-jdk8-latest', maven: 'apache-maven-3.9.6') {
                    sh 'touch /tmp/isJenkins.txt'
                    sh 'mvn -f target-platform/pom.xml clean install -Pno-mirror -Pcheck-exists-plugin'
                    sh 'mvn -f kura/pom.xml clean install -Pcheck-exists-plugin'
                    sh 'mvn -f kura/distrib/pom.xml clean install -DbuildAll'
                    sh 'mvn -f kura/examples/pom.xml clean install -Pcheck-exists-plugin'
                }
            }
        }
    }

    stage('Generate test reports') {
        dir('kura') {
            junit 'kura/test/*/target/surefire-reports/*.xml,kura/examples/test/*/target/surefire-reports/*.xml'
        }
    }

    stage('Archive .deb artifacts') {
        dir('kura') {
            archiveArtifacts artifacts: 'kura/distrib/target/*.deb', onlyIfSuccessful: true
        }
    }
}
