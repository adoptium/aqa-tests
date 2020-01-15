<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[1]https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
-->

# Prerequisites:

For a quick reference, you can see how most of the prereqs are setup on a Linux system in our test Dockerfiles, in [openjdk-tests/external/example-test/Dockerfile](https://github.com/AdoptOpenJDK/openjdk-tests/blob/master/external/example-test/dockerfile/Dockerfile#L33)

  * ant 1.7.1 or above with ant-contrib.jar
  * make 4.1 or above
  * perl 5.10.1 or above** 
  * curl 7.20.0 or above (needs -J/--remote-header-name support)
  
  * docker (needed if you wish to run Docker-based external test group)

Note for testing on Windows platform, cygwin is also required.  

At the AdoptOpenJDK project, Ansible scripts are used to setup test machines.  You can find them in the ansible directory of the [openjdk-infrastructure repo](https://github.com/AdoptOpenJDK/openjdk-infrastructure).