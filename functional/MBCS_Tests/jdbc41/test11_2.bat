@echo off
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem      https://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

SETLOCAL
SET PWD=%~dp0
SET OUTPUT="output"
SET DERBY_JAR=%PWD%\derby\db-derby-10.14.2.0-lib\lib\derby.jar
SET CLASSPATH=%PWD%\jdbc41.jar;%DERBY_JAR%
if %JDK_VERSION% == 8 (
   SET JAVA_BIN=%TEST_JDK_HOME%/jre/bin
) else (
   SET JAVA_BIN=%TEST_JDK_HOME%/bin
)
%JAVA_BIN%\java Main %1 %2 %3 %4 %5

fc %PWD%\expected\win_%2.expected_11.txt %OUTPUT% > fc.out 2>&1
exit %errorlevel%

