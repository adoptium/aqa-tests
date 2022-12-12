#/bin/bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

source $(dirname "$0")/test_base_functions.sh
# This script is used as the new entrypoint for saved criu-restore-ready-with-jdk docker image
echo "Restore tests from Checkpoint"

echo "export GLIBC_TUNABLES=glibc.pthread.rseq=0:glibc.cpu.hwcaps=-XSAVEC,-XSAVE,-AVX2,-ERMS,-AVX,-AVX_Fast_Unaligned_Load";
export GLIBC_TUNABLES=glibc.pthread.rseq=0:glibc.cpu.hwcaps=-XSAVEC,-XSAVE,-AVX2,-ERMS,-AVX,-AVX_Fast_Unaligned_Load
echo "export LD_BIND_NOT=on";
export LD_BIND_NOT=on

checkpoint_folders="/aqa-tests/TKG/output_*/cmdLineTester_criu_keepCheckpoint*"
output_file="testOutput" # File "testOutput" is used to store all outputs
result_code=0

for checkpoint_folder in $checkpoint_folders
do
    cd $checkpoint_folder
    criu restore -D ./cpData --shell-job
    cur_test_name="$(basename ${checkpoint_folder})"
    result_restore=$(grep "Post-checkpoint" $output_file)
    if [[ $result_restore ]]; then
        echo "Test $cur_test_name passed"
    else
        echo "Test $cur_test_name failed"
        cat $output_file
        result_code=1
    fi
done

exit $result_code
