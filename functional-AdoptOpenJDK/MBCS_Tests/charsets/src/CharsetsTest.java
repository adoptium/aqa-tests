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
import java.nio.*;
import java.util.*;
import java.lang.reflect.*;

@SuppressWarnings("unchecked")
public class CharsetsTest {

    final static Object[] sbcs;
    final static Object[] ebcdic_dbcs;
    final static Object[] ebcdic_mix;

    final static byte[] sbcs_space = new byte[]{0x40};
    final static byte[] dbcs_space = new byte[]{0x40,0x40};
    final static byte[] mix_space  = new byte[]{0x0E,0x40,0x40,0x0F};

    static Class<?> ArrayDecoder = null;
    static Class<?> ArrayEncoder = null;
    static Method ArrayDecoder_decode = null;
    static Method ArrayEncoder_encode = null;

    static int err_cnt = 0;

    static {
        try {
            ArrayDecoder = Class.forName("sun.nio.cs.ArrayDecoder");
            ArrayEncoder = Class.forName("sun.nio.cs.ArrayEncoder");
            ArrayDecoder_decode = ArrayDecoder.getDeclaredMethod("decode",
                byte[].class, int.class, int.class, char[].class);
            ArrayEncoder_encode = ArrayEncoder.getDeclaredMethod("encode",
                char[].class, int.class, int.class, byte[].class);
        } catch (Exception e) {
        }
        {
            Vector<TreeSet[]> vector = new Vector<TreeSet[]>();
            TreeSet<Integer>[] sets = new TreeSet[1];
            TreeSet<Integer> set = new TreeSet<Integer>();
            for(int i=0x0; i<=0xFF; i++) set.add(i);
            sets[0] = set;
            vector.add(sets);
            sbcs = vector.toArray();
        }
        {
            Vector<TreeSet[]> vector = new Vector<TreeSet[]>();
            TreeSet<Integer>[] sets = new TreeSet[2];
            TreeSet<Integer> set0 = new TreeSet<Integer>();
            for(int i=0x40; i<=0xFE; i++) set0.add(i);
            TreeSet<Integer> set1 = new TreeSet<Integer>();
            for(int i=0x40; i<=0xFE; i++) set1.add(i);
            sets[0] = set0;
            sets[1] = set1;
            vector.add(sets);
            ebcdic_dbcs = vector.toArray();
        }
        {
            Vector<TreeSet[]> vector = new Vector<TreeSet[]>();
            TreeSet<Integer>[] sets = new TreeSet[1];
            TreeSet<Integer> set = new TreeSet<Integer>();
            for(int i=0x0; i<=0xFF; i++) set.add(i);
            sets[0] = set;
            vector.add(sets);
            sets = new TreeSet[4];
            TreeSet<Integer> set0 = new TreeSet<Integer>();
            set0.add(0x0E);
            TreeSet<Integer> set1 = new TreeSet<Integer>();
            for(int i=0x40; i<=0xFE; i++) set1.add(i);
            TreeSet<Integer> set2 = new TreeSet<Integer>();
            for(int i=0x40; i<=0xFE; i++) set2.add(i);
            TreeSet<Integer> set3 = new TreeSet<Integer>();
            set3.add(0x0F);
            sets[0] = set0;
            sets[1] = set1;
            sets[2] = set2;
            sets[3] = set3;
            vector.add(sets);
            ebcdic_mix = vector.toArray();
        }
    };

