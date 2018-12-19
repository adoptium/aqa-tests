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

public class conv {
    public static void convert(String fin, String ein, String fout, String eout) {
        int counter = 0;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fin), ein));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout), eout)), true);
            String line;
            while (reader.ready()) {
                line = reader.readLine();
                writer.println(line);
                
                ++counter;
            }
            writer.close();
            reader.close();
        } catch (java.io.IOException e){
            System.err.println("I/O exception in line " + counter);
            e.printStackTrace();
        }
    }
    
    public static void main(java.lang.String[] args) {
        if (args.length < 4) {
            System.err.println("Usage :  conv <input_filename> <from_encoding> <output_filename> <destination_encoding>");
            System.exit(1);
        }
        conv.convert(args[0],args[1],args[2],args[3]);
    }
}
