@echo off
@echo ##### setting up test string variables #####
@echo TEST_STRING :  a string contains one DBCS word for testing
@echo TEST_STRINGS : a string contains multiple DBCS words for testing

set TEST_STRING=¶İ¼Ş•\¦ƒeƒXƒg‡@‡A‡I‡SÊa`
set TEST_STRINGS=Š¿š•\¦”\—Í ƒJƒi¶Å‚j‚‚‚Kana ‹L†:\~P\_`ac|‘’Ê ŠOš:úUúhV‡Š‡‚‡„ ŠOš:‡T‡U‡@‡A‡I‡S 

@echo TEST_STRING= %TEST_STRING%
@echo TEST_STRINGS= %TEST_STRINGS%


