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
use JSON;

sub getDataFromService {
	my %vars;
	my ($url, $key) =  @_;
	my $curl='curl --silent --max-time 120 ' . $url;
	my $jsonData = qx{$curl};
	if ($? != 0) {
		print "Failed to execute curl with error code: $?.\n";
	}
	my $data = from_json($jsonData);
	foreach my $element (@{$data}) {
		$vars{ $element->{$key} } = $element;
	}
	return \%vars;
}

1;