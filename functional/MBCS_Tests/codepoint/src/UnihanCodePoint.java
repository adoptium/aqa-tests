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
import java.util.*;
import java.util.regex.*;

public class UnihanCodePoint
{
  static TreeMap < Integer, Long > unicodeNumMap =
    new TreeMap <> ();
  public static void main (String[]args) throws Exception
  {
    String[] vals;
    if (args.length > 0)
      {
        vals = args;
      }
    else
      {
        vals = new String[2];
        vals[0] = UnicodeVers.getFiles("UnicodeData")[0];
        vals[1] = UnicodeVers.getFiles("Unihan_IRGSources")[0];
      }

    if (vals.length < 2)
      {
	System.err.
	  println ("Usage: UnihanCodePoint UnicodeDataFile UnihanFile");
	System.exit (1);
      }

    String fname = vals[0];
    System.out.println("--- Reading NumValue from "+fname+" ---");
    BufferedReader unicodeDataReader =
      new BufferedReader (new FileReader (fname));
    while (unicodeDataReader.ready ())
      {
	String[]values = unicodeDataReader.readLine ().split (";");
	if (!values[8].isEmpty () && values[8].indexOf ("/") == -1)
	  {
	    unicodeNumMap.put (Integer.
			       valueOf (Integer.parseInt (values[0], 16)),
			       Long.valueOf (Long.parseLong (values[8])));
	  }
      }
    unicodeDataReader.close ();

    fname = vals[1];

    System.out.println("--- Checking "+fname+" ---");
    TreeMap < Integer, TreeSet <Integer> > unicodeMap =
      new TreeMap < Integer, TreeSet <Integer> > ();
    BufferedReader reader = new BufferedReader (new FileReader (fname));
    while (reader.ready ())
      {
	String line = reader.readLine ();
	Scanner scanner = new Scanner (line);
	try
	{
	  scanner.findInLine ("^U\\+([0-9A-Fa-f]{4,6})\\s");
	  MatchResult result = scanner.match ();
	  if (result.groupCount () == 1)
	    {
	      int unicode = Integer.parseInt (result.group (1), 16);
	      Integer unicode_key = Integer.valueOf (unicode & 0xFFFFFF80);
	      TreeSet < Integer > unicodeSet;
	      if (unicodeMap.containsKey (unicode_key))
		{
		  unicodeSet = unicodeMap.get (unicode_key);
		  unicodeSet.add (Integer.valueOf (unicode));
		}
	      else
		{
		  unicodeSet = new TreeSet < Integer > ();
		  unicodeSet.add (Integer.valueOf (unicode));
		  unicodeMap.put (unicode_key, unicodeSet);
		}

	    }
	}
	catch (java.lang.IllegalStateException ise)
	{
	}
      }
    reader.close ();

    for (Integer i:unicodeMap.keySet ().toArray (new Integer[0]))
      {
	TreeSet < Integer > unicodeSet = unicodeMap.get (i);
	StringBuffer sb = new StringBuffer ();
      for (Integer j:unicodeSet.toArray (new Integer[0]))
	  {
	    sb.append (new String (Character.toChars (j.intValue ())));
	  }
	//System.out.println("U+%04x:", ((Integer)i).intValue());
	//System.out.println(sb.toString());
	processLine (sb.toString ());
      }
    System.out.println("--- Done ---");
  }

  private static void processLine (String line)
  {
    int length = line.length ();
    int cpCount = line.codePointCount (0, length);
    //System.out.println ("length:         " + length);
    //System.out.println ("codePointCount: " + cpCount);
    //System.out.println(line);


    for (int i = 0; i < length; i++)
      {
	char c = line.charAt (i);
	if (Character.isHighSurrogate (c))
	  {
	    int codePoint =
	      Character.toCodePoint (line.charAt (i), line.charAt (i + 1));
	    if (codePoint != line.codePointAt (i))
	      {
		System.err.println ("Failed: " + line);
	      }
	    else
	      {
		supCodePointTest (codePoint);
	      }
	  }
	else
	  {
	    if (!Character.isLowSurrogate (c))
	      {
		codePointTest (c);
	      }
	  }
      }
    appendCodePointTest (line);
  }

