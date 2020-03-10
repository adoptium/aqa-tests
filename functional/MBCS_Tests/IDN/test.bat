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

SET CLASSPATH=%PWD%\IDN.jar
SET OUTPUT=output.txt
call %PWD%\check_env_windows.bat
if exist %OUTPUT% (del %OUTPUT%)
for %%i in (%PWD%\win_%LOCALE%_*_txt) do (
    %JAVA_BIN%\java IDNFromFile %%i
    type toAscii.txt >> %OUTPUT%
    type toUnicode.txt >> %OUTPUT%
)

call %PWD%\setup_%LOCALE%.bat
echo URL=http://%TEST_HOSTNAME% >> %OUTPUT%
echo converting URL from UNICODE to ACE... >> %OUTPUT%
FOR /F "usebackq" %%i IN (`%JAVA_BIN%\java IDNtoASCII -e TEST_HOSTNAME`) DO  set ToASCII=%%i
echo ACE=http://%ToASCII% >> %OUTPUT%

echo.
echo URL=http://%ToASCII% >> %OUTPUT%
echo converting URL from ACE to UNICODE... >> %OUTPUT%
FOR /F "usebackq" %%i IN (`%JAVA_BIN%\java IDNtoUNICODE %ToASCII%`) DO  set ToUNICODE=%%i
echo UNICODE=http://%ToUNICODE% >> %OUTPUT%

fc %PWD%\expected_windows_%LOCALE%.txt %OUTPUT% > fc.out 2>&1
exit %errorlevel%
