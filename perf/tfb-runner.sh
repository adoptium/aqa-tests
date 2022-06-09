test=$1
dockerfile=$2
TEST_RESROOT=$3

cd ${TEST_RESROOT}
testcase=` echo ${dockerfile} | cut -d'.' -f 1`
dockerfile=${TEST_RESROOT}/frameworks/Java/${test}/${dockerfile}

if [ ! -f ${dockerfile} ]; then
  echo "Warning: ${dockerfile} not found"
  exit 1
fi

cp -r ${TEST_JDK_HOME} ${TEST_RESROOT}/frameworks/Java/${test}/jdk
awk '/CMD.*/ && !x {print "COPY jdk /opt/java/openjdk"; x=1} 1' ${dockerfile} > tmpt && mv tmpt ${dockerfile}
awk '/CMD.*/ && !x {print "ENV JAVA_HOME=/opt/java/openjdk"; x=1} 1' ${dockerfile} > tmpt && mv tmpt ${dockerfile}
awk '/CMD.*/ && !x {print "ENV PATH=/opt/java/openjdk/bin:$PATH"; x=1} 1' ${dockerfile} > tmpt && mv tmpt ${dockerfile}
cat ${dockerfile}
./tfb --test $testcase

# get removable results
docker run --entrypoint /bin/bash  --rm --network tfb -v /var/run/docker.sock:/var/run/docker.sock -v ${TEST_RESROOT}:/FrameworkBenchmarks techempower/tfb -c "chmod -R 777 /FrameworkBenchmarks/results"