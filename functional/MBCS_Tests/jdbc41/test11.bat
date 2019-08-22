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

SET OUTPUT=output.txt
call %PWD%\check_env_windows.bat

call %PWD%\..\data\setup_%LOCALE%.bat
call %PWD%\data\setjdbc41_%LOCALE%.bat
SET DERBY_JAR=%PWD%\derby\db-derby-10.14.2.0-lib\lib\derby.jar
SET CLASSPATH=%PWD%\jdbc41.jar;%DERBY_JAR%
echo LOCALE = %LOCALE% > %OUTPUT%
echo JDBC41_TABLE_NAME = %JDBC41_TABLE_NAME% >> %OUTPUT%
echo JDBC41_CNAME = %JDBC41_CNAME% >> %OUTPUT%
echo --- Create Table and Insert test data in JavaDB. >> %OUTPUT%
%JAVA_BIN%\java jdbc41autoclose %TEST_STRINGS%  >> %OUTPUT% 2>&1
REM %JAVA_BIN%\java jdbc41RowSetProvider  >> %OUTPUT% 2>&1
echo --- Drop table in JavaDB. >> %OUTPUT%
%JAVA_BIN%\java jdbc41droptb  >> %OUTPUT% 2>&1

fc %PWD%\expected\win_%LOCALE%.expected_11.txt %OUTPUT% > fc.out 2>&1
exit %errorlevel%

