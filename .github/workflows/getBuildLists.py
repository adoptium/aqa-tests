import sys
import json


def main():
    print(sys.argv)
    result = []
    for argument in sys.argv:
        if (argument.startswith('perf/')):
            result.append('perf')
        elif (argument.startswith('system/')):
            result.append('system')
        elif (argument.startswith('functional/')):
            result.append('functional')
        elif (argument.startswith('openjdk/')):
            result.append('openjdk')
    if not result:
        result.append('skip')
    print('::set-output name=build_lists::{}'.format(json.dumps(result)))


if __name__ == "__main__":
    main()
