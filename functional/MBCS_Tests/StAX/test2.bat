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
call %PWD%\set_variable.bat
SET RESULT=0
SET DATAFILE=drinks_%2.xml
SET EXPECTEDFOLDER=windows_%2
if %3 == cn (
   SET DATAFILE=drinks_%2-%3.xml
   SET EXPECTEDFOLDER=windows_%2-%3
)
if %3 == tw (
   call %PWD%\data2\setup_zh-tw.bat
   SET DATAFILE=drinks_%2-%3.xml
   SET EXPECTEDFOLDER=windows_%2-%3
)

%JAVA_BIN%\java -cp %PWD%\StAX.jar Main %PWD%\data\%DATAFILE% %PWD%\data\%DATAFILE% %1 %2 %3 %4 %5
fc %PWD%expected\%EXPECTEDFOLDER%\read_cursor.html read_cursor.html > fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\%EXPECTEDFOLDER%\read_event.html read_event.html >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\%EXPECTEDFOLDER%\write_cursor.xml write_cursor.xml >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\%EXPECTEDFOLDER%\write_event.xml write_event.xml >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

exit %RESULT%