    static Object[] generateDecoderData(Vector<TreeSet[]> b2cData, CharsetDecoder cd, byte[] ba, int idx) throws Exception {
        if (null == b2cData) {
            b2cData = new Vector<TreeSet[]>();
            TreeSet<Integer>[] sets = new TreeSet[1];
            sets[0] = new TreeSet<Integer>();
            ba = new byte[1];
            b2cData.add(sets);
            generateDecoderData(b2cData, cd, ba, 0);
        } else {
            for(int i=0; i<=0xFF; i++) {
                ba[idx] = (byte) i;
                ByteBuffer bb = ByteBuffer.wrap(ba);
                CharBuffer cb = CharBuffer.allocate(10);
                cd.reset();
                CoderResult cr = cd.decode(bb, cb, false);
                if (cr.isError()) continue;
                if (cr.isOverflow()) {
                    throw new Exception(cr.toString());
                }
                if (cb.position() > 0) {
                    cr = cd.decode(bb, cb, true);
                    if (cr.isError()) continue;
                    if (cr.isOverflow()) {
                        throw new Exception(cr.toString());
                    }
                    cr = cd.flush(cb);
                    if (cr.isError()) continue;
                    if (cr.isOverflow()) {
                        throw new Exception(cr.toString());
                    }
                    TreeSet<Integer>[] sets = (TreeSet<Integer>[]) b2cData.get(ba.length - 1);
                    for (int j = 0; j < ba.length; j++) {
                        sets[j].add((int) ba[j] & 0xFF);
                    }
                    continue;
                } else {
                    if (ba.length == 10) throw new Exception("Exceed max byte buffer:"+cd.charset().name());
                    byte[] tmp_ba = new byte[ba.length + 1];
                    System.arraycopy(ba, 0, tmp_ba, 0, ba.length);
                    if (b2cData.size() < tmp_ba.length) {
                        TreeSet<Integer>[] sets = new TreeSet[tmp_ba.length];
                        for (int j = 0; j < sets.length; j++) {
                            sets[j] = new TreeSet<Integer>();
                        }
                        b2cData.add(sets);
                    }
                    generateDecoderData(b2cData, cd, tmp_ba, idx + 1);
                }
            }
        }
        return b2cData.toArray();
    }

    static Object[] initDecoderData(Charset charset) throws Exception {
        CharsetDecoder cd = charset.newDecoder();
        CharsetEncoder ce = charset.newEncoder();
        if (cd.maxCharsPerByte() == 1 && ce.maxBytesPerChar() == 1) {
            return sbcs;
        }
        boolean isSBCS = false;
        boolean isDBCS = false;
        boolean isMIX  = false;
        if (Arrays.equals(sbcs_space, "\u0020".getBytes(charset))) isSBCS = true;
        if (Arrays.equals(dbcs_space, "\u3000".getBytes(charset))) isDBCS = true;
        if (Arrays.equals(mix_space,  "\u3000".getBytes(charset))) isMIX  = true;
        if (isSBCS & !isMIX) return sbcs;
        //if (isDBCS) return ebcdic_dbcs;
        if (isMIX) return ebcdic_mix;

        byte[] iso2022 = "\u3000".getBytes(charset);
        if (iso2022[0] == (byte)0x1B) return null;

        return generateDecoderData(null, cd, null, 0);
    }

    static boolean isUnicodeConverter(Charset charset) throws Exception {
        CharsetDecoder cd = charset.newDecoder();
        cd.onMalformedInput(CodingErrorAction.IGNORE).onUnmappableCharacter(CodingErrorAction.IGNORE);
        CharsetEncoder ce = charset.newEncoder();
        ce.onMalformedInput(CodingErrorAction.IGNORE).onUnmappableCharacter(CodingErrorAction.IGNORE);

        CharBuffer cb = CharBuffer.wrap(new char[]{'\uFFFD'});
        ce.reset();
        ByteBuffer bb = ce.encode(cb);
        cd.reset();
        bb.position(0);
        CharBuffer cb1 = cd.decode(bb);
        return (1 == cb1.limit() && cb1.get(0) == '\uFFFD');
    }

