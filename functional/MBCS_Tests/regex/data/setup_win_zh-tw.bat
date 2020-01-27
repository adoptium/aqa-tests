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

java SimpleGrep ечешецефепех %PWD%\win_zh-tw.txt
@echo --- Confirm that the line(s) includes "ечешецефепех". 
@echo --- Did you get the line(s)?

java SimpleGrep "еч*еф" %PWD%\win_zh-tw.txt
@echo --- Confirm that the line(s) includes the pattern "еч*еф". 
@echo --- Did you get the line(s) ?

java SimpleGrep "^жA" %PWD%\win_zh-tw.txt
@echo --- Confirm that the line(s) starts with "жA".
@echo --- Did you get the line ?

java SimpleGrep иХ %PWD%\win_zh-tw.txt
@echo --- Confirm that the line(s) includes "иХ". 
@echo --- Did you get the line ?

java SimpleGrep кz %PWD%\win_zh-tw.txt
@echo --- Confirm that the line(s) includes "кz". 
@echo --- Did you get the line?

java SimpleGrep \u8868\u5642\u5341\u8c79 %PWD%\win_zh-tw.txt
@echo --- Confirm that the line(s) includes "іь®ГЁh". 
@echo --- Did you get the line ?


@echo\
@echo ------------ Pattern replacement test ------------

java RegexReplaceTest ечешецефепех aiueo %PWD%\win_zh-tw.txt -v
@echo --- Confirm that "ечешецефепех" was replaced by "aiueo". 
@echo --- OK ?

java RegexReplaceTest иХ а·аёає %PWD%\win_zh-tw.txt -v
@echo --- Confirm that "иХ" was replaced by "а·аёає". 
@echo --- OK ?

java RegexReplaceTest кz \\ %PWD%\win_zh-tw.txt -v
@echo --- Confirm that "кz" was replaced by "\". 
@echo --- OK ?

java RegexReplaceTest \u8868\u5642\u5341\u8c79 ¤B¤C %PWD%\win_zh-tw.txt -v
@echo --- Confirm that "Єндч¤Q°\" was replaced by "¤B¤C". 
@echo --- OK ?

