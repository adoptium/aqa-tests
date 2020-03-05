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

SET OUTPUT=output.txt
SET CLASSPATH=%PWD%\env.jar
call %PWD%\..\data\setup_%LOCALE%.bat
echo "invoking EnvTest"> %OUTPUT%
%JAVA_BIN%\java EnvTest >> %OUTPUT%

fc %PWD%\expected_windows_%LOCALE%.txt %OUTPUT% > fc.out 2>&1
exit %errorlevel%
