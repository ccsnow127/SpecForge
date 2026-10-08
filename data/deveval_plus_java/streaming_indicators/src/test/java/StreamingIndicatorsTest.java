import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

// Nested types referenced via their enclosing class name (StreamingIndicators.Foo).

/**
 * JUnit 5 tests for StreamingIndicators -- one-to-one mapping to every Python test in
 * test_streaming_indicators.py.
 */
public class StreamingIndicatorsTest {

    private static final double EPS = 1e-6;

    /** Sum reduction used by RollingStat tests. */
    private static final java.util.function.Function<List<Double>, Double> SUM =
            xs -> { double s = 0; for (double x : xs) s += x; return s; };

    // ===============================================================
    // RollingStat
    // ===============================================================

    @Test
    public void test_RollingStat_update_returns_none_until_full() {
        StreamingIndicators.RollingStat rs = new StreamingIndicators.RollingStat(3, SUM);
        assertNull(rs.update(1));
        assertNull(rs.update(2));
    }

    @Test
    public void test_RollingStat_update_returns_value_when_full() {
        StreamingIndicators.RollingStat rs = new StreamingIndicators.RollingStat(3, SUM);
        rs.update(1);
        rs.update(2);
        Double result = rs.update(3);
        assertEquals(6.0, result, EPS);
    }

    @Test
    public void test_RollingStat_rolling_window() {
        StreamingIndicators.RollingStat rs = new StreamingIndicators.RollingStat(3, SUM);
        rs.update(1);
        rs.update(2);
        rs.update(3);
        Double result = rs.update(4);
        assertEquals(9.0, result, EPS); // sum([2,3,4])
    }

    @Test
    public void test_RollingStat_compute_does_not_mutate() {
        StreamingIndicators.RollingStat rs = new StreamingIndicators.RollingStat(3, SUM);
        rs.update(1);
        rs.update(2);
        Double result = rs.compute(3);
        assertEquals(6.0, result, EPS);
        assertNull(rs.value());
    }

    @Test
    public void test_RollingStat_value_property() {
        StreamingIndicators.RollingStat rs = new StreamingIndicators.RollingStat(3, SUM);
        rs.update(10);
        rs.update(20);
        assertNull(rs.value());
        rs.update(30);
        assertEquals(60.0, rs.value(), EPS);
    }

    @Test
    public void test_RollingStat_init_with_points() {
        StreamingIndicators.RollingStat rs = new StreamingIndicators.RollingStat(3, SUM,
                Arrays.asList(10.0, 20.0, 30.0));
        assertEquals(60.0, rs.value(), EPS);
    }

    @Test
    public void test_RollingStat_period_must_be_greater_than_1() {
        assertThrows(IllegalArgumentException.class,
                () -> new StreamingIndicators.RollingStat(1, SUM));
    }

    // ===============================================================
    // Max
    // ===============================================================

    @Test
    public void test_Max_basic_sequence() {
        StreamingIndicators.Max m = new StreamingIndicators.Max(3);
        assertNull(m.update(5));
        assertNull(m.update(3));
        assertEquals(8.0, m.update(8), EPS);
        assertEquals(8.0, m.update(1), EPS); // max([3,8,1])
        assertEquals(8.0, m.update(6), EPS); // max([8,1,6])
    }

    @Test
    public void test_Max_all_same() {
        StreamingIndicators.Max m = new StreamingIndicators.Max(3);
        m.update(5);
        m.update(5);
        assertEquals(5.0, m.update(5), EPS);
    }

    @Test
    public void test_Max_decreasing() {
        StreamingIndicators.Max m = new StreamingIndicators.Max(3);
        m.update(10);
        m.update(8);
        assertEquals(10.0, m.update(6), EPS);
        assertEquals(8.0, m.update(4), EPS); // max([8,6,4])
    }

    // ===============================================================
    // Min
    // ===============================================================

    @Test
    public void test_Min_basic_sequence() {
        StreamingIndicators.Min m = new StreamingIndicators.Min(3);
        assertNull(m.update(5));
        assertNull(m.update(3));
        assertEquals(3.0, m.update(8), EPS);
        assertEquals(1.0, m.update(1), EPS); // min([3,8,1])
        assertEquals(1.0, m.update(6), EPS); // min([8,1,6])
    }

