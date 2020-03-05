#!/bin/sh
################################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
################################################################################
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

