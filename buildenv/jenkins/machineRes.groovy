timestamps {
    timeout(time: 12, unit: 'HOURS') {
         if (!params.NODE || !params.RESERVATION_LENGTH) {
            error "Please provide Node and RESERVATION_LENGTH"
        }
        def matchingNodes = jenkins.model.Jenkins.instance.getLabel(NODE).getNodes()
        if (matchingNodes.size() == 1 && matchingNodes[0].getDisplayName().contains("${NODE}")) {
            echo "NODE value passed is a specific machine"
            // Get Node's labels
            def nodeLabels = matchingNodes[0].getLabelString().tokenize(' ')
            if (!nodeLabels.contains("ci.role.test")) {
                error "Cannot reserve a machine that does not contain ci.role.test label. Please select a different machine."
            }
        } else {
            echo "NODE value passed is a set of label(s)"
            // Only allow reserving machines with ci.role.test
            NODE += "&&ci.role.test"
        }
        node (NODE) {
            try {
                CURRENT_TIME = sh (returnStdout: true, script: 'date').trim()
                END_TIME = "+${RESERVATION_LENGTH} minutes"
                if (!NODE_LABELS.contains('osx')) {
                    END_TIME = sh (returnStdout: true, script: "date -d 'now + ${RESERVATION_LENGTH} minutes'").trim()
                }
 
                echo "You have reserved: ${NODE_NAME} ${JENKINS_URL}computer/${NODE_NAME}"
                echo "It is now ${CURRENT_TIME}. Your session will end at: ${END_TIME}"
                if (NODE_LABELS.contains('osx')) {
                    RESERVATION_LENGTH = (RESERVATION_LENGTH.toInteger() * 60).toString()
                }
                sh "sleep ${RESERVATION_LENGTH}m"
            } finally {
                echo "Your session will be ended in 60s"
                // 1 minute buffer
                sh 'sleep 60'
            }
        }
    }
}