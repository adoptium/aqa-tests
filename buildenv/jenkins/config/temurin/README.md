# Temurin Test Configuration

This directory contains JSON configuration files for Temurin test execution via `aqaTestPipeline.groovy`.

## Overview

The configuration has been migrated from `ci-jenkins-pipelines/pipelines/jobs/configurations/jdk*_pipeline_config.groovy` to centralize test parameters in the aqa-tests repository.

## Directory Structure

```
temurin/
├── nightly/
│   ├── default.json      # Default config for JDK 25+
│   └── jdk21.json        # JDK 21 specific config
├── release/
│   ├── default.json      # Default config for JDK 25+
│   └── jdk21.json        # JDK 21 specific config
├── weekly/
│   ├── default.json      # Default config for JDK 25+
│   └── jdk21.json        # JDK 21 specific config
└── README.md
```

## Configuration Structure

Each JSON file contains an array of configuration objects with the following structure:

### Top-Level Fields

- **TEST_FLAG**: Test flag identifier (e.g., "NONE", "FIPS140_2")
- **COMMENT**: Description of the configuration
- **PLATFORM_TARGETS**: Array of platform-to-target mappings

### Configuration Sections

#### 1. GLOBAL_BUILD_CONFIG
Global settings applied to all tests:
- `JDK_REPO`: JDK repository URL
- `JDK_BRANCH`: JDK branch to test against
- `OPENJ9_BRANCH`: OpenJ9 branch (if applicable)
- `VENDOR_TEST_REPOS`: Vendor-specific test repositories
- `VENDOR_TEST_BRANCHES`: Vendor test branches
- `VENDOR_TEST_DIRS`: Vendor test directories
- `LABEL`: Jenkins node label
- `LABEL_ADDITION`: Additional Jenkins node labels
- `KEEP_REPORTDIR`: Whether to keep test report directory
- `PARALLEL`: Parallelization mode ("None", "Dynamic")
- `NUM_MACHINES`: Number of machines for parallel execution
- `TEST_TIME`: Expected test completion time (minutes)
- `USE_TESTENV_PROPERTIES`: Use testenv properties
- `ADOPTOPENJDK_BRANCH`: AQA-tests branch
- `ACTIVE_NODE_TIMEOUT`: Node timeout (hours)
- `DYNAMIC_COMPILE`: Enable dynamic compilation
- `RERUN_FAILURE`: Rerun failed tests
- `RERUN_ITERATIONS`: Number of rerun iterations
- `BUILD_LIST`: Specific builds to test
- `TIME_LIMIT`: Test time limit (hours)
- `ADDITIONAL_TEST_PARAMS`: Additional parameters map

#### 2. TARGET_SPECIFIC_CONFIG
Settings for specific test targets (functional, openjdk, jck, perf, external, dev):
```json
"functional": {
    "DYNAMIC_COMPILE": true,
    "KEEP_REPORTDIR": true,
    "PARALLEL": "None"
}
```

#### 3. PLATFORM_SPECIFIC_CONFIG
Platform-specific overrides for specific targets:
```json
"special.system": {
    "x86-64_linux": {
        "LABEL": "ci.role.test.repro&&hw.arch.x86&&sw.os.linux"
    }
}
```

#### 4. PLATFORM_ADDITIONAL_TEST_LABELS
Additional Jenkins labels per platform:
```json
"x86-64_linux": "!sw.tool.glibc.2_12",
"ppc64_aix": "sw.os.aix.7_2TL5"
```

#### 5. PLATFORM_ADDITIONAL_TEST_PARAMS
Additional test parameters per platform:
```json
"x86-64_linux": {
    "CLOUD_PROVIDER": "azure"
},
"ppc64_aix": {
    "TIME_LIMIT": "30"
}
```

## Configuration Hierarchy

