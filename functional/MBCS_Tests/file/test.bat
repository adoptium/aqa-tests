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
SET CLASSPATH=%PWD%\file.jar
SET SRC_DIR=file_%LOCALE%
if exist %SRC_DIR% (rd /S /Q %SRC_DIR%)
SET DATAFILE=%SRC_DIR%.zip

SET ZIP=C:\7-Zip\7z.exe
if exist %ZIP% goto TEST1
SET ZIP=7z.exe
where /Q %ZIP%
IF %ERRORLEVEL% NEQ 0 (  echo -----------------------------------
                         echo Please install 7-zip to c:\7-Zip\
                         echo -----------------------------------
                         goto END
)

:TEST1

%JAVA_BIN%\java MimeDecoder %PWD%\data\%DATAFILE%.base64 %PWD%\data\%DATAFILE%
%ZIP% x %PWD%\data\%DATAFILE% > NUL




if exist tmp\. rd /s/q tmp
md tmp


%JAVA_BIN%\java FileOperatorWin %SRC_DIR% tmp\%SRC_DIR% C > tmp\%OUTPUT% 2>&1
%JAVA_BIN%\java FileOperatorWin %SRC_DIR% tmp\%SRC_DIR% R >> tmp\%OUTPUT% 2>&1
%JAVA_BIN%\java FileOperatorWin %SRC_DIR% tmp\%SRC_DIR% D >> tmp\%OUTPUT% 2>&1
 

fc %PWD%\expected\windows_%LOCALE%.txt tmp\%OUTPUT% > fc.out 2>&1
:END
exit %errorlevel%
