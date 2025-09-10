def uploadPackages(String repoDistribution, String repoModule, Boolean setupPromotion = false) {
    stage ("Upload packages parameters check") {
        echo "Distribution: ${repoDistribution}"
        echo "Module: ${repoModule}"

        // Check "distribution" parameter is set and valid
        assert repoDistribution instanceof String
        assert repoDistribution ==~ /kura-\d+/

        // Check "module" parameter is set and valid
        def valid_modules = [
            "base"
        ]

        assert repoModule instanceof String
        assert valid_modules.contains(repoModule)
    }

    stage("Upload .deb packages to Artifactory") {
        def debFilesOutput = sh(script: "find kura -type f -name '*.deb'", returnStdout: true).trim()
        def debFiles = debFilesOutput ? debFilesOutput.split("\n") : []

        debFiles.each {
            withCredentials([usernameColonPassword(credentialsId: 'repo.eclipse.org-bot-account', variable: 'USERPASS')]) {
                sh(
                    script: "curl -u \"\$USERPASS\" -H \"Content-Type: multipart/form-data\" --data-binary \"@./${it}\" \"https://repo3.eclipse.org/repository/kura-apt/\"",
                    returnStatus: true
                )
            }
        }

        if (setupPromotion) {
            // TODO
        }

    }
}

return this
