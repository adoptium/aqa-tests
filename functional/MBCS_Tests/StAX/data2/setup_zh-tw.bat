@echo off
@echo (1) UTF-8= G1
@echo    
@echo    ªíä÷¤Q°\¦Ë¼Å¡ãÙ¬¡¯¢I¡¦ÅÃ¸¯
set TEST_STRING1=ªíä÷¤Q°\¦Ë¼Å¡ã

@echo (2) UTF-8 G2
@echo    
@echo    ®èªíä÷¤Q°\¦Ë¼Å¡ãÙ¬¡¯¢I
set TEST_STRING2=®èªí

@echo (3) Full angle character
@echo    
@echo    ¢Ï¢Ð¢Ñ¢°¢·¢¸¢±¢°³ü®Ã¨h¶L¤@¤B¤C¡þ¢@¡ã
set TEST_STRING3=¢Ï¢Ð¢Ñ¢°¢·¢¸¢±¢°³ü®Ã¨h¶L¤@¤B¤C¡þ¢@

@echo (4) Half-angle character
@echo    
@echo    ABCCDE18921³ü®Ã¨h¶L³ü¶L°Ñ¸v¥î³°¬m®Ã¨h¬B/\\
set TEST_STRING4=ABCCDE18921³ü¶L°Ñ¸v¥î³°¬m®Ã¨h¬B/\\

set TEST_STRING=®èªíä÷¤Q°\¦Ë¼Å¡ãÙ¬¢Ï¢Ð¢Ñ¢°¢·¢¸¢±¢°³ü®Ã¨h¶L³ü¶L°Ñ¸v¥î³°¬m®Ã¨h¬B¤@¤B¤C¡þ¢@
set TEST_STRING_SED=%TEST_STRING%
set TEST_STRINGS=%TEST_STRING1% %TEST_STRING2% %TEST_STRING3% %TEST_STRING4%
set TEST_STRINGS_SED=%TEST_STRINGS%