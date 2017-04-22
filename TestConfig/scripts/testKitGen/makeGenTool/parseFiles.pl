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
use Text::CSV;

# specs that are specified in this array will be ignored.
# That is, all modes will not run on these specs even if these specs exist.
# This array should be mainly used for specs that we do not care any more
# but still exist in ottawa.csv (i.e., Linux PPC BE).
my @ignoredSpecs =
  qw(linux_x86-32_hrt linux_x86-64_cmprssptrs_gcnext linux_ppc_purec linux_ppc-64_purec linux_ppc-64_cmprssptrs_purec linux_x86-64_cmprssptrs_cloud);

sub getFileData {
	my ( $modesxml, $ottawacsv ) = @_;
	my $modes = parseModesXML($modesxml);
	my $data = parseOttawaCSV($ottawacsv, $modes);
	return $data;
}

sub parseOttawaCSV {
	my ( $file, $modes ) = @_;
	my %results          = ();
	my %specPlatMapping  = ();
	my @specs            = ();
	my @capabilities     = ();
	my $lineNum          = 0;
	my $fhIn;

	if (!open( $fhIn, '<', $file )) {
		say "Cannot open ottawa.csv. You can provide the file with option --ottawaCsv=<path>.";
		return;
	}
	my $csv          = Text::CSV->new( { sep_char => ',' } );
	while ( my $line = <$fhIn> ) {
		if ( $csv->parse($line) ) {
			my @fields = $csv->fields();

			# Since the spec line has an empty title, we cannot do string match. We assume the second line is spec.
			if ( $lineNum++ == 1 ) {
				push( @specs, @fields );
			}
			elsif ( $fields[0] =~ /^plat$/ ) {
				foreach my $i ( 1 .. $#fields ) {
					my %spm = ();
					$spm{'spec'} = $specs[$i];
					$spm{'platform'} = $fields[$i];
					$specPlatMapping{ $spm{'spec'} } = \%spm;
				}	
			}
			elsif ( $fields[0] =~ /^capabilities$/ ) {
				push( @capabilities, @fields );
			}
			elsif ( $fields[0] =~ /^variation:(.*)/ ) {
				my $modeNum = $1;

				# remove string Mode if it exists
				$modeNum =~ s/^Mode//;
				foreach my $mode (keys %{$modes}) {
					if ( $mode =~ /^$modeNum$/ ) {
						my @invalidSpecs = ();
						foreach my $j ( 1 .. $#fields ) {
							if ( $fields[$j] =~ /^no$/ ) {
								push( @invalidSpecs, $specs[$j] );
							}
						}

						# remove @ignoredSpecs from @invalidSpecs array
						my %ignoredSpecs = map +( $_ => 1 ),
						  @ignoredSpecs;
						@invalidSpecs = grep !$ignoredSpecs{$_},
						  @invalidSpecs;

						# if @invalidSpecs array is empty, set it to none
						# Otherwise, set $modes[$i]{'invalidSpecs'} = @invalidSpecs
						if (!@invalidSpecs) {
							push( @invalidSpecs, "none" );
						}
						#update invalidSpecs values
						$modes->{$mode}{'invalidSpecs'} = \@invalidSpecs;
						last;
					}
				}
			}
		}
		else {
			warn "Line could not be parsed: $line\n";
		}
	}
	close $fhIn;
	$results{'modes'} = $modes;
	$results{'specPlatMapping'} = \%specPlatMapping;
	return \%results;
}

sub parseModesXML {
	my %modes = ();
	my $file  = $_[0];
	my $fhIn;
	if (!open( $fhIn, '<', $file )) {
        	say "Cannot open modes.xml. You can provide the file with option --modeXml=<path>.";
        	return;
	}
	while ( my $line = <$fhIn> ) {

		# matching line <mode number="107-OSR">
		if ( $line =~ /(mode)\W*number.*"(.+?)"/ ) {
			my %mode = ();
			$mode{$1} = $2;

			my @keys = ( "description", "clArg" );
			my @args = ();

			# read next line in the file
			while ( my $nextLine = <$fhIn> ) {
				foreach my $i ( 0 .. $#keys ) {
					if ( $nextLine =~ /($keys[$i])>(.+?)(<\/$keys[$i])/ ) {
						my $value = $2;

						# clArg line - <clArg>-Xgcpolicy:optthruput</clArg>
						if ( $keys[$i] =~ /$keys[1]/ ) {
							push( @args, $value );
						}
						else {
							$mode{ $keys[$i] } = $value;
						}
						$mode{'invalidSpecs'} = undef;
					}
				}
				if ( $nextLine =~ /<\/mode/ ) {

					# add args array into mode hash
					$mode{ $keys[1] } = \@args;

					# break out the loop
					last;
				}
			}

			# add mode into modes hash
			$modes{ $mode{'mode'} } = \%mode;
		}
	}
	close $fhIn;
	return \%modes;
}
