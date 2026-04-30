# AQA Test Pipeline Configuration

## Overview

This directory contains JSON configuration files that define test execution parameters for different variants (temurin, openj9, ibm) and build types (nightly, weekly, release).

## Purpose

The configuration files allow you to:
1. **Decouple test configuration from build pipeline** - Test parameters are defined in JSON files instead of hardcoded in build pipeline
2. **Centralize test settings** - All test-related configuration in one place
3. **Support flexible test execution** - Run tests independently from builds using `aqaTestPipeline.groovy`
4. **Enable variant-specific customization** - Different settings for different JDK variants

## Directory Structure

```
config/
├── README.md (this file)
├── temurin/
│   ├── nightly/
│   │   ├── default.json
│   │   └── jdk<version>.json (optional, version-specific overrides)
│   ├── weekly/
│   │   └── default.json
│   └── release/
│       └── default.json
├── openj9/
│   ├── nightly/
│   │   ├── default.json
│   │   └── jdk8.json
│   ├── weekly/
│   │   └── default.json
│   └── release/
│       └── default.json
└── ibm/
    └── (similar structure)
```

## Configuration File Format

### Basic Structure

```json
[
    {
        "TEST_FLAG": "NONE",
        "PLATFORM_TARGETS": [
            { "aarch64_linux": "defaultTestTargets" },
            { "x86-64_linux": "defaultTestTargets" }
        ],
        "GLOBAL_BUILD_CONFIG": {
            // Global settings applied to all platforms/targets
        },
        "TARGET_SPECIFIC_CONFIG": {
            // Settings specific to test targets (functional, openjdk, jck, etc.)
        },
        "PLATFORM_SPECIFIC_CONFIG": {
            // Settings specific to platform+target combinations
        }
    }
]
```

### Configuration Sections

#### 1. TEST_FLAG
Specifies special test flags (e.g., "FIPS140_2", "FIPS140_3_OpenJCEPlusFIPS", or "NONE")

#### 2. PLATFORM_TARGETS
List of platform-target pairs:
```json
"PLATFORM_TARGETS": [
    { "aarch64_linux": "defaultTestTargets" },
    { "x86-64_windows": "sanity.functional,sanity.openjdk" }
]
```

Supported platforms:
- `aarch64_linux`, `aarch64_mac`
- `x86-64_linux`, `x86-64_mac`, `x86-64_windows`
- `ppc64_aix`, `ppc64le_linux`
- `s390x_linux`

Target values:
- `defaultTestTargets` - Expands to full test suite
- `defaultFipsTestTargets` - FIPS-specific tests
- Comma-separated list: `"sanity.functional,extended.openjdk,sanity.jck"`

#### 3. GLOBAL_BUILD_CONFIG
Parameters applied to all test jobs unless overridden:

```json
"GLOBAL_BUILD_CONFIG": {
    "JDK_REPO": "https://github.com/adoptium/temurin-build",
    "JDK_BRANCH": "master",
    "OPENJ9_BRANCH": "",
    "VENDOR_TEST_REPOS": "",
    "VENDOR_TEST_BRANCHES": "",
    "VENDOR_TEST_DIRS": "",
    "LABEL": "",
    "LABEL_ADDITION": "",
    "KEEP_REPORTDIR": false,
    "PARALLEL": "Dynamic",
    "NUM_MACHINES": "3",
    "TEST_TIME": "120",
    "USE_TESTENV_PROPERTIES": false,
    "ADOPTOPENJDK_BRANCH": "master",
    "ACTIVE_NODE_TIMEOUT": "1",
    "DYNAMIC_COMPILE": false,
    "RERUN_FAILURE": true,
    "RERUN_ITERATIONS": "1",
    "BUILD_LIST": "",
    "TIME_LIMIT": "10",
    "ADDITIONAL_TEST_PARAMS": {}
}
```

**Parameter Descriptions:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `JDK_REPO` | JDK source repository URL | `"https://github.com/adoptium/temurin-build"` |
| `JDK_BRANCH` | JDK source branch | `"master"` |
| `OPENJ9_BRANCH` | OpenJ9 branch (for openj9 variant) | `"master"` |
| `VENDOR_TEST_REPOS` | Vendor-specific test repository | `"git@github.ibm.com:runtimes/test.git"` |
| `VENDOR_TEST_BRANCHES` | Vendor test branch | `"master"` |
| `VENDOR_TEST_DIRS` | Vendor test directories | `"functional"` |
| `LABEL` | Jenkins node label requirement | `"ci.role.test.repro"` |
| `LABEL_ADDITION` | Additional node labels | `"sw.tool.docker"` |
| `KEEP_REPORTDIR` | Keep test report directory | `true` or `false` |
| `PARALLEL` | Parallel execution mode | `"Dynamic"`, `"None"`, or number |
| `NUM_MACHINES` | Number of parallel machines | `"3"` |
| `TEST_TIME` | Expected test completion time (mins) | `"120"` |
| `USE_TESTENV_PROPERTIES` | Use test environment properties | `true` or `false` |
| `ADOPTOPENJDK_BRANCH` | AQA tests branch | `"master"` |
| `ACTIVE_NODE_TIMEOUT` | Node timeout (hours) | `"1"` |
| `DYNAMIC_COMPILE` | Compile tests dynamically | `true` or `false` |
| `RERUN_FAILURE` | Rerun failed tests | `true` or `false` |
| `RERUN_ITERATIONS` | Number of rerun attempts | `"1"`, `"3"` |
| `BUILD_LIST` | Specific test builds to run | `"functional/OpenJcePlusTests"` |
| `TIME_LIMIT` | Job time limit (hours) | `"10"` |
| `ADDITIONAL_TEST_PARAMS` | Custom parameters | `{"CUSTOM_PARAM": "value"}` |

