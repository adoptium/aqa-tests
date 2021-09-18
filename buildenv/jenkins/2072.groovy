#!groovy
String json_path = "../2072_test.json"
File json = new File(json_path)
def get_key_value_regex = ~/"(?<key>[^"]*)"\s*:\s*"(?<value>[^"]*)"/
def key, value
def should_run_grinder = false // Assume that the job should not be run again
def map = [:]
def run_grinder(map) {
    map.each { k, v -> 
        println k + " " + v
    }
}
/**
 Get each of the key-value pairs for each json value.
 If we have reached the end of a job and
 the issue is closed, run grinder on it.
 Otherwise ignore it */
json.eachLine { line ->
    // End of job
    if (line.contains("}")) {
        if (should_run_grinder) {
            run_grinder(map)
        }
        should_run_grinder = false
        map = [:]
    } else {
        match = line =~ get_key_value_regex
        if (match) {
            key = match.group("key")
            value = match.group("value")
            if (key == "GIT_ISSUE_STATUS" && value == "closed") {
                should_run_grinder = true
            }
            map[key] = value
        }
    }
}