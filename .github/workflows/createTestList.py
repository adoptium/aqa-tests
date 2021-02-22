import sys
import os
import ast

def main():
    
    sanity_format = "sanity.{}"
    formattedTests = []
    
    targetNames = ast.literal_eval(os.getenv('TARGET_LIST'))
    
    for argument in targetNames:
        formattedTests.append(sanity_format.format(argument))
    
    allTests = ','.join(formattedTests)    
    testTargets = 'TESTLIST={}'.format(allTests)
    
    print(testTargets)
    print('::set-output name=test_targets_str::{}'.format(testTargets))

if __name__ == "__main__":
    main()
