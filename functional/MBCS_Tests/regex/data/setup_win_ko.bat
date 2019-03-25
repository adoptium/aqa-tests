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

echo ------------ Pattern matching test ------------

java SimpleGrep "Æ¯¼ö" %PWD%\win_ko.txt
echo --- Confirm that the line(s) includes "Æ¯¼ö". 
echo --- Did you get the line(s) 14,23,31,43 ?

java SimpleGrep "gnome" %PWD%\win_ko.txt
echo --- Confirm that the line(s) includes the pattern "gnome". 
echo --- Did you get the line(s) 11,12,49,52,54,78 ?

java SimpleGrep "^¤¡" %PWD%\win_ko.txt
echo --- Confirm that the line(s) starts with "¤¡".
echo --- Did you get the line(s) 98,185 ?

java SimpleGrep ¹®Á¦ %PWD%\win_ko.txt
echo --- Confirm that the line(s) includes "¹®Á¦". 
echo --- Did you get the line(s) 85,86,88 ?

java SimpleGrep "Ê¥áÜ" %PWD%\win_ko.txt
echo --- Confirm that the line(s) includes "Ê¥áÜ". 
echo --- Did you get the line(s) 234,235,236 ?

java SimpleGrep "\u5475\u5475\u5927\u7b11" %PWD%\win_ko.txt
echo --- Confirm that the line(s) includes "Ê§Ê§ÓÞáÅ". "
echo --- Did you get the line 124 ?

java SimpleGrep ¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð %PWD%\win_ko.txt
echo --- Confirm that the line(s) includes any of  ¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð
echo --- Did you get the line(s) 98  ?

echo;
echo ------------ Pattern replacement test ------------

java RegexReplaceTest £±£²£³£´£µ£¶£·£¸£¹ ¢Å¢Æ¢Ç¢È¢É¢Ê¢Ë¢Ì¢Í %PWD%\win_ko.txt -v
echo --- Confirm that "£±£²£³£´£µ£¶£·£¸£¹" was replaced by "¢Å¢Æ¢Ç¢È¢É¢Ê¢Ë¢Ì¢Í". 
echo --- OK ?

java RegexReplaceTest °¡Á¤±³»ç Ê«ïÔÎçÞÔ %PWD%\win_ko.txt -v
echo --- Confirm that "°¡Á¤±³»ç" was replaced by "Ê«ïÔÎçÞÔ". 
echo --- OK ?

java RegexReplaceTest "ËþÙý" "°Ë¹®" %PWD%\win_ko.txt -v
echo --- Confirm that "ËþÙý" was replaced by "°Ë¹®". 
echo --- OK ?

java RegexReplaceTest "°¡.*È£" "Ê«Ê«ûÂûÂ" %PWD%\win_ko.txt -v
echo --- Confirm that "°¡.*È£" was replaced by "Ê«Ê«ûÂûÂ". 
echo --- OK ?

java RegexReplaceTest "\u5bb6\u7cfb" "Ê«ÍªÝ­" %PWD%\win_ko.txt -v
echo --- Confirm that "Ê«Í§"replaced by "Ê«ÍªÝ­". 
echo --- OK ?
