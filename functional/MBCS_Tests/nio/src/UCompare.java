import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;

public class UCompare {
    public static String readUData(String s, String csName)
        throws Exception {
        return Files.readAllLines(Paths.get(s), Charset.forName(csName))
            .stream()
            .collect(Collectors.joining(System.lineSeparator()));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println(
                "Usage: java UCompare file1 encoding1 file2 encoding2");
            System.exit(1);
        } else {
            System.exit(readUData(args[0], args[1])
                .equals(readUData(args[2], args[3])) ? 0 : 2);
        } 
    }
}
