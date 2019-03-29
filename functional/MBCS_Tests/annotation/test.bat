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
REM SET CLASSPATH=.
FOR /F "usebackq" %%i IN (`cscript //NOLOGO %PWD%\locale.vbs`) DO SET LOCALE=%%i
SET STATUS=UKNOWN
if %LOCALE% == ja SET STATUS=OK
if %LOCALE% == ko SET STATUS=OK
if not %STATUS% == OK (
    echo SKIPPED!  This testcase is designed for Japanese or Korean Windows environment. 
    exit 0
)
SET CP= -cp "%PWD%annotation.jar"

call %PWD%\..\data\setup_%LOCALE%.bat
if exist tmp rd /S /Q tmp
mkdir tmp
copy %PWD%\*.java tmp 2>&1 > NUL
copy %PWD%\ChgWord.vbs tmp 2>&1 > NUL
cd tmp

FOR /F "usebackq" %%i IN (`%JAVA_BIN%\java %CP% CheckValidData %TEST_STRINGS%`) DO set TS=%%i

cscript //nologo ChgWord.vbs TEST_STRING %TS% DefineAnnotation_org.java > DefineAnnotation.java
cscript //nologo ChgWord.vbs TEST_STRING %TS% AnnotatedTest_org.java > AnnotatedTest.java

FOR /F "usebackq" %%j IN (`%JAVA_BIN%\java %CP% SDKPath`) DO set SDKPATH=%%j

@echo compiling...
%SDKPATH%\javac DefineAnnotation.java AnnotatedTest.java

@echo execute javap
%SDKPATH%\javap AnnotatedTest > javap.txt 2>&1
fc javap.txt %PWD%\expected\win_%LOCALE%.def.txt > fc.txt 2>&1
if ErrorLevel 1 (
copy fc.txt diff.txt > nul 2>&1
)

%JAVA_BIN%\java %CP% SourceVersionCheck %PWD%\expected\win_%LOCALE%.pro.txt 2>> diff.txt

for %%F in (diff.txt) do (
  if %%~zF==0 (
@echo PASSED
    exit 0
  ) else (
@echo FAILED
    exit 1
  )
)
