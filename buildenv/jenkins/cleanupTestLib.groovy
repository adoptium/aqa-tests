def dependencyDir = params.DEPENDENCY_DIR ?: 'externalDependency'

def platformMap = [
    'amac' : '(ci.role.test||ci.role.test.fips)&&hw.arch.aarch64&&(sw.os.osx||sw.os.mac)',
    'xmac' : '(ci.role.test||ci.role.test.fips)&&hw.arch.x86&&sw.os.mac',
    'xlinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.x86&&sw.os.linux',
    'plinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.ppc64le&&sw.os.linux',
    'zlinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.s390x&&sw.os.linux',
    'alinux' : '(ci.role.test||ci.role.test.fips)&&hw.arch.aarch64&&sw.os.linux',
    'win' : '(ci.role.test||ci.role.test.fips)&&hw.arch.x86&&sw.os.windows',
    'aix' : '(ci.role.test||ci.role.test.fips)&&hw.arch.ppc64&&sw.os.aix',
    'zos' : 'ci.role.test&&hw.arch.s390x&&sw.os.zos'
]

timeout(time: 72, unit: 'HOURS') {
    timestamps {
        def parallelJobs = [:]
        platformMap.each { platform, label ->
            echo "${platform}: ${label}"
            if (params.PLATFORMS == 'all' || params.PLATFORMS == platform) {
                parallelJobs[platform] = {
                    def cmd = ''
                    try {
                        timeout(time: 15, unit: 'MINUTES') {
                            node(label) {
                                echo "Running on node with label: ${label}"
                                def jenkinsHome = env.WORKSPACE.replace('\\', '/').replaceAll('/workspace/.*', '')
                                jenkinsHome = (platform == 'win') ? jenkinsHome.replaceAll('/', '//') + '/' : jenkinsHome
                                cmd = "hostname; if [ -d ${jenkinsHome}/${dependencyDir} ]; then ls -alt ${jenkinsHome}/${dependencyDir}/; rm -rf ${jenkinsHome}/${dependencyDir}/*; ls -alt ${jenkinsHome}/${dependencyDir}/; else echo 'Directory does not exist: ${jenkinsHome}/${dependencyDir}'; fi;"
                                def cmd_clean_maven_libs = "if [ -d ${jenkinsHome}/.m2 ]; then ls -alt ${jenkinsHome}/.m2/; rm -rf ${jenkinsHome}/.m2/*; ls -alt ${jenkinsHome}/.m2/; else echo 'Directory does not exist: ${jenkinsHome}/.m2/'; fi"
                                cmd += cmd_clean_maven_libs
                            }
                        }
                    } catch (Exception e) {
                        echo "No node with label '${label}' is available. Skipping..."
                        echo 'Exception: ' + e.toString()
                    }
                    if (cmd) {
                        build job: 'all-nodes-matching-labels', parameters: [
                            string(name: 'LABEL', value: label),
                            string(name: 'COMMAND', value: cmd),
                            string(name: 'TIMEOUT_TIME', value: '48')
                        ], propagate: true
                    }
                }
            }
        }
        parallel parallelJobs
    }
}
