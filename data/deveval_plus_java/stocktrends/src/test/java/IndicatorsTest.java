import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IndicatorsTest {

    private Indicators.DataFrame sampleDf;
    private Indicators.Renko renko;
    private Indicators.LineBreak lineBreak;
    private Indicators.PnF pnf;

    @BeforeEach
    public void setUp() {
        sampleDf = new Indicators.DataFrame();
        sampleDf.add(new Indicators.OHLCRow("2023-01-01", 100.0, 102.0, 99.0, 101.0));
        sampleDf.add(new Indicators.OHLCRow("2023-01-02", 101.0, 105.0, 100.0, 104.0));
        sampleDf.add(new Indicators.OHLCRow("2023-01-03", 104.0, 108.0, 103.0, 107.0));
        sampleDf.add(new Indicators.OHLCRow("2023-01-04", 107.0, 108.0, 92.0, 94.0));
        sampleDf.add(new Indicators.OHLCRow("2023-01-05", 94.0, 95.0, 85.0, 88.0));
        sampleDf.add(new Indicators.OHLCRow("2023-01-06", 88.0, 115.0, 87.0, 112.0));

        renko = new Indicators.Renko(sampleDf);
        lineBreak = new Indicators.LineBreak(sampleDf);
        pnf = new Indicators.PnF(sampleDf);
    }

    @Test
    public void testInstrumentConstants() {
        Indicators.Instrument inst = new Indicators.Instrument(sampleDf);
        assertEquals(0, inst.UPTREND_CONTINUAL);
        assertEquals(1, inst.UPTREND_REVERSAL);
        assertEquals(2, inst.DOWNTREND_CONTINUAL);
        assertEquals(3, inst.DOWNTREND_REVERSAL);
    }

    @Test
    public void testInstrumentOhlcSet() {
        Indicators.Instrument inst = new Indicators.Instrument(sampleDf);
        assertTrue(inst.df.size() > 0);
    }

    @Test
    public void testInstrumentInitValid() {
        Indicators.Instrument inst = new Indicators.Instrument(sampleDf);
        assertEquals(6, inst.df.size());
    }

    @Test
    public void testInstrumentInitStoresCopy() {
        Indicators.Instrument inst = new Indicators.Instrument(sampleDf);
        sampleDf.get(0).close = 999.0;
        assertEquals(101.0, inst.df.get(0).close);
    }

    @Test
    public void testInstrumentInitMissingColumns() {
        // OHLCRow always has all OHLC fields; test empty DataFrame instead
        Indicators.DataFrame df = new Indicators.DataFrame();
        Indicators.Instrument inst = new Indicators.Instrument(df);
        assertEquals(0, inst.df.size());
    }

    @Test
    public void testInstrumentInitStoresOdf() {
        Indicators.Instrument inst = new Indicators.Instrument(sampleDf);
        assertNotNull(inst.odf);
        assertEquals(inst.odf.size(), inst.df.size());
        assertNotSame(inst.odf, inst.df);
    }

    @Test
    public void testInstrumentValidateDfPartialColumns() {
        // OHLCRow always has all OHLC fields; verify single-row DF works
        Indicators.DataFrame df = new Indicators.DataFrame();
        df.add(new Indicators.OHLCRow("2023-01-01", 1.0, 2.0, 0.0, 1.0));
        Indicators.Instrument inst = new Indicators.Instrument(df);
        assertEquals(1, inst.df.size());
    }

    @Test
    public void testInstrumentValidateDfExtraColumnsOk() {
        sampleDf.add(new Indicators.OHLCRow("2023-01-07", 100.0, 200.0, 300.0, 400.0));
        Indicators.Instrument inst = new Indicators.Instrument(sampleDf);
        assertEquals(7, inst.df.size());
    }

    @Test
    public void testRenkoInheritsAndDefaults() {
        assertTrue(renko instanceof Indicators.Instrument);
        assertEquals(1, renko.brickSize);
        assertEquals(Indicators.Renko.PERIOD_CLOSE, renko.chartType);
        assertEquals(1, renko.PERIOD_CLOSE);
        assertEquals(2, renko.PRICE_MOVEMENT);
    }

    @Test
    public void testRenkoPeriodCloseBricks() {
        Indicators.DataFrame result = renko.periodCloseBricks();
        assertEquals(48, result.size());
        assertEquals(100.0, result.get(0).open);
        assertEquals(101.0, result.get(0).close);
    }

    @Test
    public void testRenkoPeriodCloseBricksHasReversals() {
        Indicators.DataFrame result = renko.periodCloseBricks();
        boolean hasTrue = false;
        boolean hasFalse = false;
        for (Indicators.OHLCRow row : result.rows) {
            if (row.uptrend) hasTrue = true;
            if (!row.uptrend) hasFalse = true;
        }
        assertTrue(hasTrue);
        assertTrue(hasFalse);
    }

    @Test
    public void testRenkoPeriodCloseBricksFirstReversal() {
        Indicators.DataFrame result = renko.periodCloseBricks();
        for (int i = 1; i < result.size(); i++) {
            if (result.get(i).uptrend != result.get(i - 1).uptrend) {
                assertEquals(7, i);
                assertFalse(result.get(i).uptrend);
                assertEquals(106.0, result.get(i).open);
                assertEquals(105.0, result.get(i).close);
                break;
            }
        }
    }

    @Test
    public void testRenkoPeriodCloseBricksUpdownCounts() {
        Indicators.DataFrame result = renko.periodCloseBricks();
        int upCount = 0;
        int downCount = 0;
        for (Indicators.OHLCRow row : result.rows) {
            if (row.uptrend) upCount++;
            else downCount++;
        }
        assertEquals(30, upCount);
        assertEquals(18, downCount);
    }

    @Test
    public void testRenkoPeriodCloseBricksLastBrick() {
        Indicators.DataFrame result = renko.periodCloseBricks();
        assertEquals(111.0, result.get(result.size() - 1).open);
        assertEquals(112.0, result.get(result.size() - 1).close);
        assertTrue(result.get(result.size() - 1).uptrend);
    }

    @Test
    public void testRenkoPriceMovementBricks() {
        renko.priceMovementBricks();
        assertEquals(94, renko.cdf.size());
        boolean hasTrue = false;
        boolean hasFalse = false;
        for (Indicators.OHLCRow row : renko.cdf.rows) {
            if (row.uptrend) hasTrue = true;
            if (!row.uptrend) hasFalse = true;
        }
        assertTrue(hasTrue);
        assertTrue(hasFalse);
    }

    @Test
    public void testRenkoPriceMovementBricksFirstBrick() {
        renko.priceMovementBricks();
        assertEquals(100.0, renko.cdf.get(0).open);
        assertEquals(101.0, renko.cdf.get(0).close);
        assertTrue(renko.cdf.get(0).uptrend);
    }

    @Test
    public void testRenkoPriceMovementBricksUpdownCounts() {
        renko.priceMovementBricks();
        int upCount = 0;
        int downCount = 0;
        for (Indicators.OHLCRow row : renko.cdf.rows) {
            if (row.uptrend) upCount++;
            else downCount++;
        }
        assertEquals(41, upCount);
        assertEquals(53, downCount);
    }

    @Test
    public void testRenkoPriceMovementBricksFirstReversal() {
        renko.priceMovementBricks();
        for (int i = 1; i < renko.cdf.size(); i++) {
            if (renko.cdf.get(i).uptrend != renko.cdf.get(i - 1).uptrend) {
                assertEquals(5, i);
                assertFalse(renko.cdf.get(i).uptrend);
                assertEquals(104.0, renko.cdf.get(i).open);
                assertEquals(103.0, renko.cdf.get(i).close);
                break;
            }
        }
    }

    @Test
    public void testRenkoGetOhlcDataPeriodClose() {
        renko.chartType = renko.PERIOD_CLOSE;
        Indicators.DataFrame result = renko.getOhlcData();
        assertEquals(48, result.size());
    }

    @Test
    public void testRenkoGetOhlcDataPriceMovement() {
        renko.chartType = renko.PRICE_MOVEMENT;
        Indicators.DataFrame result = renko.getOhlcData();
        assertEquals(94, result.size());
    }

    @Test
    public void testLineBreakInheritsAndDefaults() {
        assertTrue(lineBreak instanceof Indicators.Instrument);
        assertEquals(3, lineBreak.lineNumber);
    }

    @Test
    public void testLineBreakUptrendReversalTrue() {
        // Python: cdf = pd.DataFrame({'low': [1, 2, 3]}) → min(low) = 1, 0.5 < 1 → True
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 1, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 2, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 3, 0));
        assertTrue(lineBreak.uptrendReversal(0.5));
    }

    @Test
    public void testLineBreakUptrendReversalFalse() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 1, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 2, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 3, 0));
        assertFalse(lineBreak.uptrendReversal(2.5));
    }

    @Test
    public void testLineBreakUptrendReversalInsufficientRows() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 1, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 2, 0));
        assertFalse(lineBreak.uptrendReversal(0.5));
    }

    @Test
    public void testLineBreakUptrendReversalBoundaryEqual() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 1, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 2, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 3, 0));
        assertFalse(lineBreak.uptrendReversal(1.0));
    }

    @Test
    public void testLineBreakUptrendReversalBoundaryJustBelow() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 1, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 2, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 0, 3, 0));
        assertTrue(lineBreak.uptrendReversal(0.99));
    }

    @Test
    public void testLineBreakDowntrendReversalTrue() {
        // Python: cdf = pd.DataFrame({'high': [3, 4, 5]}) → max(high) = 5, 6 > 5 → True
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 3, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 4, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 5, 0, 0));
        assertTrue(lineBreak.downtrendReversal(6));
    }

    @Test
    public void testLineBreakDowntrendReversalFalse() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 3, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 4, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 5, 0, 0));
        assertFalse(lineBreak.downtrendReversal(4));
    }

    @Test
    public void testLineBreakDowntrendReversalInsufficientRows() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 3, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 4, 0, 0));
        assertFalse(lineBreak.downtrendReversal(10));
    }

    @Test
    public void testLineBreakDowntrendReversalBoundaryEqual() {
        lineBreak.cdf = new Indicators.DataFrame();
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 3, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 4, 0, 0));
        lineBreak.cdf.add(new Indicators.OHLCRow("d", 0, 5, 0, 0));
        assertFalse(lineBreak.downtrendReversal(5));
    }

    @Test
    public void testLineBreakGetOhlcData() {
        Indicators.DataFrame result = lineBreak.getOhlcData();
        assertEquals(6, result.size());
        boolean hasTrue = false;
        boolean hasFalse = false;
        for (Indicators.OHLCRow row : result.rows) {
            if (row.uptrend) hasTrue = true;
            if (!row.uptrend) hasFalse = true;
        }
        assertTrue(hasTrue);
        assertTrue(hasFalse);
    }

    @Test
    public void testLineBreakGetOhlcDataInitialEntries() {
        Indicators.DataFrame result = lineBreak.getOhlcData();
        assertEquals(100.0, result.get(0).open);
        assertEquals(101.0, result.get(0).close);
        assertTrue(result.get(0).uptrend);
        assertEquals(104.0, result.get(2).open);
        assertEquals(107.0, result.get(2).close);
    }

    @Test
    public void testLineBreakGetOhlcDataReversalValues() {
        Indicators.DataFrame result = lineBreak.getOhlcData();
        assertFalse(result.get(3).uptrend);
        assertEquals(107.0, result.get(3).open);
        assertEquals(94.0, result.get(3).close);
        assertTrue(result.get(5).uptrend);
        assertEquals(112.0, result.get(5).close);
    }

    @Test
    public void testLineBreakGetOhlcDataShortData() {
        Indicators.DataFrame shortDf = new Indicators.DataFrame();
        shortDf.add(new Indicators.OHLCRow("2023-01-01", 100.0, 103.0, 99.0, 102.0));
        shortDf.add(new Indicators.OHLCRow("2023-01-02", 102.0, 105.0, 101.0, 104.0));
        Indicators.LineBreak lb = new Indicators.LineBreak(shortDf);
        Indicators.DataFrame result = lb.getOhlcData();
        assertEquals(2, result.size());
    }

    @Test
    public void testPnFInheritsAndDefaults() {
        assertTrue(pnf instanceof Indicators.Instrument);
        assertEquals(2, pnf.boxSize);
        assertEquals(3, pnf.reversalSize);
    }

    @Test
    public void testPnFBrickSize() {
        assertEquals(pnf.boxSize, pnf.boxSize);
        assertEquals(2, pnf.boxSize);
    }

    @Test
    public void testPnFBrickSizeFollowsBoxSize() {
        pnf.boxSize = 5;
        assertEquals(5, pnf.boxSize);
    }

    @Test
    public void testPnFGetStateUptrendContinual() {
        assertEquals(pnf.UPTREND_CONTINUAL, pnf.getState(true, 1));
    }

    @Test
    public void testPnFGetStateUptrendReversal() {
        assertEquals(pnf.UPTREND_REVERSAL, pnf.getState(true, -1));
    }

    @Test
    public void testPnFGetStateDowntrendContinual() {
        assertEquals(pnf.DOWNTREND_CONTINUAL, pnf.getState(false, -1));
    }

    @Test
    public void testPnFGetStateDowntrendReversal() {
        assertEquals(pnf.DOWNTREND_REVERSAL, pnf.getState(false, 1));
    }

    @Test
    public void testPnFGetStateZeroBricks() {
        assertNull(pnf.getState(true, 0));
    }

    @Test
    public void testPnFGetStateLargeBricks() {
        assertEquals(pnf.UPTREND_CONTINUAL, pnf.getState(true, 100));
        assertEquals(pnf.UPTREND_REVERSAL, pnf.getState(true, -100));
        assertEquals(pnf.DOWNTREND_REVERSAL, pnf.getState(false, 100));
        assertEquals(pnf.DOWNTREND_CONTINUAL, pnf.getState(false, -100));
    }

    @Test
    public void testPnFRoundit() {
        assertEquals(10, pnf.roundIt(12, 5));
        assertEquals(15, pnf.roundIt(13, 5));
        assertEquals(10, pnf.roundIt(10, 5));
    }

    @Test
    public void testPnFRounditNegative() {
        assertEquals(-5, pnf.roundIt(-3, 5));
        assertEquals(0, pnf.roundIt(0, 5));
    }

    @Test
    public void testPnFRounditDifferentBase() {
        assertEquals(6, pnf.roundIt(6, 2));
        assertEquals(8, pnf.roundIt(7, 2));
    }

    @Test
    public void testPnFGetOhlcData() {
        Indicators.DataFrame result = pnf.getOhlcData();
        assertEquals(23, result.size());
        boolean hasTrue = false;
        boolean hasFalse = false;
        for (Indicators.OHLCRow row : result.rows) {
            if (row.uptrend) hasTrue = true;
            if (!row.uptrend) hasFalse = true;
        }
        assertTrue(hasTrue);
        assertTrue(hasFalse);
    }

    @Test
    public void testPnFGetOhlcDataFirstBox() {
        Indicators.DataFrame result = pnf.getOhlcData();
        assertEquals(98, result.get(0).open);
        assertEquals(100, result.get(0).close);
        assertTrue(result.get(0).uptrend);
    }

    @Test
    public void testPnFGetOhlcDataReversalPoint() {
        Indicators.DataFrame result = pnf.getOhlcData();
        assertTrue(result.get(3).uptrend);
        assertFalse(result.get(4).uptrend);
        assertEquals(104, result.get(4).open);
        assertEquals(102, result.get(4).close);
    }

    @Test
    public void testPnFGetOhlcDataUpdownCounts() {
        Indicators.DataFrame result = pnf.getOhlcData();
        int upCount = 0;
        int downCount = 0;
        for (Indicators.OHLCRow row : result.rows) {
            if (row.uptrend) upCount++;
            else downCount++;
        }
        assertEquals(15, upCount);
        assertEquals(8, downCount);
    }

    @Test
    public void testPnFGetBarOhlcData() {
        Indicators.DataFrame result = pnf.getBarOhlcData();
        assertEquals(1, result.size());
        assertTrue(result.get(0).open >= 0);
        assertTrue(result.get(0).close >= 0);
    }

    @Test
    public void testPnFGetBarOhlcDataValues() {
        Indicators.DataFrame result = pnf.getBarOhlcData();
        assertEquals(100.0, result.get(0).open);
        assertEquals(112.0, result.get(0).close);
        assertEquals(112.0, result.get(0).high);
    }

    @Test
    public void testPnFGetBarOhlcDataColumns() {
        Indicators.DataFrame result = pnf.getBarOhlcData();
        assertNotNull(result.get(0).date);
        assertTrue(result.get(0).high >= 0);
        assertTrue(result.get(0).low >= 0);
    }
}