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

SET CP=%PWD%\sealed_classes.jar
SET CP_junit=%PWD%\junit4.jar

%JAVA_BIN%\java -cp %CP% GenerateTestSource %TEST_STRINGS%
%JAVA_BIN%\javac -Xstdout compile_sbcs_err.out -cp %CP_junit% SealedClassSbcsTest.java
%JAVA_BIN%\java -cp %CP% FixFile compile_sbcs_err.out SealedClassSbcsTest SealedClassCETest %TEST_STRINGS%
%JAVA_BIN%\javac -Xstdout compile_err.out -cp %CP_junit% SealedClassCETest.java

fc compile_err.out expected_result.txt > fc.out 2>&1
if %errorlevel% neq 0 exit %errorlevel%

%JAVA_BIN%\javac -cp %CP_junit% SealedClassTest.java
%JAVA_BIN%\java -cp %CP_junit%; junit.textui.TestRunner SealedClassTest

exit %errorlevel%
