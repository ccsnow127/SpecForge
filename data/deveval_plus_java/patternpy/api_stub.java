// API Stub — class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Tradingpatterns {

    /**
     * Minimal DataFrame: ordered map of named double[] columns, all same length.
     * NaN represents missing / null values (like pandas NaN / numpy nan).
     */
    public static class DataFrame {
        private final LinkedHashMap<String, double[]> columns = new LinkedHashMap<>();
        private final LinkedHashMap<String, String[]> stringColumns = new LinkedHashMap<>();
        private int length;

        /** Create empty DataFrame. */
        public DataFrame() { /* ... */ }

        /** Create DataFrame from column name/value pairs. */
        public DataFrame(String[] names, double[][] cols) { /* ... */ }

        /** Number of rows. */
        public int size() { /* ... */ return 0; }

        /** Whether a numeric column exists. */
        public boolean hasColumn(String name) { /* ... */ return false; }

        /** Get raw numeric column array. */
        public double[] col(String name) { /* ... */ return null; }

        /** Add or replace a numeric column. */
        public void putColumn(String name, double[] data) { /* ... */ }

        /** Deep copy (numeric columns). */
        public DataFrame copy() { /* deep copy */ return null; }

        /** Set of numeric column names. */
        public Set<String> columnNames() { /* ... */ return null; }

        /** Whether a string column exists. */
        public boolean hasStringColumn(String name) { /* ... */ return false; }

        /** Get raw string column array. */
        public String[] stringCol(String name) { /* ... */ return null; }

        /** Add or replace a string column. */
        public void putStringColumn(String name, String[] data) { /* ... */ }

        /** Set of string column names. */
        public Set<String> stringColumnNames() { /* ... */ return null; }

        /** Deep copy including string columns. */
        public DataFrame deepCopy() { /* deep copy all */ return null; }
    }

    // ── Column helper methods (static, package-private) ──
    // shift, rollingMax, rollingMin, rollingMean, rollingStd, rollingTrend,
    // diff, gt, lt, ge, le, eq, and, isTrue, sub, add, mul(array,array),
    // mul(array,scalar), div(array,scalar), nanArray, emptyStringArray
    // These are internal implementation details used by the pattern functions.

    // ── Pattern detection functions ──

    /** Detect Head and Shoulder / Inverse Head and Shoulder patterns. */
    public static DataFrame detect_head_shoulder(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame detect_head_shoulder(DataFrame df) { /* default window=3 */ return null; }

    /** Detect Multiple Top / Multiple Bottom patterns. */
    public static DataFrame detect_multiple_tops_bottoms(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame detect_multiple_tops_bottoms(DataFrame df) { /* default window=3 */ return null; }

    /** Calculate support and resistance levels using rolling mean/std. */
    public static DataFrame calculate_support_resistance(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame calculate_support_resistance(DataFrame df) { /* default window=3 */ return null; }

    /** Detect Ascending / Descending Triangle patterns. */
    public static DataFrame detect_triangle_pattern(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame detect_triangle_pattern(DataFrame df) { /* default window=3 */ return null; }

    /** Detect Wedge Up / Wedge Down patterns. */
    public static DataFrame detect_wedge(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame detect_wedge(DataFrame df) { /* default window=3 */ return null; }

    /** Detect Channel Up / Channel Down patterns. */
    public static DataFrame detect_channel(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame detect_channel(DataFrame df) { /* default window=3 */ return null; }

    /** Detect Double Top / Double Bottom patterns. */
    public static DataFrame detect_double_top_bottom(DataFrame df, int window, double threshold) { /* ... */ return null; }
    public static DataFrame detect_double_top_bottom(DataFrame df, int window) { /* default threshold=0.05 */ return null; }
    public static DataFrame detect_double_top_bottom(DataFrame df) { /* default window=3, threshold=0.05 */ return null; }

    /** Detect trendline support/resistance via linear regression. */
    public static DataFrame detect_trendline(DataFrame df, int window) { /* ... */ return null; }
    public static DataFrame detect_trendline(DataFrame df) { /* default window=2 */ return null; }

    /** Find pivot points: HH, LL, LH, HL signals. Uses lowercase 'high'/'low' columns. */
    public static DataFrame find_pivots(DataFrame df) { /* ... */ return null; }
}
