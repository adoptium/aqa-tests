#!groovy

def AQA_REPO = params.AQA_REPO ?: "adoptium"
def AQA_BRANCH = params.AQA_BRANCH ?: "master"
def JDK_VERSIONS = params.JDK_VERSIONS.trim().split("\\s*,\\s*");
def PLATFORMS = params.PLATFORMS.trim().split("\\s*,\\s*");
def TARGETS = params.TARGETS.trim().split("\\s*,\\s*");
def LABEL = (params.LABEL) ?: ""

// Use BUILD_USER_ID if set and jdk-JDK_VERSIONS
def DEFAULT_SUFFIX = (env.BUILD_USER_ID) ? "${env.BUILD_USER_ID} - jdk-${params.JDK_VERSIONS}" : "jdk-${params.JDK_VERSIONS}"
def PIPELINE_DISPLAY_NAME = (params.PIPELINE_DISPLAY_NAME) ? "#${currentBuild.number} - ${params.PIPELINE_DISPLAY_NAME}" : "#${currentBuild.number} - ${DEFAULT_SUFFIX}"

timestamps{
    // Set the AQA_TEST_PIPELINE Jenkins job displayName
    currentBuild.setDisplayName(PIPELINE_DISPLAY_NAME)
    node(LABEL) {
        cleanWs disableDeferredWipeout: true, deleteDirs: true
        TIME_LIMIT =  params.TIME_LIMIT ? params.TIME_LIMIT.toInteger() : 1
        timeout(time: TIME_LIMIT, unit: 'HOURS') {
            // change openjdk-tests to aqa-tests
            sh "curl -Os https://raw.githubusercontent.com/${AQA_REPO}/aqa-tests/${AQA_BRANCH}/buildenv/jenkins/tapVerification/aqaTap.sh"
            sh "chmod 755 aqaTap.sh"
            JDK_VERSIONS.each { JDK_VERSION ->
                PLATFORMS.each { PLATFORM ->
                    String[] tokens = PLATFORM.split('_')
                    def os = tokens[1];
                    def arch = tokens[0];
                    if (arch.contains("x86-64")){
                        arch = "x64"
                    } else if (arch.contains("x86-32")) {
                        arch ="x86-32"
                    }

                    def filter = "*.tar.gz"
                    if (os.contains("windows")) {
                        filter = "*.zip"
                    }
                    def short_name = "hs"
                    def jdk_impl = "hotspot"
                    if (params.VARIANT == "openj9") {
                        short_name = "j9"
                        jdk_impl = params.VARIANT
                    }
                    def download_url = params.CUSTOMIZED_SDK_URL ? params.CUSTOMIZED_SDK_URL : ""
                    def sdk_resource_value = SDK_RESOURCE

                    if (SDK_RESOURCE == "customized" ) {
                        if (params.TOP_LEVEL_SDK_URL) {
                            // Use release structure with TAP files at root
                            // example: <jenkins_url>/job/AQA_Test_Pipeline_Release/473/artifact/*zip*/archive.zip
                            download_url = params.TOP_LEVEL_SDK_URL + "artifact/*zip*/archive.zip"

                            dir("${WORKSPACE}") {
                                env.PLATFORM = PLATFORM
                                def PLATFORM_DIR = params.PLATFORM_DIR ? "${params.PLATFORM_DIR}" : "${PLATFORM}"

                                echo "Using release structure with TAP files at root"
                                echo "Filtering for targets: ${TARGETS.join(', ')}"

                                if (params.CUSTOMIZED_SDK_URL_CREDENTIAL_ID) {
                                    withCredentials([usernamePassword(credentialsId: "${params.CUSTOMIZED_SDK_URL_CREDENTIAL_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                        sh "curl -u \$USERNAME:\$PASSWORD -L -o archive.zip '${download_url}'"
                                    }
                                } else {
                                    sh "curl -L -o archive.zip '${download_url}'"
                                }

                                sh "unzip -q archive.zip -d temp_extract || true"
                                sh "mkdir -p AQAvitTapFiles/${PLATFORM_DIR}"

                                // Build target filter patterns for find command
                                def targetPatterns = TARGETS.collect { target ->
                                    "-name '*_${target}_${PLATFORM}.tap' -o -name '*_${target}_${PLATFORM}_*.tap'"
                                }.join(' -o ')

                                // Find and copy only TAP files matching the specified targets and platform
                                sh """
                                    find temp_extract \\( ${targetPatterns} \\) | while read file; do
                                        if [ -f "\$file" ]; then
                                            cp "\$file" "AQAvitTapFiles/${PLATFORM_DIR}/" 2>/dev/null || true
                                            echo "Copied: \$(basename \$file)"
                                        fi
                                    done
                                """
                                sh "rm -rf temp_extract archive.zip"
                            }
                        }
                    }
                }
            }

            // archive
            sh "tar -cf - ./AQAvitTapFiles | (pigz -9 2>/dev/null || gzip -9) > AQAvitTapFiles.tar.gz"
            archiveArtifacts artifacts: "AQAvitTapFiles.tar.gz", fingerprint: true, allowEmptyArchive: false
        }
    }
}
