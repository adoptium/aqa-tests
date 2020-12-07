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

$patch = shift(@ARGV);

while(<>) {
  chomp;
  if (/^([^=:]*)[=:](.*)$/) {
    $L{$1}=$2;
  }
} 

unless (defined($L{$patch})) {
  $pdate = (split(/\./,$patch))[1];
  foreach $i (reverse split(/,/,$L{'versions'})) {
    $tpatch = $patch;
    $tpatch =~ s/$pdate/$i/;
    if ($tpatch le $patch) {
      if (defined($L{$tpatch})) {
        $patch = $tpatch;
        print STDERR "Patch entry was changed to $patch\n";
        last;
      }
    }
  }
}

@a = split(/,/,$L{$patch});
$k = $patch;
$k =~ s/\..*$//;
$cnt = 1;
foreach $w (@a) {
#print ">>>".$w."\n";
  if ($w =~ /^(.*):(\d+)\.\.(\d+)$/) {
    $b1 = $1;
    $b2 = $2;
    $b3 = $3;
    $key = $k;
    $key = $k.".".$b1 if $b1 ne "";
    for($v = $b2; $v <= $b3; $v+=1) {
#      printf("%4d ",$cnt++);
      $keyv = $key.".".$v;
      print $keyv.":".$L{$keyv}."\n";
    }
  }
}