Parameters are applied in the following order (later overrides earlier):
1. GLOBAL_BUILD_CONFIG
2. TARGET_SPECIFIC_CONFIG (matches test target like "functional", "openjdk")
3. PLATFORM_SPECIFIC_CONFIG (matches specific platform for specific target)
4. PLATFORM_ADDITIONAL_TEST_LABELS (appended to LABEL_ADDITION)
5. PLATFORM_ADDITIONAL_TEST_PARAMS (merged into ADDITIONAL_TEST_PARAMS)
6. Pipeline parameters (highest priority)

## Platform Naming Convention

Platforms follow the format: `<arch>_<os>`

Examples:
- `x86-64_linux`
- `aarch64_mac`
- `ppc64_aix`
- `s390x_linux`
- `x86-64_alpine-linux`
- `riscv64_linux`
- `aarch64_windows`

## Test Target Naming Convention

Test targets follow the format: `<level>.<group>`

Levels:
- `sanity`: Quick smoke tests
- `extended`: More comprehensive tests
- `special`: Special configuration tests
- `dev`: Development/debugging tests

Groups:
- `functional`: Functional tests
- `openjdk`: OpenJDK test suite
- `system`: System tests
- `perf`: Performance tests
- `jck`: JCK compliance tests
- `external`: External project tests

Examples:
- `sanity.functional`
- `extended.openjdk`
- `special.system`
- `dev.functional`

## Default Test Targets

The `defaultTestTargets` variable is expanded based on VARIANT and BUILD_TYPE:

### Temurin Nightly
```
sanity.functional,extended.functional,special.openjdk,sanity.openjdk,extended.openjdk,sanity.perf,extended.perf,sanity.jck,sanity.system,extended.system
```

### Temurin Release/Weekly
```
sanity.functional,extended.functional,special.functional,special.openjdk,sanity.openjdk,extended.openjdk,sanity.system,extended.system,sanity.perf,extended.perf,sanity.jck,extended.jck,special.jck
```

## Version-Specific Configurations

### JDK 21 and Earlier
- Uses `sw.os.aix.7_2` for ppc64_aix (not 7_2TL5)
- No TIME_LIMIT override for ppc64_aix

### JDK 25+
- Uses `sw.os.aix.7_2TL5` for ppc64_aix
- TIME_LIMIT: 30 hours for ppc64_aix

## Migration from ci-jenkins-pipelines

The configuration was migrated from:
```
ci-jenkins-pipelines/pipelines/jobs/configurations/jdk*_pipeline_config.groovy
```

Key mappings:
- `test.weekly` → Test targets list
- `additionalTestLabels` → PLATFORM_ADDITIONAL_TEST_LABELS
- `additionalTestParams` → PLATFORM_ADDITIONAL_TEST_PARAMS

## Usage

The configuration is automatically loaded by `aqaTestPipeline.groovy` based on:
- `VARIANT` parameter (e.g., "temurin")
- `BUILD_TYPE` parameter (e.g., "nightly", "release", "weekly")
- `JDK_VERSION` parameter (e.g., "21", "25")

Example:
```groovy
// Loads: buildenv/jenkins/config/temurin/nightly/jdk21.json
VARIANT = "temurin"
BUILD_TYPE = "nightly"
JDK_VERSION = "21"
```

## Adding New Configurations

To add a new JDK version configuration:

1. Create version-specific file if needed:
   ```
   buildenv/jenkins/config/temurin/<build_type>/jdk<version>.json
   ```

2. Copy from `default.json` and modify as needed

3. Update `aqaTestPipeline.groovy` if special logic is required

## Testing Configuration Changes

After modifying configuration files:

1. Validate JSON syntax
2. Test with a single platform first
3. Verify parameter propagation in Jenkins job logs
4. Check that test jobs are triggered correctly

## Related Files

- `buildenv/jenkins/aqaTestPipeline.groovy` - Main pipeline that uses these configs
- `buildenv/jenkins/JenkinsfileBase` - Test job execution logic
- `buildenv/jenkins/testJobTemplate` - Test job template for auto-generation