    @Test
    public void test_Min_increasing() {
        StreamingIndicators.Min m = new StreamingIndicators.Min(3);
        m.update(2);
        m.update(4);
        assertEquals(2.0, m.update(6), EPS);
        assertEquals(4.0, m.update(8), EPS); // min([4,6,8])
    }

    // ===============================================================
    // SMA
    // ===============================================================

    @Test
    public void test_SMA_basic() {
        StreamingIndicators.SMA sma = new StreamingIndicators.SMA(3);
        sma.update(2);
        sma.update(4);
        Double result = sma.update(6);
        assertEquals(4.0, result, EPS);
    }

    @Test
    public void test_SMA_rolling() {
        StreamingIndicators.SMA sma = new StreamingIndicators.SMA(3);
        sma.update(2);
        sma.update(4);
        sma.update(6);
        Double result = sma.update(8);
        assertEquals(6.0, result, EPS); // mean([4,6,8])
    }

    @Test
    public void test_SMA_compute_without_mutate() {
        StreamingIndicators.SMA sma = new StreamingIndicators.SMA(3);
        sma.update(2);
        sma.update(4);
        Double result = sma.compute(6);
        assertEquals(4.0, result, EPS);
        assertNull(sma.value());
    }

    // ===============================================================
    // SD
    // ===============================================================

    @Test
    public void test_SD_basic() {
        StreamingIndicators.SD sd = new StreamingIndicators.SD(3);
        sd.update(10);
        sd.update(20);
        Double result = sd.update(30);
        // numpy.std([10,20,30]) = sqrt(((10-20)^2+(20-20)^2+(30-20)^2)/3)
        //                       = sqrt(200/3)
        double expected = Math.sqrt(200.0 / 3.0);
        assertEquals(expected, result, EPS);
    }

    // ===============================================================
    // EMA
    // ===============================================================

    @Test
    public void test_EMA_first_value_is_sma() {
        StreamingIndicators.EMA ema = new StreamingIndicators.EMA(3);
        ema.update(10);
        ema.update(20);
        Double result = ema.update(30);
        assertEquals(20.0, result, EPS); // SMA of [10,20,30]
    }

    @Test
    public void test_EMA_subsequent_uses_formula() {
        StreamingIndicators.EMA ema = new StreamingIndicators.EMA(3);
        ema.update(10);
        ema.update(20);
        ema.update(30);
        Double result = ema.update(40);
        // mult = 2/(1+3) = 0.5
        // EMA = 40*0.5 + 20*0.5 = 30
        assertEquals(30.0, result, EPS);
    }

    @Test
    public void test_EMA_returns_none_before_period() {
        StreamingIndicators.EMA ema = new StreamingIndicators.EMA(3);
        assertNull(ema.update(10));
        assertNull(ema.update(20));
    }

    // ===============================================================
    // WMA
    // ===============================================================

    @Test
    public void test_WMA_basic() {
        StreamingIndicators.WMA wma = new StreamingIndicators.WMA(3);
        wma.update(10);
        wma.update(20);
        Double result = wma.update(30);
        // weights=[1,2,3], den=6 -> (10+40+90)/6 = 140/6
        assertEquals(140.0 / 6.0, result, EPS);
    }

    @Test
    public void test_WMA_returns_none_before_full() {
        StreamingIndicators.WMA wma = new StreamingIndicators.WMA(3);
        assertNull(wma.update(10));
        assertNull(wma.update(20));
    }

    @Test
    public void test_WMA_compute_without_mutate() {
        StreamingIndicators.WMA wma = new StreamingIndicators.WMA(3);
        wma.update(10);
        wma.update(20);
        Double result = wma.compute(30);
        assertEquals(140.0 / 6.0, result, EPS);
        assertNull(wma.value);
    }

    // ===============================================================
    // SMMA
    // ===============================================================

