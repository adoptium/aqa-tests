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

java SimpleGrep Ç†Ç¢Ç§Ç¶Ç® %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes "Ç†Ç¢Ç§Ç¶Ç®". 
@echo --- Did you get the line(s) 11,12 and 47 ?

java SimpleGrep "Ç†.*Ç®" %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes the pattern "Ç†*Ç®". 
@echo --- Did you get the line(s) 11,12,47,48,50 and 52 ?

java SimpleGrep "^äø" %PWD%\win_ja.txt
@echo --- Confirm that the line(s) starts with "äø".
@echo --- Did you get the line 53,54 and 55 ?

java SimpleGrep ∂≈ %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes half-width Katakana "∂≈". 
@echo --- Did you get the line 19,20 and 39 ?

java SimpleGrep Åè %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes "Åè" (full-width Yen symbol). 
@echo --- Did you get the line 24 and 66 ?

java SimpleGrep \\ %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes "\" (half-width Yen symbol). 
@echo --- Did you get the line 33 and 35 ?

java SimpleGrep "ï@.*É\" %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes the pattern "ï@.*É\". 
@echo --- Did you get the line 81 ?

java SimpleGrep \u3042\u304b\u3055 %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes "Ç†Ç©Ç≥". 
@echo --- Did you get the line 31 ?

java SimpleGrep [áçáéáèá~áSáX] %PWD%\win_ja.txt
@echo --- Confirm that the line(s) includes any of áç,áé,áè,á~,áS or áX. 
@echo --- Did you get the line 60,61 and 63 ?


@echo\
@echo ------------ Pattern replacement test ------------

java RegexReplaceTest Ç†Ç¢Ç§Ç¶Ç® aiueo %PWD%\win_ja.txt -v
@echo --- Confirm that "Ç†Ç¢Ç§Ç¶Ç®" was replaced by "aiueo". 
@echo --- OK ?

java RegexReplaceTest ∂≈ ÉJÉ^ÉJÉi %PWD%\win_ja.txt -v
@echo --- Confirm that "∂≈" was replaced by "ÉJÉ^ÉJÉi". 
@echo --- OK ?

java RegexReplaceTest Åè \\ %PWD%\win_ja.txt -v
@echo --- Confirm that "Åè" was replaced by "\". 
@echo --- OK ?

java RegexReplaceTest "ï@.*É\" "ÇTÇÉï∂éöóÒ" %PWD%\win_ja.txt -v
@echo --- Confirm that "ï@.*É\" was replaced by "ÇTÇÉï∂éöóÒ". 
@echo --- OK ?

java RegexReplaceTest \u3042\u304b\u3055 É\Á^ %PWD%\win_ja.txt -v
@echo --- Confirm that "Ç†Ç©Ç≥" was replaced by "É\Á^". 
@echo --- OK ?

java RegexReplaceTest [áçáéáèá~áSáX] äOéö %PWD%\win_ja.txt -v
@echo --- Confirm that any of "áçáéáèá~áSáX" were replaced by "äOéö". 
@echo --- OK ?
