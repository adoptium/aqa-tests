#!groovy

List<Map<String, Object>> json = []

node (params.LABEL) {
  checkout scm
  stage('Triggering python script')
  {
    sh """ls -1dq ${WORKSPACE}/aqa-tests/openjdk/excludes/* | 
    python3 ${WORKSPACE}/aqa-tests/scripts/disabled_tests/exclude_parser.py > ${WORKSPACE}/aqa-tests/scripts/disabled_tests/problem_list.json"""
    trigger_issue_status()
  }
  stage('readJSON') {
    def json_path = "${WORKSPACE}/aqa-tests/scripts/disabled_tests/output.json"
    json = readJSON(file: json_path) as List<Map<String, Object>>
  }
}

stage('Launch Grinder Jobs')
{
  launch_grinders(json)
}

/**
  Get the parameters specified as a string list.
  Get each of the key-value pairs for each json value.
  If the issue for a job is closed, and JDK_VERSION and 
  JDK_IMPL match the parameters specified, 
  run grinder on it. Otherwise ignore it
  */
def launch_grinders(List<Map<String, Object>> json) {

  def jdk_ver = params.JDK_VERSION.split(',')
  def jdk_imp =  params.JDK_IMPL.split(',')

  json.eachWithIndex { dict, _ ->
    if (dict["ISSUE_TRACKER_STATUS"] == "closed" && jdk_ver.contains(dict["JDK_VERSION"].toString()) && jdk_imp.contains(dict["JDK_IMPL"])) {
      run_grinder(dict)
    }
  }
}

/**
  Consumes git token and triggers issue_tracker 
  Generates output.json
  */
def trigger_issue_status() {

  if (params.AQA_ISSUE_TRACKER_CREDENTIAL_ID) {
    withCredentials([string(credentialsId: "${params.AQA_ISSUE_TRACKER_CREDENTIAL_ID}", variable: 'TOKEN')]) {
        sh """export AQA_ISSUE_TRACKER_GITHUB_USER=eclipse_aqavit
        export AQA_ISSUE_TRACKER_GITHUB_TOKEN=${TOKEN}
        python3 ${WORKSPACE}/aqa-tests/scripts/disabled_tests/issue_status.py --infile problem_list.json > ${WORKSPACE}/aqa-tests/scripts/disabled_tests/output.json"""
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