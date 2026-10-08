// API Stub — class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.util.ArrayList;
import java.util.List;

public class Indicators {

    public static class OHLCRow {
        public String date;
        public double open;
        public double high;
        public double low;
        public double close;
        public boolean uptrend;
        public int index;

        public OHLCRow(String date, double open, double high, double low, double close, boolean uptrend) { /* ... */ }
        public OHLCRow(String date, double open, double high, double low, double close) { /* ... */ }
    }

    public static class DataFrame {
        public List<OHLCRow> rows = new ArrayList<>();

        public void add(OHLCRow row) { /* ... */ }
        public OHLCRow get(int index) { /* ... */ }
        public int size() { /* ... */ }
        public DataFrame copy() { /* deep copy */ }
        public DataFrame tail(int n) { /* last n rows */ }
        public double minOf(String column) { /* min of "low" or "close" column */ }
        public double maxOf(String column) { /* max of "high" or "close" column */ }
    }

    public static class Instrument {
        protected DataFrame odf;  // original data (copy)
        protected DataFrame df;   // working data (copy)
        public static final int UPTREND_CONTINUAL = 0;
        public static final int UPTREND_REVERSAL = 1;
        public static final int DOWNTREND_CONTINUAL = 2;
        public static final int DOWNTREND_REVERSAL = 3;

        public Instrument(DataFrame df) { /* store copies in odf and df */ }
    }

    public static class Renko extends Instrument {
        protected DataFrame cdf;
        public double brickSize = 1;
        public static final int PERIOD_CLOSE = 1;
        public static final int PRICE_MOVEMENT = 2;
        public int chartType = PERIOD_CLOSE;

        public Renko(DataFrame df) { /* ... */ }
        public DataFrame getOhlcData() { /* dispatch to periodCloseBricks or priceMovementBricks based on chartType */ }
        public DataFrame periodCloseBricks() { /* build Renko bricks from close prices, return cdf */ }
        public void priceMovementBricks() { /* build Renko bricks from high/low prices, stores in cdf */ }
    }

    public static class LineBreak extends Instrument {
        protected DataFrame cdf;
        public int lineNumber = 3;

        public LineBreak(DataFrame df) { /* ... */ }
        public boolean uptrendReversal(double close) { /* true if close < min low of last lineNumber rows in cdf */ }
        public boolean downtrendReversal(double close) { /* true if close > max high of last lineNumber rows in cdf */ }
        public DataFrame getOhlcData() { /* build line break chart, return cdf */ }
    }

    public static class PnF extends Instrument {
        protected DataFrame cdf;
        public double boxSize = 2;
        public int reversalSize = 3;

        public PnF(DataFrame df) { /* ... */ }
        public Integer getState(boolean uptrendP1, int bricks) { /* return trend state or null if bricks==0 */ }
        public int roundIt(double x, int base) { /* round x to nearest multiple of base using banker's rounding */ }
        public DataFrame getOhlcData() { /* build point-and-figure chart, return cdf */ }
        public DataFrame getBarOhlcData() { /* single-bar summary: max close, max high from cdf */ }
    }
}