    @Test
    public void test_SMMA_basic() {
        StreamingIndicators.SMMA smma = new StreamingIndicators.SMMA(3);
        // ema_period = 5, so first value at 5th update
        for (double v : new double[] { 1, 2, 3, 4 }) smma.update(v);
        assertNull(smma.value);
        Double result = smma.update(5);
        assertEquals(3.0, result, EPS); // SMA of [1..5]
    }

    @Test
    public void test_SMMA_subsequent() {
        StreamingIndicators.SMMA smma = new StreamingIndicators.SMMA(3);
        for (double v : new double[] { 1, 2, 3, 4, 5 }) smma.update(v);
        Double result = smma.update(6);
        // mult = 2/(1+5) = 1/3
        // EMA = 6*(1/3) + 3*(2/3) = 4
        assertEquals(4.0, result, EPS);
    }

    // ===============================================================
    // RMA
    // ===============================================================

    @Test
    public void test_RMA_initial_is_mean() {
        StreamingIndicators.RMA rma = new StreamingIndicators.RMA(3);
        rma.update(10);
        rma.update(20);
        Double result = rma.update(30);
        // update(10): rma=10
        // update(20): rma = (1/3)*20 + (2/3)*10 = 13.3333
        // update(30): rma = (1/3)*30 + (2/3)*13.3333 = 18.8889
        assertEquals(18.8889, result, 1e-3);
    }

    @Test
    public void test_RMA_value_property() {
        StreamingIndicators.RMA rma = new StreamingIndicators.RMA(3);
        rma.update(10);
        assertEquals(10.0, rma.value(), EPS);
    }

    // ===============================================================
    // VWAP
    // ===============================================================

    @Test
    public void test_VWAP_single_candle() {
        StreamingIndicators.VWAP vwap = new StreamingIndicators.VWAP();
        StreamingIndicators.Candle c = new StreamingIndicators.Candle(0, 12, 10, 11, 100);
        Double result = vwap.update(c);
        assertEquals(11.0, result, EPS);
    }

    @Test
    public void test_VWAP_two_candles() {
        StreamingIndicators.VWAP vwap = new StreamingIndicators.VWAP();
        vwap.update(new StreamingIndicators.Candle(0, 12, 10, 11, 100));
        Double result = vwap.update(new StreamingIndicators.Candle(0, 14, 11, 13, 200));
        assertEquals(3633.333 / 300.0, result, 1e-3);
    }

    @Test
    public void test_VWAP_compute_without_mutate() {
        StreamingIndicators.VWAP vwap = new StreamingIndicators.VWAP();
        vwap.update(new StreamingIndicators.Candle(0, 12, 10, 11, 100));
        Double result = vwap.compute(new StreamingIndicators.Candle(0, 14, 11, 13, 200));
        assertEquals(3633.333 / 300.0, result, 1e-3);
        assertEquals(11.0, vwap.value(), EPS);
    }

    @Test
    public void test_VWAP_zero_volume() {
        StreamingIndicators.VWAP vwap = new StreamingIndicators.VWAP();
        assertNull(vwap.value());
    }

    @Test
    public void test_VWAP_init_with_dataframe() {
        List<StreamingIndicators.Candle> candles = Arrays.asList(
                new StreamingIndicators.Candle(0, 12, 10, 11, 100),
                new StreamingIndicators.Candle(0, 14, 11, 13, 200));
        StreamingIndicators.VWAP vwap = new StreamingIndicators.VWAP(candles);
        assertEquals(3633.333 / 300.0, vwap.value(), 1e-3);
    }

    // ===============================================================
    // RSI
    // ===============================================================

    @Test
    public void test_RSI_basic() {
        StreamingIndicators.RSI rsi = new StreamingIndicators.RSI(5);
        double[] prices = { 10, 11, 12, 11, 12, 13 };
        Double[] results = new Double[prices.length];
        for (int i = 0; i < prices.length; i++) results[i] = rsi.update(prices[i]);
        for (int i = 0; i < 5; i++) assertNull(results[i]);
        // gains=[1,1,0,1,1], losses=[0,0,1,0,0]
        // avg_gain=0.8, avg_loss=0.2, rs=4, rsi=80
        assertEquals(80.0, results[5], EPS);
    }

