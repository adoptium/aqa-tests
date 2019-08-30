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

call %PWD%\check_env_windows.bat
call %PWD%\..\data\setup_%LOCALE%.bat

if exist write_cursor.xml del write_cursor.xml
if exist write_event.xml del write_event.xml

%JAVA_BIN%\java -cp %PWD%\StAX.jar StAXReadCursor %PWD%data\drinks_%LOCALE%.xml >read_cursor.html 2>&1
%JAVA_BIN%\java -cp %PWD%\StAX.jar StAXReadEveIter %PWD%data\drinks_%LOCALE%.xml > read_event.html 2>&1
%JAVA_BIN%\java -cp %PWD%\StAX.jar StAXWriteCursor %TEST_STRINGS% > log 2>&1
%JAVA_BIN%\java -cp %PWD%\StAX.jar StAXWriteEveIter %TEST_STRINGS% >> log 2>&1

SET RESULT=0
fc %PWD%expected\Windows_%LOCALE%\read_cursor.html read_cursor.html > fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\Windows_%LOCALE%\read_event.html read_event.html >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\Windows_%LOCALE%\write_cursor.xml write_cursor.xml >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

fc %PWD%expected\Windows_%LOCALE%\write_event.xml write_event.xml >> fc.out 2>&1
if %errorlevel% neq 0 SET RESULT=1

exit %RESULT%
