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

## `exclude_parser.py`
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