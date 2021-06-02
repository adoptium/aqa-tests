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

@echo ------------ Pattern matching test ------------

java SimpleGrep ª¢ß­ô×»õÓú %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes "ª¢ß­ô×»õÓú". 
@echo --- Did you get the line(s) 11,12 and 47 ?

java SimpleGrep "ª¢.*Óú" %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes the pattern "ª¢*Óú". 
@echo --- Did you get the line(s) 11,12,47,48,50 and 52 ?

java SimpleGrep "^íÓ" %PWD%\win_ko.txt
@echo --- Confirm that the line(s) starts with "íÓ".
@echo --- Did you get the line 53,54 and 55 ?

java SimpleGrep ´·é¡ %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes half-width Katakana "´·é¡". 
@echo --- Did you get the line 19,20 and 39 ?

java SimpleGrep ¡Í %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes "¡Í" (full-width Yen symbol). 
@echo --- Did you get the line 24 and 66 ?

java SimpleGrep \\ %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes "\" (half-width Yen symbol). 
@echo --- Did you get the line 33 and 35 ?

java SimpleGrep "íÏ.*Âú" %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes the pattern "íÏ.*Âú". 
@echo --- Did you get the line 81 ?

java SimpleGrep ª¢ª«ªµ %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes "ª¢ª«ªµ". 
@echo --- Did you get the line 31 ?

java SimpleGrep [¢à¢å¨õ¥´] %PWD%\win_ko.txt
@echo --- Confirm that the line(s) includes any of ¢à,¢å,¨õ or ¥´. 
@echo --- Did you get the line 60,61 and 63 ?


@echo\
@echo ------------ Pattern replacement test ------------

java RegexReplaceTest ª¢ß­ô×»õÓú aiueo %PWD%\win_ko.txt -v
@echo --- Confirm that "ª¢ß­ô×»õÓú" was replaced by "aiueo". 
@echo --- OK ?

java RegexReplaceTest ´·é¡ Üþ°ÖÜþ¸Ø %PWD%\win_ko.txt -v
@echo --- Confirm that "´·é¡" was replaced by "Üþ°ÖÜþ¸Ø". 
@echo --- OK ?

java RegexReplaceTest ¡Í \\ %PWD%\win_ko.txt -v
@echo --- Confirm that "¡Í" was replaced by "\". 
@echo --- OK ?

java RegexReplaceTest "íÏ.*Âú" "£µ£ã»Ï½Àéè" %PWD%\win_ko.txt -v
@echo --- Confirm that "íÏ.*Âú" was replaced by "£µ£ã»Ï½Àéè". 
@echo --- OK ?

java RegexReplaceTest ª¢ª«ªµ Âúï» %PWD%\win_ko.txt -v
@echo --- Confirm that "ª¢ª«ªµ" was replaced by "Âúï»". 
@echo --- OK ?

java RegexReplaceTest [¢à¢å¨õ¥´] èâ½À %PWD%\win_ko.txt -v
@echo --- Confirm that any of "¢à¢å¨õ¥´" were replaced by "èâ½À". 
@echo --- OK ?
