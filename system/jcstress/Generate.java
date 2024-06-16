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


public class Generate {

    // longest generated classes have 2131 tests
    private static final int LIMIT = parseLimit();
    private static final int MAX_NATURAL_ITERATIONS = parseMaxNaturalIterations();

    private static final boolean SMALL_GROUPS = parseSmallGroups();

    //those namespaces will not be merged together. This is literal eaquls comparison
    private static final String[] NOT_MERGE_ABLE_GROUPS = new String[]{
            "org",
            "org.openjdk",
            "org.openjdk.jcstress",
            "org.openjdk.jcstress.tests",
    };

    private static final boolean SPLIT_BIG_BASES = parseSplitBigBases();

    private static final String[] NOT_SPLIT_ABLE_GROUPS = parseSplitImsplittable();
    private static final String TEMPLATE = """
            <test>
            	<testCaseName>-TARGET-</testCaseName>
            	<!-- -COMMENT-  -->
                <!-- -DISABLED-  -->
                   <command>
                     if [ "x${JC_CORES}" = "x" ] ; then export JC_CORES="-CORES-" ; else JC_CORES="-c $JC_CORES" ;fi\\
                     if [ "x${JC_TIME_BUDGET}" = "x" ] ; then export JC_TIME_BUDGET="-TB-" ; else JC_TIME_BUDGET="-tb $JC_TIME_BUDGET" ;fi\\
                     $(JAVA_COMMAND) $(JVM_OPTIONS) -jar $(Q)$(LIB_DIR)$(D)-JARFILE-$(Q) $(APPLICATION_OPTIONS) $JC_TIME_BUDGET  $JC_CORES  $(APPLICATION_OPTIONS) -t "-REGEX-"; \\
                     $(TEST_STATUS)
                   </command>
            	<levels>
            		<level>dev</level>
            	</levels>
            	<groups>
            		<group>system</group>
            	</groups>
            </test>""";
    private static final String HEADER = """
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
    private static final String FOOTER = "</playlist>";
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
        if (SMALL_GROUPS) {
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
        final Map<String, Integer> splitGroupsCounters = new HashMap<>();
        while (true) {
            i++;
            if (i > MAX_NATURAL_ITERATIONS) {
                break;
            }
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
        if (SMALL_GROUPS) {
            System.err.println("Small groups will be created.");
        } else {
            System.err.println("Small groups will not be created. Intentional?");
        }
        if (SPLIT_BIG_BASES) {
            System.err.println("Huge groups will be split to more subsets. Exclude list is of length of " + NOT_SPLIT_ABLE_GROUPS.length);
        } else {
            System.err.println("Huge groups will NOT be split to more subsets. Intentional?");
        }
        System.err.println("Max count of natural grouping iterations is " + MAX_NATURAL_ITERATIONS);
        if (parseUseFQN()) {
            System.err.println("FQNs will be used in selectors");
        } else {
            System.err.println("Only N from FQN will be used. This saves space, but risks duplicate matches");
        }
        if (getCoresForPlaylist() == 0) {
            System.err.println("Cores limit for final playlist is not used");
        } else {
            System.err.println("Cores for final playlist are " + getCoresForPlaylist() + ". Intentional?");
        }
        if (isTimeBudgetSet()) {
            System.err.println("Time budget is " + getCoresForPlaylist() + ". Intentional?");
        } else {
            System.err.println("Time budget is not used. Intentional?");
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
        String timeBudgetString = "";
        if (isTimeBudgetSet()) {
            timeBudgetString = "-tb " + getTimeBudget();
        }
        int cores = getCoresForPlaylist();
        String coresString = "";
        if (cores > 0) {
            coresString = "-c " + cores;
        }
        System.out.println(HEADER);
        System.out.println(TEMPLATE
                .replace("-DISABLED-", "")
                .replace("-COMMENT-", "The only enabled target running all")
                .replace("-JARFILE-", jarName)
                .replace("-CORES-", coresString)
                .replace("-TB-", "-tb 1h")
                .replace("-TARGET-", "all")
                .replace("-REGEX-", ".*"));
        int q = 0;
        for (GroupWithCases group : groups) {
            q++;
            System.out.println(TEMPLATE
                    .replace("-DISABLED-", "" +
                            "                   <disables>\n" +
                            "                     <disable>\n" +
                            "                       <comment>all targets except main one are for manual usage only</comment>\n" +
                            "                     </disable>\n" +
                            "                   </disables>")
                    .replace("-COMMENT-", q + "/" + groups.size() + " " + group.toStringNoRegex())
                    .replace("-JARFILE-", jarName)
                    .replace("-CORES-", coresString)
                    .replace("-TB-", timeBudgetString)
                    .replace("-TARGET-", group.toTarget())
                    .replace("-REGEX-", group.toSelector()));
        }
        System.out.println(FOOTER);
    }

    private static boolean isTimeBudgetSet() {
        return !(getTimeBudget() == null || getTimeBudget().trim().equals("0") || getTimeBudget().trim().equals(""));
    }

    private static void testTimesByRunningJcstress(List<GroupWithCases> groups) throws IOException, InterruptedException {
        final List<GroupWithCases> results = new ArrayList<>();
        //It may happen we will kill it in runtime... good to print at least something
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                calculateStats(results, groups);
            }
        });
        System.err.println("Starting measuring individual targets on " + getCoresForPlaylist() + " core(s) with" + jvm);
        int counter = 0;
        for (GroupWithCases group : groups) {
            counter++;
            Date start = new Date();
            System.err.println(counter + "/" + groups.size() + " " + start + " starting " + group.toStringNoRegex());
            List<String> args = getBasicJcstressCommand(jvm);
            args.add("-t");
            args.add(group.toSelector());
            ProcessBuilder ps = new ProcessBuilder(args.toArray(new String[0]));
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

    /**
     * @return time with unit. Eg 100s or 30m
     */
    private static String getTimeBudget() {
        return System.getenv("TIME_BUDGET") == null ? null : System.getenv("TIME_BUDGET");
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
            if (SPLIT_BIG_BASES && !isExcludedFromSplitting(groupName)) {
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
        List<String> args = getBasicJcstressCommand(jvm);
        args.add("-l");
        ProcessBuilder ps = new ProcessBuilder(args.toArray(new String[0]));
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

    private static List<String> getBasicJcstressCommand(String jvm) {
        List<String> args = new ArrayList<>();
        args.add(jvm);
        args.add("-jar");
        args.add(jarFile.getAbsolutePath());
        //warning, many tests needs two or more cores!
        int cores = getCoresForPlaylist();
        if (cores > 0) {
            args.add("-c");
            args.add(cores + "");
        }
        if (isTimeBudgetSet()) {
            args.add("-tb");
            args.add(getTimeBudget());
        }
        return args;
    }

    private static int parseLimit() {
        return Integer.parseInt(System.getenv("LIMIT") == null ? "100" : System.getenv("LIMIT"));
    }

    private static boolean parseSmallGroups() {
        if ("true".equals(System.getenv("SMALL_GROUPS"))) {
            return true;
        } else {
            return false;
        }
    }

    private static int parseMaxNaturalIterations() {
        //for very long, default was Integer.MAX_VALUE
        return Integer.parseInt(System.getenv("MAX_NATURAL_ITERATIONS") == null ? "3" : System.getenv("MAX_NATURAL_ITERATIONS"));
    }


    private static boolean parseUseFQN() {
        if ("true".equals(System.getenv("FQN"))) {
            return true;
        } else {
            return false;
        }
    }

    private static String[] parseSplitImsplittable() {
        if ("false".equals(System.getenv("SPLIT_ALL"))) {
            // those groups are  known to contain a lot of tests,
            // however are known to run very quickly, so we do not want them to be split
            return new String[]{
                    /*both thsoe grops are highly affecte dby CORES. 2 cors, 2 hours. 4cores, 45days! */
                    /*So based on agreement with cores, those shoudl be excluded/includes...*/
                    "org.openjdk.jcstress.tests.seqcst.sync.*",
                    "org.openjdk.jcstress.tests.seqcst.volatiles.*",
            };
        } else {
            return new String[0];
        }
    }

    private static boolean parseSplitBigBases() {
        if ("true".equals(System.getenv("SPLIT_BIG_BASES"))) {
            return true;
        } else {
            return false;
        }
    }


    public static void calculateStats(List<GroupWithCases> results, List<GroupWithCases> resultsExpected) {
        System.out.println("Exiting");
        System.out.println("Results gathered: " + results.size() + " of expected " + resultsExpected.size() + "; 100% time of longest group, n% time of ideal group from really run results");
        if (results.isEmpty()) {
            return;
        }
        //first total time
        long totalTime = 0;
        for (GroupWithCases time : results) {
            totalTime += (long) (time.tests.getMainOne());
        }
        long avgTimeExpected = totalTime / results.size();
        if (isTimeBudgetSet()) {
            System.out.println(" - The calculations from real run are provided. They are based on meassured times and real results (not expected results)");
            System.out.println(" - You had -tb " + getTimeBudget() + " set, so yours expected avg tim is " + getTimeBudget() + " and not the measured real values bellow");
            System.out.println(" - Your workload should have run " + resultsExpected.size() + " * " + getTimeBudget() + " but run " + secondsToDays(totalTime) + " and was finished " + results.size() + " from " + resultsExpected.size());
            System.out.println(" - See https://bugs.openjdk.org/browse/CODETOOLS-7903750");
        }
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
            if (percentAvg == 100) {
                percentAvg = 100 - percentAvg;
                sign = "";
            } else if (percentAvg > 100) {
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

    private interface TestDetails {
        int getMainOne();

        int getAll();

        void add(Object testDetails);

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
                    this.regex = regex.replace("org.openjdk.jcstress.", "");
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
