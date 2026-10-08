import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for Tradingpatterns — mirrors test_tradingpatterns.py exactly.
 */
public class TradingpatternsTest {

    // ── Data generators matching numpy.random.RandomState(42) ──

    /**
     * Equivalent of make_ohlc_df(n=15, seed=42) with uppercase columns.
     */
    private static Tradingpatterns.DataFrame makeOhlcDf15() {
        double[] close = {
            100.4967141530112258, 100.3584498518400494, 101.0061383899407446,
            102.5291682463487604, 102.2950148716254262, 102.0608779146762544,
            103.6400907301836440, 104.4075254593365543, 103.9380510734015957,
            104.4806111169875606, 104.0171934241751046, 103.5514636706048464,
            103.7934259421708703, 101.8801456975130719, 100.1552278650000432
        };
        double[] high = {
            101.9144934950947885, 101.0676906428181070, 101.9443553627435648,
            103.5787110112893004, 103.4791198479509831, 103.7386418567657813,
            104.4396014034211788, 105.6788771169569685, 105.3266729266946555,
            105.0502867360675623, 105.4285107020272676, 104.3072498561357833,
            104.3910033316487898, 103.8034740033930774, 102.1036759146118840
        };
        double[] low = {
            98.7841181308365321, 99.4015291980799987, 100.3596302189311729,
            101.0028187065805270, 101.1347861310160283, 101.3778205624090845,
            102.3973253650167408, 103.8559426776637338, 102.0740704702834165,
            103.5924411445875393, 102.5234099976441371, 102.5838970564707324,
            102.5133239104041536, 100.5600802784981482, 99.3779461817117493
        };
        double[] open = {
            100.2155703883907449, 100.7276831418377583, 101.0918225305357367,
            102.4713441051546425, 102.1444630238307809, 101.3216169194925413,
            103.2801686259862919, 104.1772060738566665, 104.4666121865110568,
            104.6524202617717947, 103.1356733464937321, 103.7135056553022423,
            103.6008848019627067, 101.5416846973600968, 100.4610660094204775
        };
        return buildDf(
            new String[]{"Open", "High", "Low", "Close"},
            new double[][]{open, high, low, close}
        );
    }

    /**
     * Equivalent of make_ohlc_df(n=20, seed=42) with uppercase columns.
     */
    private static Tradingpatterns.DataFrame makeOhlcDf20() {
        double[] close = {
            100.4967141530112258, 100.3584498518400494, 101.0061383899407446,
            102.5291682463487604, 102.2950148716254262, 102.0608779146762544,
            103.6400907301836440, 104.4075254593365543, 103.9380510734015957,
            104.4806111169875606, 104.0171934241751046, 103.5514636706048464,
            103.7934259421708703, 101.8801456975130719, 100.1552278650000432,
            99.5929403357590672, 98.5801092154246561, 98.8943565480199283,
            97.9863324724987166, 96.5740287711634267
        };
        double[] high = {
            101.6808191293367827, 102.0362137939295764, 101.8056490631782793,
            103.8005199039691746, 103.6836367249184860, 102.6305535337562560,
            105.0514080080358070, 105.1633116448674912, 104.5356284628795152,
            106.4039394228675661, 105.9656414737869454, 105.2640596927795400,
            104.7503465959309210, 102.5266538685226436, 101.6815774047682766,
            100.7531690763684651, 99.2631665676918260, 100.1371219131868315,
            98.5379152541715371, 98.4380093742816058
        };
        double[] low = {
            99.6085441806112044, 98.8646664253090819, 100.0385717758066306,
            101.2490662145820437, 100.9749494526105025, 101.2835962313879605,
            101.6857137885368019, 102.7448262242948829, 102.0288026610553089,
            102.6383700913460899, 102.6203434559584764, 101.6686523180701727,
            103.1606871890929966, 101.0861714038843502, 99.5873869316342422,
            98.6049448396141770, 97.4970932808904394, 97.9873330003590866,
            96.2432262087708210, 95.5388987811230379
        };
        double[] open = {
            100.6685232977954598, 99.4769297741586769, 101.1681803746381405,
            102.3366271061405968, 101.9565538714724511, 102.3667160590966887,
            104.1555904914316244, 104.8731655188946519, 103.5184423117902810,
            104.3260049290619520, 104.1828251398768828, 104.0392362341660260,
            103.5538388232482276, 101.7873162091811565, 99.6020603779970344,
            98.9948370237187305, 98.9863721266217595, 99.5724765623053401,
            97.9503274117085567, 97.0757952201094412
        };
        return buildDf(
            new String[]{"Open", "High", "Low", "Close"},
            new double[][]{open, high, low, close}
        );
    }

