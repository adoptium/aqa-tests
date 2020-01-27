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

SETLOCAL
SET PWD=%~dp0
SET BASE=%PWD%
call %PWD%\check_env_windows.bat
call %PWD%\..\data\setup_%LOCALE%.bat
FOR /F "usebackq" %%i IN (`%JAVA_BIN%\java -cp %PWD%\CLDR_11.jar PrintLanguageTag`) DO SET LANGTAG=%%i

%JAVA_BIN%\java -cp %PWD%\CLDR_11.jar CheckZHTW
if ErrorLevel 1 (SET USE_ZHTW_WORKAROUND=true)

echo "Running ..."
%JAVA_BIN%\java -cp %PWD%\CLDR_11.jar MainStarter

%JAVA_BIN%\java -cp %PWD%\CLDR_11.jar CLDR11

if %USE_ZHTW_WORKAROUND%==true (
    for /D %%i in ( expected_TimeZoneTestA-zh-TW-CLDR.log TimeZoneTestA-zh-TW-JRE.log ) DO (
        copy /Y %%i %%i.orig > NUL 2>&1
        %JAVA_BIN%\java -cp %PWD%\CLDR_11.jar ModifyZHTW %%i
    )
)

SET FLAG=0

REM FC command is not executed for *.HOST.log.  The case does not work on Windows Server 2016.

fc DecimalFormatSymbolsTest-%LANGTAG%-DEFAULT.log DecimalFormatSymbolsTest-%LANGTAG%-CLDR,JRE.log > fc1.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalFormatSymbolsTest-%LANGTAG%-DEFAULT.log expected_DecimalFormatSymbolsTest-%LANGTAG%-DEFAULT.log > fc2.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalFormatSymbolsTest-%LANGTAG%-CLDR.log expected_DecimalFormatSymbolsTest-%LANGTAG%-CLDR.log > fc3.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalFormatSymbolsTest-%LANGTAG%-JRE.log expected_DecimalFormatSymbolsTest-%LANGTAG%-JRE.log > fc4.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc DecimalFormatSymbolsTest-%LANGTAG%-HOST.log %PWD%win_expected_DecimalFormatSymbolsTest-%LANGTAG%-HOST.log > fc5.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalFormatSymbolsTest-%LANGTAG%-SPI.log expected_DecimalFormatSymbolsTest-%LANGTAG%-SPI.log > fc6.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

fc DateFormatSymbolsTest-%LANGTAG%-DEFAULT.log DateFormatSymbolsTest-%LANGTAG%-CLDR,JRE.log > fc7.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DateFormatSymbolsTest-%LANGTAG%-DEFAULT.log expected_DateFormatSymbolsTest-%LANGTAG%-DEFAULT.log > fc8.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DateFormatSymbolsTest-%LANGTAG%-CLDR.log expected_DateFormatSymbolsTest-%LANGTAG%-CLDR.log > fc9.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DateFormatSymbolsTest-%LANGTAG%-JRE.log expected_DateFormatSymbolsTest-%LANGTAG%-JRE.log > fc10.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc DateFormatSymbolsTest-%LANGTAG%-HOST.log %PWD%\win_expected_DateFormatSymbolsTest-%LANGTAG%-HOST.log> fc11.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc DateFormatSymbolsTest-%LANGTAG%-SPI.log expected_DateFormatSymbolsTest-%LANGTAG%-SPI.log > fc12.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

fc DecimalStyleTest-%LANGTAG%-DEFAULT.log DecimalStyleTest-%LANGTAG%-CLDR,JRE.log > fc13.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalStyleTest-%LANGTAG%-DEFAULT.log DecimalStyleTest-%LANGTAG%-CLDR,JRE.log > fc14.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalStyleTest-%LANGTAG%-CLDR.log DecimalStyleTest-%LANGTAG%-CLDR,JRE.log > fc15.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalStyleTest-%LANGTAG%-JRE.log DecimalStyleTest-%LANGTAG%-CLDR,JRE.log > fc16.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc DecimalStyleTest-%LANGTAG%-HOST.log DecimalStyleTest-%LANGTAG%-CLDR,JRE.log > fc17.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc DecimalStyleTest-%LANGTAG%-SPI.log DecimalStyleTest-%LANGTAG%-CLDR,JRE.log > fc18.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

