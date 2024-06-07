import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Generator can generate aprox 7-36 groups, based on limit
 * Limit 5 - 236 groups, from those  9 "small gathering" (~10minutes per group)
 * Limit 50 - 83 groups, from those  17 "small gathering" (~90minutes per group)
 * Limit 100 - 53 groups, from those  19 "small gathering" (~4h per group)
 * Limit 250 - 26 groups, from those  14 "small gathering"
 * Limit 500 - 15 groups, from those  11 "small gathering"
 * Limit 1000 - 9 groups, from those  6 "small gathering"
 * Limit 2000 - 6 groups, from those  3 "small gathering" (~1.5day each)
 * Limit 50000 - 5 groups, from those  1 "small gathering" (unklnown, slector to long)
 * al tests in batch ~11.5 of day
 * Note that it do not splig big generated classes. Those have some 2000 tests, and will remain not split
 * <p>
 * Note, that LIMIT is not strictly honored. It is jsut saying, that if there LIMIT of testes or more, it wil not be grouped.
 * Note, that NOT_MERGE_ABLE_GROUPS may nopt be complete. It depends on size of LIMIT, and I had not tried all.
 */
public class Generate {

    // longest generated classes have 2131 tests
    private static final int LIMIT = Integer.parseInt(System.getenv("LIMIT") == null ? "100" : System.getenv("LIMIT"));
    private static final boolean smallGroups = true;
    //those namespaces can match more than just themselves, so can not be "nicely" merged (but will be gathered in small.groups if possible)
    private static final String[] NOT_MERGE_ABLE_GROUPS =
            new String[]{
                    "org.openjdk.jcstress.tests",
                    "org.openjdk.jcstress.tests.acqrel",
                    "org.openjdk.jcstress.tests.copy",
                    "org.openjdk.jcstress.tests.tearing",
                    "org.openjdk.jcstress.tests.init",
                    "org.openjdk.jcstress.tests.accessAtomic",
                    "org.openjdk.jcstress.tests.coherence",
                    "org.openjdk.jcstress.tests.atomicity",
                    "org.openjdk.jcstress.tests.atomicity.varHandles",
                    "org.openjdk.jcstress.tests.atomicity.varHandles.arrays",
                    "org.openjdk.jcstress.tests.atomicity.varHandles.fields",
                    "org.openjdk.jcstress.tests.atomics",
                    "org.openjdk.jcstress.tests.atomics.booleans",
                    "org.openjdk.jcstress.tests.atomics.integer",
                    "org.openjdk.jcstress.tests.atomics.longs",
                    "org.openjdk.jcstress.tests.fences",
                    "org.openjdk.jcstress.tests.fences.varHandles",
                    "org.openjdk.jcstress.tests.locks",
                    "org.openjdk.jcstress.tests.locks.mutex",
                    "org.openjdk.jcstress.tests.locks.stamped",
                    "org.openjdk.jcstress.tests.memeffects.basic",
                    "org.openjdk.jcstress.tests.memeffects.basic.atomic",
                    "org.openjdk.jcstress.tests.acqrel.varHandles",
                    "org.openjdk.jcstress.tests.accessAtomic.fields",
                    "org.openjdk.jcstress.tests.accessAtomic.varHandles",
                    "org.openjdk.jcstress.tests.coherence.varHandles"
            };
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

