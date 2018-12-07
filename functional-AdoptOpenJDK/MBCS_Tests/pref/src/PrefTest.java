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

/*
 * PrefTest.java
 * - A sample program for Preferences class
 */
import java.util.prefs.*;
import java.awt.event.*;
public class PrefTest {
    static private String ourNodeName = "/mynode/myKey";
    static private String key = "myName";
    
    static void setName(String name) {
        Preferences prefs = Preferences.userRoot().node(ourNodeName);
        
        prefs.put(key, name);
    }
    
    static void getName() {
        Preferences prefs = Preferences.userRoot().node(ourNodeName);
        
        String myName = prefs.get(key, "");
        if (myName.equals("")) {
            System.out.println("You name is NOT DEFINED.");
        } else {
            System.out.println("Your name is " + myName + ".");
        }
    }
    
    static void removeName() {
        Preferences prefs = Preferences.userRoot().node(ourNodeName);
        
        prefs.remove(key);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: java PregTest [yourname | -q | -r]");
            System.exit(1);
        }
        
        if (args[0].equals("-q")) {
            PrefTest.getName();
        } else if (args[0].equals("-r")) {
            PrefTest.removeName();
        } else {
            PrefTest.setName(args[0]);
        }
    }
}
