#!groovy

timestamps {
    timeout (time: 20, unit: 'MINUTES') {
        node ("ci.role.test&&hw.arch.x86&&sw.os.linux&&sw.os.rhel") {
            cleanWs()
            stage('Clone ebc-gateway-http Repo') {
                git branch: "$params.EBC_BRANCH", credentialsId: "$params.GIT_CREDENTIALS_ID", url: "$params.EBC_REPO"
            }
            stage('Call ebc_complete.sh') {
                withCredentials([usernamePassword(credentialsId: "$params.EBC_CREDENTIALS_ID",
                        passwordVariable: "PASSWORD_VAR", usernameVariable: "USERNAME_VAR")]) {
                    sh(script: """
                        echo Run ./ebc_complete.sh to delete EBC node ...
                        export intranetId_USR=$USERNAME_VAR
                        export intranetId_PSW=$PASSWORD_VAR
                        export demandId=${params.demandId}
                        ./ebc_complete.sh
                    """)
                }
            }
        }
    }
}
