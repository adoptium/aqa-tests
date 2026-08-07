# JCK Test Configuration

This directory contains JCK-specific test configurations for Temurin builds. JCK tests are triggered separately from regular tests using the `remoteTriggerTemurinJCK()` function when `BUILD_TYPE == 'RELAY'`.

## Directory Structure

```
jck/
├── README.md           # This file
├── default.json        # Base JCK configuration for JDK 24+ (latest default)
├── jdk8.json          # JDK 8 specific overrides (if needed)
├── jdk11.json         # JDK 11 specific overrides (if needed)
├── jdk17.json         # JDK 17 specific overrides (if needed)
└── jdk21.json         # JDK 21 and earlier overrides
```

**Note:** `default.json` contains settings for JDK 24+ (the latest/current default). Older JDK versions (8, 11, 17, 21) use override files only if their settings differ from the default.

## Configuration Hierarchy

JCK configurations are loaded with the following priority (later overrides earlier):

1. **default.json** - Base configuration for JDK 24+ (latest default)
2. **jdk${VERSION}.json** - Version-specific overrides for older versions (e.g., jdk21.json for JDK 8-21)

**Loading Logic:**
- JDK 24, 25, 26, etc. → Use `default.json` only
- JDK 21 and earlier → Use `default.json` + `jdk21.json` (jdk21.json overrides default)
- JDK 17 → Use `default.json` + `jdk17.json` (if jdk17.json exists, otherwise falls back to jdk21.json or default)

## Key Differences from Regular Test Configuration

### 1. Separate Trigger Mechanism
- Regular tests: Triggered by `generateJobs()` function
- JCK tests: Triggered by `remoteTriggerTemurinJCK()` when `BUILD_TYPE == 'RELAY'`

### 2. Build Types
- JCK tests only run for **weekly** and **release** builds
- No nightly JCK runs

### 3. Platform-Specific customJtx Paths
JCK tests require platform-specific `customJtx` paths defined in `PLATFORM_APPLICATION_OPTIONS`:

```json
"PLATFORM_APPLICATION_OPTIONS": {
    "x86-64_linux": "customJtx=/home/jenkins/jck_run/jdk${JDK_VERSION}/linux/temurin.jtx",
    "x86-64_mac": "customJtx=/Users/jenkins/jck_run/jdk${JDK_VERSION}/mac/temurin.jtx",
    "x86-64_windows": "customJtx=c:/Users/jenkins/jck_run/jdk${JDK_VERSION}/windows/temurin.jtx"
}
```

### 4. Disabled Platforms
Some platforms don't run certain JCK test targets:

```json
"DISABLED_PLATFORMS": {
    "dev.jck": ["ppc64_aix", "sparcv9_solaris", "x86-64_solaris"]
}
```

### 5. Test Targets
JCK has four test targets with different parallelization strategies:
- `sanity.jck` - Serial execution (1 machine)
- `extended.jck` - Parallel on primary platforms (x86-64_linux, x86-64_windows, x86-64_mac)
- `special.jck` - Serial execution (1 machine)
- `dev.jck` - Serial execution, requires interactive label on Windows

## Configuration Sections

### GLOBAL_BUILD_CONFIG
Base settings applied to all JCK tests:
```json
"GLOBAL_BUILD_CONFIG": {
    "RERUN_ITERATIONS": "1",
    "RERUN_FAILURE": true,
    "KEEP_REPORTDIR": true,
    "AUTO_AQA_GEN": false,
    "SETUP_JCK_RUN": false
}
```

### TARGET_SPECIFIC_CONFIG
Settings for specific JCK test targets:
```json
"TARGET_SPECIFIC_CONFIG": {
    "sanity.jck": {
        "PARALLEL": "None",
        "NUM_MACHINES": "1"
    },
    "extended.jck": {
        "PARALLEL": "None",
        "NUM_MACHINES": "1"
    }
}
```

### PLATFORM_SPECIFIC_CONFIG
Platform and target-specific overrides:
```json
"PLATFORM_SPECIFIC_CONFIG": {
    "extended.jck": {
        "x86-64_linux": {
            "PARALLEL": "Dynamic",
            "NUM_MACHINES": "2"
        }
    },
    "dev.jck": {
        "x86-64_windows": {
            "LABEL_ADDITION": "ci.role.test.interactive"
        },
        "aarch64_windows": {
            "LABEL_ADDITION": "ci.role.test.interactive"
        }
    }
}
```