    @Test
    public void test_RSI_subsequent() {
        StreamingIndicators.RSI rsi = new StreamingIndicators.RSI(5);
        for (double p : new double[] { 10, 11, 12, 11, 12, 13 }) rsi.update(p);
        Double result = rsi.update(10);
        // avg_gain = (0.8*4 + 0)/5 = 0.64
        // avg_loss = (0.2*4 + 3)/5 = 0.76
        // rs = 0.64/0.76, rsi = 100 - 100/(1 + rs)
        assertEquals(45.714, result, 0.01);
    }

    // ===============================================================
    // TRANGE
    // ===============================================================

    @Test
    public void test_TRANGE_first_candle() {
        StreamingIndicators.TRANGE tr = new StreamingIndicators.TRANGE();
        Double result = tr.update(new StreamingIndicators.Candle(0, 12, 10, 11));
        assertEquals(2.0, result, EPS);
    }

    @Test
    public void test_TRANGE_second_candle() {
        StreamingIndicators.TRANGE tr = new StreamingIndicators.TRANGE();
        tr.update(new StreamingIndicators.Candle(0, 12, 10, 11));
        Double result = tr.update(new StreamingIndicators.Candle(0, 14, 9, 13));
        assertEquals(5.0, result, EPS); // max(5,3,2)
    }

    @Test
    public void test_TRANGE_compute_no_mutate() {
        StreamingIndicators.TRANGE tr = new StreamingIndicators.TRANGE();
        tr.update(new StreamingIndicators.Candle(0, 12, 10, 11));
        Double result = tr.compute(new StreamingIndicators.Candle(0, 14, 9, 13));
        assertEquals(5.0, result, EPS);
        assertEquals(11.0, tr.prevClose, EPS);
    }

    @Test
    public void test_TRANGE_gap_up() {
        StreamingIndicators.TRANGE tr = new StreamingIndicators.TRANGE();
        tr.update(new StreamingIndicators.Candle(0, 10, 8, 9));
        Double result = tr.update(new StreamingIndicators.Candle(0, 15, 12, 14));
        assertEquals(6.0, result, EPS); // max(3,6,3)
    }

    // ===============================================================
    // CPR
    // ===============================================================

    @Test
    public void test_CPR_compute() {
        StreamingIndicators.CPR cpr = new StreamingIndicators.CPR();
        double[] result = cpr.compute(new StreamingIndicators.Candle(0, 120, 80, 110));
        assertEquals(103.33, result[0], 0.01);
        assertEquals(100.0, result[1], EPS);
        assertEquals(106.66, result[2], 0.01);
    }

    @Test
    public void test_CPR_update() {
        StreamingIndicators.CPR cpr = new StreamingIndicators.CPR();
        Double[] result = cpr.update(new StreamingIndicators.Candle(0, 120, 80, 110));
        Double[] val = cpr.value();
        assertEquals(val[0], result[0], EPS);
        assertEquals(val[1], result[1], EPS);
        assertEquals(val[2], result[2], EPS);
        assertEquals(103.33, cpr.cpr, 0.01);
        assertEquals(100.0, cpr.bc, EPS);
        assertEquals(106.66, cpr.tc, 0.01);
    }

    @Test
    public void test_CPR_value_initially_none() {
        StreamingIndicators.CPR cpr = new StreamingIndicators.CPR();
        Double[] v = cpr.value();
        assertNull(v[0]);
        assertNull(v[1]);
        assertNull(v[2]);
    }

    // ===============================================================
    // ATR
    // ===============================================================

    @Test
    public void test_ATR_returns_none_before_period() {
        StreamingIndicators.ATR atr = new StreamingIndicators.ATR(3);
        StreamingIndicators.Candle c1 = new StreamingIndicators.Candle(10, 12, 10, 11);
        StreamingIndicators.Candle c2 = new StreamingIndicators.Candle(11, 14, 9, 13);
        assertNull(atr.update(c1));
        assertNull(atr.update(c2));
    }

