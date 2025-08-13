def dependencyDir = params.DEPENDENCY_DIR ?: 'externalDependency'

def platformMap = [
    'amac' : '(ci.role.test||ci.role.test.fips)&&hw.arch.aarch64&&(sw.os.osx||sw.os.mac)',
    'xmac' : '(ci.role.test||ci.role.test.fips)&&hw.arch.x86&&sw.os.mac',
    'xlinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.x86&&sw.os.linux',
    'plinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.ppc64le&&sw.os.linux',
    'zlinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.s390x&&sw.os.linux',
    'alinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.aarch64&&sw.os.linux',
    'win' : '(ci.role.test||ci.role.test.fips)&&hw.arch.x86&&sw.os.windows',
    'aix' : '(ci.role.test||ci.role.test.fips)&&hw.arch.ppc64&&sw.os.aix'
]

timeout(time: 72, unit: 'HOURS') {
    timestamps {
        def parallelJobs = [:]

        platformMap.each { platform, label ->
            echo "${platform}: ${label}"
            if (platforms == 'all' || platforms == platform) {
                if (params.PLATFORMS == 'all' || params.PLATFORMS == platform) {
                    def cmd = "hostname; if [ -d /home/jenkins/${dependencyDir} ]; then ls -alt /home/jenkins/${dependencyDir}/; rm -rf /home/jenkins/${dependencyDir}/*; ls -alt /home/jenkins/${dependencyDir}/; else echo 'Directory does not exist: /home/jenkins/${dependencyDir}'; fi;"
                    def cmd_clean_maven_libs = "if [ -d /home/jenkins/.m2 ]; then ls -alt /home/jenkins/.m2/; rm -rf /home/jenkins/.m2/*; ls -alt /home/jenkins/.m2/; else echo 'Directory does not exist: /home/jenkins/.m2/'; fi"
                    if (platform == 'win') {
                        cmd = "hostname; if [ -d C://Users//jenkins//${dependencyDir} ]; then ls -alt C://Users//jenkins//${dependencyDir}//;rm -rf C://Users//jenkins//${dependencyDir}//*;ls -alt C://Users//jenkins//${dependencyDir}//; else echo 'Directory does not exist: C://Users//jenkins//${dependencyDir}'; fi;"
                        cmd_clean_maven_libs = "if [ -d C://Users//jenkins//.m2 ]; then ls -alt C://Users//jenkins//.m2/; rm -rf C://Users//jenkins//.m2/*; ls -alt C://Users//jenkins//.m2/; else echo 'Directory does not exist: C://Users//jenkins//.m2'; fi"
                    } else if (platform == 'amac' || platform == 'xmac') {
                        cmd = "hostname; if [ -d /Users/jenkins/${dependencyDir} ]; then ls -alt /Users/jenkins/${dependencyDir}/; rm -rf /Users/jenkins/${dependencyDir}/*; ls -alt /Users/jenkins/${dependencyDir}/; else echo 'Directory does not exist: /Users/jenkins/${dependencyDir}/'; fi;"
                        cmd_clean_maven_libs = "if [ -d /Users/jenkins/.m2 ]; then ls -alt /Users/jenkins/.m2/; rm -rf /Users/jenkins/.m2/*; ls -alt /Users/jenkins/.m2/; else echo 'Directory does not exist: /Users/jenkins/.m2'; fi"
                    }
                    cmd += cmd_clean_maven_libs

                    parallelJobs[platform] = {
                        stage("Build on ${platform}") {
                            echo "Running job for platform: ${platform} with label: ${label}"
                            build job: 'all-nodes-matching-labels', parameters: [
                            string(name: 'LABEL', value: label),
                            string(name: 'COMMAND', value: cmd),
                            string(name: 'TIMEOUT_TIME', value: '24')
                        ], propagate: false
                        }
                    }
                }
            }
            parallel parallelJobs
        }
    }
}
