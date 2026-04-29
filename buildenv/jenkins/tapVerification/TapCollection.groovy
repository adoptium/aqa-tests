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
                        
                        // If artifact doesn't exist in AQAvitTaps, directly copy tap files from target directory
                        if (!artifactExists) {
                            echo "Artifact not found in AQAvitTaps for build ${build}, copying tap files from target/**/temurin/AQAvitTaps/..."
                            try {
                                // Directly copy only .tap files using the known path pattern
                                // ** matches any number of directory levels
                                copyArtifacts (
                                    filter: "target/**/temurin/AQAvitTaps/*.tap",
                                    fingerprintArtifacts: true,
                                    projectName: "${Release_PipelineJob_Name}",
                                    selector: specific("${build}"),
                                    target: "${build}/",
                                    flatten: true,
                                    optional: true
                                )
                                
                                // Move the tap files to tapsDir
                                def tapCount = sh(
                                    script: """
                                        count=0
                                        if [ -d "${build}" ]; then
                                            find ${build} -maxdepth 1 -name '*.tap' -type f 2>/dev/null | while read tapfile; do
                                                if [ -f "\$tapfile" ]; then
                                                    mv "\$tapfile" ${tapsDir}/
                                                    count=\$((count + 1))
                                                    echo "Moved: \$(basename \$tapfile)"
                                                fi
                                            done
                                        fi
                                        ls -1 ${tapsDir}/*.tap 2>/dev/null | wc -l
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
