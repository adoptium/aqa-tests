# Run AQA GitHub Action Documentation

## Usage

On any Pull Request (PR), make a comment starting with the keywords `run aqa` followed by any [arguments](#arguments) to start a build.

Example:
```
run aqa --sdk_resource nightly --build_list openjdk --target sanity.openjdk --jdk_version 8 11 --jdk_impl hotspot --platform x86-64_linux 
```

## Arguments

Most arguments are similar to their [Jenkins Grinder](https://ci.adoptopenjdk.net/job/Grinder) counterparts.

All arguments allow more than one parameter. The parameters of each argument will be used to create a matrix job that will test every combination of the parameters.

### --help

This argument accepts no values. When this argument is supplied, a comment will be created with a link to this documentation. All other arguments will be ignored and no builds will be started.

This argument is equivalently invoked by `-h`.

### --sdk_resource

Supported values are:

- `nightly`
- `releases`
- `customized`

`nightly` and `releases` pull the latest from AdoptOpenJDK.

If `nightly` / `releases` is specified, the values of [`--jdk_version`](#--jdk_version) and [`--jdk_impl`](#--jdk_impl) are used to determine the JDK version and implementation to pull.

If `customized` is specified, the SDK archive is downloaded from the URL provided to the argument [`--customized_sdk_url`](#--customized_sdk_url) and is unpacked according to the archive extension provided to the argument [`--archive_extension`](#--archive_extension). The value of[`--jdk_version`](#--jdk_version) should be set accordingly since it is used to cache JDKs which are used multiple times during the workflow.

### --customized_sdk_url

A URL to an SDK archive. The archive will be unpacked according to the archive extension provided to the argument [`--archive_extension`](#--archive_extension).

This argument is only used when [`--sdk_resource`](#--sdk_resource) is `customized`.

### --archive_extension

Supported values are:

- `.zip`
- `.tar`
- `.7z`

If [`--customized_sdk_url`](#--customized_sdk_url) downloads a zip file, specify `.zip`.

If [`--customized_sdk_url`](#--customized_sdk_url) downloads a tar or tar.gz file, specify `.tar`.

If [`--customized_sdk_url`](#--customized_sdk_url) downloads a 7z file, specify `.7z`.

This argument is only used when [`--sdk_resource`](#--sdk_resource) is `customized`.

The default value is `.tar`.

### --build_list

The test category / directory.

Supported values are:

- `openjdk`
- `functional`
- `system`
- `perf`
- `external`

The default value is `openjdk`.

### --target

The test target to execute. Can be a specific testcase name or different test level under build_list.

e.g. `_jdk_math` under the `openjdk` build_list.

The default value is `_jdk_math`.

### --platform

The platform to run the build on.

Can be the name of any [GitHub-hosted runner](https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners#supported-runners-and-hardware-resources) (e.g. `ubuntu-latest`) or one of the following:

- `x86-64_linux`
- `x86-64_mac`
- `x86-64_windows`

Each of the above `x86-64_*` options map to `ubuntu-latest`, `macos-latest`, and `windows-latest` GitHub-hosted runners respectively.

The default value is `ubuntu-latest`.

### --jdk_version

The Java version that tests are running against.

Supported values are: 8, 9, 10, 11, 12, 13, ...

The default value is `8`.

### --jdk_impl

JVM implementation.

Supported values are:

- `hotspot`
- `openj9`

The default value is `openj9`.

### --openjdk_testrepo

GitHub repository and branch of the openjdk-tests to use.

The format is `<repository>:<branch>`.

The default value is `AdoptOpenJDK/openjdk-tests:master`.

This option is unavailable in `openjdk-tests` repositories because it will always use the head repo and branch of the PR instead.

### --tkg_repo

GitHub repository and branch of the TestKitGen (TKG) to use.

The format is `<repository>:<branch>`.

The default value is `adoptium/TKG:master`.

This option is unavailable in `TKG` repositories because it will always use the head repo and branch of the PR instead.

## Status Reports

After every successful invocation of `run aqa`, a bot will respond to you with a comment containing the list of build parameters it will run against, and provide a hyperlinked run ID that can be followed to view the status of the workflow.

If the invocation of `run aqa` was unsuccessful due to missing or invalid arguments, the bot will instead respond with a comment containing an appropriate error message.

Once the builds are completed, the bot will create a new comment notifying you of the workflow status, and provide another hyperlinked run ID which can be followed to view more details of the builds that occurred in the workflow.
