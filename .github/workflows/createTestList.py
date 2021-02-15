import sys

def main():
    print(sys.argv)
    result = []
    print('::set-output name=test_targets_str::{}'.format(sys.argv[1]))

    


if __name__ == "__main__":
    main()
