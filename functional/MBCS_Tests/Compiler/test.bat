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
SET CLASSPATH=%PWD%\Compiler.jar;.
SET OUTPUT=%PWD%\output.txt
SET STATUS=UKNOWN
if %LOCALE% == ja SET STATUS=OK
if %LOCALE% == ko SET STATUS=OK
if not %STATUS% == OK (
    echo SKIPPED!  This testcase is designed for Japanese or Korean Windows environment. 
    exit 0
)


call %PWD%\..\data\setup_%LOCALE%.bat
FOR /F "usebackq" %%i IN (`%JAVA_BIN%\java CheckValidDataEnv TEST_STRINGS`) DO  set TS=%%i

cscript //nologo %PWD%\ChgWord.vbs TEST_STRING %TS% %PWD%\class_org.java > %TS%.java
%JAVA_BIN%\java CompilerTest1 %TS%.java > %OUTPUT% 2>&1
fc %PWD%\expected_Windows_%LOCALE%.txt %OUTPUT% > fc.out 2>&1
exit %errorlevel%