  private static void appendCodePointTest (String line)
  {
    StringBuffer buf = new StringBuffer ();
    int length = line.length ();

    for (int i = 0; i < length; i++)
      {
	int codePoint = line.codePointAt (i);
	if (Character.isSupplementaryCodePoint (codePoint))
	  {
	    buf.appendCodePoint (codePoint);
	  }
	else
	  {
	    if (!Character.isLowSurrogate ((char) codePoint))
	      {
		buf.appendCodePoint (codePoint);
	      }
	  }
      }

    if (!buf.toString ().equals (line))
      {
	System.err.println ("Failed: appendCodePoint with " + line);
      }

  }

  private static void codePointTest (char c)
  {
    int codePoint = (int) c;

    if (!(Character.isValidCodePoint (codePoint) == true))
      {
	System.err.format ("Failed: isValidCodePoint(U+%X)\n", codePoint);
      }

    if (!(Character.isSupplementaryCodePoint (codePoint) == false))
      {
	System.err.format ("Failed: isSupplementaryCodePoint(U+%X)\n",
			   codePoint);
      }

    if (!(Character.isHighSurrogate (c) == false))
      {
	System.err.format ("Failed: Character.isHighSurrogate(%X)\n", c);
      }
    if (!(Character.isLowSurrogate (c) == false))
      {
	System.err.format ("Failed: Character.isLowSurrogate(%X)\n", c);
      }
    char[] ch = Character.toChars (codePoint);
    if (!(ch.length == 1))
      {
	System.err.format ("Failed: toChars(U+%X)\n", codePoint);
      }
    //System.out.format ("ch[0]: %X\n", (int) ch[0]);

    //Character class Test
    StringWriter writer = new StringWriter ();
    PrintWriter out = new PrintWriter (writer);
    if (Character.isLowerCase (codePoint) != false)
      out.println ("isLowerCase(): " + Character.isLowerCase (codePoint));
    if (Character.isUpperCase (codePoint) != false)
      out.println ("isUpperCase(): " + Character.isUpperCase (codePoint));
    if (Character.isTitleCase (codePoint) != false)
      out.println ("isTitleCase(): " + Character.isTitleCase (codePoint));
    if (Character.isDigit (codePoint) != false)
      out.println ("isDigit(): " + Character.isDigit (codePoint));
    if (Character.isDefined (codePoint) != true)
      out.println ("isDefined(): " + Character.isDefined (codePoint));
    if (Character.isLetter (codePoint) != true)
      out.println ("isLetter(): " + Character.isLetter (codePoint));
    if (Character.isLetterOrDigit (codePoint) != true)
      out.println ("isLetterOrDigit(): " +
		   Character.isLetterOrDigit (codePoint));
    if (Character.isJavaIdentifierStart (codePoint) != true)
      out.println ("isJavaIdentifierStart(): " +
		   Character.isJavaIdentifierStart (codePoint));
    if (Character.isJavaIdentifierPart (codePoint) != true)
      out.println ("isJavaIdentifierPart(): " +
		   Character.isJavaIdentifierPart (codePoint));
    if (Character.isUnicodeIdentifierStart (codePoint) != true)
      out.println ("isUnicodeIdentifierStart(): " +
		   Character.isUnicodeIdentifierStart (codePoint));
    if (Character.isUnicodeIdentifierPart (codePoint) != true)
      out.println ("isUnicodeIdentifierPart(): " +
		   Character.isUnicodeIdentifierPart (codePoint));
    if (Character.isIdentifierIgnorable (codePoint) != false)
      out.println ("isIdentifierIgnorable(): " +
		   Character.isIdentifierIgnorable (codePoint));
    if (Character.isIdentifierIgnorable (codePoint) != false)
      out.println ("isSpaceChar(): " + Character.isSpaceChar (codePoint));
    if (Character.isWhitespace (codePoint) != false)
      out.println ("isWhitespace(): " + Character.isWhitespace (codePoint));
    if (Character.isISOControl (codePoint) != false)
      out.println ("isISOControl(): " + Character.isISOControl (codePoint));
    if (Character.isMirrored (codePoint) != false)
      out.println ("isMirrored(): " + Character.isMirrored (codePoint));
    if (Character.isBmpCodePoint (codePoint) != true)
      out.println ("isBmpCodePoint(): " + Character.isBmpCodePoint (codePoint));
    if (Character.isSurrogate(ch[0]) != false)
      out.println ("isSurrogate(ch[0]): "+Character.isSurrogate(ch[0]));

    if (Character.toLowerCase (codePoint) != codePoint)
      out.format ("toLowerCase(): U+%X\n", Character.toLowerCase (codePoint));
    if (Character.toUpperCase (codePoint) != codePoint)
      out.format ("toUpperCase(): U+%X\n", Character.toUpperCase (codePoint));
    if (Character.toTitleCase (codePoint) != codePoint)
      out.format ("toTitleCase(): U+%X\n", Character.toTitleCase (codePoint));
    if (Character.getNumericValue ((int) codePoint) != -1)
      {
	if (unicodeNumMap.containsKey (codePoint))
	  {
	    if (Character.getNumericValue (codePoint) !=
		unicodeNumMap.get (codePoint))
	      out.format ("getNumericValue(): %d, expected %d\n",
			  Character.getNumericValue (codePoint),
			  unicodeNumMap.get (codePoint));
	  }
	else
	  {
	    out.format ("getNumericValue(): %d\n",
			Character.getNumericValue (codePoint));
	  }
      }
    if (Character.getType (codePoint) != 5)
      out.format ("getType(): %d\n", Character.getType (codePoint));
    if (Character.getDirectionality (codePoint) != 0)
      out.format ("getDirectionality(): %d\n",
		  Character.getDirectionality (codePoint));

    out.close ();
    String output = writer.toString ();

    if (!output.isEmpty ())
      {
	System.err.format ("ch[0]: %X\n", (int) ch[0]);
	System.err.println (output);
      }
  }