### PLATFORM_APPLICATION_OPTIONS
Platform-specific APPLICATION_OPTIONS (mainly for customJtx paths):
```json
"PLATFORM_APPLICATION_OPTIONS": {
    "x86-64_linux": "customJtx=/home/jenkins/jck_run/jdk${JDK_VERSION}/linux/temurin.jtx"
}
```

### PLATFORM_ADDITIONAL_TEST_LABELS
Additional labels required for specific platforms:
```json
"PLATFORM_ADDITIONAL_TEST_LABELS": {
    "aarch64_mac": "sw.os.osx.12"
}
```

### DISABLED_PLATFORMS
Platforms where specific test targets should not run:
```json
"DISABLED_PLATFORMS": {
    "dev.jck": ["ppc64_aix", "sparcv9_solaris", "x86-64_solaris"]
}
```

### JCK_GIT_REPO_TEMPLATE
Template for JCK Git repository URL:
```json
"JCK_GIT_REPO_TEMPLATE": "git@github.com:temurin-compliance/JCK${JDK_VERSION}-unzipped.git"
```

## Version-Specific Configurations

### JDK 21 and Earlier (jdk21.json)
JDK 21 and earlier versions do NOT need the `-Djava.awt.headless=false` option that was introduced in JDK 24 (see [JDK-8185862](https://bugs.openjdk.org/browse/JDK-8185862)):

```json
"PLATFORM_SPECIFIC_CONFIG": {
    "jck": {
        "x86-64_windows": {
            "ADDITIONAL_TEST_PARAMS": {
                "EXTRA_OPTIONS": "-Xmx512m"
            }
        }
    }
}
```

**Key Difference:**
- **JDK 24+** (default.json): `-Djava.awt.headless=false -Xmx512m` on Windows
- **JDK 21 and earlier** (jdk21.json): `-Xmx512m` only on Windows

## Usage in remoteTriggerTemurinJCK()

The `remoteTriggerTemurinJCK()` function should:

1. Load `jck/default.json`
2. Load version-specific override (e.g., `jck/jdk24.json`) if it exists
3. Merge configurations (version-specific overrides default)
4. For each platform and JDK version:
   - Check if platform is in DISABLED_PLATFORMS for the target
   - Get platform-specific APPLICATION_OPTIONS (customJtx path)
   - Get platform-specific ADDITIONAL_TEST_PARAMS
   - Get platform-specific LABEL and LABEL_ADDITION
   - Determine PARALLEL and NUM_MACHINES settings
   - Build parameter list and trigger test job

## Example Parameter Generation

For `extended.jck` on `x86-64_linux` with JDK 21:

```groovy
def paramList = [
    SDK_RESOURCE: 'customized',
    TARGETS: 'extended.jck',
    JCK_GIT_REPO: 'git@github.com:temurin-compliance/JCK21-unzipped.git',
    CUSTOMIZED_SDK_URL: "${sdkUrl}",
    JDK_VERSIONS: "21",
    PARALLEL: "Dynamic",                    // From PLATFORM_SPECIFIC_CONFIG
    NUM_MACHINES: "2",                      // From PLATFORM_SPECIFIC_CONFIG
    PLATFORMS: "x86-64_linux",
    APPLICATION_OPTIONS: "customJtx=/home/jenkins/jck_run/jdk21/linux/temurin.jtx",
    RERUN_ITERATIONS: "1",
    RERUN_FAILURE: "true"
]
```

## Migration from Hardcoded Groovy

All JCK-specific settings previously hardcoded in Groovy should now be in these JSON files:
- ✅ Parallel execution settings
- ✅ Platform-specific EXTRA_OPTIONS
- ✅ Platform-specific APPLICATION_OPTIONS
- ✅ customJtx paths
- ✅ Disabled platforms
- ✅ Interactive labels for Windows dev.jck
- ✅ Version-specific overrides (JDK 24+)

## Benefits

1. **Centralized Configuration** - All JCK settings in one place
2. **Version Management** - Easy to add/modify version-specific settings
3. **Platform Management** - Clear platform-specific configurations
4. **Maintainability** - No hardcoded values in Groovy
5. **Flexibility** - Easy to test configuration changes without code changes