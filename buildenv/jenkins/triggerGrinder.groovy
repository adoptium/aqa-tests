#!groovy
node {
   checkout scm
  //  : [$class: 'GitSCM',
  //                           branches: [[name: "${scm.branches[0].name}"]],
  //                           extensions: [
  //                               [$class: 'CleanBeforeCheckout'],
  //                               [$class: 'CloneOption', reference: ref_cache],
  //                               [$class: 'RelativeTargetDirectory', relativeTargetDir: 'aqa-tests']],
  //                           userRemoteConfigs: [[url: "${gitConfig.getUrl()}"]]
                        // ]
  stage('readJSON')
  {
    def json_path = "${WORKSPACE}/aqa-tests/disabledTestParser/output.json"
    def json = readJSON file: json_path
    /**
    Get each of the key-value pairs for each json value.
    If the issue for a job is closed, run grinder on it.
    Otherwise ignore it */
    json.eachWithIndex { dict, _ ->
      if (dict["GIT_ISSUE_STATUS"] == "closed") {
        run_grinder(dict)
      }
    }
  }
}
/**
  This runs when we have found a job w/ git_issue_status = closed.
  Collects all parameters and runs grinder
  */
def run_grinder(map) {
  stage("running grinder")
  {
    def childParams = []
    def queryString

    map.each { k, v ->
      if (k!="ISSUE_TRACKER_STATUS" && k!="ISSUE_TRACKER") {
          childParams << string(name: "${k}", value: "${v}")
      }
    }

    for (e in childparams) {
      println e
    }

    build job: Grinder, parameters: childParams, propagate: false
  }
}
