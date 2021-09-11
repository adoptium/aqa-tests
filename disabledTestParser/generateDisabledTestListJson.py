import itertools
import os
import json
import argparse

platform_map = {
    "arm-linux": ["arm_linux"],
    "linux-aarch64": ["aarch64_linux"],
    "linux-arm": ["arm_linux"],
    "linux-ppc64le": ["ppc64le_linux"],
    "linux-s390x": ["s390x_linux"],
    "linux-x64": ["x86-64_linux"],
    "macosx-x64": ["x86-64_mac", "aarch64_mac"],
    "windows-x64": ["x86-64_windows"],
    "windows-x86": ["x86-64_windows", "x86-32_windows"],
    "x86-64_windows": ["x86-64_windows"],
    "z/OS-s390x": ["s390x_zos", "s390x_linux"]
}


def get_jdk_version_and_impl(exclude_list_file):
    # As of now, the ProblemList*.txt files are named in the following format:
    # ProblemList_openjdk<JDK_VERSION >-<JDK_IMPL>.txt  # for JDK_IMPL = openj9 and sap
    # or
    # ProblemList_openjdk<JDK_VERSION>.txt              # for JDK_IMPL = hotspot ?
    temp = exclude_list_file.replace("ProblemList_openjdk", "") \
        .replace(".txt", "") \
        .split("-")

    jdk_version = temp[0]
    jdk_impl = "hotspot" if len(temp) == 1 else temp[1]
    return jdk_version, jdk_impl


def get_tests_from_exclude_file(exclude_list_file):
    # 'exclude tests' are lines that are not empty AND do not start with #
    with open(exclude_list_file, mode='r') as f:
        return [line for line in f.readlines() if line.strip() not in '' and not line.strip().startswith("#")]


def resolve_platform(platform_string):
    revolved_platform_list = []
    list_of_unresolved_platform_names = [s.strip() for s in platform_string.split(",") if s.strip() not in '']
    for unresolved_platform_name in list_of_unresolved_platform_names:
        if unresolved_platform_name == "generic-all":
            return "all"
        elif unresolved_platform_name == "linux-all":
            revolved_platform_list.append([platform_map[s] for s in platform_map.keys() if "linux" in s])
        elif unresolved_platform_name == "macosx-all":
            revolved_platform_list.append([platform_map[s] for s in platform_map.keys() if "macosx" in s])
        elif unresolved_platform_name == "windows-all":
            revolved_platform_list.append([platform_map[s] for s in platform_map.keys() if "windows" in s])
        elif unresolved_platform_name == "aix-all":
            revolved_platform_list.append([["ppc32_aix", "ppc64_aix"]])
        else:
            if unresolved_platform_name not in platform_map:
                print(f"Could not resolve the '{unresolved_platform_name}' to any of the valid platform names")
                exit(3)

            revolved_platform_list.append([platform_map[unresolved_platform_name]])

    # flatten list of lists of lists to a set of unique values
    resolved_platforms = set(itertools.chain(*itertools.chain(*revolved_platform_list)))
    return ','.join(resolved_platforms)


def get_test_details(test):
    test_details_dict = {}
    test_tokens = test.split()
    test_details_dict["TARGET"] = "jdk_custom"
    test_details_dict["CUSTOM_TARGET"] = test_tokens[0]
    test_details_dict["GIT_ISSUE"] = test_tokens[1]
    test_details_dict["PLATFORM"] = resolve_platform(test_tokens[2])
    return test_details_dict


def main():
    parser = argparse.ArgumentParser(description="Generate disabled test list JSON file from "
                                                 "exclude/ProblemList*.txt files")
    parser.add_argument('--exclude_dir', type=str, help='directory containing ProblemList*.txt files')
    parser.add_argument('--json_out_dir', type=str, help='absolute path to place exclude JSON file')
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.realpath(__file__))
    # if the dir containing the exclude ProblemList*.txt is not passed, the attempt to use openjdk/excludes/ dir instead
    exclude_dir = args.exclude_dir if args.exclude_dir is not None else os.path.join(script_dir, "..", "openjdk", "excludes")
    # if output dir is not explicitly specified, dump the exclude list JSON in the `pwd`
    output_dir = args.json_out_dir if args.json_out_dir is not None else os.getcwd()

    if not os.path.exists(exclude_dir):
        print(f"Exclude directory '{exclude_dir}' does not exist")
        exit(3)

    if not os.path.exists(output_dir):
        print(f"Output directory '{output_dir}' does not exist")
        exit(3)

    print(f"Translating '{exclude_dir}/ProblemList*.txt' files to a single '{output_dir}/ProblemList.json'")
    problem_list_details = []
    for exclude_list_file in os.listdir(exclude_dir):
        test_list = get_tests_from_exclude_file(os.path.join(exclude_dir, exclude_list_file))
        if len(test_list) > 0:
            for test in test_list:
                test_details = get_test_details(test)
                test_details["JDK_VERSION"], test_details["JDK_IMPL"] = get_jdk_version_and_impl(exclude_list_file)
                problem_list_details.append(test_details)

    exclude_list_json = os.path.join(output_dir, "ProblemList.json")
    with open(exclude_list_json, mode='w') as f:
        f.write(json.dumps(problem_list_details, indent=4))

    print("Done")


if __name__ == '__main__':
    main()
