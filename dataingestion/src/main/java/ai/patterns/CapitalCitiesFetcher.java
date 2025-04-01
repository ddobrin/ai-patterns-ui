package ai.patterns;

import static ai.patterns.utils.Ansi.*;

import ai.patterns.CapitalData;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class CapitalCitiesFetcher {

    public static final String WIKIPEDIA_URL = "https://en.wikipedia.org";
    private static final Properties CAPITAL_ATTRIBUTES = new Properties();

    public static void main(String[] args) throws IOException {

        Element table = Jsoup.connect(WIKIPEDIA_URL + "/wiki/List_of_national_capitals").get()
            .select("table.wikitable")
            .getFirst();

        List<List<Element>> matrix = tableToMatrix(table);
        matrix = matrix.subList(1, matrix.size()); // skip the header row

        matrix.stream()
            .map(row -> {
                Element capitalElem = row.get(0);
                Element countryElem = row.get(1);
                Element continentElem = row.get(2);

                Element capitalLink = capitalElem.select("a").first();
                Element countryLink = countryElem.select("a").first();

                CapitalData.CapitalCity capitalCity = new CapitalData.CapitalCity(capitalLink.text(), capitalLink.attr("href"));
                CapitalData.Country country = new CapitalData.Country(countryLink.text(), countryLink.attr("href"));

                List<CapitalData.Continent> continents = Arrays.stream(continentElem.text().split("/"))
                    .filter(s -> !s.isBlank())
                    .map(s -> CapitalData.Continent.valueOf(s.toUpperCase().replace(" ", "_")))
                    .collect(Collectors.toList());

                CapitalData.Article article = getArticle(capitalCity.link());

                return new CapitalData(capitalCity, country, continents, article);
            })
            .forEach(capitalData -> {
                printCityDetails(capitalData);
                saveFile(capitalData);
            });

        CAPITAL_ATTRIBUTES.store(new FileWriter("capitals/capital.properties"), "Metadata about national capitals of the world");
    }

    private static String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
            .toLowerCase()
            .replaceAll("\\p{IsM}+", "")
            .replaceAll("\\p{IsP}+", " ")
            .trim()
            .replaceAll("\\s+", "_");
    }

    private static void saveFile(CapitalData capitalData) {
        String normalizedCityAndCountry = normalizeName(capitalData.capitalCity().city() + " " + capitalData.country().country());

        try {
            Files.createDirectories(Path.of("capitals", "txt"));
            Files.createDirectories(Path.of("capitals", "md"));
            Files.createDirectories(Path.of("capitals", "html"));

            // Save markdown file
             Files.writeString(
                 Path.of("capitals", "md", normalizedCityAndCountry + ".md"),
                 capitalData.article().markdown(),
                 StandardCharsets.UTF_8
             );
            // Save HTML file
             Files.writeString(
                 Path.of("capitals", "html", normalizedCityAndCountry + ".html"),
                 capitalData.article().html(),
                 StandardCharsets.UTF_8
             );
            // Save text file
            Files.writeString(
                Path.of("capitals", "txt", normalizedCityAndCountry + ".txt"),
                capitalData.article().text(),
                StandardCharsets.UTF_8
            );

            CAPITAL_ATTRIBUTES.put(normalizedCityAndCountry + ".city", capitalData.capitalCity().city());
            CAPITAL_ATTRIBUTES.put(normalizedCityAndCountry + ".country", capitalData.country().country());
            CAPITAL_ATTRIBUTES.put(normalizedCityAndCountry + ".continents", capitalData.continents().stream()
                    .map(Enum::name)
                .collect(Collectors.joining(",")));

            System.out.println(green("✓ Saved files: ") + yellow(normalizedCityAndCountry));
        } catch (IOException e) {
            System.err.println(red("✗ Failed to save files: ") + yellow(normalizedCityAndCountry + ": " + e.getMessage()));
        }
    }

    private static void printCityDetails(CapitalData capitalData) {
        System.out.println(cyan("-".repeat(50)));
        System.out.println(red(capitalData.capitalCity().city()) + " — " + capitalData.capitalCity().link());
        System.out.println(cyan(capitalData.country().country()) + " — " + capitalData.country().link());
        System.out.println(green(capitalData.continents().toString()));
    }

    private static CapitalData.Article getArticle(String link) {
        try {
            Element content = Jsoup.connect(WIKIPEDIA_URL + link).get().select("div#mw-content-text").getFirst();

            String html = content.html();
            String text = content.text();

            FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();
            String markdown = converter.convert(html);
//            String markdown = text;

            return new CapitalData.Article(markdown, html, text);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return new CapitalData.Article("", "", "");
        }
    }

    /// Translated from JavaScript to use jsoup, inspired from this
    /// [StackOverflow question](https://stackoverflow.com/questions/17919272/iterate-over-table-cells-re-using-rowspan-values)
    private static List<List<Element>> tableToMatrix(Element table) {
        List<List<Element>> matrix = new ArrayList<>();

        for (int i = 0; i < table.select("tr").size(); i++) {
            Element row = table.select("tr").get(i);
            matrix.add(new ArrayList<>());

            int j = 0;
            int k = 0;

            while (j < (!matrix.isEmpty() ? matrix.getFirst().size() : 0) || k < row.select("td, th").size()) {
                Element aboveCell = (i > 0 && j < matrix.get(i - 1).size()) ? matrix.get(i - 1).get(j) : null;
                if (aboveCell != null && aboveCell.parent().elementSiblingIndex() + getRowSpan(aboveCell) > i) {
                    for (int span = 0; span < getColSpan(aboveCell); span++) {
                        matrix.get(i).add(aboveCell);
                    }
                    j += getColSpan(aboveCell);
                } else if (k < row.select("td, th").size()) {
                    Element cell = row.select("td, th").get(k++);
                    for (int span = 0; span < getColSpan(cell); span++) {
                        matrix.get(i).add(cell);
                    }
                    j += getColSpan(cell);
                }
            }
        }
        return matrix;
    }

    private static int getRowSpan(Element cell) {
        String rowspan = cell.attr("rowspan");
        return rowspan.isEmpty() ? 1 : Integer.parseInt(rowspan);
    }

    private static int getColSpan(Element cell) {
        String colspan = cell.attr("colspan");
        return colspan.isEmpty() ? 1 : Integer.parseInt(colspan);

    }
}