    private static int getJcstressTests(String clazz) throws Exception {
        Class cl = jarFileClasses.loadClass(clazz);
        int tests = getMethodsAnnotatedWith(cl, new String[]{"Actor", "Arbiter"}).size();
        return tests;
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

    public static void main(String... args) throws Exception {
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
        String jarName = new File(jar).getName();
        System.err.println("Loading " + jarFile.getAbsolutePath());
        System.err.println("Limit is " + LIMIT + "; no group with more then " + LIMIT + " of tests should be merged down.");
        ProcessHandle processHandle = ProcessHandle.current();
        jvm = processHandle.info().command().orElse("java");
        List<GroupWithCases> tests = listTestsFromJars(jvm, jar);
        System.err.println("total tests files: " + tests.size());
        print(tests);
        List<GroupWithCases> groups = tests;
        int i = 0;
        while (true) {
            i++;
            List<GroupWithCases> bigCandidate = groupTests(groups);
            if (bigCandidate.size() == groups.size()) {
                break;
            }
            groups = bigCandidate;
            System.err.println("Natural Groups " + i + " : " + groups.size());
            print(groups);
        }
        if (smallGroups) {
            int x = 0;
            while (true) {
                x++;
                List<GroupWithCases> bigCandidate = mergeSmallGroups(x, groups);
                if (bigCandidate.size() == groups.size()) {
                    break;
                }
                groups = bigCandidate;
                System.err.println("Small Groups " + x + " : " + groups.size());
                print(groups);
            }
        }
        Collections.sort(groups);
        System.err.println("Checksum:");
        checksum(groups, tests, false, true, true);
        System.err.println("Passed!");
        sortByCount(groups);
        print(groups);
        if ("true".equals(System.getenv("JUST_REGEXES"))) {
            for (GroupWithCases group : groups) {
                System.out.println(group.regex);
            }
        } else if ("do".equals(System.getenv("JUST_REGEXES")) || "test".equals(System.getenv("JUST_REGEXES"))) {
            //warning, many tests needs two or more cores
            int cores = Integer.parseInt(System.getenv("CORES") == null ? "2" : System.getenv("CORES"));
            final List<GroupWithCases> results = new ArrayList<>();
            //It may happen ne will kill it in runtime.. good to print at least something
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    System.out.println("Exited");
                    System.out.println("Results gathered: " + results.size());
                    if (results.isEmpty()) {
                        return;
                    }
                    sortByCount(results);
                    Collections.reverse(results);
                    int longest = results.get(0).tests;
                    long totalTime = 0;
                    for (GroupWithCases time : results) {
                        totalTime += (long) (time.tests);
                        int percent = (time.tests / (longest / 100));
                        String prefix = "";
                        if (time.tests < 30) {
                            prefix = "Error? ";
                        }
                        System.out.println(prefix + time.name + " with " + time.classes + "tests took " + time.tests + "s (" + percent + "%)");
                    }
                    System.out.println("Total time: " + totalTime / 60l + " minutes");
                }
            });
            System.out.println("Starting measuring individual targets on " + cores + " core(s) with" + jvm);
            for (GroupWithCases group : groups) {
                Date start = new Date();
                System.out.println(start + " starting " + group.toStringNoRegex());
                ProcessBuilder ps = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath(), "-c", cores + "", "-t", group.toSelector());
                for (String cmd : ps.command()) {
                    System.out.print(cmd + " ");
                }
                System.out.println();
                ps.redirectErrorStream(true);
                Process pr = ps.start();
                BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line); //?
                }
                pr.waitFor();
                in.close();
                Date finished = new Date();
                long deltaSeconds = (finished.getTime() - start.getTime()) / 1000l;
                results.add(new GroupWithCases(group.name, group.regex, (int) deltaSeconds, group.tests));
                System.out.println(finished + " finished " + group.name + " in " + (deltaSeconds / 60) + " minutes");
            }
        } else {
            int cores = Integer.parseInt(System.getenv("CORES") == null ? "0" : System.getenv("CORES"));
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


    }

    private static void checksum(List<GroupWithCases> groups, List<GroupWithCases> tests, boolean passes, boolean warning, boolean errors) {
        int issues = 0;
        int totalcounter = 0;
        List<GroupWithCases> matched = new ArrayList<>(tests.size());
        for (GroupWithCases group : groups) {
            int counter = 0;
            for (GroupWithCases test : tests) {
                if (test.name.matches(".*(" + group.regex + ")")) {
                    counter++;
                    totalcounter++;
                    matched.add(test);
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
        if (issues == 0 && totalcounter != tests.size()) {
            for (GroupWithCases test : tests) {
                if (!matched.contains(test)) {
                    System.err.println("Never matched: " + test);
                }
            }
            throw new RuntimeException("Some tests were lost on the fly! Expected " + tests.size() + " got " + totalcounter);
        }
        if (issues > 0) {
            throw new RuntimeException("Some (" + issues + ") targets run more then they should. Enhance blacklist, commit and rerun");
        }
    }

    private static void title1(GroupWithCases group) {
        System.err.println("target " + group.toTarget() + " expects " + group.classes + " hits for -t " + group.regex + "; got:");
    }

    private static List<GroupWithCases> mergeSmallGroups(int id, List<GroupWithCases> groups1) {
        GroupWithCases candidate = new GroupWithCases("small.groups." + id, "", 0, 0);
        List<GroupWithCases> groups2 = new ArrayList<>(150);
        for (GroupWithCases origGroup : groups1) {
            if (origGroup.tests >= LIMIT) {
                groups2.add(origGroup);
            } else {
                if (candidate.tests < LIMIT) {
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
        if ("true".equals(System.getenv("VERBOSE"))) {
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
                return t1.tests - t2.tests;
            }
        });
    }

    private static List<GroupWithCases> groupTests(List<GroupWithCases> tests) throws Exception {
        List<String> exludes = Arrays.stream(NOT_MERGE_ABLE_GROUPS).toList();
        List<GroupWithCases> groups1 = new ArrayList<>(300);
        for (GroupWithCases test : tests) {
            String groupName = test.name.substring(0, test.name.lastIndexOf('.'));
            GroupWithCases candidate;
            if (exludes.contains(groupName) || test.tests > LIMIT) {
                candidate = test;
            } else {
                candidate = new GroupWithCases(groupName, test.regex, false);
                candidate.add(test.tests, test.classes);
            }
            int i = groups1.indexOf(candidate);
            if (i >= 0) {
                groups1.get(i).add(candidate.tests, candidate.classes);
                groups1.get(i).appendRegex(candidate.regex);
            } else {
                groups1.add(candidate);
            }
        }
        return groups1;
    }

    private static List<GroupWithCases> listTestsFromJars(String jvm, String jar) throws Exception {
        ProcessBuilder ps = new ProcessBuilder(jvm, "-jar", jar, "-l");
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
                    totalTest += clazz.tests;
                    tests.add(clazz);
                }
            }
        }
        pr.waitFor();
        in.close();
        System.err.println("Total test cases: " + totalTest);
        return tests;
    }

    private static class GroupWithCases implements Comparable<GroupWithCases> {
        final String name;
        String regex;
        int tests;
        int classes;

        public GroupWithCases(String name, boolean clazz) throws Exception {
            this(name, name, clazz);
        }

        public GroupWithCases(String name, String regex, boolean clazz) throws Exception {
            this.name = name;
            this.regex = regex;
            if (clazz) {
                String innerGroup = name;
                while (true) {
                    try {
                        int testsFound = getJcstressTests(innerGroup);
                        tests = testsFound;
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
                tests = 0;
                classes = 0;
            }
        }

        public GroupWithCases(String name, String regex, int tests, int classes) {
            this.name = name;
            this.regex = regex;
            this.tests = tests;
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

        public void add(int tests, int classes) {
            this.tests += tests;
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
            //currently no op, but there were attempts to repalce | operator by different ones
            return regex;
        }
    }
}
