#!groovy

def json = readJSON file: "disabledTestParser/output.json."
def jenkins_url = "https://ci.adoptopenjdk.net/job/Grinder/buildWithParameters"

/**
 This runs when we have found a job w/ git_issue_status = closed.
 Collects all parameters and runs grinder
 */
def run_grinder(map) {
  def list = []
  def queryString

  map.each { k, v ->
    list.add(k + "=" + v)
  }

  queryString = list.join("&")
  url = jenkins_url + "/?" + queryString

  // The Grinder trigger will look something like this,
  // but it requires tokens which I do not have yet.
  // def url = new URL(jenkins_url + "/?" + queryString)
  // def connection = url.openConnection()
  // connection.with {
  //     doOutput = true
  //     requestMethod = 'POST'
  //     outputStream.withWriter { writer ->
  //         writer << queryString
  //     }
  //     println content.text
  // }

  println url
}

/**
 Get each of the key-value pairs for each json value.
 If the issue for a job is closed, run grinder on it.
 Otherwise ignore it */
json.eachWithIndex { dict, _ ->
  if (dict["GIT_ISSUE_STATUS"] == "closed") {
    run_grinder(dict)
  }
}
