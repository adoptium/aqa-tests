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
#
use Test::Simple tests => 5;
use FindBin;

$base = $FindBin::Bin."/";
print "base ".$base."\n";
$jar = "-cp ".$base."locale_matching.jar";

my @list=(
"LocaleFilterTest1",
"LocaleFilterTest2",
"LocaleFilterTest3",
"LocaleLookupTest1",
"LocaleLookupTest2"
);

foreach my $target(@list){
    my $flag = false;
    system($ENV{'JAVA_BIN'}."/java ".$jar." ".$target." > ".$target.".log 2>&1");
    open(DATA, "< ".$target.".log");
    while (my $line = <DATA>) {
        chomp($line);
        if ($line =~ /Passed/) {
            $flag=true;
        }
    }
    close(DATA);
    ok( $flag eq true, $target);
}

