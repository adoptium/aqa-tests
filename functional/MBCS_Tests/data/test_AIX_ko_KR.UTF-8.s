#!/bin/ksh
# (1) Hangul Compatibility Jamo
#    
#    ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣㆎ
#    ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣㆎ
export TEST_STRING1=ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣㆎ

# (2) Enclosed CJK Letters and Months
#    
#    ㈀㈁㈂㈃㈄㈅㈆㈎㈏㈐㈑㈒㈓㈔㈜㉠㉡㉢㉣㉤㉥㉦㉿
#    ㈀㈁㈂㈃㈄㈅㈆㈎㈏㈐㈑㈒㈓㈔㈜㉠㉡㉢㉣㉤㉥㉦㉿
export TEST_STRING2=㈀㈁㈂㈃㈄㈅㈆㈎㈏㈐㈑㈒㈓㈔㈜㉠㉡㉢㉣㉤㉥㉦㉿

# (3) Hangul Syllables
#    
#    한국일본가대베애케훠
#    한국일본가대베애케훠
export TEST_STRING3=한국일본가대베애케훠

# (4) 漢字
#    
#    韓國日本伽佳假價禧稀羲詰
#    韓國日本伽佳假價禧稀羲詰
export TEST_STRING4=韓國日本伽佳假價禧稀羲詰

export HTML_CHARSET=UTF-8
export TEST_STRING=ㄱㄴㄷㅏㅑㅓ㈀㈁㈂한국일본韓國日本
export TEST_STRING_SED=$TEST_STRING
export TEST_STRINGS=$TEST_STRING1" "$TEST_STRING2" "$TEST_STRING3" "$TEST_STRING4
export TEST_STRINGS_SED=$TEST_STRINGS

