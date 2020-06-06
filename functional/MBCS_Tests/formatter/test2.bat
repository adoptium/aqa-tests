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
SET CLASSPATH=%PWD%\formatter.jar
call %PWD%\set_variable.bat
SET EXPECTEDFILE=expected_windows_%2.txt

if %3 == jp ( SET CITY=Tokyo )
if %3 == kr ( SET CITY=Seoul )
if %3 == cn ( SET CITY=Shanghai
              SET EXPECTEDFILE=expected_windows_%2-%3.txt )
if %3 == tw ( SET CITY=Taipei
              SET EXPECTEDFILE=expected_windows_%2-%3.txt )

%JAVA_BIN%\java -Duser.timezone=Asia/%CITY% Main %1 %2 %3 %4 %5

fc %PWD%\%EXPECTEDFILE% output > fc.out 2>&1
exit %errorlevel%
