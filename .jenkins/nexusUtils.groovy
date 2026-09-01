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

    stage("Upload .deb packages to Nexus") {
        def debFiles = findFiles(glob: 'distrib/**/*.deb')

        if (debFiles.size() == 0) {
            error("No .deb files found to upload")
        }

        debFiles.each {
            withCredentials([usernameColonPassword(credentialsId: 'repo.eclipse.org-bot-account', variable: 'USERPASS')]) {
                def status = sh(
                    script: """
                        curl -u \"\$USERPASS\" \
                        -w '%{http_code}' \
                        -H \"Content-Type: multipart/form-data\" \
                        --data-binary \"@./${it}\" \
                        \"https://repo.eclipse.org/repository/kura-apt-dev/\" \
                        -o /dev/null
                    """,
                    returnStdout: true
                ).trim()

                if (status != "201") {
                    error("Returned status code = $status")
                }
            }
        }

        if (setupPromotion) {
            // TODO
        }

    }
}

return this
