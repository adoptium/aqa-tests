#!/usr/bin/perl
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

undef @out;
$_ = <>;
chomp;
$_=(split(/:/,$_))[0];
@b = split(/\./,$_);
splice(@b,1,0,"") if $#b == 1;
$min = $b[2];
$max = $b[2];
$b[2]+=1;
while(<>) {
  chomp;
  $_=(split(/:/,$_))[0];
  @a = split(/\./,$_);
  splice(@a,1,0,"") if $#a == 1;
  if (join('.',@a) eq join('.',@b)) {
    $max+=1;
  } else {
    push @out,"$b[1]:$min..$max";
    $min=$a[2];
    $max=$a[2];
  }
  $a[2]+=1;
  @b=@a;
}
push @out,"$b[1]:$min..$max";
print join(',',@out)."\n";
