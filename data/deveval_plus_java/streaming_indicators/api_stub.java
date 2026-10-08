// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.
//
// Numerical mapping note:
//   Python returns ``None`` from update/value before warmup completes. In Java
//   we mirror that by returning a *boxed* ``Double`` (or other reference type)
//   whose ``null`` value plays the role of ``None``. Floating-point comparisons
//   in tests use ``assertEquals(expected, actual, 1e-6)``.

import java.util.*;
import java.util.function.Function;

public class StreamingIndicators {

    // ---------------------------------------------------------------
    // Candle: a simple POJO representing a single OHLCV candle.
    // Mirrors the Python ``dict`` with keys 'open','high','low','close','volume'.
    // ---------------------------------------------------------------
    public static class Candle {
        public double open;
        public double high;
        public double low;
        public double close;
        public double volume;
        public Candle() { /* ... */ }
        public Candle(double open, double high, double low, double close, double volume) { /* ... */ }
        /** Convenience constructor used when volume is irrelevant. */
        public Candle(double open, double high, double low, double close) { /* ... */ }
    }

    // ---------------------------------------------------------------
    // Brick: a Renko brick.
    // ---------------------------------------------------------------
    public static class Brick {
        public int direction;
        public int brickNum;
        public double wickSize;
        public double brickSize;
        public double brickEndPrice;
        public double price;
        public Brick() { /* ... */ }
    }

    // ---------------------------------------------------------------
    // HeikinAshiCandle: result of a Heikin-Ashi computation.
    // ---------------------------------------------------------------
    public static class HeikinAshiCandle {
        public double open;
        public double high;
        public double low;
        public double close;
        public HeikinAshiCandle() { /* ... */ }
    }

    // ---------------------------------------------------------------
    // RollingStat: abstract class for any indicator computed by applying
    // a function over the latest ``period`` points.
    // ---------------------------------------------------------------
    public static class RollingStat {
        public final int period;
        public final Deque<Double> points;
        public final Function<List<Double>, Double> func;

        /**
         * @param period  Number of data points in the rolling window (>1).
         * @param func    Reduction function over a list of doubles.
         * @param points  Optional initial points; only the last ``period`` are kept.
         * @throws IllegalArgumentException if period <= 1.
         */
        public RollingStat(int period, Function<List<Double>, Double> func, List<Double> points) { /* ... */ }
        public RollingStat(int period, Function<List<Double>, Double> func) { /* ... */ }

        /** Compute (without mutating) the value if this point were appended. */
        public Double compute(double point) { /* ... */ return null; }
        /** Append a point and return the current value (or null if window not full). */
        public Double update(double point) { /* ... */ return null; }
        /** Current value, or null if the window has fewer than ``period`` points. */
        public Double value() { /* ... */ return null; }
    }

    /** Maximum in a rolling window. */
    public static class Max extends RollingStat {
        public Max(int period) { super(period, null); /* ... */ }
        public Max(int period, List<Double> points) { super(period, null, points); /* ... */ }
    }

    /** Minimum in a rolling window. */
    public static class Min extends RollingStat {
        public Min(int period) { super(period, null); /* ... */ }
        public Min(int period, List<Double> points) { super(period, null, points); /* ... */ }
    }

    /** Simple Moving Average. */
    public static class SMA extends RollingStat {
        public SMA(int period) { super(period, null); /* ... */ }
        public SMA(int period, List<Double> points) { super(period, null, points); /* ... */ }
    }

    /** Standard Deviation (population, like numpy.std with default ddof=0). */
    public static class SD extends RollingStat {
        public SD(int period) { super(period, null); /* ... */ }
        public SD(int period, List<Double> points) { super(period, null, points); /* ... */ }
    }

    // ---------------------------------------------------------------
    // EMA: Exponential Moving Average. Returns null until ``period`` points seen.
    // ---------------------------------------------------------------
    public static class EMA {
        public final int period;
        public final int smoothingFactor;
        public final double mult;
        public final Deque<Double> points;
        public Double value;

        public EMA(int period) { /* ... */ }
        public EMA(int period, int smoothingFactor) { /* ... */ }

        /** Compute (without mutating) the value if this point were appended. */
        public Double compute(double point) { /* ... */ return null; }
        /** Append a point and return the current EMA value (or null before warmup). */
        public Double update(double point) { /* ... */ return null; }
    }

    /** Weighted Moving Average. */
    public static class WMA {
        public final int period;
        public final Deque<Double> points;
        public Double value;

        public WMA(int period) { /* ... */ }
        public Double compute(double point) { /* ... */ return null; }
        public Double update(double point) { /* ... */ return null; }
    }

    /** Smoothed Moving Average (alias for an EMA of period {@code 2*period - 1}). */
    public static class SMMA {
        public final int period;
        public final int emaPeriod;
        public final EMA ema;
        public Double value;

