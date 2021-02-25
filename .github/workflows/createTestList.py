import sys
import os
import ast
import xml.etree.ElementTree as ET

def getTestCaseNames(path):
    testCaseNames = []
    
    tree = ET.parse(f'{path}playlist.xml')
    root = tree.getroot()
    for child in root:
        testCaseNames.append(child[0].text)
    
    return testCaseNames


def getPlaylistPath(dirName):
    if dirName == 'openjdk':
        return './openjdk-tests/openjdk/'
    return ''

def main():
    
    sanity_format = "sanity.{}"
    formattedTests = []
    
    targetNames = ast.literal_eval(os.getenv('TARGET_LIST'))
    
    for argument in targetNames:
        formattedTests.append(sanity_format.format(argument))
        if argument == 'openjdk':
            playlistPath = getPlaylistPath(argument)
            testCaseNames = getTestCaseNames(playlistPath)
            
    allTests = ','.join(formattedTests)    
    testTargets = 'TESTLIST={}'.format(allTests)
    
    print(testTargets)
    print('::set-output name=test_targets_str::{}'.format(testTargets))

if __name__ == "__main__":
    main()