    static void decode_data(CharsetDecoder cd, int idx, byte[] ba, TreeSet<Integer>[] range) throws Exception {
        if (idx == ba.length - 1) {
            for(Integer intV : range[idx]) {
                ba[idx] = (byte)intV.intValue();
                char[] ca = new char[(int)Math.ceil(cd.maxCharsPerByte()*ba.length)];
                cd.reset();
                int len = (int) ArrayDecoder_decode.invoke(ArrayDecoder.cast(cd),ba,0,ba.length,ca);
                StringBuffer sb0 = new StringBuffer();
                for(int j=0; j<len; j++) sb0.append(String.format("\\u%04X", (int)ca[j]));
                StringBuffer sb1 = new StringBuffer();
                try {
                    cd.reset();
                    CharBuffer cb = cd.decode(ByteBuffer.wrap(ba));
                    for(int j=0; j<cb.limit(); j++)
                        sb1.append(String.format("\\u%04X", (int)cb.get(j)));
                } catch (Exception e) {
                    throw new Exception(e.toString());
                }
                if (!(sb0.toString().equals(sb1.toString()))) {
                    System.out.print(cd.charset().name()+":");
                    for(byte b : ba) System.out.printf("\\x%02X",(int)b&0xFF);
                    System.out.println(":"+sb0.toString()+"<>"+sb1.toString());
                    err_cnt++;
                }
            }
        } else {
            for(Integer intV : range[idx]) {
                ba[idx] = (byte)intV.intValue();
                decode_data(cd, idx+1, ba, range);
            }
        }
    }

    static void encode_data(CharsetEncoder ce) throws Exception {
        for(int i=Character.MIN_CODE_POINT; i<=Character.MAX_CODE_POINT; i++) {
            if (!Character.isDefined(i)) continue;
            if (i<=Character.MAX_VALUE) {
                if (Character.isHighSurrogate((char)i)) continue;
                if (Character.isLowSurrogate((char)i)) continue;
            }
            char[] ca = Character.toChars(i);
            ce.reset();
            byte[] ba = new byte[(int)Math.ceil(ce.maxBytesPerChar()*ca.length)];
            int len = (int)ArrayEncoder_encode.invoke(ArrayEncoder.cast(ce),ca,0,ca.length,ba);
            StringBuffer sb0 = new StringBuffer();
            for(int j=0; j<len; j++) sb0.append(String.format("\\x%02X", (int)ba[j] & 0xFF));
            StringBuffer sb1 = new StringBuffer();
            try {
                 ce.reset();
                 ByteBuffer bb = ce.encode(CharBuffer.wrap(ca));
                 for(int j=0; j<bb.limit(); j++)
                     sb1.append(String.format("\\x%02X", (int)bb.get(j) & 0xFF));
            } catch (Exception e) {
                 throw new Exception(e.toString());
            }
            if (!(sb0.toString().equals(sb1.toString()))) {
                System.out.println(ce.charset().name()+":");
                for(char c : ca) System.out.printf("\\u%04X",(int)c);
                System.out.println(":"+sb0.toString()+"<>"+sb1.toString());
                err_cnt++;
            }
        }
    }

    static void charset_test(Charset charset) throws Exception {
        if (!charset.canEncode()) {
            System.out.println("Encoder is not supported by charset "+charset.name()+". Skipping..." );
            return;
        }
        if (isUnicodeConverter(charset)) {
            System.out.println("Charset "+charset.name()+" is Unicode compatible charset. Skipping..." );
            return;
        }
        CharsetDecoder cd = charset.newDecoder();
        CharsetEncoder ce = charset.newEncoder();
        if (!(ArrayDecoder.isInstance(cd)) || !(ArrayEncoder.isInstance(ce))) {
            System.out.println("Charset "+charset.name()+
                " does not support ArrayDecoder and/or ArrayEncoder interface. Skipping..." );
            return;
        }
        System.out.println(charset.name());
        cd.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        ce.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        // Decoder
        Object[] objs = initDecoderData(charset);
        for(int i=0; i< objs.length; i++) {
            TreeSet<Integer>[] sets = (TreeSet<Integer>[]) objs[i];
            if (sets[0].size() == 0) continue;
            for(int j=0; j<sets.length; j++) {
                if (0 == sets[j].size()) continue;
                System.out.printf("[\\x%02X-\\x%02X]",sets[j].first(),sets[j].last());
            }
            System.out.println();
            decode_data(cd, 0, new byte[sets.length], sets);
        }
        // Encoder
        encode_data(ce);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            for(String csName : args) {
                charset_test(Charset.forName(csName));
            }
        } else {
            for(Charset charset : Charset.availableCharsets().values()) {
                charset_test(charset);
            }
        }
        System.out.printf("Test: %s%n", err_cnt > 0 ? "failed" : "OK");
        System.exit(err_cnt > 0 ? 1 : 0);
    }
}