        public SMMA(int period) { /* ... */ }
        public Double compute(double point) { /* ... */ return null; }
        public Double update(double point) { /* ... */ return null; }
    }

    /** Wilder/Pine ``ta.rma`` moving average used inside RSI. Each value is rounded to 4 dp. */
    public static class RMA {
        public final int period;
        public final Deque<Double> points;
        public Double rma;

        public RMA(int period) { /* ... */ }
        public Double value() { /* ... */ return null; }
        public Double update(double point) { /* ... */ return null; }
    }

    /** Volume Weighted Average Price. */
    public static class VWAP {
        public double tpvSum;
        public double volSum;

        public VWAP() { /* ... */ }
        public VWAP(List<Candle> candles) { /* ... */ }
        public Double vwap() { /* ... */ return null; }
        public Double value() { /* ... */ return null; }
        public Double compute(Candle candle) { /* ... */ return null; }
        public Double update(Candle candle) { /* ... */ return null; }
    }

    /** Relative Strength Index. */
    public static class RSI {
        public final int period;
        public final Deque<Double> points;
        public final Deque<Double> gains;
        public final Deque<Double> losses;
        public Double avgGain;
        public Double avgLoss;
        public Double rsi;
        public Double value;

        public RSI(int period) { /* ... */ }
        public Double update(double point) { /* ... */ return null; }
    }

    /** True Range. */
    public static class TRANGE {
        public Double prevClose;
        public Double value;

        public TRANGE() { /* ... */ }
        public Double compute(Candle candle) { /* ... */ return null; }
        public Double update(Candle candle) { /* ... */ return null; }
    }

    /** Central Pivot Range. */
    public static class CPR {
        public Double cpr;
        public Double bc;
        public Double tc;

        public CPR() { /* ... */ }
        /** Returns a length-3 array {cpr, bc, tc} (each rounded to 2dp). */
        public double[] compute(Candle candle) { /* ... */ return null; }
        /** Returns {cpr, bc, tc}; entries are null until first update. */
        public Double[] value() { /* ... */ return null; }
        public Double[] update(Candle candle) { /* ... */ return null; }
    }

    /** Average True Range. */
    public static class ATR {
        public final int period;
        public final TRANGE TR;
        public double atr;
        public Double value;
        public int count;

        public ATR(int period) { /* ... */ }
        public Double compute(Candle candle) { /* ... */ return null; }
        public Double update(Candle candle) { /* ... */ return null; }
    }

    /** Bollinger Bands. */
    public static class BBands {
        public final int period;
        public final double stddevMult;
        public final RollingStat MA;
        public final SD SDev;

        public BBands(int period, double stddevMult) { /* ... */ }
        public BBands(int period, double stddevMult, List<Double> points) { /* ... */ }

        public Double middleband() { /* ... */ return null; }
        public Double upperband() { /* ... */ return null; }
        public Double lowerband() { /* ... */ return null; }
        /** Returns {upper, middle, lower}; any may be null before warmup. */
        public Double[] value() { /* ... */ return null; }
        public Double[] compute(double point) { /* ... */ return null; }
        public Double[] update(double point) { /* ... */ return null; }
    }

    /** Heikin-Ashi candles. */
    public static class HeikinAshi {
        public HeikinAshiCandle value;
        public HeikinAshi() { /* ... */ }
        public HeikinAshiCandle compute(Candle candle) { /* ... */ return null; }
        public HeikinAshiCandle update(Candle candle) { /* ... */ return null; }
    }

    /** Renko bricks. */
    public static class Renko {
        public final List<Brick> bricks;
        public int currentDirection;
        public Double brickEndPrice;
        public double pwick;
        public double nwick;
        public int brickNum;
        public List<Brick> value;

        public Renko() { /* ... */ }
        public Renko(Double startPrice) { /* ... */ }
        /** Returns the list of bricks created on this update, or null if none. */
        public List<Brick> update(double price, Double brickSize) { /* ... */ return null; }
    }

    /**
     * Comparator used by {@link IsOrder}. Should return true if the new
     * element satisfies the ordering relative to the previous element.
     */
    @FunctionalInterface
    public interface OrderComparator {
        boolean test(double a, double b);
    }

    /** Checks whether the most recent {@code length} points are in a given order. */
    public static class IsOrder {
        public final OrderComparator comparator;
        public final int length;
        public final Deque<Double> q;
        public boolean fresh;
        public int orderIdx;
        public boolean isOrdered;
        public boolean value;

        /**
         * Construct from a string operator: one of {@code ">", "<", ">=", "<=", "=="}.
         */
        public IsOrder(String comparator, int length) { /* ... */ }
        /** Construct from a custom comparator. */
        public IsOrder(OrderComparator comparator, int length) { /* ... */ }
        public boolean update(double element) { /* ... */ return false; }
    }
}
