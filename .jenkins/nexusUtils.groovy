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
        def debFiles = findFiles(glob: 'kura/**/*.deb')

        if (debFiles.size() == 0) {
            error("No .deb files found to upload")
        }

        debFiles.each {
            withCredentials([usernameColonPassword(credentialsId: 'repo.eclipse.org-bot-account', variable: 'USERPASS')]) {
                sh(
                    script: """
                        curl -u \"\$USERPASS\" \
                        -H \"Content-Type: multipart/form-data\" \
                        --data-binary \"@./${it}\" \
                        \"https://repo3.eclipse.org/repository/kura-apt-dev/\"
                    """,
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
