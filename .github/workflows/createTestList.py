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
    elif dirName == 'system':
        return './openjdk-tests/system/daaLoadTest/'
    elif dirName == 'functional':
        return './openjdk-tests/functional/SyntheticGCWorkload/'
    elif dirName == 'perf':
        return './openjdk-tests/perf/bumbleBench/'
      
def getTestCaseNameStatic(dirName):
  if dirName == 'openjdk':
    return 'jdk_custom'
  elif dirName == 'system':
    return 'ClassLoadingTest'
  elif dirName == 'functional':
    return 'SyntheticGCWorkload_concurrentSlackAuto_1k_J9'
  elif dirName == 'perf':
    return 'dacapo-eclipse'

def main():
    
    dirName = sys.argv[1]
    
    if (dirName == 'openjdk') or (dirName == 'system') or (dirName == 'functional') or (dirName == 'perf'):
      
#       playlistPath = getPlaylistPath(dirName)
#       testCaseName = getTestCaseName(playlistPath)
      testCaseName = getTestCaseNameStatic(dirName)
            
      testTarget = 'TESTLIST={}'.format(testCaseName)

      print(testTarget)
      print('::set-output name=test_target_str::{}'.format(testTarget))


if __name__ == "__main__":
    main()
