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

package net.adoptopenjdk.casa.agent;

import java.lang.instrument.Instrumentation;

/**
 * The class that is launched when the Synthetic GC workload is run as a java
 * agent
 */
public class Agent {

    final private static String CACHING = "-c";
    final private static String SESSIONS = "-s";

    public static void premain(String args, Instrumentation inst) {

        String[] argsArr = null;
        if (args != null) {
            argsArr = args.split(" ");
        } else {
            // throw exception saying we need params
            throw new IllegalArgumentException(
                    "Must provide arguments. Use -c to launch caching workload, or -s to launch sessions workload");
        }

        if (argsArr[0].equals(SESSIONS)) {
            // launch sessions workload
            String[] sessionsArgs = new String[argsArr.length - 1];
            // remove the -s flag, pass everything else to premain runner
            System.arraycopy(argsArr, 1, sessionsArgs, 0, sessionsArgs.length);

            net.adoptopenjdk.casa.workload_sessions.Main.premainRunner(sessionsArgs);

        } else if (argsArr[0].equals(CACHING)) {
            // launch caching workload
            String[] cachingArgs = new String[argsArr.length - 1];
            // remove the -c flag, pass everything else to runner
            System.arraycopy(argsArr, 1, cachingArgs, 0, cachingArgs.length);
            try {
                net.adoptopenjdk.casa.workload_caching.Main.premainRunner(cachingArgs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            throw new IllegalArgumentException(
                    "Invalid first argument --- First argument must be '-c' (caching workload) or '-s' (sessions workload)");
        }

    }
}