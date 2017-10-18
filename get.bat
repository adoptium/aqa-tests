:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::     http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.

set /A args_count=0
for %%x in (%*) do (
	set /A args_count+=1
)
set openjdktest=%CD%
if %args_count% == 1 set openjdktest=%1
if %args_count% == 2 (
	set openjdktest=%2
	echo Get binary openjdk...
	cd %1
	md openjdkbinary
	cd openjdkbinary
	wget https://github.com/AdoptOpenJDK/openjdk-releases/releases/download/jdk8u152-b04/OpenJDK8_x64_Win_jdk8u152-b04.zip
	unzip OpenJDK8_x64_Win_jdk8u152-b04.zip
)

cd %openjdktest%\TestConfig
md lib
cd lib
echo Get third party libs...
wget http://download.forge.ow2.org/asm/asm-5.0.1.jar
wget https://downloads.sourceforge.net/project/junit/junit/4.10/junit-4.10.jar
ren asm-5.0.1.jar asm-all-5.0.1.jar

md TestNG
cd TestNG
wget http://central.maven.org/maven2/org/testng/testng/6.10/testng-6.10.jar
wget http://central.maven.org/maven2/com/beust/jcommander/1.48/jcommander-1.48.jar
ren testng-6.10.jar testng.jar
ren jcommander-1.48.jar jcommander.jar

cd %openjdktest%\TestConfig\lib
echo Get jtreg...
wget https://ci.adoptopenjdk.net/job/jtreg/lastSuccessfulBuild/artifact/jtreg-4.2.0-tip.tar.gz
tar xf jtreg-4.2.0-tip.tar.gz

cd %openjdktest%\openjdk_regression
echo Get openjdk...
git clone -b dev https://github.com/AdoptOpenJDK/openjdk-jdk8u.git