/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.patterns.data;

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
