## Why AQAvit Uses the '3 Layer Cake' Architecture

The Adoptium Quality Assurance (AQA) process which is encapsulated within the [Eclipse AQAvit project](https://projects.eclipse.org/projects/adoptium.aqavit), plays a crucial role in ensuring the reliability, stability, and security of OpenJDK builds distributed by the [Eclipse Temurin project](https://projects.eclipse.org/projects/adoptium.temurin) (both AQAvit and Temurin are active projects under the top-level [Eclipse Adoptium project](https://projects.eclipse.org/projects/adoptium).  AQA employs a layered architectural design, affectionately called the '3 Layer Cake', consisting of - Test Result Summary Service ([TRSS](https://github.com/adoptium/aqa-test-tools/tree/master/TestResultSummaryService)), the CI Systems layer, and TestKitGen ([TKG](https://github.com/adoptium/TKG)). This documentation explores the rationale behind the adoption of the '3 Layer Cake' architecture and its significance in delivering high-quality Java runtimes.

### Overview of the '3 Layer Cake' Architecture:

![LayeredDesign_3LayerCake](../diagrams/LayeredDesign_3LayerCake.jpg)

The '3 Layer Cake' architecture in Adoptium AQA comprises three interconnected layers, each responsible for specific aspects of testing and build validation:

- **Test Result Summary Service (TRSS):** TRSS serves as the top layer and is responsible for monitoring multiple CI servers, generating graphical aggregate summaries, providing deep historical data, and offering search, sort, and filter capabilities. It also supports pluggable parsers, forming the basis for deep analytics and deep learning services.

- **The CI layer:** The middle layer is the Continuous Integration (CI) layer and can be occupied by one or more systems such as Jenkins, Tekton, Azure DevOps (AzDo), GitHub Actions, and others.  We use Jenkins heavily for daily and release work at the Adoptium project.  The CI layer is responsible for scheduling builds, distributing tests across multiple nodes and platforms, and providing basic GUI views of test results. They also enable basic forms of parallelization.  

We purposefully keep this layer with fewer enhancements or special features which allows us to be more portable and ensures we have the option to swap this layer out with different types of CI servers as needed.  It also means we do not have to duplicate functionality across the many different types of systems that can occupy this layer.

- **TaskKitGen (TKG):** The bottom layer, TKG, categorizes logically, generates test targets based on playlists (level/platform/version/implementation-specific), and executes tests via common command-line or make target patterns. TKG adds tests through auto-detected directories, standardizes exclusion, and dynamically generates test playlists for smart parallelization.

### The Rationale Behind '3 Layer Cake' Architecture:

The adoption of the '3 Layer Cake' architecture in Adoptium AQA is driven by several key reasons:

- **Modular Testing Approach:** The '3 Layer Cake' architecture provides a modular testing approach, dividing responsibilities among three layers. This modularity allows for easy maintenance, upgrades, and improvements of individual layers, promoting flexibility and adaptability to evolving testing needs.

- **Comprehensive Testing Coverage:** Each layer in the architecture addresses specific aspects of testing. TRSS focuses on summary services and deep analytics, CI Systems handle continuous integration and basic GUI views, and TKG is responsible for test categorization and execution. This layered approach ensures comprehensive testing coverage of OpenJDK builds.

- **Efficient Bug Resolution:** The architecture's layered design allows for efficient bug resolution. Issues identified in a specific layer can be isolated and addressed within that layer, streamlining the bug-fixing process and reducing turnaround time.

- **Scalability and Platform Flexibility:** The '3 Layer Cake' architecture can scale effortlessly to accommodate varying testing requirements and support multiple CI systems. Its platform flexibility ensures compatibility with different operating systems, architectures, and cloud environments, enhancing the build's portability.

- **Data-Driven Decisions:** The TRSS layer provides graphical aggregate summaries and deep historical data, enabling data-driven decisions in the AQA process. This data-driven approach allows the AQA team to monitor trends, identify patterns, and make informed decisions for further improvement.

### In Conclusion:
The layered design employed in the Eclipse AQAvit project is a strategic choice that enhances the reliability, scalability, and efficiency of the testing process for OpenJDK builds. By dividing responsibilities across the layers, AQAvit achieves comprehensive testing coverage in a broad set of environments.  By design, this approach to quality assurance guarantees agility and recoverability and is highly adaptive to new requirements. This architecture ensures the delivery of high-quality Java runtimes, reinforcing the Eclipse AQAvit project's commitment to excellence and building trust among developers and users worldwide.