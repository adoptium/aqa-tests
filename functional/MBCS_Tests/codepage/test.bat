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
FOR /F "usebackq" %%i IN (`cscript //NOLOGO %PWD%\locale.vbs`) DO SET LOCALE=%%i
SET CLASSPATH=%PWD%\codepage.jar
SET STATUS=UKNOWN
if %LOCALE% == ja SET STATUS=OK
if %LOCALE% == ko SET STATUS=OK
if not %STATUS% == OK (
    echo SKIPPED!  This testcase is designed for Japanese or Korean Windows environment. 
    exit 0
)
call %PWD%\set_%LOCALE%.bat
call %PWD%\..\data\setup_%LOCALE%.bat
%JAVA_BIN%\java conv %PWD%\WIN_%CHARMAP%.txt %ENCODING% tmp.txt %ENCODING% > \nul 2>&1
fc %PWD%\WIN_%CHARMAP%.txt tmp.txt > fc.out 2>&1
exit %errorlevel%
