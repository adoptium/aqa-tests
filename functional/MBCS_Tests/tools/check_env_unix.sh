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

setData() {
    . ${BASE}/../data/test_${FULLLANG}
}

showMessage() {
    echo "SKIPPED! ${FULLLANG} is not supported"
    exit 0
}

case "${FULLLANG}" in
    "AIX_Ja_JP.IBM-943"|\
    "AIX_ja_JP.IBM-eucJP"|\
    "AIX_JA_JP.UTF-8"|\
    "AIX_ko_KR.IBM-eucKR"|\
    "AIX_KO_KR.UTF-8"|\
    "Linux_ja_JP.UTF-8"|\
    "Linux_ko_KR.UTF-8") setData ;;
    "AIX_ja_JP.UTF-8"|\
    "AIX_ko_KR.UTF-8") FULLLANG=${FULLLANG}.s
                       setData ;;
    *) showMessage ;;
esac     









