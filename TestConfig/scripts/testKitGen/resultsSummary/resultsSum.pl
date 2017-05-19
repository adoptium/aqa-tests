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

use strict;
use warnings;
use Data::Dumper;
use feature 'say';

my $resultFile;
my $failuremkarg;
my $tapFile;
my $diagnostic = 'failure';

for (my $i = 0; $i < scalar(@ARGV); $i++) {
	my $arg = $ARGV[$i];
	if ($arg =~ /^\-\-failuremk=/) {
		($failuremkarg) = $arg =~ /^\-\-failuremk=(.*)/;
	} elsif ($arg =~ /^\-\-resultFile=/) {
		($resultFile) = $arg =~ /^\-\-resultFile=(.*)/;
	} elsif ($arg =~ /^\-\-tapFile=/) {
		($tapFile) = $arg =~ /^\-\-tapFile=(.*)/;
	} elsif ($arg =~ /^\-\-diagnostic=/) {
		($diagnostic) = $arg =~ /^\-\-diagnostic=(.*)/;
	}
}

if (!$failuremkarg) {
	die "Please specify a vaild file path using --failuremk= option!";
}

my $failures = resultReporter();
failureMkGen($failuremkarg, $failures);

sub resultReporter {
	my $numOfFailed = 0;
	my $numOfPassed = 0;
	my $numOfSkipped = 0;
	my $numOfTotal = 0;
	my @passed;
	my @failed;
	my @skipped;
	my $tapString = '';
	
	open(my $fhIn, '<', $resultFile) or die "Cannot open file $resultFile!";

	print "\n\n";

	while ( my $result = <$fhIn> ) {
		if ($result =~ /===============================================\n/) {
			my $output = "  ---\n    output:\n      |\n";
			$output .= '        ' . $result;
			my $testName = '';
			while ( $result = <$fhIn> ) {
				if ($result =~ /Running test (.*) \.\.\.\n/) {
					$testName = $1;
				} elsif ($result =~ /_PASSED\n$/) {
					$result =~ s/_PASSED\n$//;
					push (@passed, $result);
					$numOfPassed++;
					$numOfTotal++;
					$tapString .= "ok " . $numOfTotal . " - " . $result . "\n";
					if ($diagnostic eq 'all') {
						if ($testName eq $result) {
							$output .= "  ...\n";
							$tapString .= $output;
						} else {
							print "warning: test description does not match test result, drop test diagnostic information!" if $tapFile;
						}
					}
					last;
				} elsif ($result =~ /_FAILED\n$/) {
					$result =~ s/_FAILED\n$//;
					push (@failed, $result);
					$numOfFailed++;
					$numOfTotal++;
					$tapString .= "not ok " . $numOfTotal . " - " . $result . "\n";
#					if (($diagnostic eq 'failure') || ($diagnostic eq 'all')) {
#						if ($testName eq $result) {
#							$output .= "  ...\n";
#							$tapString .= $output;
#						} else {
#							print "warning: test description does not match test result, drop test diagnostic information!" if $tapFile;
#						}
#					}
					last;	
				} elsif ($result =~ /_SKIPPED\n$/) {
					$result =~ s/_SKIPPED\n$//;
					push (@skipped, $result);
					$numOfSkipped++;
					$numOfTotal++;
					$tapString .= "ok " . $numOfTotal . " - " . $result . " # skip\n";
					last;
				}
				$output .= '        ' . $result;
			}
		}
	}

	#generate tap output
	if ($tapFile) {
		open(my $fhOut, '>', $tapFile) or die "Cannot open file $tapFile!";
		print $fhOut "1.." . $numOfTotal . "\n";
		print $fhOut $tapString;
		close $fhOut;
	}

	#generate console output
	print "TEST TARGETS SUMMARY\n";
	print "+++++++++++++++++++++++++++++++++++++++++++++++\n";
	print "TOTAL: $numOfTotal   PASS: $numOfPassed   FAIL: $numOfFailed   SKIP: $numOfSkipped\n";

	if ($numOfPassed != 0) {
		printTests(\@passed, "PASSED");
	}

	if ($numOfFailed == 0) {
		print "\n";
		print "ALL TESTS PASSED\n";
	} else {
		printTests(\@failed, "FAILED");
	}

	if ($numOfSkipped != 0) {
		printTests(\@skipped, "SKIPPED");
	}

	print "+++++++++++++++++++++++++++++++++++++++++++++++\n";

	close $fhIn;

	return \@failed;
}

sub printTests() {
	my ($tests, $tag) = @_;
	print "\n";
	print "$tag test targets:\n\t";
	print join("\n\t", @{$tests});
	print "\n";
}

sub failureMkGen {
	my $failureMkFile = $_[0];
	my $failureTargets = $_[1];
	if (@$failureTargets) {
		open( my $fhOut, '>', $failureMkFile ) or die "Cannot create make file $failureMkFile!";
		my $headerComments =
		"########################################################\n"
		. "# This is an auto generated file. Please do NOT modify!\n"
		. "########################################################\n"
		. "\n";
		print $fhOut $headerComments;
		print $fhOut "failed:";
		print $fhOut " \\\nrmResultFile";
		foreach my $target (@$failureTargets) {
			print $fhOut " \\\n" . $target;
		}
		print $fhOut " \\\nresultsSummary";
		close $fhOut;
	}
}
