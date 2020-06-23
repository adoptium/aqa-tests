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
@echo off
rem @echo (1) UTF-8= G1
rem @echo    
rem @echo    ªíä÷¤Q°\¦Ë¼Å¡ãÙ¬¡¯¢I¡¦ÅÃ¸¯
set TEST_STRING1=ªíä÷¤Q°\¦Ë¼Å¡ã

rem @echo (2) UTF-8 G2
rem @echo    
rem @echo    ®èªíä÷¤Q°\¦Ë¼Å¡ãÙ¬¡¯¢I
set TEST_STRING2=®èªí

rem @echo (3) Full angle character
rem @echo    
rem @echo    ¢Ï¢Ð¢Ñ¢°¢·¢¸¢±¢°³ü®Ã¨h¶L¤@¤B¤C¡þ¢@¡ã
set TEST_STRING3=¢Ï¢Ð¢Ñ¢°¢·¢¸¢±¢°³ü®Ã¨h¶L¤@¤B¤C¡þ¢@

rem @echo (4) Half-angle character
rem @echo    
rem @echo    ABCCDE18921³ü®Ã¨h¶L³ü¶L°Ñ¸v¥î³°¬m®Ã¨h¬B/\\
set TEST_STRING4=ABCCDE18921³ü¶L°Ñ¸v¥î³°¬m®Ã¨h¬B/\\

set TEST_STRING=®èªíä÷¤Q°\¦Ë¼Å¡ãÙ¬¢Ï¢Ð¢Ñ¢°¢·¢¸¢±¢°³ü®Ã¨h¶L³ü¶L°Ñ¸v¥î³°¬m®Ã¨h¬B¤@¤B¤C¡þ¢@
set TEST_STRING_SED=%TEST_STRING%
set TEST_STRINGS=%TEST_STRING1% %TEST_STRING2% %TEST_STRING3% %TEST_STRING4%
set TEST_STRINGS_SED=%TEST_STRINGS%