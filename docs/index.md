---
layout: home
title: Home
nav_order: 1
---

<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<p align="center">
  <a href="https://adoptium.net/aqavit">
    <img src="https://adoptium.net/images/aqavit-light.png" alt="AQAvit Logo" width="250">
  </a>
</p>

# AQAvit Documentation

Welcome to the documentation for the **Eclipse AQAvit** project — the quality assurance toolkit for OpenJDK builds distributed by the [Eclipse Adoptium](https://adoptium.net) project.

---

## Explanation

| Document | Description |
|----------|-------------|
| [AQAvit Manifesto](pages/explanations/Manifesto) | The guiding principles of AQAvit |
| [Scope](pages/explanations/Scope) | What AQAvit covers |
| [Layered Design](pages/explanations/LayeredDesign) | The 3-Layer Cake architecture |

---

## How-to Guides

| Document | Description |
|----------|-------------|
| [User Guide](pages/userGuide) | How to run AQA tests on your machine or CI |
| [Run AQA GitHub Action](pages/howto/RunAqa) | Trigger test runs via PR comments |
| [Contribute a Test Suite](pages/howto/first-contrib) | Step-by-step first contribution guide |
| [Creating OpenJDK Test Defects](pages/howto/Guidance-for-Creating-OpenJDK-Test-Defects) | Bug reporting guidance |
| [Triage Guide](pages/howto/triage/Triage) | How to triage test failures |
| [Triage Checklist](pages/howto/triage/TriageChecklist) | Quick-reference checklist for triage |
| [How to Triage an Adoptium Release](pages/howto/triage/HowToTriageanAdoptiumRelease) | Release triage walkthrough |

---

## Reference

| Document | Description |
|----------|-------------|
| [Prerequisites](pages/reference/Prerequisites) | Required tools and setup |
| [AQAvit Terminology](pages/reference/AQAvit-terminology) | Common terms used in AQAvit |
| [How OpenJDK Tests Map to AQAvit Targets](pages/How-OpenJDK-tests-map-to-AQAvit-test-targets) | Mapping guide |
| [Jenkins Features Reference](pages/reference/JenkinFeatures) | Configure Grinder jobs on Jenkins |
| [Label Schema](pages/reference/LabelSchema) | Jenkins machine labelling schema |

---

## Tutorials

| Document | Description |
|----------|-------------|
| _Coming soon_ | Tutorials will be added here |

---

## About AQAvit

AQAvit (Adoptium Quality Assurance vitality) is the quality assurance program used to verify OpenJDK builds. It runs a broad set of tests — functional, performance, security, and third-party application tests — to ensure that every Temurin release meets enterprise-grade quality standards.

- **Source repository:** [adoptium/aqa-tests](https://github.com/adoptium/aqa-tests)
- **CI dashboard:** [ci.adoptium.net](https://ci.adoptium.net)
- **Test results:** [trss.adoptium.net](https://trss.adoptium.net)
- **Slack:** [#testing channel](https://adoptium.slack.com/archives/C5219G28G)
