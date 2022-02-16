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
IF "%TEST_STRINGS_SED%" == "\=\\" SET TEST_STRINGS_SED=%TEST_STRINGS:\=\\%
IF "%TEST_STRINGS_SED%" == "" SET TEST_STRINGS_SED=%TEST_STRINGS:\=\\%

SET CLASSPATH=%PWD%\coin.jar
%JAVA_BIN%\java %JAVA_OPTIONS% SwitchTest > SwitchTest.code
SET CLASSPATH=
%JAVA_BIN%\java %JAVA_OPTIONS% SwitchTestCode > SwitchTest.log 2>&1

SET CLASSPATH=%PWD%\coin.jar
%JAVA_BIN%\java %JAVA_OPTIONS% BinaryIntegralTest > BinaryIntegralTest.log 2>&1

%JAVA_BIN%\java %JAVA_OPTIONS% ExceptionTest > ExceptionTest.code
SET CLASSPATH=
%JAVA_BIN%\java %JAVA_OPTIONS% ExceptionTestCode > ExceptionTest.log 2>&1

SET CLASSPATH=%PWD%\coin.jar
%JAVA_BIN%\java %JAVA_OPTIONS% DiamondTest > DiamondTest.code
SET CLASSPATH=
%JAVA_BIN%\java %JAVA_OPTIONS% DiamondTestCode > DiamondTest.log 2>&1

SET CLASSPATH=%PWD%\coin.jar
%JAVA_BIN%\java %JAVA_OPTIONS% TryWithResourcesTest > TryWithResourcesTest.code
SET CLASSPATH=
%JAVA_BIN%\java %JAVA_OPTIONS% TryWithResourcesTestCode > TryWithResourcesTest.log 2>&1

SET CLASSPATH=%PWD%\coin.jar
%JAVA_BIN%\java %JAVA_OPTIONS% SimplifiedVarargsTest > SimplifiedVarargsTest.code
SET CLASSPATH=
%JAVA_BIN%\java %JAVA_OPTIONS% SimplifiedVarargsTestCode > SimplifiedVarargsTest.log

C:\Strawberry\perl\bin\perl %PWD%\resultCheck.pl
exit %errorlevel%

