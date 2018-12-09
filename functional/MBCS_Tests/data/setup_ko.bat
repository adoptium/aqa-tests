@echo off
@echo (1) Hangul Compatibility Jamo
@echo    
@echo    ¤¡¤¤¤§¤©¤±¤²¤µ¤·¤¸¤º¤»¤¼¤½¤¾¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð¤Ñ¤Ó¤þ
@echo    ¤¡¤¤¤§¤©¤±¤²¤µ¤·¤¸¤º¤»¤¼¤½¤¾¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð¤Ñ¤Ó¤þ
set TEST_STRING1=¤¡¤¤¤§¤©¤±¤²¤µ¤·¤¸¤º¤»¤¼¤½¤¾¤¿¤Á¤Ã¤Å¤Ç¤Ë¤Ì¤Ð¤Ñ¤Ó¤þ

@echo (2) Enclosed CJK Letters and Months
@echo    
@echo    ©±©²©³©´©µ©¶©·©¿©À©Á©Â©Ã©Ä©Å¢ß¨±¨²¨³¨´¨µ¨¶¨·¢Þ
@echo    ©±©²©³©´©µ©¶©·©¿©À©Á©Â©Ã©Ä©Å¢ß¨±¨²¨³¨´¨µ¨¶¨·¢Þ
set TEST_STRING2=©±©²©³©´©µ©¶©·©¿©À©Á©Â©Ã©Ä©Å¢ß¨±¨²¨³¨´¨µ¨¶¨·¢Þ

@echo (3) Hangul Syllables
@echo    
@echo    ÇÑ±¹ÀÏº»°¡´ëº£¾ÖÄÉÈÌ
@echo    ÇÑ±¹ÀÏº»°¡´ëº£¾ÖÄÉÈÌ
set TEST_STRING3=ÇÑ±¹ÀÏº»°¡´ëº£¾ÖÄÉÈÌ

@echo (4) ùÓí®
@echo    
@echo    ùÛÏÐìíÜâÊ¡Ê¢Ê£Ê¤ýûýüýýýþ
@echo    ùÛÏÐìíÜâÊ¡Ê¢Ê£Ê¤ýûýüýýýþ
set TEST_STRING4=ùÛÏÐìíÜâÊ¡Ê¢Ê£Ê¤ýûýüýýýþ

set TEST_STRING=¤¡¤¤¤§¤¿¤Á¤Ã©±©²©³ÇÑ±¹ÀÏº»ùÛÏÐìíÜâ
set TEST_STRING_SED=%TEST_STRING%
set TEST_STRINGS=%TEST_STRING1% %TEST_STRING2% %TEST_STRING3% %TEST_STRING4%
set TEST_STRINGS_SED=%TEST_STRINGS%

