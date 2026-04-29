pipeline {
    agent { label 'ci.role.test&&hw.arch.x86&&sw.os.linux' }
    parameters {
        string(name: 'RELEASE_TRIAGE_REPO', defaultValue: 'adoptium/aqa-tests', description: 'Triage repo')
        string(name: 'RELEASE_TRIAGE_ISSUE_NUMBER', defaultValue: '', description: 'Triage issue number')
        string(name: 'Release_PipelineJob_Name', defaultValue: '', description: 'Jenkins Pipeline job name')
        string(name: 'Release_PipelineJob_Numbers', defaultValue: '', description: 'Jenkins Pipeline job number, Comma separated numbers')
    }
    stages {
        stage('Tap Collection') { 
            steps {
                script {
                    def issueUrl = "https://api.github.com/repos/${RELEASE_TRIAGE_REPO}/issues/${RELEASE_TRIAGE_ISSUE_NUMBER}"
                    def tapsDir = 'TAPs'
                    sh """
                        if [ ! -d "${tapsDir}" ]; then
                            mkdir -p "${tapsDir}"
                        fi
                    """
                    def commentsUrl = "${issueUrl}/comments"
                    def issueFile = 'issue.json'
                    def commentsFile = 'comments.json'
                    withCredentials([string(credentialsId: "${params.GITHUB_CREDENTIAL}", variable: 'GITHUB_TOKEN')]) {
                        sh """
                            curl -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/vnd.github.v3-json" ${issueUrl} -o ${issueFile}
                            curl -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/vnd.github.v3-json" ${commentsUrl} -o ${commentsFile}
                            """
                    }

                    def issue = readJSON file: "${issueFile}"
                    def comments = readJSON file: "${commentsFile}"

                    // Function to download attachments from text
                    def downloadAttachments = { text ->
                        text.split(/\r?\n/).each { line ->
                            /* groovylint-disable-next-line LineLength */
                            if ((line.contains("https://github.com/${RELEASE_TRIAGE_REPO}/files/") || line.contains('https://github.com/user-attachments/files/')) && line.endsWith(')')) {
                                def url = line.substring(line.indexOf('(https://github.com/user-attachments/files/') + 1, line.lastIndexOf(')'))
                                def filename = url.split('/').last()
                                sh "curl -L -o ${tapsDir}/${filename} ${url}"
                            }
                        }
                    }
                    // Download attachments from issue description
                    downloadAttachments(issue.body)

                    // Download attachments from issue comments
                    comments.each { comment ->
                        downloadAttachments(comment.body)
                    }
                    //Download Taps from upsteam
                    def builds = "${Release_PipelineJob_Numbers}".split(',')
                    def tapTars = "AQAvitTapFiles.tar.gz"
                    builds.each { build ->
                        echo "build is ${build}"
                        // Try to copy artifacts from AQAvitTaps directory
                        def artifactExists = false
                        try {
                            copyArtifacts (
                                filter: "AQAvitTaps/${tapTars}",
                                fingerprintArtifacts: true,
                                projectName: "${Release_PipelineJob_Name}",
                                selector: specific("${build}"),
                                target: "${build}/",
                                flatten: true
                            )
                            // Check if the artifact file actually exists
                            artifactExists = sh(script: "test -f ${build}/${tapTars}", returnStatus: true) == 0
                            if (artifactExists) {
                                echo "Found ${tapTars} in AQAvitTaps directory for build ${build}"
                                sh "tar -xzvf ${build}/${tapTars} -C ${tapsDir}"
                            }
                        } catch (Exception e) {
                            echo "Failed to copy ${tapTars} from AQAvitTaps directory: ${e.message}"
                            artifactExists = false
                        }
                        
                        // If artifact doesn't exist in AQAvitTaps, search recursively in target directory
                        if (!artifactExists) {
                            echo "Artifact not found in AQAvitTaps for build ${build}, searching recursively in target directory..."
                            try {
                                // Copy all artifacts from the build
                                copyArtifacts (
                                    filter: "**/*",
                                    fingerprintArtifacts: true,
                                    projectName: "${Release_PipelineJob_Name}",
                                    selector: specific("${build}"),
                                    target: "${build}/",
                                    optional: true
                                )
                                
                                // Recursively find all .tap files in any AQAvitaps directories under target
                                def tapCount = sh(
                                    script: """
                                        if [ -d "${build}/target" ]; then
                                            # Find all AQAvitaps directories recursively under target
                                            find ${build}/target -type d -iname 'AQAvitaps' 2>/dev/null | while read aqadir; do
                                                echo "Searching in: \$aqadir"
                                                # Find all .tap files in each AQAvitaps directory
                                                find "\$aqadir" -type f -name '*.tap' 2>/dev/null | while read tapfile; do
                                                    echo "Found: \$tapfile"
                                                    cp "\$tapfile" ${tapsDir}/ 2>/dev/null && echo "Copied: \$(basename \$tapfile)"
                                                done
                                            done
                                            # Count total tap files copied
                                            ls -1 ${tapsDir}/*.tap 2>/dev/null | wc -l
                                        else
                                            echo "0"
                                        fi
                                    """,
                                    returnStdout: true
                                ).trim()
                                
                                if (tapCount.toInteger() > 0) {
                                    echo "Successfully collected ${tapCount} tap file(s) from target directory for build ${build}"
                                } else {
                                    echo "No tap files found in target directory for build ${build}"
                                }
                            } catch (Exception e) {
                                echo "Failed to search for tap files in target directory: ${e.message}"
                            }
                        }
                    }

                    sh """
                        cd ${tapsDir}/
                        tar -czf ${tapTars} *
                    """
                    archiveArtifacts artifacts: "${tapsDir}/${tapTars}", allowEmptyArchive: true
                }
            }
        }

        stage('Verification') {
            steps {
                echo "Taps verification jobs"
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
