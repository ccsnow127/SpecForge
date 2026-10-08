// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.io.*;
import java.util.*;

public class Geotext {

    // ---------------------------------------------------------------
    // Helper type: parsed data index
    // ---------------------------------------------------------------
    /**
     * Container for the three lookup tables loaded from the data files.
     * Mirrors the Python {@code namedtuple('Index', 'nationalities cities countries')}.
     */
    public static class Index {
        public final Map<String, String> nationalities;
        public final Map<String, String> cities;
        public final Map<String, String> countries;

        public Index(Map<String, String> nationalities,
                     Map<String, String> cities,
                     Map<String, String> countries) {
            this.nationalities = nationalities;
            this.cities = cities;
            this.countries = countries;
        }
    }

    // ---------------------------------------------------------------
    // Shared index loaded once (mirrors GeoText.index class attribute).
    // ---------------------------------------------------------------
    public static final Index index = null;

    // ---------------------------------------------------------------
    // Per-instance fields populated by the constructor.
    // ---------------------------------------------------------------
    public List<String> countries;
    public List<String> cities;
    public List<String> nationalities;
    /** Insertion order matches the Python OrderedDict produced by Counter.most_common(). */
    public LinkedHashMap<String, Integer> countryMentions;

    // ---------------------------------------------------------------
    // get_data_path
    // ---------------------------------------------------------------
    /**
     * Returns the resource path used to look up a packaged data file.
     */
    public static String getDataPath(String path) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // read_table
    // ---------------------------------------------------------------
    /**
     * Parses a delimited data file from the classpath into a (lower-case key) -> value map.
     *
     * @param filename  Resource name under {@code /data_file/}.
     * @param usecols   Two-element array: index of key column, index of value column.
     * @param sep       Field delimiter.
     * @param comment   Lines beginning with this string are skipped.
     * @param encoding  File encoding (e.g. "utf-8").
     * @param skip      Number of leading lines to skip.
     */
    public static Map<String, String> readTable(String filename,
                                                int[] usecols,
                                                String sep,
                                                String comment,
                                                String encoding,
                                                int skip) throws IOException { /* ... */ return null; }

    /** Convenience overload using the Python defaults (usecols={0,1}, sep="\t", comment="#", encoding="utf-8", skip=0). */
    public static Map<String, String> readTable(String filename) throws IOException { /* ... */ return null; }

    // ---------------------------------------------------------------
    // build_index
    // ---------------------------------------------------------------
    /**
     * Loads nationalities, countries, cities and city patches from the packaged data files.
     */
    public static Index buildIndex() { /* ... */ return null; }

    // ---------------------------------------------------------------
    // GeoText.__init__
    // ---------------------------------------------------------------
    /**
     * Parses {@code text} and extracts cities, countries, nationalities and country mentions.
     *
     * @param text     The free-form text to scan.
     * @param country  If non-null, restricts {@link #cities} to those located in this country code.
     */
    public Geotext(String text, String country) { /* ... */ }

    /** Convenience constructor with no country filter. */
    public Geotext(String text) { /* ... */ }
}
