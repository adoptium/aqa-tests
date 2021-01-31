import argparse
import json

def map_platforms(platforms):
  """ Takes in a list of platforms and translates Grinder platorms to corresponding GitHub-hosted runners.
      This function both modifies and returns the 'platforms' argument.
  """

  platform_map = {
    'x86-64_windows': 'windows-latest',
    'x86-64_mac': 'mac-latest',
    'x86-64_linux': 'ubuntu-latest'
  }

  for i, platform in enumerate(platforms):
    if platform in platform_map:
      platforms[i] = platform_map[platform]

  return platforms

def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()
    action = subparsers.add_parser('action', add_help=False)
    # TODO: Fetch the valid choices for each parameter from somewhere instead of hard-coding them?
    action.add_argument('--sdk_resource', default='nightly', choices=['nightly', 'releases'], nargs='+')
    action.add_argument('--build_list', required=True, choices=['openjdk', 'functional', 'system', 'perf', 'external'], nargs='+')
    action.add_argument('--target', required=True, nargs='+')
    action.add_argument('--platform', required=True, nargs='+')
    action.add_argument('--jdk_version', required=True, nargs='+')
    action.add_argument('--jdk_impl', required=True, nargs='+')
    args = parser.parse_args()

    output = {
      'sdk_resource': args.sdk_resource,
      'build_list': args.build_list,
      'target': args.target,
      'platform': map_platforms(args.platform),
      'jdk_version': args.jdk_version,
      'jdk_impl': args.jdk_impl
    }
    # Set parameters as output: As JSON, and each item individually
    print('::set-output name=build_parameters::{}'.format(json.dumps(output)))
    for key, value in output.items():
      print('::set-output name={}::{}'.format(key, value))

if __name__ == "__main__":
    main()
