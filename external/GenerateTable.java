import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
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
                        line = line + statusToColor(status) + "|";
                    }
                }
                System.out.println("|" + jdk + "/" + os + osSuffix + line);
            }
        }
    }

    private String statusToColor(String status) {
        return "<span style='color:" + getColor(status) + ";'>" + status + "</span>";
    }

    private String getColor(String status) {
        String color = "yellow";
        switch (status) {
            case "PASSED":
                return color = "green";
            case "FAILED":
                return color = "red";
            case "ERROR":
                return color = "DarkRed";
            default:
                return color = "blue";
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
