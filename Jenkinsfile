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
        gitLabConnection('gitlab.eclipse.org')
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
        dir("kura") {
            try {
                // Single reactor from the root parent pom: it aggregates bom, kura (all bundles +
                // wrapper bundles), distrib and test, ordered by inter-module dependencies.
                timeout(time: 3, unit: 'HOURS') {
                    withMaven(jdk: 'temurin-jdk21-latest', maven: 'apache-maven-3.9.9', publisherStrategy: 'EXPLICIT') {
                        sh "mvn -B -ntp clean install -Pcheck-exists-plugin"
                    }
                }
            } finally {
                // In the finally block so a failing test still publishes its report: the reactor is
                // fail-fast, so a test failure aborts the Build stage before this would otherwise run.
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/**/TEST-*.xml, **/target/test-reports/**/TEST-*.xml'
            }
        }
    }

    stage ("Deploy on Nexus") {
        // Call uploadPackages only if we are on the default branch,
        // if we have DEB packages to upload and if the user has set the pushArtifacts parameter to true
        if (env.BRANCH_IS_PRIMARY) {
            echo "Uploading DEB packages..."

            dir("kura") {
                def distribPom = readMavenPom file: 'distrib/pom.xml'

                def repoDistribution = distribPom.properties['kura.repo.distribution']
                def repoModule = distribPom.properties['kura.repo.module']

                def nexusUtils = load '.jenkins/nexusUtils.groovy'
                nexusUtils.uploadPackages(repoDistribution, repoModule)
            }
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
                withMaven(jdk: 'temurin-jdk21-latest', maven: 'apache-maven-3.9.9', publisherStrategy: 'EXPLICIT') {
                    withSonarQubeEnv(credentialsId: 'sonarcloud-token') {
                        // Check if on primary branch
                        def analysisParameters = ""
                        if (env.CHANGE_ID) {
                            analysisParameters = "-Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} -Dsonar.pullrequest.base=${env.CHANGE_TARGET} -Dsonar.pullrequest.key=${env.CHANGE_ID}"
                        } else {
                            analysisParameters = "-Dsonar.branch.name=${env.BRANCH_NAME}"
                        }

                        sh """
                            mvn -B -ntp -f kura/pom.xml org.sonarsource.scanner.maven:sonar-maven-plugin:5.6.0.6792:sonar \
                                -Dsonar.organization=eclipse \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                ${analysisParameters} \
                                -Dsonar.projectKey=org.eclipse.kura:kura \
                                -Dsonar.exclusions=**/*.xml,**/*.yml,test-util/**/*,emulator/**/*,com.codeminders.hidapi-parent/**/*,org.moka7/**/*,org.eclipse.soda.dk.comm-parent/**/*,org.eclipse.kura.sun.misc/**/*,log4j2-api-config/**/*,org.usb4java/**/*,usb4java-javax/**/* \
                                -Dsonar.cpd.exclusions=**/*Metatype.java,**/*Options.java \
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
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                error "Pipeline aborted due to sonar quality gate failure: ${qg.status}"
            }
        }
    }
}
