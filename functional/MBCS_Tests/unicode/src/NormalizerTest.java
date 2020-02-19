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
import java.text.*;
import java.util.*;

public class NormalizerTest {
    public static void main(String[] args) throws Exception {
        String line = null;
        String[] data = new String[5];
        boolean flag1=false;

        String[] vals;
        if (args.length > 0) {
            vals = args;
        } else {
            String FS = System.getProperty("file.separator");
            String BASE = System.getenv("BASE");
            if (null == BASE) BASE = ".";
            long version = JavaVersion.getVersion();
            if (version >= 13000000L) {
                vals = new String[]{ BASE+FS+"NormalizationTest-12.1.0.txt" };
            } else if (version >= 12000000L) {
                vals = new String[]{ BASE+FS+"NormalizationTest-11.0.0.txt" };
            } else if (version >= 11000000L) {
                vals = new String[]{ BASE+FS+"NormalizationTest-10.0.0.txt" };
            } else {
                vals = new String[0];
            }
        }

        FileInputStream fis = new FileInputStream(vals[0]);
        InputStreamReader isr = new InputStreamReader(fis,"utf8");
        BufferedReader br = new BufferedReader(isr);
        int count = 0;
        int good = 0;
        int err = 0;

        System.out.println("Checking NormalizerTest");
        TreeSet <String>p1data = new TreeSet<String>();
        while ((line = br.readLine()) != null) {
            count ++;
            if(line.startsWith("@Part1")) {
                flag1 = true;
            }
            if(line.startsWith("@Part2")) {
                flag1 = false;
            }

            String[] n_data1 = line.split(";");
            if(n_data1.length < 5 ) {
                continue;
            }
            try {
                for (int i=0 ; i<5 ; i++ ) {
                    StringBuffer sb = new StringBuffer();
                    for (String s : n_data1[i].split(" ")) {
                        sb.append(Character.toChars(Integer.parseInt(s,16)));
                    }
                    data[i] = sb.toString();
                }
                if (flag1) {
                    p1data.add(data[0]);
                }

                if (data[1].equals(NFC(data[0])) &&
                        data[1].equals(NFC(data[1])) &&
                        data[1].equals(NFC(data[2]))) {
                    good ++;
                } else {
                    System.err.println("NFC Error(1) at Line:"+ count + "  "+ n_data1[1] + ":"
                            + translate(NFC(data[0]))+":"+ translate(NFC(data[1]))+":"+ translate(NFC(data[2])));
                    err ++;
                }

                if (data[3].equals(NFC(data[3])) &&
                        data[3].equals(NFC(data[4]))) {
                    good ++;
                }else {
                    System.err.println("NFC Error(3) at Line:"+ count + "  "+ n_data1[3] + ":"
                            + translate(NFC(data[3]))+":"+ translate(NFC(data[4])));
                    err ++;
                }

                if (data[2].equals(NFD(data[0])) &&
                        data[2].equals(NFD(data[1])) &&
                        data[2].equals(NFD(data[2]))) {
                    good ++;
                } else {
                    System.err.println("NFD Error(2) at Line:"+ count + "  "+ n_data1[2] + ":"
                            + translate(NFD(data[0]))+":"+ translate(NFD(data[1]))+":"+ translate(NFD(data[2])));
                    err ++;
                }

                if (data[4].equals(NFD(data[3])) &&
                        data[4].equals(NFD(data[3]))) {
                    good ++;
                } else {
                    System.err.println("NFD Error(4) at Line:"+ count + "  "+ n_data1[4] + ":"
                            + translate(NFD(data[3]))+":"+ translate(NFD(data[4])));
                    err ++;
                }

                if (data[3].equals(NFKC(data[0])) &&
                        data[3].equals(NFKC(data[1])) &&
                        data[3].equals(NFKC(data[2])) &&
                        data[3].equals(NFKC(data[3])) &&
                        data[3].equals(NFKC(data[4]))){
                    good ++;
                } else {
                    System.err.println("NFKC Error(3) at Line:"+ count + "  "+ n_data1[3] + ":"
                            + translate(NFKC(data[0]))+":"+ translate(NFKC(data[1]))+":"+ translate(NFKC(data[2]))
                            + ":"+ translate(NFKC(data[3])) + ":"+ translate(NFKC(data[4])));
                    err ++;
                }
                if (data[4].equals(NFKD(data[0])) &&
                        data[4].equals(NFKD(data[1])) &&
                        data[4].equals(NFKD(data[2])) &&
                        data[4].equals(NFKD(data[3])) &&
                        data[4].equals(NFKD(data[4]))){
                    good ++;
                } else {
                    System.err.println("NFKD Error(4) at Line:"+ count + "  "+ n_data1[4] + ":"
                            + translate(NFKD(data[0]))+":"+ translate(NFKD(data[1]))+":"+ translate(NFKD(data[2]))
                            + ":"+ translate(NFKD(data[3])) + ":"+ translate(NFKD(data[4])));
                    err ++;
                }
            } catch(NumberFormatException e) {
            }
        }

        br.close();
        isr.close();
        fis.close();

        System.out.println("  Phase1   Inspection times : " + (good+err));        
        if (err == 0) {
            System.out.println("  Phase1   Test is Passed.");
        } else {
            System.err.println("  Phase1   Error : " + err);
            System.err.println("  Phase1   Test is Failed.");
        }

        count = 0;
        err = 0;
        good = 0;
        for (int i=Character.MIN_CODE_POINT ; i<=Character.MAX_CODE_POINT ; i++) {
            if(Character.isDefined(i)) {
                count ++;
                String str1 = new String(Character.toChars(i));
                if (p1data.contains(str1)!=true) {
                    if (str1.equals(NFC(str1)) &&
                            str1.equals(NFD(str1)) &&
                            str1.equals(NFKC(str1)) &&
                            str1.equals(NFKD(str1))) {
                        good ++;
                    } else {
                        System.err.println(String.format("Error at %04X", i));
                        err ++;
                    }
                }
            }
        }
        System.out.println("  Phase2   Defined Unicode characters : " + count);
        System.out.println("  Phase2   Ignored characters :  " + (count-(good+err)));
        if (err == 0) {
            System.out.println("  Phase2   Test is Passed.");
        } else {
            System.err.println("  Phase2   Error : " + err);
            System.err.println("  Phase2   Test is Failed.");
        }
    }

    private static String NFC(String s) {
        return (Normalizer.normalize(s,Normalizer.Form.NFC));
    }

    private static String NFD(String s) {
        return (Normalizer.normalize(s,Normalizer.Form.NFD));
    }

    private static String NFKC(String s) {
        return (Normalizer.normalize(s,Normalizer.Form.NFKC));
    }
    private static String NFKD(String s) {
        return (Normalizer.normalize(s,Normalizer.Form.NFKD));
    }
    private static String translate(String s) {
        char[] a = s.toCharArray();
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<a.length ; i++){
            sb.append(String.format(" %04X", (int)a[i]));
        }
        return sb.toString().substring(1);
    }
}
