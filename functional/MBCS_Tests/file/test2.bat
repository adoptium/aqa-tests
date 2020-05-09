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

SET OUTPUT=output.txt
SET CLASSPATH=%PWD%\file.jar
SET SRC_DIR=file_%2
SET DATAFILE=file_%2.jar
SET EXPECTEDFILE=windows_%2.txt
if %3 == cn ( 
   SET SRC_DIR=file_%2-cn
   SET DATAFILE=file_%2-cn.jar
   SET EXPECTEDFILE=windows_%2-%3.txt 
)
if %3 == tw ( 
   SET SRC_DIR=file_%2-tw
   SET DATAFILE=file_%2-tw.jar
   SET EXPECTEDFILE=windows_%2-%3.txt 
)

%JAVA_BIN%\java MimeDecoder %PWD%\data\%DATAFILE%.base64 %PWD%\data\%DATAFILE%

if exist %JAVA_BIN%\jar.exe (
    %JAVA_BIN%\jar -xf %PWD%\data\%DATAFILE% > NUL
) else (
    %JAVA_BIN%\..\..\bin\jar -xf %PWD%\data\%DATAFILE% > NUL
)

if exist tmp\. rd /s/q tmp
md tmp

%JAVA_BIN%\java Main %SRC_DIR% tmp\%SRC_DIR% "MODE" %2 %3 %4

fc %PWD%\expected\%EXPECTEDFILE% output > fc.out 2>&1
exit %errorlevel%
