#!groovy
stage('read file and run grinder')
{
  steps {
  def json_path = "disabledTestParser/output.json"
  def json = readJSON file: json_path
  }

/**
  This runs when we have found a job w/ git_issue_status = closed.
  Collects all parameters and runs grinder
  */
def run_grinder(map) {
  stage("run_grinder")
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

/**
Get each of the key-value pairs for each json value.
If the issue for a job is closed, run grinder on it.
Otherwise ignore it */
json.eachWithIndex { dict, _ ->
  if (dict["GIT_ISSUE_STATUS"] == "closed") {
    run_grinder(dict)
  }