import sys
import os
import ast
import xml.etree.ElementTree as ET

def getTestCaseName(path):
  
    tree = ET.parse(f'{path}playlist.xml')
    root = tree.getroot()
    if len(root):
      return root[0][0].text
    else:
        return ''

def getPlaylistPath(dirName):
    if dirName == 'openjdk':
        return './openjdk-tests/openjdk/'
    else if dirName == 'system':
        return './openjdk-tests/system/daaLoadTest/'
    else if dirName === 'functional':
        return './openjdk-tests/functional/SyntheticGCWorkload/'
    else if dirName == 'perf':
        return './openjdk-tests/perf/bumbleBench/'

def main():
    
    sanity_format = "sanity.{}"
    formattedTests = []
    
    targetNames = ast.literal_eval(os.getenv('TARGET_LIST'))
    
    for targetName in targetNames:
        formattedTests.append(sanity_format.format(targetName))
        if targetName == 'openjdk' || targetName == 'system' || targetName == 'functional' || targetName == 'perf':
            playlistPath = getPlaylistPath(targetName)
            testCaseNames = getTestCaseNames(playlistPath)
            
            testTargets = 'TESTLIST={}'.format(allTests)

            print(testTargets)
            print('::set-output name=test_targets_str::{}'.format(testTargets))

if __name__ == "__main__":
    main()
