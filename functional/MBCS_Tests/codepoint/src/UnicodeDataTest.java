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

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

public class UnicodeDataTest {
    static Hashtable<Byte, String> charType = new Hashtable<Byte, String>();
    static Hashtable<Byte, String> charDirect = new Hashtable<Byte, String>();
    static {
        charType.put(Character.COMBINING_SPACING_MARK, "Mc");
        charType.put(Character.CONNECTOR_PUNCTUATION, "Pc");
        charType.put(Character.CONTROL, "Cc");
        charType.put(Character.CURRENCY_SYMBOL, "Sc");
        charType.put(Character.DASH_PUNCTUATION, "Pd");
        charType.put(Character.DECIMAL_DIGIT_NUMBER, "Nd");
        charType.put(Character.ENCLOSING_MARK, "Me");
        charType.put(Character.END_PUNCTUATION, "Pe");
        charType.put(Character.FINAL_QUOTE_PUNCTUATION, "Pf");
        charType.put(Character.FORMAT, "Cf");
        charType.put(Character.INITIAL_QUOTE_PUNCTUATION, "Pi");
        charType.put(Character.LETTER_NUMBER, "Nl");
        charType.put(Character.LINE_SEPARATOR, "Zl");
        charType.put(Character.LOWERCASE_LETTER, "Ll");
        charType.put(Character.MATH_SYMBOL, "Sm");
        charType.put(Character.MODIFIER_LETTER, "Lm");
        charType.put(Character.MODIFIER_SYMBOL, "Sk");
        charType.put(Character.NON_SPACING_MARK, "Mn");
        charType.put(Character.OTHER_LETTER, "Lo");
        charType.put(Character.OTHER_NUMBER, "No");
        charType.put(Character.OTHER_PUNCTUATION, "Po");
        charType.put(Character.OTHER_SYMBOL, "So");
        charType.put(Character.PARAGRAPH_SEPARATOR, "Zp");
        charType.put(Character.PRIVATE_USE, "Co");
        charType.put(Character.SPACE_SEPARATOR, "Zs");
        charType.put(Character.START_PUNCTUATION, "Ps");
        charType.put(Character.SURROGATE, "Cs");
        charType.put(Character.TITLECASE_LETTER, "Lt");
        charType.put(Character.UNASSIGNED, "Cn");
        charType.put(Character.UPPERCASE_LETTER, "Lu");

        charDirect.put(Character.DIRECTIONALITY_ARABIC_NUMBER, "AN");
        charDirect.put(Character.DIRECTIONALITY_BOUNDARY_NEUTRAL, "BN");
        charDirect.put(Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR, "CS");
        charDirect.put(Character.DIRECTIONALITY_EUROPEAN_NUMBER, "EN");
        charDirect.put(Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR, "ES");
        charDirect.put(Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR, "ET");
        charDirect.put(Character.DIRECTIONALITY_FIRST_STRONG_ISOLATE, "FSI");
        charDirect.put(Character.DIRECTIONALITY_LEFT_TO_RIGHT, "L");
        charDirect.put(Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING, "LRE");
        charDirect.put(Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE, "LRI");
        charDirect.put(Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE, "LRO");
        charDirect.put(Character.DIRECTIONALITY_NONSPACING_MARK, "NSM");
        charDirect.put(Character.DIRECTIONALITY_OTHER_NEUTRALS, "ON");
        charDirect.put(Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR, "B");
        charDirect.put(Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT, "PDF");
        charDirect.put(Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE, "PDI");
        charDirect.put(Character.DIRECTIONALITY_RIGHT_TO_LEFT, "R");
        charDirect.put(Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC, "AL");
        charDirect.put(Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING, "RLE");
        charDirect.put(Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE, "RLI");
        charDirect.put(Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE, "RLO");
        charDirect.put(Character.DIRECTIONALITY_SEGMENT_SEPARATOR, "S");
        charDirect.put(Character.DIRECTIONALITY_UNDEFINED, "");
        charDirect.put(Character.DIRECTIONALITY_WHITESPACE, "WS");
    }
    public static void main(String[] args) throws Exception {
        TreeSet<Integer> codePointTable = new TreeSet<Integer>();
        for (int codePoint = 0;
             codePoint <= Character.MAX_CODE_POINT;codePoint++){
            if (Character.isDefined(codePoint)){
                codePointTable.add(codePoint);
            }
        }
        String[] vals = null;
        long version = JavaVersion.getVersion();
        if (args.length > 0) {
            vals = args;
        } else {
            String FS = System.getProperty("file.separator");
            String BASE = System.getenv("BASE");
            if (null == BASE) {
                BASE = ".";
                if (version >= 12000000L) {
                    vals = new String[]{ BASE+FS+"UnicodeData-11.0.0.txt",
                                         BASE+FS+"UnicodeData-u32FF.txt" };
                } else if (version >= 11000003L) {
                    vals = new String[]{ BASE+FS+"UnicodeData-10.0.0.txt",
                                         BASE+FS+"UnicodeData-u32FF.txt" };
                } else if (version >= 11000000L) {
                    vals = new String[]{ BASE+FS+"UnicodeData-10.0.0.txt" };
                } else {
                    vals = new String[0];
                }
            } else {
                if (version >= 12000000L) {
                    vals = new String[]{ BASE+FS+"data"+FS+"UnicodeData-11.0.0.txt",
                                         BASE+FS+"data"+FS+"UnicodeData-u32FF.txt" };
                } else if (version >= 11000003L) {
                    vals = new String[]{ BASE+FS+"data"+FS+"UnicodeData-10.0.0.txt",
                                         BASE+FS+"data"+FS+"UnicodeData-u32FF.txt" };
                } else if (version >= 11000000L) {
                    vals = new String[]{ BASE+FS+"data"+FS+"UnicodeData-10.0.0.txt" };
                } else {
                    vals = new String[0];
                }
            }
        }

        for(String fname : vals) {
            System.out.println("--- Checking "+fname+" ---");
            BufferedReader reader = new BufferedReader(new FileReader(fname));
            while (reader.ready()) {
                String[] values = reader.readLine().split(";");
                int codePointStart = Integer.parseInt(values[0], 16);
                /* Following code support: http://www.unicode.org/versions/corrigendum8.html */
                /* Corrigendum #8: Bidi_Class Fix for U+070F SYRIAC ABBREVIATION MARK        */
                if (version < 12000000L) {
                    if (0x070F == codePointStart) values[4] = "AL";
                }
                StringWriter writer = new StringWriter ();
                PrintWriter out = new PrintWriter (writer);

                // codePointOf() test since 9
                String charName = "";
                TreeSet<Integer> codePointRange = new TreeSet<Integer>();
                codePointRange.add(codePointStart);
                if (values[1].endsWith(", First>")) {
                    if (!reader.ready()) break;
                    String[] valuesEnd = reader.readLine().split(";");
                    int codePointEnd = Integer.parseInt(valuesEnd[0], 16);
                    while((++codePointStart) <= codePointEnd) {
                        codePointRange.add(codePointStart);
                    }
                } else if (!values[1].equals("<control>") &&
                    !values[1].endsWith(", Last>") ){
                    charName = values[1];
                }else if (values[1].equals("<control>")){
                    // special cases; ref JDK-7071819
                   if (codePointStart == 0x07){
                        charName = "BEL";
                    }else if (codePointStart == 0x80){
                        charName = "PADDING CHARACTER";
                    }else if (codePointStart == 0x81){
                        charName = "HIGH OCTET PRESET";
                    }else if (codePointStart == 0x99){
                        charName = "SINGLE GRAPHIC CHARACTER INTRODUCER";
                    }else if (values.length > 10 &&
                              values[10].length() > 0){
		              // values[10] Unicode 1.0 Name
                        charName = values[10];
                    }
                }
              for (int codePoint : codePointRange) {
                if (charName.length() > 0){
                    int cpOf = Character.codePointOf(charName);
                    if (cpOf != codePoint){
                        out.format("codePointOf(): %x\n", cpOf);
                    }
                    int[] arry = {codePoint};
                    String str = new String(arry, 0, 1);
                    // check \x{xxxx} pattern
                    String regX = "\\x{" + Integer.toHexString(codePoint)
                                 + "}";
                    if (!Pattern.matches(regX, str)){
                        out.format("Pattern.matches(): %s\n", regX);
                    }
                    // check \N{Unicode Character Name} pattern
                    String regN = "\\N{" + charName + "}";
                    if (!Pattern.matches(regN, str)){
                        out.format("Pattern.matches(): %s\n", regN);
                    }
                }

                int num = Character.getNumericValue(codePoint);
                String numVal = num < 0 ? "" : Integer.toString(num);
                String vv = "";
                if (codePoint >= 0x41 && codePoint <= 0x5A) {
                    vv = Integer.toString(codePoint - 0x41 + 10);
                } else if (codePoint >= 0x61 && codePoint <= 0x7A) {
                    vv = Integer.toString(codePoint - 0x61 + 10);
                } else if (codePoint >= 0xFF21 && codePoint <= 0xFF3A) {
                    vv = Integer.toString(codePoint - 0xFF21 + 10);
                } else if (codePoint >= 0xFF41 && codePoint <= 0xFF5A) {
                    vv = Integer.toString(codePoint - 0xFF41 + 10);
                } else if (codePoint >= 0x16B60 && codePoint <= 0x16B61) {
                    // 10,000,000,000 & 1,000,000,000,000
                    // Over int max, 2,147,483,647
                    // "-2 if the character has a numeric value but the value
                    // can not be represented as a nonnegative int value;"
                    vv = "";
                } else if (values[8].indexOf("/") == -1 &&
                           values[8].indexOf("-") == -1) {
                    vv = values[8];
                }
                if (!vv.equals(numVal))
                    out.format("getNumericValue(): %d %s\n", num, vv);

                if (!values[2].equals(charType.get((byte)Character.getType(codePoint)))) {
                    out.format("getType(): %d %s %s\n", Character.getType(codePoint), charType.get(Character.getType(codePoint)), values[2]);
                }
                if (!values[4].equals(charDirect.get(Character.getDirectionality(codePoint))))
                    out.format("getDirectionality(): [%s] [%s]\n", charDirect.get(Character.getDirectionality(codePoint)),values[4]);
                if (!values[9].equalsIgnoreCase(Character.isMirrored(codePoint) ? "Y" : "N"))
                    out.println ("isMirrored(): " + Character.isMirrored (codePoint)+", "+values[9]);
                if (values.length > 12) {
                    if (Character.toUpperCase(codePoint) != codePoint) {
                        if (!values[12].equalsIgnoreCase(String.format("%04X", Character.toUpperCase(codePoint)))) {
                            out.format("[A]toUpperCase(): [%04X] [%s]\n", Character.toUpperCase (codePoint), values[12]);
                        }
                    }
                    if (values[12].isEmpty()) {
                        if (Character.isUpperCase(codePoint))
                            if (Character.toUpperCase(codePoint) != codePoint)
                                out.println ("isUpperCase(): " + Character.isUpperCase(codePoint));
                    } else {
                        if (!values[12].equalsIgnoreCase(String.format("%04X", Character.toUpperCase(codePoint)))) {
                            out.format("[B]toUpperCase(): [%04X] [%s]\n", Character.toUpperCase (codePoint), values[12]);
                        }
                    }
                }
                if (values.length > 13) {
                    if (Character.toLowerCase(codePoint) != codePoint) {
                        if (!values[13].equalsIgnoreCase(String.format("%04X", Character.toLowerCase(codePoint)))) {
                            out.format("[A]toLowerCase(): [%04X] [%s]\n", Character.toLowerCase(codePoint), values[13]);
                        }
                    }
                    if (values[13].isEmpty()) {
                        if (Character.isLowerCase(codePoint))
                            if (Character.toLowerCase(codePoint) != codePoint)
                                out.println ("isLowerCase(): " + Character.isLowerCase(codePoint));
                    } else {
                        if (!values[13].equalsIgnoreCase(String.format("%04X", Character.toLowerCase(codePoint)))) {
                            out.format("[B]toLowerCase(): [%04X] [%s]\n", Character.toLowerCase(codePoint), values[13]);
                        }
                    }
                }
                if (values.length > 14) {
                    if (Character.toTitleCase(codePoint) != codePoint) {
                        if (!values[14].equalsIgnoreCase(String.format("%04X", Character.toTitleCase(codePoint)))) {
                            out.format("[A]toTitleCase(): [%04X] [%s]\n", Character.toTitleCase(codePoint), values[14]);
                        }
                    }
                    if (values[14].isEmpty()) {
                        if (Character.isTitleCase(codePoint))
                            if (Character.toTitleCase(codePoint) != codePoint)
                                out.println ("isTitleCase(): " + Character.isTitleCase(codePoint));
                    } else {
                        if (!values[14].equalsIgnoreCase(String.format("%04X", Character.toTitleCase(codePoint)))) {
                            out.format("[B]toTitleCase(): [%04X] [%s]\n", Character.toTitleCase(codePoint), values[14]);
                        }
                    }
                }
                out.close ();
                String output = writer.toString ();

                if (!output.isEmpty ()) {
                    System.err.printf("U+%04x%n", codePoint);
                    System.err.println (output);
                }
                if (Character.isDefined(codePoint)) codePointTable.remove(codePoint);
              }
            }
            reader.close();
            System.out.println("--- Done ---");
        }
        if (codePointTable.size() > 0) { 
            System.err.printf("%d characters were not checked%n", codePointTable.size());
        }

        // Check all Unicode range
        for (int codePoint = 0;
             codePoint <= Character.MAX_CODE_POINT;codePoint++){
            if (Character.isDefined(codePoint)){
                String name = Character.getName(codePoint);
                if (name.length() > 0){
                    int code = Character.codePointOf(name);
                    if (code != codePoint){
                        System.err.printf("U+%04x%n", codePoint);
                        System.err.println("codePointOf(getName()) is different. "+ Integer.toHexString(code));
                    }
                }
            }
        }
    }
}
