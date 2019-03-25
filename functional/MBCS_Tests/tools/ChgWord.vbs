rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem      https://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

Option Explicit

Function RegExpReplace(str1, reg, str2)
  Dim regEx
  Set regEx = New RegExp
  regEx.Pattern = reg
  regEx.Global = True
  RegExpReplace = regEx.Replace(str1, str2)
End Function

If WScript.Arguments.Count < 3 Then
  WScript.StdErr.Write "Usage: " + RegExpReplace(WScript.FullName, "^.*\\", "") + " " + WScript.ScriptName + " OldString NewString File [NewFile]"
  WScript.Quit(-1)
End if

Const ForReading = 1, ForWriting = 2
Dim fso, inFile, outFile, oldfile
Set fso = WScript.CreateObject("Scripting.FileSystemObject")
oldfile = WScript.Arguments.Item(2)

If (fso.FileExists(oldfile) = False) Then
  WScript.StdErr.Write "File " + oldfile + " does not exist"
  WScript.Quit(-1)
End If

Set inFile = fso.OpenTextFile(oldfile, ForReading)
If WScript.Arguments.Count = 4 Then
  Set outFile = fso.OpenTextFile(WScript.Arguments.Item(3), ForWriting, True)
Else
  Set outFile = WScript.StdOut
End If
outFile.Write(Replace(infile.ReadAll, WScript.Arguments.Item(0), WScript.Arguments.Item(1)))
inFile.Close
outFile.Close
