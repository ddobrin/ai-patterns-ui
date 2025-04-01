package ai.patterns.utils;

import ai.patterns.CapitalData;
import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class CapitalsFileLoader {

    public static List<CapitalData> loadFiles(String fileType) throws IOException {
        Properties CAPITAL_PROPERTIES = new Properties();
        CAPITAL_PROPERTIES.load(Files.newBufferedReader(Path.of("data/capitals/capital.properties")));

        Path textFilesPath = Paths.get("data/capitals/" + fileType);

        return Files.list(textFilesPath)
            .map(path -> {
                String fileNameWithoutExtension = path.toFile().getName().replace("." + fileType, "");

                String cityName =
                    String.valueOf(CAPITAL_PROPERTIES.get(fileNameWithoutExtension + ".city"));
                String countryName =
                    String.valueOf(CAPITAL_PROPERTIES.get(fileNameWithoutExtension + ".country"));
                List<CapitalData.Continent> continents =
                    Arrays.stream(CAPITAL_PROPERTIES.getProperty(fileNameWithoutExtension + ".continents").split(","))
                        .filter(s -> !s.isEmpty())
                        .map(CapitalData.Continent::valueOf)
                        .toList();

                String content = "";
                try {
                    content = String.join("\n", Files.readAllLines(path, Charsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return new CapitalData(
                    new CapitalData.CapitalCity(cityName, ""),
                    new CapitalData.Country(countryName, ""),
                    continents,
                    new CapitalData.Article(content, content, content));
            }).toList();
    }
}
