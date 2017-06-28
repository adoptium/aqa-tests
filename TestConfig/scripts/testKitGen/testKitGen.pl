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

use Cwd;
use strict;
use warnings;
use lib "./makeGenTool";
require "mkgen.pl";
use File::Basename;
use File::Path qw/make_path/;
use Data::Dumper;
use feature 'say';

my $all            = 0;
my $projectRootDir = '';
my $modesxml       = "../../resources/modes.xml";
my $ottawacsv      = "../../resources/ottawa.csv";
my $graphSpecs     = '';
my $output         = '';
my @allSubsets = ( "SE80", "SE90" );

foreach my $argv (@ARGV) {
	if ( $argv =~ /^\-\-graphSpecs=/ ) {
		($graphSpecs) = $argv =~ /^\-\-graphSpecs=(.*)/;
	}
	elsif ( $argv =~ /^\-\-projectRootDir=/ ) {
		($projectRootDir) = $argv =~ /^\-\-projectRootDir=(.*)/;
	}
	elsif ( $argv =~ /^\-\-output=/ ) {
		($output) = $argv =~ /^\-\-output=(.*)/;
	}
	elsif ( $argv =~ /^\-\-modesXml=/) {
		($modesxml) = $argv =~ /^\-\-modesXml=(.*)/;
	}
	elsif ( $argv =~ /^\-\-ottawaCsv=/) {
		($ottawacsv) = $argv =~ /^\-\-ottawaCsv=(.*)/;
	}
	else {
		print
"This program will search projectRootDir provided and find/parse playlist.xml to generate \n"
		  . "makefile (per project)\n"
		  . "jvmTest.mk and specToPlat.mk - under TestConfig\n";
		print "Options:\n"
		  . "--graphSpecs=<specs>    Comma separated specs that the build will run on.\n"
		  . "--output=<path>         Path to output makefiles.\n"
		  . "--projectRootDir=<path> Root path for searching playlist.xml.\n"
		  . "--modesXml=<path>       Path to modes.xml file.\n"
		  . "                        If the modesXml is not provided, the program will try to find modes.xml under projectRootDir/TestConfig/resources.\n"
		  . "--ottawaCsv=<path>      Path to ottawa.csv file.\n"
  		  . "                        If the ottawaCsv is not provided, the program will try to find ottawa.csv under projectRootDir/TEST_Playlists/playlistgen.\n";
		die "Please specify valid options!";
	}
}

if ( !$projectRootDir ) {
	$projectRootDir = getcwd . "/../../..";
	if ( -e $projectRootDir ) {
		print
"projectRootDir is not provided. Set projectRootDir = $projectRootDir\n";
	}
	else {
		die
"Please specify a valid project directory using option \"--projectRootDir=\".";
	}
}

if ( !$graphSpecs ) {
	$graphSpecs =
	"aix_ppc,aix_ppc-64,aix_ppc-64_purec,aix_ppc_purec,mac_os,linux_390,linux_390-64,linux_390-64_cs,linux_390-64_purec,linux_390_purec,linux_arm,linux_ppc-64_le,linux_ppc-64_le_purec,linux_x86,linux_x86-64,linux_x86-64_purec,linux_x86_purec,win_x86,win_x86-64,win_x86-64_purec,win_x86_purec,zos_390,zos_390-64,zos_390-64_purec,zos_390_purec";
	print "graphSpecs is not provided. Set graphSpecs = $graphSpecs\n";
}

# run make file generator
my $tests = runmkgen( $projectRootDir, \@allSubsets, $output, $graphSpecs, $modesxml, $ottawacsv );

print "\nTEST AUTO GEN SUCCESSFUL\n";