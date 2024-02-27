#!/usr/bin/env bash
#
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

#!/usr/bin/env bash

# Check for whether the tag values of the JDKxx_BRANCH variable in the 
# testenv.properties file exists

teFile=$1
jdkVersion=$2
repoFromPropFile="JDK${jdkVersion}_REPO"
branchFromPropFile="JDK${jdkVersion}_BRANCH"

usage ()
{
	echo 'Usage : checkTags.sh testenv.properties JDK_VERSION'
}

getProperty() {
  grep "${1}" ${teFile} | cut -d'=' -f2
}

setProperty(){
  awk -v pat="^$1=" -v newval="$1=$2" '{ if ($0 ~ pat) print newval; else print $0; }' $teFile > $teFile.tmp
  mv $teFile.tmp $teFile
}

workingTag="$(getProperty ${branchFromPropFile})"
workingRepo="$(getProperty ${repoFromPropFile})"

git ls-remote --exit-code -t $workingRepo $workingTag
if [ "$?" -eq "2" ]; then
    # tag name does not exist, check if branch name exists
    branchExists=$(git ls-remote --heads $workingRepo ${workingTag})
    if [[ -z ${branchExists} ]]; then
      # strip out '-ga' from the tag name since it does not exist
      workingTagPrefix=${workingTag//"-ga"/""}

      # find the latest tag name with the same workingTagPrefix as the ga tag that does not currently exist
      latestTag=$(git ls-remote --refs --tags $workingRepo ${workingTagPrefix}\* | cut -d '/' -f 3 | grep -v "_adopt" | tail -n1 )
        if [[ $latestTag ]]; then
          echo "Override non-existent $workingTag with $branchFromPropFile=$latestTag and write to $teFile "
          export $branchFromPropFile=$latestTag
          setProperty $branchFromPropFile $latestTag 
        else
          echo "Unable to resolve the latest $branchFromPropFile tag from $repoFromPropFile"
          exit 1
        fi
      else
        echo "Use branch name $workingTag from $teFile"
        exit 0
    fi
else
  echo "Use tag name $workingTag from $teFile"
fi