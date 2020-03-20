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

$OS=$^O; #OS name
chomp($OS);
$SYSENC=`locale charmap`;
chomp($SYSENC);
$lang = $ENV{LANG};
$i = index($lang,".");
if ($i == -1) {
    $i = length($lang);
}
$lang = substr($lang, 0, $i);
$FULLLANG = $OS."_".$lang.".".$SYSENC;

undef %LOC;
foreach $l (
    "aix_Ja_JP.IBM-943","aix_ja_JP.IBM-eucJP","aix_JA_JP.UTF-8",
    "aix_ko_KR.IBM-eucKR","aix_KO_KR.UTF-8",
    "aix_zh_CN.IBM-eucCN","aix_Zh_CN.GB18030","aix_ZH_CN.UTF-8",
    "aix_zh_TW.IBM-eucTW","aix_Zh_TW.big5","aix_ZH_TW.UTF-8",
    "aix_ja_JP.UTF-8", "aix_ko_KR.UTF-8",
    "aix_zh_Hans_CN.UTF-8", "aix_zh_Hant_TW.UTF-8",
    "linux_ja_JP.UTF-8","linux_ko_KR.UTF-8","linux_zh_CN.UTF-8","linux_zh_TW.UTF-8")
{
    $LOC{$l} = "";
}

if (defined($LOC{$FULLLANG})) {
    $FULLLANG .= $LOC{$FULLLANG};
} else {
    for($i = 0; $i < 5; $i+=1) {
        ok(1 == 1,"skip");
    }
    print "SKIPPED! $FULLLANG is not supported.\n";
    exit(0);
}

unless (defined($ENV{'JAVA_BIN'})) {
    if (-f $ENV{'TEST_JDK_HOME'}."/jre/bin/java"){
        $ENV{'JAVA_BIN'} = $ENV{'TEST_JDK_HOME'}."/jre/bin"
    }else{
        $ENV{'JAVA_BIN'} = $ENV{'TEST_JDK_HOME'}."/bin"
    }
}

my @list=(
"LocaleFilterTest1",
"LocaleFilterTest2",
"LocaleFilterTest3",
"LocaleLookupTest1",
"LocaleLookupTest2"
);

foreach my $target(@list){
    my $flag = 0;
    system($ENV{'JAVA_BIN'}."/java ".$jar." ".$target." > ".$target.".log 2>&1");
    open(DATA, "< ".$target.".log");
    while (my $line = <DATA>) {
        chomp($line);
        if ($line =~ /Passed/) {
            $flag=1;
        }
    }
    close(DATA);
    ok( $flag == 1, $target);
}

