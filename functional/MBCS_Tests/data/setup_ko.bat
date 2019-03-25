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

REM @echo (1) Hangul Compatibility Jamo
REM @echo    
REM @echo    ¤¡¤¤¤§¤©¤±¤²¤µ¤·¤¸¤º¤»¤¼¤½¤¾¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð¤Ñ¤Ó¤þ
REM @echo    ¤¡¤¤¤§¤©¤±¤²¤µ¤·¤¸¤º¤»¤¼¤½¤¾¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð¤Ñ¤Ó¤þ
set TEST_STRING1=¤¡¤¤¤§¤©¤±¤²¤µ¤·¤¸¤º¤»¤¼¤½¤¾¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð¤Ñ¤Ó¤þ

REM @echo (2) Enclosed CJK Letters and Months
REM @echo    
REM @echo    ©±©²©³©´©µ©¶©·©¿©À©Á©Â©Ã©Ä©Å¢ß¨±¨²¨³¨´¨µ¨¶¨·¢Þ
REM @echo    ©±©²©³©´©µ©¶©·©¿©À©Á©Â©Ã©Ä©Å¢ß¨±¨²¨³¨´¨µ¨¶¨·¢Þ
set TEST_STRING2=©±©²©³©´©µ©¶©·©¿©À©Á©Â©Ã©Ä©Å¢ß¨±¨²¨³¨´¨µ¨¶¨·¢Þ

REM @echo (3) Hangul Syllables
REM @echo    
REM @echo    ÇÑ±¹ÀÏº»°¡´ëº£¾ÖÄÉÈÌ
REM @echo    ÇÑ±¹ÀÏº»°¡´ëº£¾ÖÄÉÈÌ
set TEST_STRING3=ÇÑ±¹ÀÏº»°¡´ëº£¾ÖÄÉÈÌ

REM @echo (4) ùÓí®
REM @echo    
REM @echo    ùÛÏÐìíÜâÊ¡Ê¢Ê£Ê¤ýûýüýýýþ
REM @echo    ùÛÏÐìíÜâÊ¡Ê¢Ê£Ê¤ýûýüýýýþ
set TEST_STRING4=ùÛÏÐìíÜâÊ¡Ê¢Ê£Ê¤ýûýüýýýþ

set TEST_STRING=¤¡¤¤¤§¤¿¤Á¤Ã©±©²©³ÇÑ±¹ÀÏº»ùÛÏÐìíÜâ
set TEST_STRING_SED=%TEST_STRING%
set TEST_STRINGS=%TEST_STRING1% %TEST_STRING2% %TEST_STRING3% %TEST_STRING4%
set TEST_STRINGS_SED=%TEST_STRINGS%