  private static void supCodePointTest (int codePoint)
  {

    if (!(Character.isValidCodePoint (codePoint)))
      {
	System.err.format ("Failed: isValidCodePoint(U+%X)\n", codePoint);
      }

    if (!(Character.isSupplementaryCodePoint (codePoint)))
      {
	System.err.format ("Failed: isSupplementaryCodePoint(U+%X)\n",
			   codePoint);
      }

    //CodePoint
    //System.out.format ("U+%X\n", codePoint);

    //Display
    //System.out.format("%c\n", codePoint);

    //UnicodeBlock
    //System.out.println ("Unicode Block: " + Character.UnicodeBlock.of (codePoint));

    //SurrogatePair Test
    char[] ch = Character.toChars (codePoint);
    //System.out.format ("ch[0]: %X ch[1]: %X\n", (int) ch[0], (int) ch[1]);

    if (!(Character.isHighSurrogate (ch[0]) == true))
      {
	System.err.format ("Failed: Character.isHighSurrogate(%X)\n", ch[0]);
      }

    if (!(Character.isLowSurrogate (ch[0]) == false))
      {
	System.err.format ("Failed: Character.isLowSurrogate(%X)\n", ch[0]);
      }

    if (!(Character.isHighSurrogate (ch[1]) == false))
      {
	System.err.format ("Failed: Character.isHighSurrogate(%X)\n", ch[1]);
      }

    if (!(Character.isLowSurrogate (ch[1]) == true))
      {
	System.err.format ("Failed: Character.isLowSurrogate(%X)\n", ch[1]);
      }

    if (!(Character.isSurrogatePair (ch[0], ch[1])))
      {
	System.err.format ("Failed: Character.isSurrogatePair(%X, %X)\n",
			   ch[0], ch[1]);
      }

    //Character.toCodePoint Test
    int codePointTest = Character.toCodePoint (ch[0], ch[1]);
    if (!(codePointTest == codePoint))
      {
	System.err.format ("Failed: toCodePoint(%X, %X)\n", ch[0], ch[1]);
      }

    //Character class Test
    StringWriter writer = new StringWriter ();
    PrintWriter out = new PrintWriter (writer);
    if (Character.isLowerCase (codePoint) != false)
      out.println ("isLowerCase(): " + Character.isLowerCase (codePoint));
    if (Character.isUpperCase (codePoint) != false)
      out.println ("isUpperCase(): " + Character.isUpperCase (codePoint));
    if (Character.isTitleCase (codePoint) != false)
      out.println ("isTitleCase(): " + Character.isTitleCase (codePoint));
    if (Character.isDigit (codePoint) != false)
      out.println ("isDigit(): " + Character.isDigit (codePoint));
    if (Character.isDefined (codePoint) != true)
      out.println ("isDefined(): " + Character.isDefined (codePoint));
    if (Character.isLetter (codePoint) != true)
      out.println ("isLetter(): " + Character.isLetter (codePoint));
    if (Character.isLetterOrDigit (codePoint) != true)
      out.println ("isLetterOrDigit(): " +
		   Character.isLetterOrDigit (codePoint));
    if (Character.isJavaIdentifierStart (codePoint) != true)
      out.println ("isJavaIdentifierStart(): " +
		   Character.isJavaIdentifierStart (codePoint));
    if (Character.isJavaIdentifierPart (codePoint) != true)
      out.println ("isJavaIdentifierPart(): " +
		   Character.isJavaIdentifierPart (codePoint));
    if (Character.isUnicodeIdentifierStart (codePoint) != true)
      out.println ("isUnicodeIdentifierStart(): " +
		   Character.isUnicodeIdentifierStart (codePoint));
    if (Character.isUnicodeIdentifierPart (codePoint) != true)
      out.println ("isUnicodeIdentifierPart(): " +
		   Character.isUnicodeIdentifierPart (codePoint));
    if (Character.isIdentifierIgnorable (codePoint) != false)
      out.println ("isIdentifierIgnorable(): " +
		   Character.isIdentifierIgnorable (codePoint));
    if (Character.isSpaceChar (codePoint) != false)
      out.println ("isSpaceChar(): " + Character.isSpaceChar (codePoint));
    if (Character.isWhitespace (codePoint) != false)
      out.println ("isWhitespace(): " + Character.isWhitespace (codePoint));
    if (Character.isISOControl (codePoint) != false)
      out.println ("isISOControl(): " + Character.isISOControl (codePoint));
    if (Character.isMirrored (codePoint) != false)
      out.println ("isMirrored(): " + Character.isMirrored (codePoint));
    if (Character.isBmpCodePoint (codePoint) != false)
      out.println ("isBmpCodePoint(): " + Character.isBmpCodePoint (codePoint));
    if ((Character.isSurrogate(ch[0]) && Character.isSurrogate(ch[1])) != true)
      out.println ("isSurrogate(ch[0]): "+Character.isSurrogate(ch[0])+
                 ", isSurrogate(ch[1]): "+Character.isSurrogate(ch[1]));
    if (Character.highSurrogate(codePoint) != ch[0])
      out.println (String.format("highSurrogate(): U+%X", Character.highSurrogate(codePoint)));
    if (Character.lowSurrogate(codePoint) != ch[1])
      out.println (String.format("lowSurrogate(): U+%X", Character.lowSurrogate(codePoint)));

    if (Character.toLowerCase (codePoint) != codePoint)
      out.format ("toLowerCase(): U+%X\n", Character.toLowerCase (codePoint));
    if (Character.toUpperCase (codePoint) != codePoint)
      out.format ("toUpperCase(): U+%X\n", Character.toUpperCase (codePoint));
    if (Character.toTitleCase (codePoint) != codePoint)
      out.format ("toTitleCase(): U+%X\n", Character.toTitleCase (codePoint));
    if (Character.getNumericValue (codePoint) != -1)
      {
	if (unicodeNumMap.containsKey (codePoint))
	  {
	    if (Character.getNumericValue (codePoint) !=
		unicodeNumMap.get (codePoint))
	      out.format ("getNumericValue(): %d, expected %d\n",
			  Character.getNumericValue (codePoint),
			  unicodeNumMap.get (codePoint));
	  }
	else
	  {
	    out.format ("getNumericValue(): %d\n",
			Character.getNumericValue (codePoint));
	  }
      }
    if (Character.getType (codePoint) != 5)
      out.format ("getType(): %d\n", Character.getType (codePoint));
    if (Character.getDirectionality (codePoint) != 0)
      out.format ("getDirectionality(): %d\n",
		  Character.getDirectionality (codePoint));
    out.close ();
    String output = writer.toString ();

    if (!output.isEmpty ())
      {
	System.err.format ("U+%X\n", codePoint);
	System.err.println ("Unicode Block: " +
			    Character.UnicodeBlock.of (codePoint));
	System.err.format ("ch[0]: %X ch[1]: %X\n", (int) ch[0], (int) ch[1]);
	System.err.println (output);
      }

    //String Constructor Test
    char[] chTest = new char[]{ ch[0], ch[1] };
    String str1 = new String (new int[]{ codePoint }, 0, 1);
    String str2 = new String (ch);
    if (!(str1.equals (str2)))
      {
	System.err.format ("Failed: String Constructor with U+%X", codePoint);
      }
  }
}
