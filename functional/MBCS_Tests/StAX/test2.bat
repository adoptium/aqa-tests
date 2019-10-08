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
if %JDK_VERSION% == 8 (
   SET JAVA_BIN=%TEST_JDK_HOME%/jre/bin
) else (
   SET JAVA_BIN=%TEST_JDK_HOME%/bin
)

%JAVA_BIN%\java -cp %PWD%\StAX.jar Main %PWD%\data\drinks_%2.xml %PWD%\data\drinks_%2.xml %1 %2 %3 %4 %5

SET RESULT=0
fc %PWD%expected\Windows_%2\read_cursor.html read_cursor.html > fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\Windows_%2\read_event.html read_event.html >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\Windows_%2\write_cursor.xml write_cursor.xml >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\Windows_%2\write_event.xml write_event.xml >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

exit %RESULT%
