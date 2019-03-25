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

call %PWD%\..\data\setup_%LOCALE%.bat
if exist tmp rd /S /Q tmp
mkdir tmp
copy %PWD%\*.java tmp 2>&1 > NUL
copy %PWD%\ChgWord.vbs tmp 2>&1 > NUL
cd tmp

%JAVA_BIN%\javac CheckValidData.java

FOR /F "usebackq" %%i IN (`%JAVA_BIN%\java CheckValidData %TEST_STRINGS%`) DO set TS=%%i

cscript //nologo ChgWord.vbs TEST_STRING %TS% DefineAnnotation_org.java > DefineAnnotation.java
cscript //nologo ChgWord.vbs TEST_STRING %TS% AnnotatedTest_org.java > AnnotatedTest.java

@echo compiling... > %OUTPUT%
%JAVA_BIN%\javac DefineAnnotation.java AnnotationProcessor.java AnnotationProcessor7.java AnnotationProcessor8.java AnnotationProcessor11.java AnnotatedTest.java

@echo execute javap >> %OUTPUT%
%JAVA_BIN%\javap AnnotatedTest >> %OUTPUT%

@echo execute javac with processor option with RELEASE_6 >> %OUTPUT%
%JAVA_BIN%\javac -processor AnnotationProcessor AnnotatedTest.java >> %OUTPUT%

@echo execute javac with processor option with RELEASE_7 >> %OUTPUT%
%JAVA_BIN%\javac -processor AnnotationProcessor7 AnnotatedTest.java >> %OUTPUT%

@echo execute javac with processor option with RELEASE_8 >> %OUTPUT%
%JAVA_BIN%\javac -processor AnnotationProcessor8 AnnotatedTest.java >> %OUTPUT%

@echo execute javac with processor option with RELEASE_11 >> %OUTPUT%
%JAVA_BIN%\javac -processor AnnotationProcessor11 AnnotatedTest.java >> %OUTPUT%

fc %PWD%\expected\win_%LOCALE%.expected.txt output.txt > fc.out 2>&1
exit %errorlevel%


