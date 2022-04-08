<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[1]https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
-->

# Disabled Tests -- Tools and Scripts

Scripts which parse various formats of lists, containing disabled JDK tests, into a uniform JSON format. In addition, a script which augments, within the generated JSON files, each disabled test with the status of their associated issue.   


### Prerequisites

Python 3.8+

### Installation

```shell
pip install -r requirements.txt
```

### Running unittests

#### All tests
```shell
# in scripts/disabled_tests
python -m unittest discover tests
```

#### Individual tests
```shell
# in scripts/disabled_tests
python -m unittest tests/test_playlist_parser.py
```

#### (Optional) run with `pytest` for better error reporting

```shell
# in scripts/disabled_tests
python -m pytest tests/test_playlist_parser.py
```

## `exclude_parser.py`

Generate disabled test list JSON file from exclude/ProblemList*.txt files

### Usage

```
usage: exclude_parser.py [-h] [--exclude_dir EXCLUDE_DIR] [--json_out JSON_OUT] [--verbose]

Generate disabled test list JSON file from exclude/ProblemList*.txt files

optional arguments:
  -h, --help            show this help message and exit
  --exclude_dir EXCLUDE_DIR
                        Source directory containing ProblemList*.txt files. Defaults to lines passed from stdin
  --json_out JSON_OUT   Destination path of the generated JSON file. Defaults to printing to stdout
  --verbose, -v         Enable logging debug mode
```

#### Output to file
```shell
ls -1dq openjdk/excludes/* |
python scripts/disabled_tests/exclude_parser.py > problem_list.json
```

#### Output to file and save log
```shell
ls -1dq openjdk/excludes/* |
python scripts/disabled_tests/exclude_parser.py > problem_list.json 2> errors.log
```

#### Dry run to see errors and warnings
```shell
ls -1dq openjdk/excludes/* |
python scripts/disabled_tests/exclude_parser.py -v > /dev/null
```


## `playlist_parser.py`

Generate disabled test list JSON file from playlist.xml files

This script does not rely on [the XSD format](https://github.com/adoptium/TKG/blob/master/resources/playlist.xsd)
enforced on playlist files in the Adoptium repos. Instead, the parser looks for and fetches only the nodes that are
required in the JSON output. (see defined XML tags in each dataclass for nodes which are considered essential)

### Usage

```
usage: playlist_parser.py [-h] [--outfile OUTFILE] [--verbose]

Generate disabled test list JSON file from playlist.xml files

optional arguments:
  -h, --help            show this help message and exit
  --outfile OUTFILE, -o OUTFILE
                        Output file, defaults to stdout
  --verbose, -v         Enable info logging level, debug level if -vv
```

#### Query playlist.xml files excluding scripts directory and output to file
```shell
find . -name "playlist.xml" -not -path "scripts" |
python scripts/disabled_tests/playlist_parser.py > playlist_problem_list.json
```

#### Query aqa-tests and openj9 repo, output to file and save debug log
```shell
find ./aqa-tests ./openj9 -name "playlist.xml"
python scripts/disabled_tests/playlist_parser.py -vv > playlist_problem_list.json 2> debug.log
```

#### Dry run to see info, errors and warnings
```shell
find . -name "playlist.xml" |
python scripts/disabled_tests/playlist_parser.py -v > /dev/null
```


## `issue_status.py`

Fetch issue status for each disabled test

### Usage

```
usage: issue_status.py [-h] [--github-user USER] [--github-token TOKEN] [--max-workers MAX_WORKERS] [--infile INFILE] [--outfile OUTFILE] [--verbose]

Fetch issue status for each disabled test

optional arguments:
  -h, --help            show this help message and exit
  --github-user USER    GitHub User used for API [env: AQA_ISSUE_TRACKER_GITHUB_USER]
  --github-token TOKEN  GitHub Token used for API [env: AQA_ISSUE_TRACKER_GITHUB_TOKEN]
  --max-workers MAX_WORKERS
                        Maximum number of threads to spawn, default determined by ThreadPoolExecutor
  --infile INFILE, -i INFILE
                        Input file, defaults to stdin
  --outfile OUTFILE, -o OUTFILE
                        Output file, defaults to stdout
  --verbose, -v         Enable info logging level, debug level if -vv
```

#### Output to file pass GitHub token through an environment variable
```shell
# Environment variables:
# export AQA_ISSUE_TRACKER_GITHUB_USER=foobar
# export AQA_ISSUE_TRACKER_GITHUB_TOKEN=abcdefghijklmnopqrst
cat exclude_out.json |
python scripts/disabled_tests/issue_status.py > out.json
```

#### Passing GitHub token using CLI
```shell
cat exclude_out.json |
python scripts/disabled_tests/issue_status.py --github-user foobar --github-token abcdefghijklmnopqrst > out.json 2> errors.log
```

#### Dry run and debug
```shell
ls -1dq openjdk/excludes/ProblemList_openjdk13.txt |
python scripts/disabled_tests/exclude_parser.py |
python scripts/disabled_tests/issue_status.py --github-user foobar --github-token abcdefghijklmnopqrst -vv --max-workers 1 > /dev/null
```

## `issue_filter.py`

Filter issues stored in JSON files

### Usage

```
usage: issue_filter.py [-h] [--jdk-version jdk_version] [--jdk-implementation jdk_implementation] [--platform platform] [--infile INFILE] [--outfile OUTFILE] [--verbose]

Filter issues stored in JSON files

Filter expressions can be provided through (in ascending order of priority):
1. command-line switches
2. environment variables

Filter expressions can be of any 2 formats:
1. a comma-separated list of exact matches
   note: whitespace around commas is considered; case-insensitive
   e.g.
   - 11,17,18+
   - openJ9
   - aarch64_linux,Aarch64_macos
2. a regular expression, prefixed with 're:'
   note: be cautious of escaping rules on your shell when using a backslash '\'
   e.g.
   - re:1[4-8]
   - re:x86\-64.*
Note: an empty expression is equivalent to not specifying the filter at all
   e.g.
   --jdk-version="" is equivalent to not specifying a filter on jdk-version

optional arguments:
  -h, --help            show this help message and exit
  --jdk-version jdk_version
                        Filter for jdk-version [env: AQA_ISSUE_FILTER_JDK_VERSION]
  --jdk-implementation jdk_implementation
                        Filter for jdk-implementation [env: AQA_ISSUE_FILTER_JDK_IMPLEMENTATION]
  --platform platform   Filter for platform [env: AQA_ISSUE_FILTER_PLATFORM]
  --infile INFILE, -i INFILE
                        Input file, defaults to stdin
  --outfile OUTFILE, -o OUTFILE
                        Output file, defaults to stdout
  --verbose, -v         Enable info logging level, debug level if -vv
```

#### Filter for hotspot; logging in debug mode
```shell
cat 1.json |
python issue_filter.py --jdk-implementation "HotSpot" -vv  # case does not matter when using format #1
```

#### Filter for macOS on M1, versions 11 and 17 using env. variables; logging in info mode
```shell
# Environment variables:
# export AQA_ISSUE_FILTER_PLATFORM=aarch64_macos
# export AQA_ISSUE_FILTER_JDK_VERSION=11,17
python issue_filter.py --infile 1.json -v
```

#### Filter for openj9, versions 12 through 17, and any platform containing "all"
```shell
# Environment variables:
# export AQA_ISSUE_FILTER_JDK_VERSION=re:1[2-7]
cat 1.json |
python issue_filter.py --jdk-implementation "OpenJ9" --platform "re:.*all.*"
```
