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

# Perl doesn't seem to have any sensible built in way to display
# a timestamp formatted HH:mm:ss.SSS (hour:mins:secs.millis)

# need to get the gmt offset because Liberty logs use the local timezone
use Time::Local;
@t = localtime(time);
$gmt_offset_in_ms = 1000*(timegm(@t) - timelocal(@t));

use Time::HiRes;

$epochMillis = int(1000*Time::HiRes::time)+$gmt_offset_in_ms;
$millis = $epochMillis % 1000;
$secs	= ( $epochMillis / 1000 ) % 60;
$mins	= ( $epochMillis / ( 1000 * 60 ) ) % 60;
$hours	= ( $epochMillis / ( 1000 * 60 * 60 ) ) % 24;
printf "%02i:%02i:%02i.%03i", $hours, $mins, $secs, $millis;