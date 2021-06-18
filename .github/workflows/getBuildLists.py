import sys
import json


def main():
    print(sys.argv)
    result = []
    # TODO: handle the change of buildenv dir
    for argument in sys.argv:
        if (argument.startswith('perf/') and ('perf' not in result)):
            result.append('perf')
        elif (argument.startswith('system/') and ('system' not in result)):
            result.append('system')
        elif (argument.startswith('functional/') and ('functional' not in result)):
            result.append('functional')
        elif (argument.startswith('openjdk/') and ('openjdk' not in result)):
            result.append('openjdk')

    if not result:
        result.append('skip')
    print('::set-output name=build_lists::{}'.format(json.dumps(result)))


if __name__ == "__main__":
    main()
