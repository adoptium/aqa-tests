#!/usr/bin/perl
$patch = shift(@ARGV);

while(<>) {
  chomp;
  if (/^([^=:]*)[=:](.*)$/) {
    $L{$1}=$2;
  }
} 

@a = split(/,/,$L{$patch});
$k = $patch;
$k =~ s/\..*$//;
$cnt = 1;
foreach $w (@a) {
print ">>>".$w."\n";
  if ($w =~ /^(.*):(\d+)\.\.(\d+)$/) {
    $b1 = $1;
    $b2 = $2;
    $b3 = $3;
    $key = $k;
    $key = $k.".".$b1 if $b1 ne "";
    for($v = $b2; $v <= $b3; $v+=1) {
      printf("%4d ",$cnt++);
      $keyv = $key.".".$v;
      print $keyv.":".$L{$keyv}."\n";
    }
  }
}
