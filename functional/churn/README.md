# churn
Test targeting garbage collector(s)

## Running churn locally
clone https://github.com/rh-openjdk/churn read  https://github.com/rh-openjdk/churn/blob/master/README and run run.sh

### Setting up the environment:
Java required. Maven recommended, churn can be compiled via direct javac if needed.
Java is used from JAVA_HOME or guessed from PATH
run.sh can use OTOOL_garbageCollector and OTOOL_JDK_VERSION instead of gc argument. See https://github.com/rh-openjdk/churn/blob/master/README for details

### Executing the testsuite
Now it is as simple as running bash script run.sh.

