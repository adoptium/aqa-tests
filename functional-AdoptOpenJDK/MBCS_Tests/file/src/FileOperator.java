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

public class FileOperator {

  public static void main(java.lang.String[]args) {
    if (args.length < 3) {
	System.out.
	  println("Usage : java FileOperator <source> <destination> {CRDL}");
	System.exit(1);
      }
    try {
      FileOperator fo = new FileOperator();
      File src = new File(args[0]);
      File dst = new File(args[1]);

      switch (args[2].charAt(0)) {
	case 'C':
	  System.out.println("File Copy Test :");
	  System.out.println("Original Directory :");
	  fo.list(src, 3);
	  fo.xcopy(src, dst);
	  System.out.println("Copied Directory :");
	  fo.list(dst, 3);
	  System.out.print("Compare : ");
	  if (fo.compare(src, dst))
	    System.out.println("Passed");
	  else
	    System.out.println("Failed");

	  System.out.println("Hit Enter Key");
	  System.in.read();
	  break;

	case 'R':
	  System.out.println("File Rename Test :");
	  System.out.println("Original Directory :");
	  fo.list(dst, 3);
	  File newdir = fo.rename(dst);
	  if (newdir != null) {
	      System.out.println("Renamed Directory :");
	      fo.list(newdir, 3);

	      System.out.println("Hit Enter Key");
	      System.in.read();

	      System.out.println("Rename-back : ");
	      dst = fo.rename(newdir);

	      System.out.println("Renamed Directory :");
	      fo.list(dst, 3);
	      System.out.print("Compare : ");
	      if (fo.compare(src, dst))
		System.out.println("Passed");
	      else
		System.out.println("Failed");

	      System.out.println("Hit Enter Key");
	      System.in.read();
	  } else
	      System.out.println("Rename Failed");
	  break;

	case 'D':
	  System.out.println("File Delete Test :");
	  System.out.println("Original Directory :");
	  fo.list(dst, 3);
	  fo.delete(dst);
	  System.out.println("Deleted Directory :");
	  fo.list(dst, 3);
	  break;

	case 'L':
	  fo.list(src, 3);
	  break;
	}

    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
  }

  public static void xcopy(File src, File dst) throws java.io.IOException {
    if (!src.exists() || !src.isDirectory())
      return;
    if (!dst.exists())
      dst.mkdirs();
    else if (!dst.isDirectory()) {
	dst.delete();
	dst.mkdirs();
    }
    String names[] = src.list();
    for (int i = 0; i < names.length; i++) {
	File s = new File(src, names[i]);
	File d = new File(dst, names[i]);
	if (s.isFile()) {
//                      System.out.println("copying file : " + s.getPath() + " to : " + d.getPath());
	    copy(s, d);
	} else if (s.isDirectory()) {
//                      System.out.println("copying directory : " + s.getPath() + " to : " + d.getPath());
	    xcopy(s, d);
	  }
      }
  }

  public static void delete(File dst) {
    if (!dst.exists())
      return;
    if (dst.isDirectory()) {
	String names[] = dst.list();
	for (int i = 0; i < names.length; i++) {
	    delete (new File(dst, names[i]));
	}
    }
//      System.out.println("deleting : " + dst.getPath());
    dst.delete();
  }

  public static File rename(File src) {
    if (!src.exists())
      return null;
    if (src.isDirectory()) {
	String names[] = src.list();
	for (int i = 0; i < names.length; i++) {
	    rename(new File(src, names[i]));
	}
    }
    String name = src.getName();
    File dst = new File(src.getParent(), reverse(name));
//      System.out.println("renaming : " + src.getPath() + " to " + dst.getPath());
    src.renameTo(dst);
    return dst;
  }

  public static void list(File src, int indent) {
    if (!src.exists())
      return;
    for (int i = 0; i < indent; i++)
      System.out.print(" ");
    System.out.println(src.getName());
    if (src.isDirectory()) {
	String names[] = src.list();
	java.util.Arrays.sort(names);
	for (int i = 0; i < names.length; i++)
	  list(new File(src, names[i]), indent + 3);
    }
  }

  public static boolean compare(File src, File dst) {
    if (!src.exists())
      if (!dst.exists())
	return true;
      else {
	  System.err.println(src.toString() + " doesn't exist.");
	  return false;
      }
    if (!dst.exists()) {
	System.err.println(dst.toString() + " doesn't exist.");
	return false;
    }
    if (!src.getName().equals(dst.getName())) {
	System.err.println("Name Mismatch : " + src + " vs " + dst);
	return false;
    }
    if (src.isDirectory()) {
	if (!dst.isDirectory()) {
	    System.err.println(dst.toString() + " is not a directory.");
	    return false;
	}
	String srclist[] = src.list();
	String dstlist[] = dst.list();
	if (srclist.length != dstlist.length) {
	    System.err.println("Contents Number Mismatch : " + src + " vs " +
				dst);
	    return false;
	}
	for (int i = 0; i < srclist.length; i++)
	  return compare(new File(src, srclist[i]),
			  new File(dst, srclist[i]));
	for (int i = 0; i < dstlist.length; i++)
	  return compare(new File(src, dstlist[i]),
			  new File(dst, dstlist[i]));
    }
    return true;
  }

  public static String reverse(String src) {
    if (src == null)
      return null;
    String dst = new String("");
    for (int i = 0; i < src.length(); i++) {
	dst = src.charAt(i) + dst;
    }
    return dst;
  }

  public static void copy(File src, File dst) throws java.io.IOException {
    FileInputStream fi = new FileInputStream(src);
    FileOutputStream fo = new FileOutputStream(dst);
    byte buf[] = new byte[1000];
    int num = 0;
    while ((num = fi.read(buf)) >= 0) {
	fo.write(buf, 0, num);
    }
    fi.close();
    fo.close();
    return;
  }

}
