## Triage checklist

You are investigating a test failure.  

### What tools and information is available to you?
#### From the Jenkins test job:
- Console output - To retrieve the grinder rerun link and the link to SDK under test (from artifactory, Jenkins, custom or API link)
- Test artifacts - To retrieve the test output files and any trace/dmp/core files if the test crashes.

### What actions to best triage the failure?

- Check for any obvious or known explanation of the problem.  This is easier the more 'in tune' you are with issues lists at various repos.  

If there is no obvious cause, you next need to find out if the failure happens on different:
- implementations (does it fail against both hotspot and openj9?)
- versions (does it fail across versions, jdk8 and jdk11?  not all tests are common across versions, but if they are this is useful)
- platforms (does it fail on a single platform, or across many? all?)
- machines (does it fail only on certain machines of a same platform, can you narrow the failure down to a particular set of machines?)



- grab links and artifacts from build
