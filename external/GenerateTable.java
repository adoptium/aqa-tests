import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateTable {

    private static final String TEST_EXCLUDE_LIST = "TEST_EXCLUDE_LIST";
    public static final String HASH_AND_DIR = "8e6b52ff5059ed40f22ffac52800b887a1b50239/external/results";
    private final String dir;
    private final String excludes;

    public GenerateTable(String dir) {
        this.dir = dir;
        if (System.getenv(TEST_EXCLUDE_LIST) == null || System.getenv(TEST_EXCLUDE_LIST).isEmpty()) {
            excludes = "(criu|tck-ubi-test|external_custom)";
        } else {
            excludes = System.getenv(TEST_EXCLUDE_LIST);
        }
    }

    public static void main(String[] args) throws Exception {
        String dir = "./results";
        if (args.length > 0) {
            dir = args[0];
        }
        new GenerateTable(dir).run();
    }

    private void run() throws IOException {
        String[] jdks = new File(dir).list();
        Set<String> columns = new HashSet<>();
        Files.walkFileTree(new File(dir).toPath(), new HashSet<>(0), 3, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String column = stripStatus(file.toFile().getName());
                if (!column.split(":")[0].matches(excludes)) {
                    columns.add(column);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        List<String> allColums = new ArrayList<>(columns);
        Collections.sort(allColums);
        Arrays.sort(jdks, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String[] s1 = o1.split("[^0-9]");
                String[] s2 = o2.split("[^0-9]");
                return Integer.valueOf(s1[0]).compareTo(Integer.valueOf(s2[0]));
            }
        });
        String headerBody = allColums.stream().collect(Collectors.joining(" | "));
        System.out.println("## pass/fail matrix 1.1.2025");
        System.out.println("full/raw links: ");
        System.out.println("pass: " + statusToColor("PASSED", null) + statusToColor("PASSED", null));
        System.out.println("fail: " + statusToColor("FAILED", null) + statusToColor("FAILED", null));
        System.out.println("error: " + statusToColor("ERROR", null) + statusToColor("ERROR", null));
        System.out.println("unknown: " + statusToColor("blah", null) + statusToColor("bleh", null));
        System.out.print("skipped: " + statusToColor("skipped", null) + statusToColor("skipped", null));
        System.out.println(" - most likely ilegal combination of conditions, most likely bad jdk version. This is iterating through targets, wich may be excluded in playlist");
        System.out.println(" From other links `Dir` `Test.sh` `Build.xml` `test.Properties` `Readme.md` and `playlist.Xml` - only dir is mandatory, although playlist and build are usually there too.");
        System.out.println("|   | " + headerBody + " |");
        String headerDelimiter = allColums.stream().map(a -> "---").collect(Collectors.joining("|"));
        System.out.println("|---|" + headerDelimiter + " |");

        for (String jdk : jdks) {
            String[] oses = new File(dir, jdk).list();
            Arrays.sort(oses);
            for (String os : oses) {
                String osSuffix = "";
                if (os.equals("DEFAULT")) {
                    osSuffix = "(ubuntu)";
                }
                String[] suites = new File(new File(dir, jdk), os).list();
                Arrays.sort(suites);
                String line = "|";
                for (int x = 0; x < suites.length; x++) {
                    String file = suites[x];
                    String status = getStatus(file);
                    String test = stripStatus(file, status);
                    if (x >= allColums.size()) {
                        throw new RuntimeException("column " + x + " do nto exists for " + test + " for " + jdk + "/" + os);
                    }
                    if (!test.split(":")[0].matches(excludes)) {
                        if (!allColums.get(x).equals(test)) {
                            throw new RuntimeException("column " + x + " should be " + allColums.get(x) + " but is " + test + " for " + jdk + "/" + os);
                        }
                        line = line + statusToNiceLink(file, os, jdk, status) + statusToRawLink(file, os, jdk, status) + linksToPlayList(file, status)  + "|";
                    }
                }
                System.out.println("|" + jdk + "/" + os + osSuffix + line);
            }
        }
    }
    private String linksToPlayList(String file, String status) throws IOException {
        String dir=file.replaceAll(":.*", "");
        String l1= "<a href='https://github.com/adoptium/aqa-tests/tree/master/external/" + dir + "'>d<a>";
        String l2= "<a href='https://github.com/adoptium/aqa-tests/tree/master/external/" + dir +"/test.sh'>t<a>";
        String l3= "<a href='https://github.com/adoptium/aqa-tests/tree/master/external/" + dir +"/build.xml'>b<a>";
        String l4= "<a href='https://github.com/adoptium/aqa-tests/tree/master/external/" + dir +"/test.properties'>p<a>";
        String l5= "<a href='https://github.com/adoptium/aqa-tests/tree/master/external/" + dir +"/README.md'>r<a>";
        String l6= "<a href='https://github.com/adoptium/aqa-tests/tree/master/external/" + dir +"/playlist.xml'>x<a>";
        return l1 + l2 + l3 + l4 + l5 + l6;
    }


    private String statusToNiceLink(String file, String os, String jdk, String status) throws IOException {
        String id=jdk + "/" + os + "/" + file;
        return "<a href='https://github.com/judovana/aqa-tests/blob/" + HASH_AND_DIR + "/" + id + "'>" + statusToColor(status, id) + "<a>";
    }

    private String statusToRawLink(String file, String os, String jdk, String status) throws IOException {
        String id=jdk + "/" + os + "/" + file;
        return "<a href='https://raw.githubusercontent.com/judovana/aqa-tests/" + HASH_AND_DIR + "/" + jdk + "/" + os + "/" + file + "'>" + statusToColor(status, id) + "<a>";
    }


    private boolean isSkipped(String status, String id) throws IOException {
        if (id == null) { return false; }
        File f = new File(dir+"/"+id);
        if (! f.exists()  ) { return false; }
        String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        return content.contains("Error: cannot find the following tests: " + id.replaceAll(".*:", "").replace("-"+status, ""));
    }

    private String statusToColor(String status, String id) throws IOException {
        if (isSkipped(status, id)) {
          return ":yellow_heart:";
        }
        String color = ":blue_heart:";
        switch (status) {
            case "PASSED":
                return color = ":green_heart:";
            case "FAILED":
                return color = ":broken_heart:";
//                return color = ":heart:";  do not the swap fromm broken_heart->heart and 2xbroken_heart to 1x broken_heart ; looks weird
            case "ERROR":
//                return color = ":broken_heart::broken_heart:";
                return color = ":boom:";
//                return color = ":broken_heart:"; do not the swap fromm broken_heart->heart and 2xbroken_heart to 1x broken_heart ; looks weird
            case "skipped": // this is artificial, normlaly it is returned only from file content
                return color = ":yellow_heart:";
            default:
                return color = ":purple_heart:";
        }
    }

    private static String stripStatus(String file) {
        return stripStatus(file, getStatus(file));
    }

    private static String stripStatus(String file, String status) {
        return file.replace("-" + status, "");
    }

    private static String getStatus(String id) {
        String[] split = id.split("-");
        if (split.length == 1) {
            return "UNKNOWN";
        } else {
            return split[split.length - 1];
        }

    }
}
