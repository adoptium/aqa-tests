# churn
Test targeting garbage collector(s).
The suite is able to operate with all GCs in OpenJDK. If your implementation is missing some, the calls to them will fall. If you have some additional one, an enabling wrapper must be created first.

## Running churn locally
clone https://github.com/rh-openjdk/churn read  https://github.com/rh-openjdk/churn/blob/master/README and run run.sh

### Setting up the environment:
Java required. Maven recommended, churn can be compiled via direct javac if needed
Java is used from JAVA_HOME or guessed from PATH.
run.sh can use OTOOL_garbageCollector and OTOOL_JDK_VERSION instead of GC argument. See https://github.com/rh-openjdk/churn/blob/master/README for details.
See https://github.com/rh-openjdk/churn/tree/master/bin for list of supported GCs. The compressed ops in upstream run can be controlled by runner or NOCOMP=-nocoops variable (for run.sh)

## Running via AQAvit
The support for compressed ops is handled by AQAvit itself. It is currently not sure if churn will be able to honor it. If not, churn will be fixed.
Similarly the java version and JAVA_HOME are handled by AQAvit.

### Executing the testsuite
The `BUILD_LIST of system/churn` contains following targets:
 * _churn_5h_allGCs
 * _churn_custom

There are major differences in them:
 * churn_5h_allGCs - Is running each GC a bit over, which runs each GC for aprox hour and half. A minimum, which can find some real GC issue.
 * churn_custom - this one is to support development, when run on the commandline you need to export at least DURATION and/or OTOOL_garbageCollector (+ many more optional, see [upstream readme](https://github.com/rh-openjdk/churn/blob/master/README)  to select DURATION in seconds and GC(or GCs). So it allows you to test your custom GC - if churn supports that, despite what other churn options suggest (eg `default` or `ALL` thinks).  When running in a Jenkins Grinder job, those are wrapped in CHURN_GCS and CHURN_DURATION

churn_5h_allGCs is using pony `ALL` keyword, which is interpreted (based on hardcoded list) as all GC in tested JVM. The set time is divided among them.
Note, that if you use `churn_custom` and enumeration, set `CHURN_GCS="ALL"` anyway,  then the time will not be divided. The `CHURN_DURATION` is in seconds. If you use set, off GCs, eg `CHURN_GCS="zgc shenandoah"`, each willt ake the full CHURN_DURATION.
The `CHURN_GCS="defaultgc"`will set the tested GC to default GC as run.sh think is right. So be aware. Although it is maintained, if your custom JDK have custom GC, it is unlikely to be known. If it have different default gc, it will correctly fail.

Where the variables should work for you (and have preference), the churn_custom is accepting  GCs and duration via `$(APPLICATION_OPTIONS)`. So from Grinder, you may use APPLICATION_OPTIONS field and set it as `"zgc shenandoah" 50000` and it will be used.

### Reading results
tap file and compressed junit xmlfile are generated. Use eg https://github.com/jenkinsci/report-jtreg-plugin or https://plugins.jenkins.io/tap/ to read them.

