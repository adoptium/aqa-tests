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

java SimpleGrep 嘨嘩嘪嘫嘮嘯嘰嘳鶣鶤鶥鶦 %PWD%\win_zh-cn.txt
@echo --- Confirm that the line(s) includes "嘨嘩嘪嘫嘮嘯嘰嘳鶣鶤鶥鶦". 
@echo --- Did you get the line(s)?

java SimpleGrep "嘨*嘪" %PWD%\win_zh-cn.txt
@echo --- Confirm that the line(s) includes the pattern "嘨*嘪". 
@echo --- Did you get the line(s) ?

java SimpleGrep "^劚" %PWD%\win_zh-cn.txt
@echo --- Confirm that the line(s) starts with "劚".
@echo --- Did you get the line ?

java SimpleGrep 噲 %PWD%\win_zh-cn.txt
@echo --- Confirm that the line(s) includes "噲". 
@echo --- Did you get the line ?

java SimpleGrep 椷 %PWD%\win_zh-cn.txt
@echo --- Confirm that the line(s) includes "椷". 
@echo --- Did you get the line?

java SimpleGrep \u628e\u99e1U\u90c2 %PWD%\win_zh-cn.txt
@echo --- Confirm that the line(s) includes "表抎駡名". 
@echo --- Did you get the line ?


@echo\
@echo ------------ Pattern replacement test ------------

java RegexReplaceTest 嘨嘩嘪嘫嘮嘯嘰嘳鶣鶤鶥鶦 aiueo %PWD%\win_zh-cn.txt -v
@echo --- Confirm that "嘨嘩嘪嘫嘮嘯嘰嘳鶣鶤鶥鶦" was replaced by "aiueo". 
@echo --- OK ?

java RegexReplaceTest 噲 僇僞僇僫 %PWD%\win_zh-cn.txt -v
@echo --- Confirm that "噲" was replaced by "僇僞僇僫". 
@echo --- OK ?

java RegexReplaceTest 椷 \\ %PWD%\win_zh-cn.txt -v
@echo --- Confirm that "椷" was replaced by "\". 
@echo --- OK ?

java RegexReplaceTest \u628e\u99e1U\u90c2 椷噲 %PWD%\win_zh-cn.txt -v
@echo --- Confirm that "表抎駡名" was replaced by "椷噲". 
@echo --- OK ?

