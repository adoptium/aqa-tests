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
SET OUTPUT=output.txt
SET CLASSPATH=%PWD%\jaxp14_11.jar
SET STATUS=UKNOWN
if %LOCALE% == ja (
SET STATUS=OK
SET XMLFILE=drinks_%LOCALE%-jp.xml
SET XSLFILE=drinks_%LOCALE%-jp.xsl
SET EXPECTEDFILE=win_%LOCALE%.html
)
if %LOCALE% == ko (
SET STATUS=OK
SET XMLFILE=drinks_%LOCALE%-kr.xml
SET XSLFILE=drinks_%LOCALE%-kr.xsl
SET EXPECTEDFILE=win_%LOCALE%.html
)
if not %STATUS% == OK (
    echo SKIPPED!  This testcase is designed for Japanese or Korean Windows environment. 
    exit 0
)

call %PWD%\..\data\setup_%LOCALE%.bat
%JAVA_BIN%\java XSLTTest  %PWD%\%XMLFILE%  %PWD%\%XSLFILE% > %OUTPUT% 2>&1
fc %PWD%\%EXPECTEDFILE% %OUTPUT% > fc.out 2>&1
exit %errorlevel%
