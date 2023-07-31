# Jenkins Machine Labelling Schema

As our Jenkins build environment expands and we incorporate more machines, it becomes imperative to establish a labelling scheme that provides flexibility and improves machine management. This labelling schema enables us to categorize machines and their attributes efficiently, facilitating the addition of new labels and preventing duplication. Additionally, it helps prevent jobs from running on unintended machines, allows for increased flexibility in pipeline scripts, and expedites the process of identifying failures based on machine attributes.

## Schema Overview

The labelling schema is divided into three top-level roots: `hw`(hardware), `sw`(software), and `ci`(continous integration). Each root corresponds to specific attributes of the machines.

## Hardware Labels (hw)

The `hw` root is used to define labels related to the hardware attributes of the machines in the Jenkins build environment.

- **hw.platform:** Specifies the hardware platform on which the machine runs. Examples include `zlinux`, `xlinux`, `plinux`, `windows`, `aix`, `zos`, and `osx`.
- **hw.arch:** Indicates the hardware architecture of the machine. Examples include `s390`, `ppc`, and `x86`.
- **hw.endian:** Specifies the endianness of the machine. For example, `le` represents little-endian, and `be` represents big-endian.
- **hw.bits:** Indicates the number of bits the machine supports, such as `64` or `32`.
- **hw.physical.cpu:** Specifies the number of physical CPUs on the machine.
- **hw.disk:** Indicates the size of the workspace disk on the machine in GB.
- **hw.memory:** Specifies the size of the memory on the machine in GB.

## Software Labels (sw)
The sw root deals with labels related to the software attributes of the machines in the Jenkins build environment.

- **sw.os:** Specifies the operating system running on the machine. This label indicates the operating system distribution and version. Examples include `rhel` (Red Hat Enterprise Linux), ubuntu (Ubuntu Linux), `sles` (SUSE Linux Enterprise Server), `aix` (IBM AIX), `osx` (Apple macOS), `windows` (Microsoft Windows), and `zos` (IBM z/OS). For operating systems with multiple versions, version numbers may be appended, such as rhel.6, ubuntu.16, and windows.10.
- **sw.tool:** Indicates various software tools installed on the machines. This label is used to identify specific software packages or tools that are essential for Jenkins jobs and builds. Examples include `gcc.xx` (different versions of the GNU Compiler Collection), `docker.xx` (Docker container runtime), `hypervisor.kvm` (Kernel-based Virtual Machine), and more.

## Continuous Integration Labels (ci)
The `ci` root is used to define labels related to the role of machines in the continuous integration process and the sponsors or organizations responsible for specific machines.

- **ci.role:** Specifies the role of the machine in the continuous integration process. This label indicates the specific function or task that the machine performs in the CI pipeline. Examples include `perf` (performance testing), `compile` (compilation tasks), test (general testing), and `test.jck` (Java Compatibility Kit testing).
- **ci.sponsor:** Identifies the sponsor or organization responsible for a specific machine. This label helps attribute ownership and responsibilities for maintaining and managing the machine. Examples include `ci.sponsor.ibm`, `ci.sponsor.ljc` (London Java Community), `ci.sponsor.joyent`, and more.

## Usage Guidelines

When creating or configuring Jenkins pipeline scripts, use the appropriate labels from the schema to specify the machines on which the jobs should run. For example, if a job requires a machine with Ubuntu 16.04 and 64-bit architecture, you can use labels like `sw.os.ubuntu.16`, `hw.bits.64`, and `x86_64`.

To maintain consistency and avoid conflicts, please adhere to this labelling schema when adding new labels or modifying existing ones. Feel free to contribute to this schema as we encounter new requirements, but remember to document any changes made for better understanding and future reference.

## Contributions
Contributions to this labelling schema are welcome. As we encounter new requirements or use cases, we may consider adding new labels to the schema. However, it's crucial to document any changes made for clarity and to ensure proper communication among team members.
