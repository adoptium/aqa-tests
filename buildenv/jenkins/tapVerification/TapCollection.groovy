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
                        text.split('\r\n').each { line ->
                            /* groovylint-disable-next-line LineLength */
                            if ((line.contains("https://github.com/${RELEASE_TRIAGE_REPO}/files/") || line.contains('https://github.com/user-attachments/files/')) && line.endsWith(')')) {
                                def url = line.substring(line.indexOf('(https') + 1, line.lastIndexOf(')'))
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
                        copyArtifacts (
                            filter: "AQAvitTaps/${tapTars}", 
                            fingerprintArtifacts: true, 
                            projectName: "${Release_PipelineJob_Name}",
                            selector: specific("${build}"),
                            target: "${build}/",
                            flatten: true
                        )
                        sh "tar -xzvf ${build}/${tapTars} -C ${tapsDir}"
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
