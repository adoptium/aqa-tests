#!/bin/bash
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

if [ $# = 5 ]; then
 PROG=$1
 TYPE=$2
 LOC=$3
 OPATCH=$4
 NPATCH=$5
else
 echo $0 Program ProviderType Locale OldPatch NewPatch
 exit 1
fi

FILE=${PROG}-${TYPE}.${NPATCH}
diff expected_$PROG-$LOC-$TYPE.log $PROG-$LOC-$TYPE.log | sed -e 's/\//\\&/g' -ne '/^> /p' | awk '{print "s/^" substr($0,3) "$/'${FILE}'." NR ":" substr($0,3) "/"}' > cldr.sed

perl src/cldr.pl $PROG-$TYPE.$OPATCH.patch src/CLDR11-$LOC.properties > cldr.file

diff -e expected_$PROG-$LOC-$TYPE.log $PROG-$LOC-$TYPE.log > cldr.ed
cat <<EOF >> cldr.ed
w
q
EOF
ed cldr.file < cldr.ed > /dev/null

sed -f cldr.sed cldr.file > cldr.tmp

diff expected_$PROG-$LOC-$TYPE.log $PROG-$LOC-$TYPE.log | grep '^> ' | awk '{print "'${FILE}'." NR ":" substr($0,3)}' | native2ascii
printf "%s" "$PROG-$TYPE.$NPATCH.patch="
perl src/gen-patch-data.pl cldr.tmp
rm -f cldr.sed cldr.file cldr.ed cldr.tmp
