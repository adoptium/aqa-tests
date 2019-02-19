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

use Test::Simple tests => 35;
use File::Compare;

@a = ('DEFAULT','CLDR','JRE','SPI');
$langtag = $ENV{'LANGTAG'};

$prefix = 'expected_';

$t1 = 'DecimalFormatSymbolsTest-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (@a) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $prefix.$f) == 0, 'diff '.$f.' '.$prefix.$f);
}

$t1 = 'DateFormatSymbolsTest-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (@a) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $prefix.$f) == 0, 'diff '.$f.' '.$prefix.$f);
}

$t1 = 'DecimalStyleTest-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (@a) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $f2) == 0, 'diff '.$f.' '.$f2);
}

$t1 = 'CurrencyTest-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (@a) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $prefix.$f) == 0, 'diff '.$f.' '.$prefix.$f);
}

$t1 = 'LocaleTest-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (@a) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $prefix.$f) == 0, 'diff '.$f.' '.$prefix.$f);
}

$t1 = 'TimeZoneTestA-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (('DEFAULT','JRE','SPI')) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $f2) == 0, 'diff '.$f.' '.$f2);
}
$f = $t1.$langtag.'-CLDR.log';
ok(compare($f, $prefix.$f) == 0, 'diff '.$f.' '.$prefix.$f);

$t1 = 'TimeZoneTestB-';
$f1 = $t1.$langtag.'-DEFAULT.log';
$f2 = $t1.$langtag.'-CLDR,JRE.log';
ok(compare($f1, $f2) == 0, $f2);
foreach $s (@a) {
  $f = $t1.$langtag.'-'.$s.'.log';
  ok(compare($f, $prefix.$f) == 0, 'diff '.$f.' '.$prefix.$f);
}
