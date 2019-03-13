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

import java.io.*;

class CheckValidDataEnv {
    public static void main(String[] args) {
        if (args.length < 1) System.exit(1);
        String line = System.getenv(args[0]);
        char ch;
        StringBuffer valid = new StringBuffer();
        for(int i = 0; i < line.length(); i++) {
            ch = line.charAt(i);
            if (valid.length() == 0) {
                if (Character.isJavaIdentifierStart(ch)) valid.append(ch);
            } else {
                if (Character.isJavaIdentifierPart(ch)) valid.append(ch);
            }
        }
        System.out.println(valid.toString());
        System.exit(0);
    }
}
