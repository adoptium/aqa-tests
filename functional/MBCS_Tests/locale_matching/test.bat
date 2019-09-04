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
SET CLASSPATH=%PWD%\locale_matching.jar
call %PWD%\check_env_windows.bat

if exist result rd /S /Q result
mkdir result
echo Testing LocaleFilterTest1 ...
%JAVA_BIN%\java -Dresult=false LocaleFilterTest1 > result\LocaleFilterTest1.out
echo Testing LocaleFilterTest2 ...
%JAVA_BIN%\java -Dresult=false LocaleFilterTest2 > result\LocaleFilterTest2.out
echo Testing LocaleFilterTest3 ...
%JAVA_BIN%\java -Dresult=false LocaleFilterTest3 > result\LocaleFilterTest3.out
echo Testing LocaleLookupTest1 ...
%JAVA_BIN%\java -Dresult=false LocaleLookupTest1 > result\LocaleLookupTest1.out
echo Testing LocaleLookupTest2 ...
%JAVA_BIN%\java -Dresult=false LocaleLookupTest2 > result\LocaleLookupTest2.out

SET FLAG=0

findstr Passed result\LocaleFilterTest1.out > find_LocaleFilterTest1.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed result\LocaleFilterTest2.out > find_LocaleFilterTest2.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed result\LocaleFilterTest3.out > find_LocaleFilterTest3.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed result\LocaleLookupTest1.out > find_LocaleLookupTest1.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed result\LocaleLookupTest2.out > find_LocaleLookupTest2.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

exit %FLAG%

