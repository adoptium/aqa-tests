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
call %PWD%\set_variable.bat

%JAVA_BIN%\java -Dresult=false Main %2 %3 %4

SET FLAG=0

findstr Passed LocaleFilterTest1.out > find_LocaleFilterTest1.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed LocaleFilterTest2.out > find_LocaleFilterTest2.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed LocaleFilterTest3.out > find_LocaleFilterTest3.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed LocaleLookupTest1.out > find_LocaleLookupTest1.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
findstr Passed LocaleLookupTest2.out > find_LocaleLookupTest2.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

exit %FLAG%