    @Test
    public void test_ATR_value_at_period() {
        StreamingIndicators.ATR atr = new StreamingIndicators.ATR(3);
        StreamingIndicators.Candle c1 = new StreamingIndicators.Candle(10, 12, 10, 11);
        StreamingIndicators.Candle c2 = new StreamingIndicators.Candle(11, 14, 9, 13);
        StreamingIndicators.Candle c3 = new StreamingIndicators.Candle(13, 15, 11, 14);
        atr.update(c1);
        atr.update(c2);
        Double result = atr.update(c3);
        // TR1=2, TR2=5, TR3=4 -> ATR=11/3
        assertEquals(11.0 / 3.0, result, EPS);
    }

    @Test
    public void test_ATR_smoothed_after_period() {
        StreamingIndicators.ATR atr = new StreamingIndicators.ATR(3);
        StreamingIndicators.Candle[] candles = {
                new StreamingIndicators.Candle(10, 12, 10, 11),
                new StreamingIndicators.Candle(11, 14, 9, 13),
                new StreamingIndicators.Candle(13, 15, 11, 14),
                new StreamingIndicators.Candle(14, 13, 10, 12),
        };
        for (int i = 0; i < 3; i++) atr.update(candles[i]);
        Double result = atr.update(candles[3]);
        // TR4 = max(3,1,4) = 4
        // ATR = (prev_atr*2 + 4)/3
        assertEquals((11.0 / 3.0 * 2 + 4) / 3.0, result, EPS);
    }

    // ===============================================================
    // BBands
    // ===============================================================

    @Test
    public void test_BBands_returns_none_before_full() {
        StreamingIndicators.BBands bb = new StreamingIndicators.BBands(3, 2.0);
        bb.update(10);
        bb.update(20);
        Double[] v = bb.value();
        assertNull(v[0]); assertNull(v[1]); assertNull(v[2]);
    }

    @Test
    public void test_BBands_basic() {
        StreamingIndicators.BBands bb = new StreamingIndicators.BBands(3, 2.0);
        bb.update(10);
        bb.update(20);
        bb.update(30);
        double ma = 20.0;
        double sd = Math.sqrt(200.0 / 3.0);
        Double[] v = bb.value();
        assertEquals(ma, v[1], EPS);
        assertEquals(ma + 2.0 * sd, v[0], EPS);
        assertEquals(ma - 2.0 * sd, v[2], EPS);
    }

    @Test
    public void test_BBands_compute_no_mutate() {
        StreamingIndicators.BBands bb = new StreamingIndicators.BBands(3, 2.0);
        bb.update(10);
        bb.update(20);
        Double[] result = bb.compute(30);
        double ma = 20.0;
        double sd = Math.sqrt(200.0 / 3.0);
        assertEquals(ma, result[1], EPS);
        assertEquals(ma + 2.0 * sd, result[0], EPS);
        assertEquals(ma - 2.0 * sd, result[2], EPS);
        assertNull(bb.middleband());
    }

    @Test
    public void test_BBands_properties() {
        StreamingIndicators.BBands bb = new StreamingIndicators.BBands(3, 1.0);
        bb.update(10);
        bb.update(20);
        bb.update(30);
        double sd = Math.sqrt(200.0 / 3.0);
        assertEquals(20.0, bb.middleband(), EPS);
        assertEquals(20.0 + sd, bb.upperband(), EPS);
        assertEquals(20.0 - sd, bb.lowerband(), EPS);
    }

    // ===============================================================
    // HeikinAshi
    // ===============================================================

    @Test
    public void test_HeikinAshi_first_candle() {
        StreamingIndicators.HeikinAshi ha = new StreamingIndicators.HeikinAshi();
        StreamingIndicators.HeikinAshiCandle result = ha.update(new StreamingIndicators.Candle(100, 110, 95, 105));
        assertEquals(102.5, result.close, EPS);
        assertEquals(100.0, result.open, EPS);
        assertEquals(110.0, result.high, EPS);
        assertEquals(95.0, result.low, EPS);
    }

