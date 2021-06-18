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
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;

public class UCompare {
    public static String readUData(String s, String csName)
        throws Exception {
        return Files.readAllLines(Paths.get(s), Charset.forName(csName))
            .stream()
            .collect(Collectors.joining(System.lineSeparator()));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println(
                "Usage: java UCompare file1 encoding1 file2 encoding2");
            System.exit(1);
        } else {
            System.exit(readUData(args[0], args[1])
                .equals(readUData(args[2], args[3])) ? 0 : 2);
        } 
    }
}