#### 4. TARGET_SPECIFIC_CONFIG
Override settings for specific test targets:

```json
"TARGET_SPECIFIC_CONFIG": {
    "functional": {
        "DYNAMIC_COMPILE": true,
        "KEEP_REPORTDIR": true,
        "PARALLEL": "None"
    },
    "openjdk": {
        "KEEP_REPORTDIR": true
    },
    "jck": {
        "KEEP_REPORTDIR": true,
        "VENDOR_TEST_REPOS": "git@github.ibm.com:runtimes/jck.git",
        "VENDOR_TEST_BRANCHES": "main",
        "VENDOR_TEST_DIRS": "jck"
    },
    "dev.openjdk": {
        "LABEL_ADDITION": "sw.tool.docker"
    },
    "special.system": {
        "LABEL": "ci.role.test.repro",
        "VENDOR_TEST_DIRS": "/test/system"
    }
}
```

Target matching:
- Exact match: `"dev.openjdk"` matches only `dev.openjdk`
- Partial match: `"functional"` matches `sanity.functional`, `extended.functional`, etc.

#### 5. PLATFORM_SPECIFIC_CONFIG
Override settings for specific platform+target combinations:

```json
"PLATFORM_SPECIFIC_CONFIG": {
    "special.system": {
        "x86-64_linux": {
            "LABEL": "ci.role.test.repro&&hw.arch.x86&&sw.os.linux"
        },
        "x86-64_mac": {
            "LABEL": "ci.role.test.repro&&hw.arch.aarch64&&(sw.os.osx||sw.os.mac)"
        },
        "aarch64_linux": {
            "LABEL": "ci.role.test.repro&&hw.arch.aarch64&&sw.os.linux"
        }
    }
}
```

## Configuration Hierarchy

Parameters are applied in this order (later overrides earlier):

1. **GLOBAL_BUILD_CONFIG** - Base configuration
2. **TARGET_SPECIFIC_CONFIG** - Target-specific overrides
3. **PLATFORM_SPECIFIC_CONFIG** - Platform+target specific overrides
4. **Pipeline Parameters** - Runtime parameter overrides

Example:
```
GLOBAL: KEEP_REPORTDIR=false
TARGET (functional): KEEP_REPORTDIR=true
PLATFORM (x86-64_linux + functional): (no override)
PIPELINE PARAM: (no override)
→ Final value: KEEP_REPORTDIR=true
```

## Usage

### 1. Standalone Test Execution

Trigger `aqaTestPipeline` job with parameters:
```groovy
build job: 'AQA_Test_Pipeline',
    parameters: [
        string(name: 'JDK_VERSIONS', value: '11'),
        string(name: 'VARIANT', value: 'temurin'),
        string(name: 'BUILD_TYPE', value: 'nightly'),
        string(name: 'PLATFORMS', value: 'x86-64_linux'),
        string(name: 'SDK_RESOURCE', value: 'releases')
    ]
```

### 2. Test Existing Build

```groovy
build job: 'AQA_Test_Pipeline',
    parameters: [
        string(name: 'JDK_VERSIONS', value: '11'),
        string(name: 'VARIANT', value: 'temurin'),
        string(name: 'BUILD_TYPE', value: 'nightly'),
        string(name: 'PLATFORMS', value: 'x86-64_linux'),
        string(name: 'SDK_RESOURCE', value: 'upstream'),
        string(name: 'UPSTREAM_JOB_NAME', value: 'Build-JDK11-linux-x64'),
        string(name: 'UPSTREAM_JOB_NUMBER', value: '123')
    ]
```

### 3. Custom Configuration via JSON Parameter

```groovy
def customConfig = [
    [
        TEST_FLAG: "NONE",
        PLATFORM_TARGETS: [
            ["x86-64_linux": "sanity.functional,sanity.openjdk"]
        ],
        GLOBAL_BUILD_CONFIG: [
            JDK_REPO: "https://github.com/custom/repo",
            PARALLEL: "None"
        ]
    ]
]

build job: 'AQA_Test_Pipeline',
    parameters: [
        string(name: 'JDK_VERSIONS', value: '11'),
        string(name: 'VARIANT', value: 'temurin'),
        string(name: 'BUILD_TYPE', value: 'nightly'),
        string(name: 'CONFIG_JSON', value: groovy.json.JsonOutput.toJson(customConfig))
    ]
```

## Migration from Build Pipeline

### Before (in openjdk_build_pipeline.groovy):
```groovy
// Tests triggered inline with build
runAQATests(testStages)
```

### After (trigger independent test pipeline):
```groovy
// Build completes, then optionally trigger tests
if (enableTests) {
    build job: 'AQA_Test_Pipeline',
        propagate: false,  // Don't fail build if tests fail
        wait: false,       // Don't wait for tests
        parameters: [
            string(name: 'JDK_VERSIONS', value: buildConfig.JAVA_TO_BUILD),
            string(name: 'VARIANT', value: buildConfig.VARIANT),
            string(name: 'BUILD_TYPE', value: 'nightly'),
            string(name: 'PLATFORMS', value: "${buildConfig.ARCHITECTURE}_${buildConfig.TARGET_OS}"),
            string(name: 'SDK_RESOURCE', value: 'upstream'),
            string(name: 'UPSTREAM_JOB_NAME', value: env.JOB_NAME),
            string(name: 'UPSTREAM_JOB_NUMBER', value: env.BUILD_NUMBER)
        ]
}
```

## Benefits

1. **Decoupling**: Build and test are independent
2. **Flexibility**: Easy to modify test parameters
3. **Reusability**: Run tests on any build
4. **Maintainability**: Centralized configuration
5. **Reliability**: Test failures don't fail builds

## Examples

See `temurin/nightly/default.json` for a complete example configuration.