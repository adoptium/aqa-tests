#!groovy

List<Map<String, Object>> json = []

node {
  checkout scm
  stage('Triggering python script')
  {
    sh "python ${WORKSPACE}/aqa-tests/disabledTestParser/generateDisabledTestListJson.py"
    sh "python ${WORKSPACE}/aqa-tests/disabledTestParser/issue_status.py"
  }
  stage('readJSON') {
    def json_path = "${WORKSPACE}/aqa-tests/disabledTestParser/output.json"
    json = readJSON(file: json_path) as List<Map<String, Object>>
  }
}

stage('Launch Grinder Jobs')
{
  launch_grinders(json)
}

def launch_grinders(List<Map<String, Object>> json) {
  /**
   Get the parameters specified as a string list.
   Get each of the key-value pairs for each json value.
   If the issue for a job is closed, and JDK_VERSION and 
   JDK_IMPL match the parameters specified, 
   run grinder on it. Otherwise ignore it
   */

  def jdk_ver = params.JDK_VERSION.split(',')
  def jdk_imp =  params.JDK_IMPL.split(',')

  json.eachWithIndex { dict, _ ->
    if (dict["ISSUE_TRACKER_STATUS"] == "closed" && jdk_ver.contains(dict["JDK_VERSION"].toString()) && jdk_imp.contains(dict["JDK_IMPL"])) {
      run_grinder(dict)
    }
  }
}

/**
 This runs when we have found a job w/ git_issue_status = closed.
 Collects all parameters and runs grinder
 */
def run_grinder(Map<String, Object> map) {
  def childParams = []

  map.each { k, v ->
    if (k != "ISSUE_TRACKER_STATUS" && k != "ISSUE_TRACKER") {
      childParams << string(name: "${k}", value: "${v}")
    }
  }

  for (e in childParams) {
    println e
  }

  def job = build job: "Grinder", parameters: childParams, propagate: true
}
