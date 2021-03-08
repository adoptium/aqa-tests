import argparse
import json
import sys

def map_platforms(platforms):
    """ Takes in a list of platforms and translates Grinder platorms to corresponding GitHub-hosted runners.
        This function both modifies and returns the 'platforms' argument.
    """
    
    platform_map = {
        'x86-64_windows': 'windows-latest',
        'x86-64_mac': 'macos-latest',
        'x86-64_linux': 'ubuntu-latest'
    }
    
    for i, platform in enumerate(platforms):
        if platform in platform_map:
            platforms[i] = platform_map[platform]
    
    return platforms

def underscore_targets(targets):
    """ Takes in a list of targets and prefixes them with an underscore if they do not have one.
    """
    result = []
    for target in targets:
        t = target
        if target[0] != '_':
            t = '_' + t
        result.append(t)
    return result

def main():

    # The keyword for this command.
    keyword = 'run aqa'
    keywords = keyword.split()

    # We assume that the first argument is/are the keyword(s).
    # e.g. sys.argv == ['action_argparse.py', 'run', 'aqa', ...]
    raw_args = sys.argv[1 + len(keywords):]

    # Strip leading and trailing whitespace. Remove empty arguments that may result after stripping.
    raw_args = list(filter(lambda empty: empty, map(lambda s: s.strip(), raw_args)))

    parser = argparse.ArgumentParser(prog=keyword, add_help=False)
    # Improvement: Automatically resolve the valid choices for each argument populate them below, rather than hard-coding choices.
    parser.add_argument('--sdk_resource', default=['nightly'], choices=['nightly', 'releases'], nargs='+')
    parser.add_argument('--build_list', default=['openjdk'], choices=['openjdk', 'functional', 'system', 'perf', 'external'], nargs='+')
    parser.add_argument('--target', default=['_jdk_math'], nargs='+')
    parser.add_argument('--platform', default=['x86-64_linux'], nargs='+')
    parser.add_argument('--jdk_version', default=['8'], nargs='+')
    parser.add_argument('--jdk_impl', default=['openj9'], choices=['hotspot', 'openj9'], nargs='+')
    args = vars(parser.parse_args(raw_args))
    # All args are lists of strings

    # Map grinder platform names to github runner names
    args['platform'] = map_platforms(args['platform'])

    # Underscore the targets if necessary
    args['target'] = underscore_targets(args['target'])

    # Output the arguments
    print('::set-output name=build_parameters::{}'.format(json.dumps(args)))
    for key, value in args.items():
        print('::set-output name={}::{}'.format(key, value))

if __name__ == "__main__":
    main()
