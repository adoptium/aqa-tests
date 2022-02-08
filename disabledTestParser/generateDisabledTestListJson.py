import os
import json
import argparse
import re
import logging

logging.basicConfig(
    format="%(levelname)s - %(message)s"
)
LOG = logging.getLogger()

OS_EXCEPTIONS = {
    "macosx": "mac",
    "z/os": "zos",
    "sunos": "solaris",
}

ARCH_EXCEPTIONS = {
    "x64": "x86-64",
    "x86": "x86-32",
}


def transform_platform(os_arch_platform: str) -> str:
    """
    Transforms an "os-arch"-formatted platform into
    an "arch_os"-formatted platform, suitable for consumption by Jenkins.

    :param os_arch_platform: the "os-arch"-formatted platform
    :return: a Jenkins-suitable platform name
    """

    # only deal in lower case
    os_arch_platform = os_arch_platform.lower()

    # split over a dash
    # EXCEPT if it is preceded by "x86" (because of "x86-32" and "x86-64")
    # or if it is preceded by "alpine" (because of linux distros like "alpine-linux")
    split_pattern = re.compile(r"(?<!x86)(?<!alpine)-")
    split_list = split_pattern.split(os_arch_platform)

    if len(split_list) != 2:
        raise ValueError(f"Cannot split {os_arch_platform!r} over regex pattern {split_pattern.pattern!r}")

    os_name, arch_name = split_list

    # rename OS if needed
    if os_name in OS_EXCEPTIONS:
        os_name = OS_EXCEPTIONS[os_name]

    # rename Arch if needed
    if arch_name in ARCH_EXCEPTIONS:
        arch_name = ARCH_EXCEPTIONS[arch_name]

    return f"{arch_name}_{os_name}"


def get_jdk_version_and_impl(exclude_list_file):
    # As of now, the ProblemList*.txt files are named in the following format:
    # ProblemList_openjdk<JDK_VERSION>-<JDK_IMPL>.txt  # for JDK_IMPL = openj9 and sap
    # or
    # ProblemList_openjdk<JDK_VERSION>.txt              # for JDK_IMPL = hotspot
    temp = re.search(r'ProblemList_openjdk(\d*)-?(.*).txt', exclude_list_file)

    jdk_version = temp.group(1)
    jdk_impl = "hotspot" if temp.group(2) == '' else temp.group(2)
    return jdk_version, jdk_impl


def get_tests_from_exclude_file(exclude_list_file):
    # 'exclude tests' are lines that are not empty AND do not start with #
    with open(exclude_list_file, mode='r') as f:
        return [(ln_num, line) for ln_num, line in enumerate(f.readlines(), 1) if line.strip() not in '' and not line.strip().startswith("#")]


def resolve_platform(platform_string, line_number, exclude_list_file):
    revolved_platform_list = []
    list_of_unresolved_platform_names = [s.strip() for s in platform_string.split(",") if s.strip()]
    for plat in list_of_unresolved_platform_names:
        if plat == "generic-all":
            return "all"

        if "_" in plat:
            LOG.warning(f'{exclude_list_file}:{line_number} : '
                        f'assuming {plat!r} already formatted to ARCH_OS; continuing without transformation')
            revolved_platform_list.append(plat)
        else:
            revolved_platform_list.append(transform_platform(plat))

    resolved_platforms = set(revolved_platform_list)
    return ','.join(resolved_platforms)


def get_test_details(test, line_number, exclude_list_file):
    test_details_dict = {}
    test_tokens = test.split(maxsplit=2)  # platform list may include spaces; split from the left 2 times max
    test_details_dict["TARGET"] = "jdk_custom"
    test_details_dict["CUSTOM_TARGET"] = test_tokens[0]
    test_details_dict["ISSUE_TRACKER"] = test_tokens[1]
    test_details_dict["PLATFORM"] = resolve_platform(test_tokens[2], line_number, exclude_list_file)
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
            for line_number, test in test_list:
                test_details = get_test_details(test, line_number, exclude_list_file)
                test_details["JDK_VERSION"], test_details["JDK_IMPL"] = get_jdk_version_and_impl(exclude_list_file)
                problem_list_details.append(test_details)

    exclude_list_json = os.path.join(output_dir, "ProblemList.json")
    with open(exclude_list_json, mode='w') as f:
        f.write(json.dumps(problem_list_details, indent=4))

    print("Done")


if __name__ == '__main__':
    main()