fc CurrencyTest-%LANGTAG%-DEFAULT.log CurrencyTest-%LANGTAG%-CLDR,JRE.log > fc19.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc CurrencyTest-%LANGTAG%-DEFAULT.log expected_CurrencyTest-%LANGTAG%-DEFAULT.log > fc20.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc CurrencyTest-%LANGTAG%-CLDR.log expected_CurrencyTest-%LANGTAG%-CLDR.log > fc21.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc CurrencyTest-%LANGTAG%-JRE.log expected_CurrencyTest-%LANGTAG%-JRE.log > fc22.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc CurrencyTest-%LANGTAG%-HOST.log %PWD%\win_expected_CurrencyTest-%LANGTAG%-HOST.log > fc23.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc CurrencyTest-%LANGTAG%-SPI.log expected_CurrencyTest-%LANGTAG%-SPI.log > fc24.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

fc LocaleTest-%LANGTAG%-DEFAULT.log LocaleTest-%LANGTAG%-CLDR,JRE.log > fc25.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc LocaleTest-%LANGTAG%-DEFAULT.log expected_LocaleTest-%LANGTAG%-DEFAULT.log > fc26.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc LocaleTest-%LANGTAG%-CLDR.log expected_LocaleTest-%LANGTAG%-CLDR.log > fc27.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc LocaleTest-%LANGTAG%-JRE.log expected_LocaleTest-%LANGTAG%-JRE.log > fc28.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc LocaleTest-%LANGTAG%-HOST.log %PWD%\win_expected_LocaleTest-%LANGTAG%-HOST.log > fc29.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc LocaleTest-%LANGTAG%-SPI.log expected_LocaleTest-%LANGTAG%-SPI.log > fc30.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

fc TimeZoneTestA-%LANGTAG%-DEFAULT.log TimeZoneTestA-%LANGTAG%-CLDR,JRE.log > fc31.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestA-%LANGTAG%-DEFAULT.log TimeZoneTestA-%LANGTAG%-CLDR,JRE.log > fc32.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestA-%LANGTAG%-JRE.log TimeZoneTestA-%LANGTAG%-CLDR,JRE.log > fc33.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc TimeZoneTestA-%LANGTAG%-HOST.log TimeZoneTestA-%LANGTAG%-CLDR,JRE.log > fc34.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestA-%LANGTAG%-SPI.log TimeZoneTestA-%LANGTAG%-CLDR,JRE.log > fc35.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestA-%LANGTAG%-CLDR.log expected_TimeZoneTestA-%LANGTAG%-CLDR.log > fc36.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

fc TimeZoneTestB-%LANGTAG%-DEFAULT.log TimeZoneTestB-%LANGTAG%-CLDR,JRE.log > fc37.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestB-%LANGTAG%-DEFAULT.log expected_TimeZoneTestB-%LANGTAG%-DEFAULT.log > fc38.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestB-%LANGTAG%-CLDR.log expected_TimeZoneTestB-%LANGTAG%-CLDR.log > fc39.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestB-%LANGTAG%-JRE.log expected_TimeZoneTestB-%LANGTAG%-JRE.log > fc40.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )
REM fc TimeZoneTestB-%LANGTAG%-HOST.log expected_TimeZoneTestB-%LANGTAG%-HOST.log > fc41.out 2>&1
REM if ErrorLevel 1 ( SET FLAG=1 )
fc TimeZoneTestB-%LANGTAG%-SPI.log expected_TimeZoneTestB-%LANGTAG%-SPI.log > fc42.out 2>&1
if ErrorLevel 1 ( SET FLAG=1 )

exit %FLAG%