    @Test
    public void test_HeikinAshi_second_candle() {
        StreamingIndicators.HeikinAshi ha = new StreamingIndicators.HeikinAshi();
        ha.update(new StreamingIndicators.Candle(100, 110, 95, 105));
        StreamingIndicators.HeikinAshiCandle result = ha.update(new StreamingIndicators.Candle(105, 115, 100, 112));
        // ha_close = (105+115+100+112)/4 = 108
        // ha_open = (100 + 102.5)/2 = 101.25
        assertEquals(108.0, result.close, EPS);
        assertEquals(101.25, result.open, EPS);
        assertEquals(115.0, result.high, EPS);
        assertEquals(100.0, result.low, EPS);
    }

    @Test
    public void test_HeikinAshi_compute_no_mutate() {
        StreamingIndicators.HeikinAshi ha = new StreamingIndicators.HeikinAshi();
        StreamingIndicators.HeikinAshiCandle result = ha.compute(new StreamingIndicators.Candle(100, 110, 95, 105));
        assertEquals(102.5, result.close, EPS);
        assertNull(ha.value);
    }

    // ===============================================================
    // Renko
    // ===============================================================

    @Test
    public void test_Renko_first_update_sets_price() {
        StreamingIndicators.Renko r = new StreamingIndicators.Renko();
        List<StreamingIndicators.Brick> result = r.update(100, 5.0);
        assertNull(result);
        assertEquals(100.0, r.brickEndPrice, EPS);
    }

    @Test
    public void test_Renko_no_brick_small_change() {
        StreamingIndicators.Renko r = new StreamingIndicators.Renko(100.0);
        List<StreamingIndicators.Brick> result = r.update(103, 5.0);
        assertNull(result);
    }

    @Test
    public void test_Renko_creates_brick_on_sufficient_change() {
        StreamingIndicators.Renko r = new StreamingIndicators.Renko(100.0);
        List<StreamingIndicators.Brick> result = r.update(106, 5.0);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).direction);
        assertEquals(105.0, result.get(0).brickEndPrice, EPS);
        assertEquals(0, result.get(0).brickNum);
    }

    @Test
    public void test_Renko_continuation_bricks() {
        StreamingIndicators.Renko r = new StreamingIndicators.Renko(100.0);
        r.update(106, 5.0);
        List<StreamingIndicators.Brick> result = r.update(112, 5.0);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).direction);
        assertEquals(110.0, result.get(0).brickEndPrice, EPS);
        assertEquals(1, result.get(0).brickNum);
    }

    @Test
    public void test_Renko_reversal_needs_double_brick() {
        StreamingIndicators.Renko r = new StreamingIndicators.Renko(100.0);
        r.update(106, 5.0);
        // change = 94-105 = -11, abs=11 >= 2*5 -> reverse, num_bricks-1 = 1
        List<StreamingIndicators.Brick> result = r.update(94, 5.0);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(-1, result.get(0).direction);
    }

    // ===============================================================
    // IsOrder
    // ===============================================================

    @Test
    public void test_IsOrder_increasing() {
        StreamingIndicators.IsOrder io = new StreamingIndicators.IsOrder(">", 3);
        assertFalse(io.update(1)); // fresh
        assertFalse(io.update(2)); // order_idx=2, < 3
        assertTrue(io.update(3));  // order_idx=3
    }

    @Test
    public void test_IsOrder_break_resets() {
        StreamingIndicators.IsOrder io = new StreamingIndicators.IsOrder(">", 3);
        io.update(1);
        io.update(2);
        io.update(3); // true
        boolean result = io.update(2); // 2>3 false -> reset
        assertFalse(result);
    }

    @Test
    public void test_IsOrder_decreasing() {
        StreamingIndicators.IsOrder io = new StreamingIndicators.IsOrder("<", 3);
        io.update(10);
        io.update(8);
        boolean result = io.update(5);
        assertTrue(result);
    }

    @Test
    public void test_IsOrder_custom_comparator() {
        StreamingIndicators.IsOrder io = new StreamingIndicators.IsOrder(
                (a, b) -> a == 2 * b, 3);
        io.update(1);
        io.update(2);
        boolean result = io.update(4);
        assertTrue(result);
        result = io.update(7);
        assertFalse(result);
    }
}
