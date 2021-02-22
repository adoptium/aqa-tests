import sys
import json


def main():
    print(sys.argv)
    result = []
    # TODO: handle the change of buildenv dir
    flag = False
    for argument in sys.argv:
        if (argument.startswith('perf/') and ('perf' not in result)):
            result.append('perf')
        elif (argument.startswith('system/') and ('system' not in result)):
            result.append('system')
        elif (argument.startswith('functional/') and ('functional' not in result)):
            result.append('functional')
        elif (argument.startswith('openjdk/') and ('openjdk' not in result)):
            result.append('openjdk')
        if argument.startswith('buildenv/') and not flag:
            print('::set-output name=build_env::true')
            flag = True
    
    if not flag:
        print('::set-output name=build_env::false')
    
    if len(result) > 0:
        print('::set-output name=test_dirs_changed::true')
    else:
        print('::set-output name=test_dirs_changed::false')
        
    
    if not result:
        result.append('skip')
    print('::set-output name=build_lists::{}'.format(json.dumps(result)))


if __name__ == "__main__":
    main()
