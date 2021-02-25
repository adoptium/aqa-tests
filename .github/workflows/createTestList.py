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
    
    dirName = sys.argv[1])
    
    if dirName == 'openjdk' || dirName == 'system' || dirName == 'functional' || dirName == 'perf':
      playlistPath = getPlaylistPath(dirName)
      testCaseNames = getTestCaseNames(playlistPath)
            
      testTarget = 'TESTLIST={}'.format(allTests)

      print(testTarget)
      print('::set-output name=test_target_str::{}'.format(testTarget))


if __name__ == "__main__":
    main()
