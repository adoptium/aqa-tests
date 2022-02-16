/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class FixFile {
  public static void main(String[] args) throws Exception {
    if (args.length < 4) System.exit(1);
    String s = new String(Files.readAllBytes(Paths.get(args[0])), Charset.defaultCharset());
    String[] arrayString = GenerateTestSource.generateArrayString(Arrays.copyOfRange(args, 3, args.length), "ChildClass", 5);
    Files.write(Paths.get("expected_result.txt"),
      s.replaceAll(args[1], args[2])
       .replaceAll("STRING0", arrayString[0])
       .replaceAll("STRING1", arrayString[1])
       .replaceAll("STRING2", arrayString[2])
       .replaceAll("STRING3", arrayString[3])
       .replaceAll("STRING4", arrayString[4]).getBytes());
  }
}
