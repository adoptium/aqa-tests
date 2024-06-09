import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The estimated times are highly CORES sensitive. Some tests do not even run with CORES=1!
 * Some groups, eg org.openjdk.jcstress.tests.seqcst.sync and org.openjdk.jcstress.tests.seqcst.volatiles are highly affected by cores (2->2hours 4=>45days!)
 * other groups are slightly less affected by cores, but still are.
 * <p>
 * This table was genrated with CORES=2 in TEST mode (thus with aprox 75% accuracy).
 * jcstress20240202  4400classes with 11500 tests.
 * Note, that `%like longest/ideal` is better closer to bigger/bigger.
 * 2 cores and org.openjdk.jcstress.tests.seqcst.sync and org.openjdk.jcstress.tests.seqcst.volatiles not split:
 * Limit 5 - 1207 groups, from those  8 "small groups" (not tried... yet... to long...)
 * split_exl Limit 10 - 603 groups, from those  7 "small groups" (0.5hours each. %like longest/ideal %17%/? (6m-2.5h)
 * split_all Limit 10 - 603 groups, from those  7 "small groups" (0.5hours each. %like longest/ideal %68%/81? (26m-38mm)
 * Limit 50 - 128 groups, from those  6 "small groups" (~2.5hhours each. %like longest/ideal %60/85% (45m-3.5h)
 * Limit 100 - 60 groups, from those  7 "small groups" (~4.5hours each. %like longest/ideal %63/79% (2.5h-7h)
 * Limit 250 - 25 groups, from those  4 "small groups" (~11hours each. %like longest/ideal 63%/77% (2.5h-17h)
 * Limit 500 - 14 groups, from those  4 "small groups" (~20hours each. %like longest/ideal 59%/60% (1.5h-1d 9h)
 * Limit 1000 - 9 groups, from those  5 "small groups" (~1day 6hours each. %like longest/ideal 42%/41% (2.5h-3d)
 * Limit 2000 - 6 groups, from those  4 "small groups" (~2day each, %like longest/ideal 41%/9% (2.5h-4d)
 * Limit 5000 - 3 groups, from those  3 "small groups" (unknown, selector argument to long for one of groups)
 * Limit 50000 - 1 groups, from those 1 "small groups" (unknown, selector argument to long)
 * all tests in batch ~11.5 of day
 * The minimal 2.5 which is invalidating huge groups a bit, are  the two excluded gorg.openjdk.jcstress.tests.seqcst.sync and org.openjdk.jcstress.tests.seqcst.volatiles,
 * <p>
 * Note, that LIMIT is not strictly honored. It is jsut saying, that if there LIMIT of testes or more, it wil not be grouped.
 * So in worse scenario, LIMIT-1+LIMIT-1 will join to group of size of (2*LIMIT)-2, but it is very rare,
 * and in addition the time of one test is very far from being constant, so this deviation in size of grtoup (LIMIT+1, <2*LIMIT)-2> is minimal.
 * If small groups are enagetOutputSbled, and they should be, there wil nearly always be some leftover group with size <= LIMIT
 * <p>
 */

/*
  echo "You can self diagnostic various iterations aka:"

#!/bin/bash
set -exo pipefail
  javac Generate.java
  #jcstress_jar=jcstress-20240222.jar
  outfile=details.html
  OUTPUT=test
  failures=0
  all=0
  echo "<html><body><details><pre>" | tee -a $outfile
  uname -a | tee -a $outfile
  head -n 40 /proc/meminfo | tee -a $outfile
  tail -n 40 /proc/cpuinfo | tee -a $outfile
  date  | tee  $outfile
  echo "</pre></details>" | tee -a $outfile
  for SPLIT_ALL in false true ; do
    echo starting SPLIT_ALL=$SPLIT_ALL | tee -a $outfile
    echo "<details>" | tee -a $outfile
    for LIMIT in 2000 1000 500 250 100 50 10; do
      echo starting SPLIT_ALL=$SPLIT_ALL LIMIT=$LIMIT | tee -a $outfile
      echo "<details>" | tee -a $outfile
      for CORES in 1 2 3 4 8; do
        echo running SPLIT_ALL=$SPLIT_ALL LIMIT=$LIMIT CORES=$CORES | tee -a $outfile
        echo "<details>" | tee -a $outfile
        echo "<pre>" | tee -a $outfile
        let all=$all+1
        echo "id: $all" " failed: $failures" | tee -a $outfile
        OUTPUT=$OUTPUT SPLIT_ALL=$SPLIT_ALL LIMIT=$LIMIT CORES=$CORES java Generate $jcstress_jar | tee -a $outfile || let failures=$failures+1
        echo "</pre>" | tee -a $outfile
        echo "</details>" | tee -a $outfile
      done
      echo "</details>" | tee -a $outfile
    done
    echo "</details>" | tee -a $outfile
  done
  date  | tee -a  $outfile
 */
public class Generate {

    // longest generated classes have 2131 tests
    private static final int LIMIT = parseLimit();

    private static final boolean smallGroups = parseSmallGroups();

    //those namespaces will not be merged together. This is literal eaquls comparison
    private static final String[] NOT_MERGE_ABLE_GROUPS = new String[]{
            "org",
            "org.openjdk",
            "org.openjdk.jcstress",
            "org.openjdk.jcstress.tests",
    };

    private static final boolean splitBigBases = parseSplitBigBases();

    private static final String[] NOT_SPLIT_ABLE_GROUPS = parseSplitImsplittable();
    private static final String template = """
            <test>
            	<testCaseName>-TARGET-</testCaseName>
            	<!-- -COMMENT-  -->
                   <command>$(JAVA_COMMAND) $(JVM_OPTIONS) -jar $(Q)$(LIB_DIR)$(D)-JARFILE-$(Q) $(APPLICATION_OPTIONS) -CORES- -t "-REGEX-"; \\
                   $(TEST_STATUS)</command>
            	<levels>
            		<level>dev</level>
            	</levels>
            	<groups>
            		<group>system</group>
            	</groups>
            </test>""";
    private static final String header = """
            <?xml version='1.0' encoding='UTF-8'?>
            <!--
            # Licensed under the Apache License, Version 2.0 (the "License");
            # you may not use this file except in compliance with the License.
            # You may obtain a copy of the License at
            #
            #      https://www.apache.org/licenses/LICENSE-2.0
            #
            # Unless required by applicable law or agreed to in writing, software
            # distributed under the License is distributed on an "AS IS" BASIS,
            # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            # See the License for the specific language governing permissions and
            # limitations under the License.
            -->
            <playlist xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../../TKG/playlist.xsd">""";
    private static final String footer = "</playlist>";
    private static URLClassLoader jarFileClasses;
    private static String jvm;
    private static File jarFile;


    public static void main(String... args) throws Exception {
        String jar = readArg(args);
        setAndPrintSetup();
        setUsedJvm();
        List<GroupWithCases> tests = listTestsClassesWithCasesFromJcStressJar(jvm, jar);
        System.err.println("total tests files: " + tests.size());
        print(tests);
        List<GroupWithCases> groups = tests;
        groups = mergeSmallGroupsToNaturalOnes(groups);
        if (smallGroups) {
            groups = mergeSmallRemainingTestsToArtificialOnes(groups);
        }
        Collections.sort(groups);
        System.err.println("Checksum:");
        checksum(groups, tests, false, true, true);
        System.err.println("Passed!");
        sortByCount(groups);
        print(groups);
        if (OutputType.STATS == getOutputStyle()) {
            for (GroupWithCases group : groups) {
                System.out.println(group.toStringNoRegex());
            }
        } else if (OutputType.REGEXES == getOutputStyle()) {
            for (GroupWithCases group : groups) {
                System.out.println(group.regex);
            }
        } else if (OutputType.DO == getOutputStyle() || OutputType.TEST == getOutputStyle()) {
            testTimesByRunningJcstress(groups);
        } else {
            printPlaylist(jarFile.getName(), groups);
        }


    }

    private static List<GroupWithCases> mergeSmallRemainingTestsToArtificialOnes(List<GroupWithCases> groups) {
        int x = 0;
        while (true) {
            x++;
            List<GroupWithCases> bigCandidate = mergeSmallGroups(x, groups);
            if (bigCandidate.size() == groups.size()) {
                break;
            }
            groups = bigCandidate;
            System.err.println("Small Group count " + x + " : " + groups.size());
            print(groups);
        }
        return groups;
    }

    private static List<GroupWithCases> mergeSmallGroupsToNaturalOnes(List<GroupWithCases> groups) throws Exception {
        int i = 0;
        final Map<String, Integer> splitGroupsCounters = new HashMap();
        while (true) {
            i++;
            List<GroupWithCases> bigCandidate = groupTests(groups, splitGroupsCounters);
            if (bigCandidate.size() == groups.size()) {
                break;
            }
            groups = bigCandidate;
            System.err.println("Natural groups round " + i + " : " + groups.size());
            print(groups);
        }
        return groups;
    }

    private static void setUsedJvm() {
        jvm = getUsedJvm();
    }

    private static String getUsedJvm() {
        ProcessHandle processHandle = ProcessHandle.current();
        return processHandle.info().command().orElse("java");
    }

    private static void setAndPrintSetup() {
        System.err.println("Loading " + jarFile.getAbsolutePath());
        System.err.println("Limit is " + LIMIT + "; no group with more then " + LIMIT + " of tests should be merged to bigger ones. Exclude list is of length of " + NOT_MERGE_ABLE_GROUPS.length);
        if (smallGroups) {
            System.err.println("Small groups will be created.");
        } else {
            System.err.println("Small groups will not be created. Intentional?");
        }
        if (splitBigBases) {
            System.err.println("Huge groups will be split to more subsets. Exclude list is of length of " + NOT_SPLIT_ABLE_GROUPS.length);
        } else {
            System.err.println("Small groups will not be created. Intentional?");
        }
        if (parseUseFQN()) {
            System.err.println("FQNs will be used in selectors");
        } else {
            System.err.println("Only N from FQN will be used. This saves space, but risks duplicate matches");
        }
        if (getCoresForPlaylist() == 0) {
            System.err.println("Cores limit for final playlist is not used. Intentional?");
        } else {
            System.err.println("Cores for final playlist are " + getCoresForPlaylist() + ".");
        }
        if (getOutputStyle() == OutputType.GENERATE) {
            System.err.println("Output will print playlist");
        } else {
            System.err.println("Output is set " + getOutputStyle());
        }
    }

    private static String readArg(String[] args) throws MalformedURLException {
        String jar = "../../../ci-jenkins-pipelines/tools/code-tools/jcstress/jcstress-20240222.jar";
        if (args.length > 0) {
            jar = args[0];
        }
        jarFile = new File(jar);
        if (!jarFile.exists()) {
            throw new RuntimeException(jar + " does not exists");
        }
        URL[] cp = {jarFile.toURI().toURL()};
        jarFileClasses = new URLClassLoader(cp);
        return jar;
    }

    private static TestDetails getJcstressTests(String clazz) throws Exception {
        Class cl = jarFileClasses.loadClass(clazz);
        int arbiters = getMethodsAnnotatedWith(cl, new String[]{"Arbiter"}).size();
        int actors = getMethodsAnnotatedWith(cl, new String[]{"Actor"}).size();
        return new ActorArbiter(actors, arbiters);
    }

    public static List<Method> getMethodsAnnotatedWith(final Class<?> type, final String[] annotationTypeNames) {
        final List<Method> methods = new ArrayList<Method>();
        Class<?> klass = type;
        while (klass != Object.class) {
            for (final Method method : klass.getDeclaredMethods()) {
                if (isAnnotationPresent(method, annotationTypeNames)) {
                    methods.add(method);
                }
            }
            klass = klass.getSuperclass();
        }
        return methods;
    }

    private static boolean isAnnotationPresent(Method method, String[] annotationTypeNames) {
        for (Annotation annotation : method.getAnnotations()) {
            for (String annotationTypeName : annotationTypeNames) {
                if (annotation.annotationType().getSimpleName().equals(annotationTypeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void printPlaylist(String jarName, List<GroupWithCases> groups) {
        int cores = getCoresForPlaylist();
        String coresString = "-c " + cores;
        if (cores <= 0) {
            coresString = "";
        }
        System.out.println(header);
        int q = 0;
        for (GroupWithCases group : groups) {
            q++;
            System.out.println(template
                    .replace("-COMMENT-", q + "/" + groups.size() + " " + group.toStringNoRegex())
                    .replace("-JARFILE-", jarName)
                    .replace("-CORES-", coresString)
                    .replace("-TARGET-", group.toTarget())
                    .replace("-REGEX-", group.toSelector()));
        }
        System.out.println(footer);
    }

    private static void testTimesByRunningJcstress(List<GroupWithCases> groups) throws IOException, InterruptedException {
        //warning, many tests needs two or more cores!
        int cores = getCoresForPlaylist();
        final List<GroupWithCases> results = new ArrayList<>();
        //It may happen we will kill it in runtime... good to print at least something
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                calculateStats(results);
            }
        });
        System.err.println("Starting measuring individual targets on " + cores + " core(s) with" + jvm);
        int counter = 0;
        for (GroupWithCases group : groups) {
            counter++;
            Date start = new Date();
            System.err.println(counter + "/" + groups.size() + " " + start + " starting " + group.toStringNoRegex());
            ProcessBuilder ps;
            if (cores <= 0) {
                ps = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath(), "-t", group.toSelector());
            } else {
                ps = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath(), "-c", cores + "", "-t", group.toSelector());
            }
            for (String cmd : ps.command()) {
                System.err.print(cmd + " ");
            }
            System.err.println();
            ps.redirectErrorStream(true);
            Process pr = ps.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            long fakeTime = -1;
            while ((line = in.readLine()) != null) {
                System.err.println(line);
                if (OutputType.TEST == getOutputStyle()) {
                    if (line.contains("Initial completion estimate:")) {
                        line = line.replaceAll(".*Initial completion estimate: ", "").replaceAll(" left;.*", "");
                        pr.destroy();
                        System.err.println("Terminated for " + line);
                        if (line.contains("d+")) {
                            String[] daysHours = line.split("d\\+");
                            fakeTime = hhmmss(daysHours[1]) + (Integer.parseInt(daysHours[0]) * 24 * 60 * 60);
                        } else {
                            fakeTime = hhmmss(line);
                        }
                        break;
                    }
                }
            }
            pr.waitFor();
            in.close();
            Date finished = new Date();
            long deltaSeconds = (finished.getTime() - start.getTime()) / 1000l;
            if (fakeTime > -1) {
                deltaSeconds = fakeTime;
            }
            results.add(new GroupWithCases(group.name, group.regex, new Time((int) deltaSeconds), group.tests.getMainOne()));
            String would = "";
            if (OutputType.TEST == getOutputStyle()) {
                would = " would ";
            }
            System.err.println(finished + would + " finished " + group.name + " in " + (deltaSeconds / 60) + " minutes");
        }
    }

    private static String secondsToDays(long seconds) {
        long days = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long secondsLeft = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);
        return days + "d+" + wrap((int) hours, 2) + ":" + wrap((int) minutes, 2) + ":" + wrap((int) secondsLeft, 2);
    }


    private static int getCoresForPlaylist() {
        return getCores(0);
    }

    private static int getCores(int def) {
        return Integer.parseInt(System.getenv("CORES") == null ? "" + def : System.getenv("CORES"));
    }

    private static OutputType getOutputStyle() {
        String output = System.getenv("OUTPUT");
        if ("do".equals(output)) {
            return OutputType.DO;
        } else if ("test".equals(output)) {
            return OutputType.TEST;
        } else if ("stats".equals(output)) {
            return OutputType.STATS;
        } else if ("regexes".equals(output)) {
            return OutputType.REGEXES;
        } else {
            return OutputType.GENERATE;
        }
    }

    private static long hhmmss(String line) {
        String[] hms = line.split(":");
        return (Integer.parseInt(hms[0]) * 60 * 60) + (Integer.parseInt(hms[1]) * 60) + (Integer.parseInt(hms[2]));
    }

    private static void checksum(List<GroupWithCases> groups, List<GroupWithCases> tests, boolean passes, boolean warning, boolean errors) {
        final List<GroupWithCases> toBeMatched = new ArrayList<>(tests);
        final List<GroupWithCases> matched = new ArrayList<>(tests.size());

        int issues = 0;
        for (GroupWithCases group : groups) {
            int counter = 0;
            for (GroupWithCases test : tests) {
                if (test.name.matches(".*(" + group.regex + ")")) {
                    counter++;
                    matched.add(test);
                    toBeMatched.remove(test);
                }
            }
            if (counter == group.classes) {
                if (passes) {
                    title1(group);
                    System.err.println("OK " + counter);
                }
            } else if (counter < group.classes) {
                if (warning) {
                    title1(group);
                    System.err.println("warning " + counter);
                }
                issues++;
            } else {
                if (errors) {
                    title1(group);
                    System.err.println("ERROR " + counter);
                }
                issues++;
            }
        }
        if (issues == 0) {
            if (matched.size() != tests.size()) {
                for (GroupWithCases test : toBeMatched) {
                    System.err.println("Never matched: " + test);
                }
                throw new RuntimeException("Some tests were not matched! Expected " + tests.size() + " got " + matched.size());
            }
            if (!toBeMatched.isEmpty()) {
                for (GroupWithCases test : toBeMatched) {
                    System.err.println("Never matched: " + test);
                }
                throw new RuntimeException("Some tests were lost! Expected 0 got " + toBeMatched.size());
            }
        } else {
            throw new RuntimeException("Some (" + issues + ") targets run more then they should. Try enhance blacklist, commit and rerun");
        }
    }

    private static void title1(GroupWithCases group) {
        System.err.println("target " + group.toTarget() + " expects " + group.classes + " hits for -t " + group.regex + "; got:");
    }

    private static List<GroupWithCases> mergeSmallGroups(int id, List<GroupWithCases> groups1) {
        GroupWithCases candidate = new GroupWithCases("small.groups." + id, "", new ActorArbiter(0, 0), 0);
        List<GroupWithCases> groups2 = new ArrayList<>(150);
        for (GroupWithCases origGroup : groups1) {
            if (origGroup.tests.getMainOne() > LIMIT) {
                groups2.add(origGroup);
            } else {
                if (candidate.tests.getMainOne() <= LIMIT) {
                    candidate.add(origGroup.tests, origGroup.classes);
                    candidate.appendRegex(origGroup.regex);
                } else {
                    groups2.add(origGroup);
                }
            }
        }
        if (candidate.classes > 0) {
            groups2.add(candidate);
        }
        return groups2;
    }

    private static void print(List<GroupWithCases> groups1) {
        if (isVerbose()) {
            sortByCount(groups1);
            for (GroupWithCases group : groups1) {
                System.err.println(group);
            }
        }
    }

    private static void sortByCount(List<GroupWithCases> groups1) {
        Collections.sort(groups1, new Comparator<GroupWithCases>() {
            @Override
            public int compare(GroupWithCases t1, GroupWithCases t2) {
                return t1.tests.getMainOne() - t2.tests.getMainOne();
            }
        });
    }

    private static List<GroupWithCases> groupTests(List<GroupWithCases> tests, Map<String, Integer> splitGroupsCounters) throws Exception {
        List<String> exludes = Arrays.stream(NOT_MERGE_ABLE_GROUPS).toList();
        List<GroupWithCases> groups1 = new ArrayList<>(300);
        for (GroupWithCases test : tests) {
            int subtestIndex = test.name.lastIndexOf('.');
            String groupName;
            if (subtestIndex < 0) {
                groupName = test.name;
            } else {
                groupName = test.name.substring(0, subtestIndex);
            }
            if (splitBigBases && !isExcludedFromSplitting(groupName)) {
                int currentId = splitGroupsCounters.getOrDefault(groupName, 1);
                String groupNameWithId = groupName + "-" + wrap(currentId, 3);
                GroupWithCases candidate;
                if (exludes.contains(groupName) || test.tests.getMainOne() > LIMIT) {
                    candidate = test;
                } else {
                    candidate = new GroupWithCases(groupNameWithId, test.regex, false);
                    candidate.add(test.tests, test.classes);
                }
                int i = groups1.indexOf(candidate);
                if (i >= 0) {
                    GroupWithCases foundGroup = groups1.get(i);
                    foundGroup.add(candidate.tests, candidate.classes);
                    foundGroup.appendRegex(candidate.regex);
                    if (foundGroup.tests.getMainOne() > LIMIT) {
                        splitGroupsCounters.put(groupName, currentId + 1);
                    }
                } else {
                    groups1.add(candidate);
                }
            } else {
                GroupWithCases candidate;
                if (exludes.contains(groupName) || test.tests.getMainOne() > LIMIT) {
                    candidate = test;
                } else {
                    candidate = new GroupWithCases(groupName, test.regex, false);
                    candidate.add(test.tests, test.classes);
                }
                int i = groups1.indexOf(candidate);
                if (i >= 0) {
                    GroupWithCases foundGroup = groups1.get(i);
                    foundGroup.add(candidate.tests, candidate.classes);
                    foundGroup.appendRegex(candidate.regex);
                } else {
                    groups1.add(candidate);
                }
            }
        }
        return groups1;
    }

    private static boolean isExcludedFromSplitting(String groupName) {
        for (String r : NOT_SPLIT_ABLE_GROUPS) {
            if (groupName.matches(r)) {
                if (isVerbose()) {
                    System.err.println(groupName + " ecluded by " + r);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isVerbose() {
        return "true".equals(System.getenv("VERBOSE"));
    }

    public static String wrap(int number, int length) {
        String s = "" + number;
        return wrap(s, length);
    }

    public static String wrap(String number, int length) {
        String s = number;
        while (s.length() < length) {
            s = "0" + s;
        }
        return s;
    }

    private static List<GroupWithCases> listTestsClassesWithCasesFromJcStressJar(String jvm, String jar) throws Exception {
        int cores = getCoresForPlaylist();
        ProcessBuilder ps;
        if (cores <= 0) {
            ps = new ProcessBuilder(jvm, "-jar", jar, "-l");
        } else {
            ps = new ProcessBuilder(jvm, "-jar", jar, "-c", cores + "", "-l");
        }
        long totalTest = 0;
        ps.redirectErrorStream(true);
        Process pr = ps.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        List<GroupWithCases> tests = new ArrayList<>(500);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("org.")) {
                if (!line.contains(".samples.") && line.contains(".tests.")) {
                    GroupWithCases clazz = new GroupWithCases(line, true);
                    totalTest += clazz.tests.getMainOne();
                    tests.add(clazz);
                }
            }
        }
        pr.waitFor();
        in.close();
        System.err.println("Total test cases: " + totalTest);
        return tests;
    }

    private static int parseLimit() {
        return Integer.parseInt(System.getenv("LIMIT") == null ? "75" : System.getenv("LIMIT"));
    }

    private static boolean parseSmallGroups() {
        if ("false".equals(System.getenv("SMALL_GROUPS"))) {
            return false;
        } else {
            return true;
        }
    }


    private static boolean parseUseFQN() {
        if ("false".equals(System.getenv("FQN"))) {
            throw new IllegalArgumentException("Selectors without FQN are known to cause mishmashes.");
        } else {
            return true;
        }
    }

    private static String[] parseSplitImsplittable() {
        if ("true".equals(System.getenv("SPLIT_ALL"))) {
            return new String[0];
        } else {
            // those groups are  known to contain a lot of tests,
            // however are known to run very quickly, so we do not want them to be split
            return new String[]{
                    /*both thsoe grops are highly affecte dby CORES. 2 cors, 2 hours. 4cores, 45days! */
                    /*So based on agreement with cores, those shoudl be excluded/includes...*/
                    "org.openjdk.jcstress.tests.seqcst.sync.*",
                    "org.openjdk.jcstress.tests.seqcst.volatiles.*",
            };

        }
    }

    private static boolean parseSplitBigBases() {
        if ("false".equals(System.getenv("SPLIT_BIG_BASES"))) {
            return false;
        } else {
            return true;
        }
    }


    public static void calculateStats(List<GroupWithCases> results) {
        System.out.println("Exiting");
        System.out.println("Results gathered: " + results.size() + "; 100% time of longest group, n% time of ideal group");
        if (results.isEmpty()) {
            return;
        }
        //first total time
        long totalTime = 0;
        for (GroupWithCases time : results) {
            totalTime += (long) (time.tests.getMainOne());
        }
        long avgTimeExpected = totalTime / results.size();
        //now details
        sortByCount(results);
        Collections.reverse(results);
        int longest = results.get(0).tests.getMainOne();
        long maxTime = Long.MIN_VALUE;
        long minTime = Long.MAX_VALUE;
        int forLongestPercentAvg = 0;
        int forAvgPercentAvg = 0;
        for (GroupWithCases time : results) {
            if (time.tests.getMainOne() > maxTime) {
                maxTime = time.tests.getMainOne();
            }
            if (time.tests.getMainOne() < minTime) {
                minTime = time.tests.getMainOne();
            }
            int percentLongest = ((time.tests.getMainOne() * 100) / longest);
            forLongestPercentAvg += percentLongest;
            long percentAvg = ((time.tests.getMainOne() * 100) / avgTimeExpected);
            //differences by both directions from 100%
            String sign = "";
            if (percentAvg > 100) {
                percentAvg = percentAvg - 100;
                sign = "+";
            } else {
                percentAvg = 100 - percentAvg;
                sign = "-";
            }
            ;
            forAvgPercentAvg += percentAvg;
            String prefix = "";
            if (time.tests.getMainOne() < 30) {
                prefix = "Error? ";
            }
            System.out.println(prefix + time.name + " with " + time.classes + "tests took " + time.tests + " [" + secondsToDays(time.tests.getMainOne()) + "] (" + percentLongest + "%)(" + sign + percentAvg + "%)");
        }
        System.out.println("Total time: " + totalTime / 60l + " minutes [" + secondsToDays(totalTime) + "]");
        System.out.println("Ideal avg time: " + avgTimeExpected / 60l + " minutes [" + secondsToDays(avgTimeExpected) + "] (100%)");
        System.out.println("Max seen  time: " + maxTime / 60l + " minutes [" + secondsToDays(maxTime) + "] (" + ((maxTime * 100) / avgTimeExpected) + "%)");
        System.out.println("Min seen  time: " + minTime / 60l + " minutes [" + secondsToDays(minTime) + "] (" + ((minTime * 100) / avgTimeExpected) + "%)");
        System.out.println("Avg differecne from longest: " + forLongestPercentAvg / results.size() + "%");
        System.out.println("Avg differecne from ideal: " + (100 - (forAvgPercentAvg / results.size())) + "%");
    }

    private enum OutputType {
        GENERATE, DO, TEST, STATS, REGEXES
    }

    private static class GroupWithCases implements Comparable<GroupWithCases> {
        final String name;
        String regex;
        TestDetails tests;
        int classes;

        public GroupWithCases(String name, boolean clazz) throws Exception {
            this(name, name, clazz);
        }

        public GroupWithCases(String name, String regex, boolean clazz) throws Exception {
            this.name = name;
            if (clazz) {
                if (parseUseFQN()) {
                    this.regex = regex;
                } else {
                    int nameIndex = regex.lastIndexOf('.');
                    if (nameIndex < 0) {
                        this.regex = regex;
                    } else {
                        this.regex = regex.substring(nameIndex+1);
                    }
                }
            } else {
                this.regex = regex;
            }
            if (clazz) {
                String innerGroup = name;
                while (true) {
                    try {
                        tests = getJcstressTests(innerGroup);
                        classes = 1;
                        break;
                    } catch (ClassNotFoundException ex) {
                        innerGroup = replaceLast(innerGroup, "\\.", "$");
                        if (!innerGroup.contains(".")) {
                            throw ex;
                        }
                    }
                }
            } else {
                tests = new ActorArbiter(0, 0);
                classes = 0;
            }
        }

        public GroupWithCases(String name, String regex, TestDetails testDetails, int classes) {
            this.name = name;
            this.regex = regex;
            this.tests = testDetails;
            this.classes = classes;
        }

        private String replaceLast(String string, String what, String by) {
            String reverse = new StringBuffer(string).reverse().toString();
            Matcher matcher = Pattern.compile(what).matcher(reverse);
            reverse = matcher.replaceFirst(matcher.quoteReplacement(by));
            string = new StringBuffer(reverse).reverse().toString();
            return string;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupWithCases that = (GroupWithCases) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public int compareTo(GroupWithCases that) {
            return this.name.compareTo(that.name);
        }

        public void add(TestDetails toAdd, int classes) {
            this.tests.add(toAdd);
            this.classes += classes;
        }

        @Override
        public String toString() {
            return toStringNoRegex() + " via (" + regex + ")";
        }

        public String toStringNoRegex() {
            return name + ": classes " + classes + "/tests " + tests;
        }

        public String toTarget() {
            return name.replace("org.openjdk.jcstress.tests.", "jcstress.");
        }

        public String title() {
            return "target " + toTarget() + " will run " + tests + " tests in -t " + regex;
        }

        public void appendRegex(String regex) {
            if (this.regex.isEmpty()) {
                this.regex = regex;
            } else {
                this.regex = this.regex + "|" + regex;
            }
        }

        public String toSelector() {
            //currently no op, but there were attempts to repalce | operator by different ones or to use more interesting wildchars
            return regex;
        }
    }

    private interface TestDetails {
        int getMainOne();

        int getAll();

        void add(Object testDetails);

    }

    private static class ActorArbiter implements TestDetails {

        int actors;
        int arbiters;

        public ActorArbiter(int actors, int arbiters) {
            this.actors = actors;
            this.arbiters = arbiters;
        }

        public int getMainOne() {
            return actors + arbiters;
        }

        public int getAll() {
            return actors + arbiters;
        }

        public void add(Object testDetails) {
            this.actors += ((ActorArbiter) testDetails).actors;
            this.arbiters += ((ActorArbiter) testDetails).arbiters;
        }

        @Override
        public String toString() {
            return getMainOne() + "(ac/ar:" + actors + "/" + arbiters + ")";
        }
    }

    private static class Time implements TestDetails {
        final int time;

        public Time(int time) {
            this.time = time;
        }

        public int getMainOne() {
            return time;
        }

        public int getAll() {
            return time;
        }

        public void add(Object testDetails) {
            throw new IllegalArgumentException("Time can not be added");
        }

        @Override
        public String toString() {
            return getMainOne() + "s";
        }
    }

}
