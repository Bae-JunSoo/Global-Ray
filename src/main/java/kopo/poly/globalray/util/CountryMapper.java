package kopo.poly.globalray.util;

import java.util.List;
import java.util.Map;

public class CountryMapper {

    private static final Map<String, String> SOURCE_COUNTRY_MAP = Map.ofEntries(
            // 미국
            Map.entry("Reuters",                  "US"),
            Map.entry("Associated Press",          "US"),
            Map.entry("CNN",                       "US"),
            Map.entry("The New York Times",        "US"),
            Map.entry("The Washington Post",       "US"),
            Map.entry("Bloomberg",                 "US"),
            Map.entry("Forbes",                    "US"),
            Map.entry("Business Insider",          "US"),
            Map.entry("TechCrunch",                "US"),
            Map.entry("ESPN",                      "US"),
            Map.entry("NBC News",                  "US"),
            Map.entry("ABC News",                  "US"),
            Map.entry("CBS News",                  "US"),
            Map.entry("Fox News",                  "US"),
            Map.entry("CNBC",                      "US"),
            Map.entry("Politico",                  "US"),
            Map.entry("The Hill",                  "US"),
            Map.entry("Axios",                     "US"),
            Map.entry("Wired",                     "US"),
            Map.entry("The Verge",                 "US"),
            Map.entry("Ars Technica",              "US"),
            Map.entry("Entertainment Weekly",      "US"),
            Map.entry("People",                    "US"),
            Map.entry("Variety",                   "US"),
            Map.entry("Hollywood Reporter",        "US"),
            Map.entry("Healthline",                "US"),
            Map.entry("Medical News Today",        "US"),
            Map.entry("Science Daily",             "US"),
            Map.entry("National Geographic",       "US"),
            Map.entry("Mashable",                  "US"),
            Map.entry("Gizmodo.com",               "US"),
            Map.entry("Space Daily",               "US"),
            Map.entry("Investor's Business Daily", "US"),
            Map.entry("Eatingwell.com",            "US"),
            Map.entry("Verywellhealth.com",        "US"),

            // 유럽
            Map.entry("BBC News",                  "EU"),
            Map.entry("The Guardian",              "EU"),
            Map.entry("The Independent",           "EU"),
            Map.entry("Financial Times",           "EU"),
            Map.entry("Sky News",                  "EU"),
            Map.entry("Daily Mail",                "EU"),
            Map.entry("The Telegraph",             "EU"),
            Map.entry("Der Spiegel",               "EU"),
            Map.entry("Le Monde",                  "EU"),
            Map.entry("Euronews",                  "EU"),
            Map.entry("Al Jazeera English",        "EU"),

            // 일본
            Map.entry("The Japan Times",           "JP"),
            Map.entry("NHK World",                 "JP"),
            Map.entry("Japan Today",               "JP"),
            Map.entry("Kyodo News",                "JP"),
            Map.entry("Mainichi",                  "JP"),
            Map.entry("Asahi Shimbun",             "JP")
    );

    // sourceName → 국가 코드
    public static String getCountry(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) return "ETC";
        return SOURCE_COUNTRY_MAP.getOrDefault(sourceName, "US");
    }

    // 국가 코드 → sourceName 목록 (서버 필터링용)
    public static List<String> getSourceNames(String country) {
        if (country == null || country.isBlank() || "ALL".equals(country)) return List.of();
        return SOURCE_COUNTRY_MAP.entrySet().stream()
                .filter(e -> e.getValue().equals(country))
                .map(Map.Entry::getKey)
                .toList();
    }
}
