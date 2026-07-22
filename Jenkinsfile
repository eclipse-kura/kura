import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

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

    stage('Build') {
        // Single reactor from the root parent pom: it aggregates bom, kura (all bundles +
        // wrapper bundles), distrib and test, ordered by inter-module dependencies.
        timeout(time: 3, unit: 'HOURS') {
            dir("kura") {
                withMaven(jdk: 'temurin-jdk21-latest', maven: 'apache-maven-3.9.9', options: [artifactsPublisher(disabled: true)]) {
                    sh "mvn clean install -Pcheck-exists-plugin -Ptests"
                }
            }
        }
    }

    stage('Generate test reports') {
        dir("kura") {
            // Three sources of JUnit XML, all published:
            //  - unit tests moved into the kura/ bundles run under surefire (surefire-reports);
            //  - the test/ reactor's pure-unit modules also run under surefire (surefire-reports);
            //  - the test/ reactor's bnd-testing integration modules write to the plugin default
            //    (test-reports) since the read-only reportsDir is no longer configured.
            junit 'kura/**/target/surefire-reports/**/TEST-*.xml, test/*/target/surefire-reports/**/TEST-*.xml, test/*/target/test-reports/**/TEST-*.xml'
        }
    }

    stage ("Deploy on Nexus") {
        // Call uploadPackages only if we are on the default branch,
        // if we have DEB packages to upload and if the user has set the pushArtifacts parameter to true
        if (env.BRANCH_IS_PRIMARY) {
            echo "Uploading DEB packages..."

            def distribPom = readMavenPom file: 'kura/distrib/pom.xml'

            def repoDistribution = distribPom.properties['kura.repo.distribution']
            def repoModule = distribPom.properties['kura.repo.module']

            def nexusUtils = load 'kura/.jenkins/nexusUtils.groovy'
            nexusUtils.uploadPackages(repoDistribution, repoModule)
        } else {
            echo "Skipping DEB upload"
            Utils.markStageSkippedForConditional(STAGE_NAME)
        }
    }

    stage('Archive .deb artifacts') {
        dir("kura") {
            archiveArtifacts artifacts: 'distrib/**/target/*.deb', onlyIfSuccessful: true
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
                            mvn -f kura/pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:5.6.0.6792:sonar \
                                -Dmaven.test.failure.ignore=true \
                                -Dsonar.organization=eclipse \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.java.binaries='target/classes' \
                                ${analysisParameters} \
                                -Dsonar.core.codeCoveragePlugin=jacoco \
                                -Dsonar.projectKey=org.eclipse.kura:kura \
                                -Dsonar.exclusions=test/**/*,**/*.xml,**/*.yml,test-util/**/*,emulator/**/*,com.codeminders.hidapi-parent/**/*,org.moka7/**/*,org.eclipse.soda.dk.comm-parent/**/*,org.eclipse.kura.sun.misc/**/*,org.eclipse.kura.camel.sun.misc/**/*,log4j2-api-config/**/*,org.usb4java/**/*,usb4java-javax/**/* \
                                -Dsonar.coverage.exclusions=org.eclipse.kura.camel/**/*,org.eclipse.kura.camel.xml/**/*,org.eclipse.kura.camel.cloud.factory/**/* \
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
