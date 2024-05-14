import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Generate {

    //all groups with this or less memebrs will be unified by preffix - if preffix is not one of excluded
    //it is interesting that for 02022024 limit 10 (and excludelist) generated 161 targets. An d with 100 it is 139 (two more items
    // had to be added to excludelist
    private static final int LIMIT = 100;
    //those namesapces can match more then just themselves, so must stay alone
    private static final String[] excludelist =
            new String[]{"org.openjdk.jcstress.tests", "org.openjdk.jcstress.tests.accessAtomic.fields",
                    "org.openjdk.jcstress.tests.atomicity", "org.openjdk.jcstress.tests.atomicity.varHandles.arrays",
                    "org.openjdk.jcstress.tests.atomicity.varHandles.fields", "org.openjdk.jcstress.tests.atomics",
                    "org.openjdk.jcstress.tests.atomics.booleans", "org.openjdk.jcstress.tests.fences",
                    "org.openjdk.jcstress.tests.fences.varHandles", "org.openjdk.jcstress.tests.locks",
                    "org.openjdk.jcstress.tests.memeffects.basic", "org.openjdk.jcstress.tests.tearing",
                    "org.openjdk.jcstress.tests.atomics.integer", "org.openjdk.jcstress.tests.atomics.longs",
                    "org.openjdk.jcstress.tests.locks.mutex",
                    "org.openjdk.jcstress.tests.locks.stamped","org.openjdk.jcstress.tests.memeffects.basic.atomic"
            };

    private static class GroupWithCases implements Comparable<GroupWithCases> {
        final String group;
        int count = 1;

        public GroupWithCases(String group) {
            this.group = group;
        }

        public GroupWithCases(String group, int count) {
            this.group = group;
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupWithCases that = (GroupWithCases) o;
            return Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group);
        }

        @Override
        public int compareTo(GroupWithCases that) {
            return this.group.compareTo(that.group);
        }

        public void add() {
            count++;
        }

        public void add(int i) {
            count += i;
        }

        @Override
        public String toString() {
            return group + ": " + count;
        }

        public String toTarget() {
            return group.replace("org.openjdk.jcstress.tests.", "jcstress.");
        }

        public String title() {
            return "target " + toTarget() + " will run " + count + " tests in -t " + group;
        }
    }

    public static void main(String... args) throws Exception {
        String jar = "../../jcstress/jcstress.jar";
        if (args.length > 0) {
            jar = args[0];
        }
        String jarName = new File(jar).getName();
        List<String> tests = listTestsFromJars(jar);
        System.err.println("total tests: " + tests.size());
        List<GroupWithCases> groups1 = groupTests(tests);
        System.err.println("Groups 1: " + groups1.size());
        //print(groups1);
        List<GroupWithCases> groups2 = mergeSmallGroups(groups1);
        System.err.println("Groups 2 : " + groups2.size());
        //print(groups2);
        Collections.sort(groups2);
        System.err.println("Checksum!");
        int issues = checksum(groups2, tests, false, false, true);
        if (issues > 0) {
            throw new RuntimeException("Some (" + issues + ") targets run more then they should. Enhance blacklist, commit and rerun");
        }
        sortByCount(groups2);
        System.out.println(header);
        for (GroupWithCases group: groups2) {
            System.out.println(template
                    .replace("-JARFILE-", jarName)
                    .replace("-TARGET-", group.toTarget())
                    .replace("-REGEX-", group.group));
        }
        System.out.println(footer);


    }

    private static int checksum(List<GroupWithCases> groups2, List<String> tests, boolean passes, boolean warning, boolean errors) {
        int issues = 0;
        for (GroupWithCases group : groups2) {
            int counter = 0;
            for (String test : tests) {
                if (test.matches(".*" + group.group + ".*")) {
                    counter++;
                }
            }
            if (counter == group.count) {
                if (passes) {
                    title1(group);
                    System.err.println("OK " + counter);
                }
            } else if (counter < group.count) {
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
        return issues;
    }

    private static void title1(GroupWithCases group) {
        System.err.println("target " + group.toTarget() + " expects " + group.count + " hits for -t " + group.group + "; got:");
    }


    private static List<GroupWithCases> mergeSmallGroups(List<GroupWithCases> groups1) {
        List<String> exludes = Arrays.stream(excludelist).toList();
        List<GroupWithCases> groups2 = new ArrayList<>(150);
        for (GroupWithCases origGroup : groups1) {
            String groupName = origGroup.group.substring(0, origGroup.group.lastIndexOf('.'));
            if (origGroup.count > LIMIT || exludes.contains(groupName)) {
                groups2.add(origGroup);
            } else {
                GroupWithCases candidate = new GroupWithCases(groupName, origGroup.count);
                int i = groups2.indexOf(candidate);
                if (i >= 0) {
                    groups2.get(i).add(candidate.count);
                } else {
                    groups2.add(candidate);
                }
            }
        }
        return groups2;
    }

    private static void print(List<GroupWithCases> groups1) {
        sortByCount(groups1);
        for (GroupWithCases group : groups1) {
            System.err.println(group);
        }
    }

    private static void sortByCount(List<GroupWithCases> groups1) {
        Collections.sort(groups1, new Comparator<GroupWithCases>() {
            @Override
            public int compare(GroupWithCases t1, GroupWithCases t2) {
                return t1.count - t2.count;
            }
        });
    }

    private static List<GroupWithCases> groupTests(List<String> tests) {
        List<String> exludes = Arrays.stream(excludelist).toList();
        List<GroupWithCases> groups1 = new ArrayList<>(300);
        for (String test : tests) {
            String groupName = test.substring(0, test.lastIndexOf('.'));
            GroupWithCases candidate;
            if (exludes.contains(groupName)) {
                candidate = new GroupWithCases(test);
            } else {
                candidate = new GroupWithCases(groupName);
            }
            int i = groups1.indexOf(candidate);
            if (i >= 0) {
                groups1.get(i).add();
            } else {
                groups1.add(candidate);
            }
        }
        return groups1;
    }

    private static List<String> listTestsFromJars(String jar) throws IOException, InterruptedException {
        ProcessBuilder ps = new ProcessBuilder("java", "-jar", jar, "-l");
        ps.redirectErrorStream(true);
        Process pr = ps.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        List<String> tests = new ArrayList<>(500);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("org.")) {
                if (!line.contains(".samples.") && line.contains(".tests.")) tests.add(line);
            }
        }
        pr.waitFor();
        in.close();
        return tests;
    }

    private static  final String template= """
            <test>
            	<testCaseName>-TARGET-</testCaseName>
                   <command>$(JAVA_COMMAND) $(JVM_OPTIONS) -jar $(Q)$(LIB_DIR)$(D)-JARFILE-$(Q) $(APPLICATION_OPTIONS) -t -REGEX-; \\
                   $(TEST_STATUS)</command>
            	<levels>
            		<level>dev</level>
            	</levels>
            	<groups>
            		<group>system</group>
            	</groups>
            </test>""";
    private static  final String header= """
<playlist xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../../TKG/playlist.xsd">""";
    private static  final String footer= "</playlist>";
}