    /**
     * Equivalent of make_ohlc_lowercase_df(n=15, seed=42) — lowercase columns for find_pivots.
     */
    private static Tradingpatterns.DataFrame makeOhlcLowercaseDf15() {
        Tradingpatterns.DataFrame uc = makeOhlcDf15();
        return buildDf(
            new String[]{"open", "high", "low", "close"},
            new double[][]{uc.col("Open"), uc.col("High"), uc.col("Low"), uc.col("Close")}
        );
    }

    /**
     * Equivalent of make_ohlc_lowercase_df(n=20, seed=42).
     */
    private static Tradingpatterns.DataFrame makeOhlcLowercaseDf20() {
        Tradingpatterns.DataFrame uc = makeOhlcDf20();
        return buildDf(
            new String[]{"open", "high", "low", "close"},
            new double[][]{uc.col("Open"), uc.col("High"), uc.col("Low"), uc.col("Close")}
        );
    }

    private static Tradingpatterns.DataFrame buildDf(String[] names, double[][] cols) {
        return new Tradingpatterns.DataFrame(names, cols);
    }

    // ═══════════════ detect_head_shoulder tests ═══════════════

    @Test
    public void test_detect_head_shoulder_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_head_shoulder(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_head_shoulder_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_head_shoulder(df.copy());
        assertTrue(result.hasStringColumn("head_shoulder_pattern"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
    }

    @Test
    public void test_detect_head_shoulder_pattern_values() {
        Tradingpatterns.DataFrame df = makeOhlcDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_head_shoulder(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("Head and Shoulder", "Inverse Head and Shoulder"));
        String[] pattern = result.stringCol("head_shoulder_pattern");
        for (String val : pattern) {
            if (val != null) {
                assertTrue(validValues.contains(val), "Unexpected value: " + val);
            }
        }
    }

    // ═══════════════ detect_multiple_tops_bottoms tests ═══════════════

    @Test
    public void test_detect_multiple_tops_bottoms_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_multiple_tops_bottoms(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_multiple_tops_bottoms_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_multiple_tops_bottoms(df.copy());
        assertTrue(result.hasStringColumn("multiple_top_bottom_pattern"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
        assertTrue(result.hasColumn("close_roll_max"));
        assertTrue(result.hasColumn("close_roll_min"));
    }

    @Test
    public void test_detect_multiple_tops_bottoms_pattern_values() {
        Tradingpatterns.DataFrame df = makeOhlcDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_multiple_tops_bottoms(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("Multiple Top", "Multiple Bottom"));
        String[] pattern = result.stringCol("multiple_top_bottom_pattern");
        for (String val : pattern) {
            if (val != null) {
                assertTrue(validValues.contains(val), "Unexpected value: " + val);
            }
        }
    }

    // ═══════════════ calculate_support_resistance tests ═══════════════

    @Test
    public void test_calculate_support_resistance_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.calculate_support_resistance(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_calculate_support_resistance_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.calculate_support_resistance(df.copy());
        assertTrue(result.hasColumn("support"));
        assertTrue(result.hasColumn("resistance"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
    }

    @Test
    public void test_calculate_support_resistance_values_are_numeric() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.calculate_support_resistance(df.copy(), 3);
        double[] support = result.col("support");
        double[] resistance = result.col("resistance");
        int supportCount = 0, resistanceCount = 0;
        for (double v : support) { if (!Double.isNaN(v)) supportCount++; }
        for (double v : resistance) { if (!Double.isNaN(v)) resistanceCount++; }
        assertTrue(supportCount > 0, "support should have at least one non-NaN value");
        assertTrue(resistanceCount > 0, "resistance should have at least one non-NaN value");
    }

    // ═══════════════ detect_triangle_pattern tests ═══════════════

    @Test
    public void test_detect_triangle_pattern_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_triangle_pattern(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_triangle_pattern_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_triangle_pattern(df.copy());
        assertTrue(result.hasStringColumn("triangle_pattern"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
    }

    @Test
    public void test_detect_triangle_pattern_values() {
        Tradingpatterns.DataFrame df = makeOhlcDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_triangle_pattern(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("Ascending Triangle", "Descending Triangle"));
        String[] pattern = result.stringCol("triangle_pattern");
        for (String val : pattern) {
            if (val != null) {
                assertTrue(validValues.contains(val), "Unexpected value: " + val);
            }
        }
    }

    // ═══════════════ detect_wedge tests ═══════════════

    @Test
    public void test_detect_wedge_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_wedge(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_wedge_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_wedge(df.copy());
        assertTrue(result.hasStringColumn("wedge_pattern"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
        assertTrue(result.hasColumn("trend_high"));
        assertTrue(result.hasColumn("trend_low"));
    }

    @Test
    public void test_detect_wedge_pattern_values() {
        Tradingpatterns.DataFrame df = makeOhlcDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_wedge(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("Wedge Up", "Wedge Down"));
        String[] pattern = result.stringCol("wedge_pattern");
        for (String val : pattern) {
            if (val != null) {
                assertTrue(validValues.contains(val), "Unexpected value: " + val);
            }
        }
    }

    // ═══════════════ detect_channel tests ═══════════════

    @Test
    public void test_detect_channel_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_channel(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_channel_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_channel(df.copy());
        assertTrue(result.hasStringColumn("channel_pattern"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
        assertTrue(result.hasColumn("trend_high"));
        assertTrue(result.hasColumn("trend_low"));
    }

    @Test
    public void test_detect_channel_pattern_values() {
        Tradingpatterns.DataFrame df = makeOhlcDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_channel(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("Channel Up", "Channel Down"));
        String[] pattern = result.stringCol("channel_pattern");
        for (String val : pattern) {
            if (val != null) {
                assertTrue(validValues.contains(val), "Unexpected value: " + val);
            }
        }
    }

    // ═══════════════ detect_double_top_bottom tests ═══════════════

    @Test
    public void test_detect_double_top_bottom_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_double_top_bottom(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_double_top_bottom_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_double_top_bottom(df.copy());
        assertTrue(result.hasStringColumn("double_pattern"));
        assertTrue(result.hasColumn("high_roll_max"));
        assertTrue(result.hasColumn("low_roll_min"));
    }

    @Test
    public void test_detect_double_top_bottom_pattern_values() {
        Tradingpatterns.DataFrame df = makeOhlcDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_double_top_bottom(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("Double Top", "Double Bottom"));
        String[] pattern = result.stringCol("double_pattern");
        for (String val : pattern) {
            if (val != null) {
                assertTrue(validValues.contains(val), "Unexpected value: " + val);
            }
        }
    }

    // ═══════════════ detect_trendline tests ═══════════════

    @Test
    public void test_detect_trendline_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_trendline(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_detect_trendline_adds_columns() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_trendline(df.copy());
        assertTrue(result.hasColumn("slope"));
        assertTrue(result.hasColumn("intercept"));
        assertTrue(result.hasColumn("support"));
        assertTrue(result.hasColumn("resistance"));
    }

    @Test
    public void test_detect_trendline_slope_computed() {
        Tradingpatterns.DataFrame df = makeOhlcDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.detect_trendline(df.copy(), 2);
        double[] slope = result.col("slope");
        int count = 0;
        for (double v : slope) { if (!Double.isNaN(v)) count++; }
        assertTrue(count > 0, "slope should have at least one non-NaN value after window warm-up");
    }

    // ═══════════════ find_pivots tests ═══════════════

    @Test
    public void test_find_pivots_returns_dataframe() {
        Tradingpatterns.DataFrame df = makeOhlcLowercaseDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.find_pivots(df.copy());
        assertNotNull(result);
    }

    @Test
    public void test_find_pivots_adds_signal_column() {
        Tradingpatterns.DataFrame df = makeOhlcLowercaseDf15();
        Tradingpatterns.DataFrame result = Tradingpatterns.find_pivots(df.copy());
        assertTrue(result.hasStringColumn("signal"));
    }

    @Test
    public void test_find_pivots_signal_values() {
        Tradingpatterns.DataFrame df = makeOhlcLowercaseDf20();
        Tradingpatterns.DataFrame result = Tradingpatterns.find_pivots(df.copy());
        Set<String> validValues = new HashSet<>(Arrays.asList("HH", "LL", "LH", "HL", ""));
        String[] signal = result.stringCol("signal");
        for (String val : signal) {
            assertTrue(validValues.contains(val), "Unexpected signal value: " + val);
        }
    }
}
