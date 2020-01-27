import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModifyZHTW {
    public static void main(String[] args) throws Exception {
        Set<String> tzData = CheckZHTW.getDataInstance();
        for (String s : args) {
            Path path = Paths.get(s);
            List<String> lines = Files.readAllLines(Paths.get(s));
            Files.write(path, lines.stream()
                .filter(x -> x.startsWith("<") || !tzData.contains(x.split("\t")[1]))
                .collect(Collectors.toList()));
        }
    }
}
