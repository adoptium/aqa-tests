:: Licensed under the Apache License, Version 2.0 (the "License");
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::     http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.

@echo off
setlocal enabledelayedexpansion

set sdkdir=
set testdir=
set platform=
set jvmversion=
set sdk_resource=nightly
set customizedURL=
set openj9_repo="https://github.com/eclipse/openj9.git"
set openj9_sha=

CALL :parseCommandLineArgs %*
CALL :getTestKitGenAndFunctionalTestMaterial
if NOT "%SDKDIR%" == "" CALL :getBinaryOpenjdk
EXIT /B %ERRORLEVEL%

:usage
echo "Usage : get.bat  -testdir openjdktestdir"
echo "               -platform x64_linux | x64_mac | s390x_linux | ppc64le_linux | aarch64_linux | ppc64_aix"
echo "                -jvmversion openjdk8 | openjdk8-openj9 | openjdk9 | openjdk9-openj9 | openjdk10 | openjdk10-sap"
echo "                [-sdkdir binarySDKDIR] : if do not have a local sdk available, specify preferred directory"
echo "                [-sdk_resource ] : indicate where to get sdk - releases, nightly , upstream or customized"
echo "                [-customizedURL ] : indicate sdk url if sdk source is set as customized"
echo "                [--openj9_repo ] : optional. OpenJ9 git repo. Default value https://github.com/eclipse/openj9.git is used if not provided"
echo "                [--openj9_sha ] : optional. OpenJ9 pull request sha."

EXIT /B 0

:parseCommandLineArgs
SET key=%1
SET value=%2

:loop
IF NOT "%1"=="" (
    IF "%1"=="-testdir" (
        SET testdir=%2
        SHIFT
    )
    IF "%1"=="-platform" (
        SET platform=%2
        SHIFT
    )
    IF "%1"=="-jvmversion" (
        SET jvmversion=%2
        SHIFT
    )
    IF "%1"=="-sdkdir" (
        SET sdkdir=%2
        SHIFT
    )
    IF "%1"=="-sdk_resource" (
        SET sdk_resource=%2
        SHIFT
    )
    IF "%1"=="-customizedURL" (
        SET customizedURL=%2
        SHIFT
    )
    IF "%1"=="-openj9_repo" (
        SET openj9_repo=%2
        SHIFT
    )
    IF "%1"=="-openj9_sha" (
        SET openj9_sha=%2
        SHIFT
    )
    SHIFT
    GOTO :loop
)

EXIT /B 0

:getBinaryOpenjdk
cd %sdkdir%

if [%customizedURL%] == [] (
	set /A nonCustomized=0
	if "%sdk_resource%" == "nightly" set /A nonCustomized=1
	if "%sdk_resource%" == "releases" set /A nonCustomized=1
	if !nonCustomized! EQU 1 (
		echo 'Get binary openjdk...'
		mkdir openjdkbinary
		set download_url=https://api.adoptopenjdk.net/%jvmversion%/%sdk_resource%/%platform%/latest/binary
		CALL :wgetSDK
	)
) else (
	set download_url=%customizedURL%
	CALL :wgetSDK
)

cd openjdkbinary

setlocal
FOR /F %%f IN ('dir /A /B') DO set jar_file_name=%%f
unzip -q %jar_file_name%

FOR /F %%d IN ('dir /AD /B') DO set unzipDir=%%d
if NOT "%unzipDir%" == "j2sdk-image" move %unzipDir% j2sdk-image
endlocal
EXIT /B 0

:getTestKitGenAndFunctionalTestMaterial
echo testdir is %testdir%
cd %testdir%
echo git clone %openj9_repo%
REM temporary use personal repo, should be fixed: git clone -q --depth 1 $OPENJ9_REPO
git clone -q --depth 1 %openj9_repo%

if NOT "%openj9_sha%" == "" (
	echo update to openj9 sha %openj9_sha%
	cd openj9
	git fetch -q --tags %openj9_repo% +refs/pull/*:refs/remotes/origin/pr/*
	git checkout -q %openj9_sha%
	cd %testdir%
)

move openj9/test/TestConfig TestConfig
move openj9/test/Utils Utils
move openj9/test/functional functional
rmdir /s /q openj9
EXIT /B 0

:wgetSDK
wget -q --no-check-certificate --header 'Cookie: allow-download=1' %download_url% --directory-prefix=%sdkdir%/openjdkbinary
if %ERRORLEVEL% NEQ 0 (
	echo "Failed to retrieve the jdk binary, exiting"
	exit 1
)
EXIT /B 0

