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

import java.nio.file.*;
import java.util.*;

public class MimeDecoder {
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && !args[0].equals(args[1])) {
            Files.write(Paths.get(args[1]),Base64.getMimeDecoder()
                 .decode(Files.readAllBytes(Paths.get(args[0]))));
        } else {
            System.err.println("Usage: java MimeDecoder srcfile targetfile");
        }
    }
}
