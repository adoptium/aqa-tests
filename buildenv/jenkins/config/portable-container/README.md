# Portable Container Config Files

This directory holds the `imageUploadMap.json` and `imagePullMap.json` configuration files
for each Portable Container Testing feature (e.g., CRIU, SCC).

## Purpose

Adding support for a new feature only requires:
1. Implementing the relevant tests in `aqa-tests/external/<feature>/`
2. Creating a new subdirectory here with `imageUploadMap.json` and `imagePullMap.json`

No changes to `openjdk_tests` are needed.

## Directory Structure

```
portable-container/
├── README.md           (this file)
├── criu/
│   ├── imageUploadMap.json
│   └── imagePullMap.json
└── scc/
    ├── imageUploadMap.json
    └── imagePullMap.json
```

## imageUploadMap.json Schema

```json
{
    "commonLabelBase": "<base Jenkins node label for this feature>",
    "buildList":       "<comma-separated BUILD_LIST value for the upload test job>",
    "target":          "<TARGET parameter value for the upload test job>",
    "excludeTargetOnPlatform": {
        "<platform>": "<substring to remove from 'target' when running on this platform>"
    },
    "platforms": {
        "<platform>": ["<labelSuffix1>", "<labelSuffix2>", ...]
    }
}
```

Each `labelSuffix` is appended to `commonLabelBase` (and any user-supplied `LABEL_ADDITION`)
with `&&` to form the full `LABEL_ADDITION` value for that machine.

## imagePullMap.json Schema

```json
{
    "commonLabelBase": "<base Jenkins node label for this feature>",
    "platforms": {
        "<platform>": ["<labelSuffix1>", "<labelSuffix2>", ...]
    }
}
```

The `imagePullMap.json` does not carry `target` or `buildList`; those are inherited
from the `imageUploadMap.json` of the same feature.

## Supported Platforms

| Platform key   | Architecture  |
|----------------|---------------|
| `x86-64_linux` | x86-64        |
| `aarch64_linux`| AArch64       |
| `s390x_linux`  | IBM Z (s390x) |
| `ppc64le_linux`| POWER (LE)    |

## Example — Adding a New Feature

Create `portable-container/myfeature/imageUploadMap.json`:
```json
{
    "commonLabelBase": "sw.tool.podman&&sw.tool.myfeature",
    "buildList": "external/myfeature",
    "target": "testList TESTLIST=disabled.myfeature_test",
    "platforms": {
        "x86-64_linux": ["sw.os.ubuntu.22&&hw.arch.x86.broadwell"]
    }
}
```

Create `portable-container/myfeature/imagePullMap.json`:
```json
{
    "commonLabelBase": "sw.tool.podman&&sw.tool.myfeature",
    "platforms": {
        "x86-64_linux": ["sw.os.ubuntu.22&&hw.arch.x86.broadwell"]
    }
}
```

The pipeline will automatically discover and run `myfeature` on the next execution.
