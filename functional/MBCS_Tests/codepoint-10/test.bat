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

%JAVA_BIN%\java -cp %PWD%codepoint.jar UnicodeDataTest %PWD%data\UnicodeData-10.0.0.txt 2>err.txt
%JAVA_BIN%\java -cp %PWD%codepoint.jar UnihanCodePoint %PWD%data\UnicodeData-10.0.0.txt %PWD%data\Unihan_IRGSources-10.0.0.txt 2>>err.txt

set SIZE=1
for /f %%i in ("err.txt") do set SIZE=%%~zi
echo SIZE= %SIZE%
exit %SIZE%

