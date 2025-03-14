package ai.patterns.dao;

import java.util.List;

public record CapitalData(
    CapitalCity capitalCity,
    Country country,
    List<Continent> continents,
    Article article
) {
    public record CapitalCity(String city, String link) { }
    public record Country(String country, String link) { }
    public record Article(String markdown, String html, String text) { }

    public enum Continent {
        AFRICA("Africa"),
        ANTARCTICA("Antarctica"),
        ASIA("Asia"),
        EUROPE("Europe"),
        NORTH_AMERICA("North America"),
        OCEANIA("Oceania"),
        SOUTH_AMERICA("South America");

        private final String displayName;

        Continent() {
            this.displayName = name();
        }

        Continent(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
