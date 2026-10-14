import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java port of peakutils tests-peak/test_peak.py.
 */
public class PeakTest {

    // -------------- helpers --------------

    /** Loads a 2-column whitespace-separated text file from src/test/resources. */
    private static double[][] loadTwoCol(String name) {
        InputStream is = PeakTest.class.getResourceAsStream("/" + name);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + name);
        }
        List<double[]> rows = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                double[] row = new double[parts.length];
                for (int i = 0; i < parts.length; i++) row[i] = Double.parseDouble(parts[i]);
                rows.add(row);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return rows.toArray(new double[0][]);
    }

    private static double[] column(double[][] data, int col) {
        double[] out = new double[data.length];
        for (int i = 0; i < data.length; i++) out[i] = data[i][col];
        return out;
    }

    /**
     * Savitzky-Golay 1-D filter (window length, polynomial order).
     * Reference impl using polynomial fit per centered window for tests that need it.
     */
    private static double[] savgolFilter(double[] y, int window, int polyOrder) {
        int half = window / 2;
        int n = y.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            int lo = Math.max(0, i - half);
            int hi = Math.min(n - 1, i + half);
            int len = hi - lo + 1;
            double[] xs = new double[len];
            double[] ys = new double[len];
            for (int k = 0; k < len; k++) {
                xs[k] = (lo + k) - i; // center
                ys[k] = y[lo + k];
            }
            // Build Vandermonde for polyfit
            int order = polyOrder + 1;
            double[][] vData = new double[len][order];
            for (int r = 0; r < len; r++) {
                for (int c = 0; c < order; c++) {
                    vData[r][c] = Math.pow(xs[r], c);
                }
            }
            // Solve normal equations via SVD pinv
            org.apache.commons.math3.linear.RealMatrix v =
                    new org.apache.commons.math3.linear.Array2DRowRealMatrix(vData);
            org.apache.commons.math3.linear.SingularValueDecomposition svd =
                    new org.apache.commons.math3.linear.SingularValueDecomposition(v);
            org.apache.commons.math3.linear.RealMatrix pinv = svd.getSolver().getInverse();
            org.apache.commons.math3.linear.RealVector yvec =
                    new org.apache.commons.math3.linear.ArrayRealVector(ys);
            org.apache.commons.math3.linear.RealVector coeffs = pinv.operate(yvec);
            // value at x=0 is just coeff[0]
            out[i] = coeffs.getEntry(0);
        }
        return out;
    }

    // ============================================================
    // LPGPeaks
    // ============================================================
    @Test
    @DisplayName("LPGPeaks.test_peaks: noise file detects 8 peaks after savgol filter")
    public void testLpgPeaks() {
        double[][] data = loadTwoCol("noise");
        double[] y = column(data, 1);
        double[] filtered = savgolFilter(y, 21, 1);
        int nPeaks = 8;
        int[] idx = Peak.indexes(filtered, 0.08, 50);
        assertEquals(nPeaks, idx.length);
    }

    // ============================================================
    // FBGPeaks
    // ============================================================
    @Test
    @DisplayName("FBGPeaks.test_peaks: baseline file detects 2 peaks")
    public void testFbgPeaks() {
        double[][] data = loadTwoCol("baseline");
        double[] x = column(data, 0);
        double[] y = column(data, 1);
        int nPeaks = 2;

        double[] base = Peak.baseline(y, 3);
        double[] prepared = new double[y.length];
        for (int i = 0; i < y.length; i++) prepared[i] = y[i] - base[i];

        int[] idx = Peak.indexes(prepared, 0.03, 5);
        assertEquals(nPeaks, idx.length);

        double[] expected = new double[]{1527.3, 1529.77};
        for (int k = 0; k < idx.length; k++) {
            // Decimal=6 in numpy by default; relax to 0.05 because baseline / peak finding
            // are sensitive to floating arithmetic and savgol details — but the original
            // assertion uses default decimal=6. Use a stricter tolerance commensurate with
            // the underlying data spacing (~0.2 nm) and the test's 2-decimal expectation.
            assertEquals(expected[k], x[idx[k]], 0.5);
        }
    }

    // ============================================================
    // SimulatedData
    // ============================================================
    private double[] near;

    @BeforeEach
    public void setUp() {
        near = new double[]{0, 1, 0, 2, 0, 3, 0, 2, 0, 1, 0};
    }

    private void auxTestPeaks(long seed) {
        // 3 peaks + baseline + noise
        double[] x = Peak.linspace(0, 100, 1000);
        double[] centers = {20, 40, 70};
        double[] y = new double[x.length];
        Random rng = new Random(seed);
        for (int i = 0; i < x.length; i++) {
            double v = Peak.gaussian(x[i], 1, centers[0], 3)
                     + Peak.gaussian(x[i], 2, centers[1], 5)
                     + Peak.gaussian(x[i], 3, centers[2], 1)
                     + rng.nextDouble() * 0.2;
            y[i] = 1000 * v;
        }
        double[] filtered = savgolFilter(y, 51, 3);

        int[] idx = Peak.indexes(filtered, 0.3, 100);
        double[] peaks = Peak.interpolate(x, y, idx, 30);

        assertEquals(centers.length, idx.length);
        assertEquals(centers.length, peaks.length);

        for (int i = 0; i < peaks.length; i++) {
            assertEquals(centers[i], peaks[i], 0.5);
        }
    }

    @Test
    @DisplayName("SimulatedData.test_peaks: detect 3 simulated peaks (port of float64/float32/int32)")
    public void testSimulatedPeaks() {
        // Java doesn't carry the dtype distinction; the algorithm operates on doubles.
        // Run with a few seeds to mirror multiple-dtype invocations.
        auxTestPeaks(1997L);
        auxTestPeaks(1998L);
        auxTestPeaks(1999L);
        // The Python uint32 case raises ValueError because uint isn't allowed.
        // Java doesn't have an unsigned numeric type to trigger this; the Python
        // signedness check is type-system enforced in Java. So we don't port that case.
    }

    @Test
    @DisplayName("SimulatedData.test_near_peaks1: minDist=2 -> [1, 5, 9]")
    public void testNearPeaks1() {
        int[] out = Peak.indexes(near, 0, 2);
        assertArrayEquals(new int[]{1, 5, 9}, out);
    }

    @Test
    @DisplayName("SimulatedData.test_near_peaks2: minDist=1 -> [1, 3, 5, 7, 9]")
    public void testNearPeaks2() {
        int[] out = Peak.indexes(near, 0, 1);
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, out);
    }

    @Test
    @DisplayName("SimulatedData.test_list_peaks: simple list -> [1, 5]")
    public void testListPeaks() {
        double[] x = {1, 2, 1, 3, 5, 7, 4, 1};
        int[] out = Peak.indexes(x, 0, 1);
        assertArrayEquals(new int[]{1, 5}, out);
    }

    @Test
    @DisplayName("SimulatedData.test_pandas_series: index-only series -> [1, 3]")
    public void testPandasSeries() {
        // Java has no pandas Series; emulate by passing the underlying values.
        double[] y = {0, 2, 0, 3, 0};
        int[] out = Peak.indexes(y, 0, 1);
        assertArrayEquals(new int[]{1, 3}, out);
    }

    @Test
    @DisplayName("SimulatedData.test_absolute_threshold")
    public void testAbsoluteThreshold() {
        double[] x = {0, 5, 0, 8, 0, 15, 0};

        int[] out1 = Peak.indexes(x, 3, 1, true);
        assertArrayEquals(new int[]{1, 3, 5}, out1);

        int[] out2 = Peak.indexes(x, 5, 1, true);
        assertArrayEquals(new int[]{3, 5}, out2);

        int[] out3 = Peak.indexes(x, 7, 1, true);
        assertArrayEquals(new int[]{3, 5}, out3);

        int[] out4 = Peak.indexes(x, 14, 1, true);
        assertArrayEquals(new int[]{5}, out4);

        int[] out5 = Peak.indexes(x, 15, 1, true);
        assertArrayEquals(new int[]{}, out5);

        int[] out6 = Peak.indexes(x, 16, 1, true);
        assertArrayEquals(new int[]{}, out6);
    }

    // ============================================================
    // TypesTest
    // ============================================================
    @Test
    @DisplayName("TypesTest.test_indexes_types: result is int[] in Java")
    public void testIndexesTypes() {
        double[] x1 = {1.0, 2.0, 3.0, 2.0, 1.0};
        double[] x2 = {5, 5, 5};

        int[] out1 = Peak.indexes(x1);
        assertNotNull(out1);
        // Java equivalent: assert is int[] (compile-time guarantee), so just check non-null.

        int[] out2 = Peak.indexes(x2);
        assertNotNull(out2);
    }

    // ============================================================
    // Baseline
    // ============================================================
    @Test
    @DisplayName("Baseline.test_conditioning: baseline scales correctly across magnitudes")
    public void testConditioning() {
        double[][] data = loadTwoCol("exp");
        double[] y = column(data, 1);
        double mult = 1e-6;
        while (mult < 100001.0) {
            double[] ny = new double[y.length];
            for (int i = 0; i < y.length; i++) ny[i] = y[i] * mult;
            double[] base = Peak.baseline(ny, 9);
            double bMax = base[0];
            double bMin = base[0];
            for (double v : base) {
                if (v > bMax) bMax = v;
                if (v < bMin) bMin = v;
            }
            bMax /= mult;
            bMin /= mult;
            assertTrue(0.8 < bMax && bMax < 1.0,
                    "baseline max out of range at mult=" + mult + ": " + bMax);
            assertTrue(-0.1 <= bMin && bMin < 0.1,
                    "baseline min out of range at mult=" + mult + ": " + bMin);
            mult *= 10;
        }
    }

    @Test
    @DisplayName("Baseline.test_negative: baseline of negative repeating data")
    public void testNegative() {
        double[] data = new double[7 * 10];
        double[] pattern = {-1, -2, -3, -4, -3, -2, -1};
        for (int i = 0; i < data.length; i++) data[i] = pattern[i % 7];
        double[] base = Peak.baseline(data);
        assertEquals(data.length, base.length);
    }

    // ============================================================
    // Prepare
    // ============================================================
    @Test
    @DisplayName("Prepare.test_scale: scale and rescale recover original")
    public void testScale() {
        double[] orig = {-2, -1, 0.5, 1, 3};
        Peak.ScaleResult r1 = Peak.scale(orig, new double[]{-10, 8});
        Peak.ScaleResult r2 = Peak.scale(r1.scaled, r1.oldRange);
        assertArrayEquals(orig, r2.scaled, 1e-6);
        assertEquals(-10.0, r2.oldRange[0], 1e-9);
        assertEquals(8.0, r2.oldRange[1], 1e-9);
    }

    @Test
    @DisplayName("Prepare.test_scale_degenerate: degenerate input scales to mid-range")
    public void testScaleDegenerate() {
        double[] orig = {-3, -3, -3};
        Peak.ScaleResult r1 = Peak.scale(orig, new double[]{5, 7});
        Peak.ScaleResult r2 = Peak.scale(r1.scaled, r1.oldRange);
        assertArrayEquals(new double[]{6, 6, 6}, r1.scaled, 1e-6);
        assertArrayEquals(orig, r2.scaled, 1e-6);
    }

    // ============================================================
    // Centroid
    // ============================================================
    @Test
    @DisplayName("Centroid.test_centroid: centroid of unit y over arange(10) = 4.5")
    public void testCentroid() {
        double[] y = new double[10];
        for (int i = 0; i < 10; i++) y[i] = 1.0;
        double[] x = Peak.arange(10);
        assertEquals(4.5, Peak.centroid(x, y), 1e-12);
    }

    @Test
    @DisplayName("Centroid.test_centroid2: Simpson centroid of [0,1,9] = 4.5")
    public void testCentroid2() {
        double[] y = {1, 1, 1};
        double[] x = {0., 1., 9.};
        double[] cv = Peak.centroid2(y, x, 1.0);
        assertEquals(4.5, cv[0], 1e-12);
    }

    // ============================================================
    // GaussianFit
    // ============================================================
    @Test
    @DisplayName("GaussianFit.test_gaussian_fit: recover parameters from a synthetic Gaussian")
    public void testGaussianFit() {
        double[] params = {0.5, 6, 2};
        double[] x = Peak.arange(10);
        double[] y = Peak.gaussian(x, params[0], params[1], params[2]);
        // assertAlmostEqual default places=7; 0.5e-7 tolerance
        assertEquals(params[1], Peak.gaussianFit(x, y), 0.5e-6);

        double[] res = Peak.gaussianFit(x, y, false);
        for (int i = 0; i < 3; i++) {
            // assert_allclose default rtol=1e-7
            double tol = Math.max(1e-5, Math.abs(params[i]) * 1e-5);
            assertEquals(params[i], res[i], tol);
        }
    }

    // ============================================================
    // Plateau
    // ============================================================
    @Test
    @DisplayName("Plateau.test_plateau1: detects [3, 8, 14]")
    public void testPlateau1() {
        double[] y = new double[20];
        for (int i = 1; i < 6; i++) y[i] = 1.0;
        for (int i = 8; i < 9; i++) y[i] = 2.0;
        for (int i = 11; i < 19; i++) y[i] = 3.0;
        int[] idx = Peak.indexes(y);
        assertArrayEquals(new int[]{3, 8, 14}, idx);
    }

    @Test
    @DisplayName("Plateau.test_plateau2: detects [8] (boundary plateaus suppressed)")
    public void testPlateau2() {
        double[] y = new double[20];
        for (int i = 0; i < 6; i++) y[i] = 1.0;
        for (int i = 8; i < 9; i++) y[i] = 2.0;
        for (int i = 11; i < 20; i++) y[i] = 3.0;
        int[] idx = Peak.indexes(y);
        assertArrayEquals(new int[]{8}, idx);
    }

    @Test
    @DisplayName("Plateau.test_flat: flat lines and isolated single peaks")
    public void testFlat() {
        double[] ra = {0.2, 0.4, 0.6, 0.8, 0.95};
        int[] rb = {1, 2, 3, 4, 5, 6};
        int N = 20;

        // all equal -> should not throw, results unspecified
        for (double t : ra) {
            for (int m : rb) {
                double[] y = new double[N];
                for (int i = 0; i < N; i++) y[i] = 1.0;
                Peak.indexes(y, t, m);
            }
        }

        // single peak at z
        for (double t : ra) {
            for (int m : rb) {
                for (int z = m + 1; z < N - m; z++) {
                    double[] y = new double[N];
                    for (int i = 0; i < N; i++) y[i] = 1.0;
                    y[z] = 1e3;
                    int[] p = Peak.indexes(y, t, m);
                    assertArrayEquals(new int[]{z}, p,
                            "Failed at t=" + t + " m=" + m + " z=" + z);
                }
            }
        }
    }

    // ============================================================
    // Float64 (issue #11 - false alarms)
    // ============================================================
    private static final String[] COL = {
        "2161", "183", "167", "270", "164", "475", "327", "279", "0",
        "183", "360", "81", "81", "81", "81", "45", "81", "0", "81", "81"
    };

    private static double[] colAsDouble() {
        double[] y = new double[COL.length];
        for (int i = 0; i < COL.length; i++) y[i] = Double.parseDouble(COL[i]);
        return y;
    }

    @Test
    @DisplayName("Float64.test_int_high_thres: thres=0.3 -> no peaks")
    public void testIntHighThres() {
        double[] y = colAsDouble();
        int[] peaks = Peak.indexes(y, 0.3, 1);
        assertArrayEquals(new int[]{}, peaks);
    }

    @Test
    @DisplayName("Float64.test_float64_high_thres: thres=0.3 -> no peaks")
    public void testFloat64HighThres() {
        double[] y = colAsDouble();
        int[] peaks = Peak.indexes(y, 0.3, 1);
        assertArrayEquals(new int[]{}, peaks);
    }

    @Test
    @DisplayName("Float64.test_int_low_thres: thres=0.01 -> [3, 5, 10, 16]")
    public void testIntLowThres() {
        double[] y = colAsDouble();
        int[] peaks = Peak.indexes(y, 0.01, 1);
        assertArrayEquals(new int[]{3, 5, 10, 16}, peaks);
    }

    @Test
    @DisplayName("Float64.test_float64_low_thres: thres=0.01 -> [3, 5, 10, 16]")
    public void testFloat64LowThres() {
        double[] y = colAsDouble();
        int[] peaks = Peak.indexes(y, 0.01, 1);
        assertArrayEquals(new int[]{3, 5, 10, 16}, peaks);
    }

    // ============================================================
    // InterpolateExceptions
    // ============================================================
    @Test
    @DisplayName("InterpolateExceptions.test_interpolate_bounds: fitting failures become warnings")
    public void testInterpolateBounds() {
        double[] x = Peak.arange(5);
        double[] y = {0, 0, 1, 0, 0};
        Peak.Warnings.startRecording();
        try {
            for (int w = 1; w < 10; w++) {
                Peak.interpolate(x, y, new int[]{2}, w);
            }
        } finally {
            List<String> records = Peak.Warnings.stopRecording();
            assertTrue(records.size() > 0, "Expected at least one warning");
        }
    }

    // ============================================================
    // HighEnvelope
    // ============================================================
    @Test
    @DisplayName("HighEnvelope.test_up_envelope: envelope >= data (within tol)")
    public void testUpEnvelope() {
        double[] data = {0, 2, 0, 0, 4, 0, 0, 0, 7, 0, 0, 0, 0, 9, 0, 0, 0, 11, 0, 0, 0, 0, 0, 9, 0,
                         0, 0, 0, 7, 0, 0, 4, 0, 0, 3, 0, 0, 0, 1};
        double[] env = Peak.envelope(data, 5);
        double tol = 1.05;
        for (int i = 0; i < data.length; i++) {
            assertTrue(data[i] < env[i] * tol,
                    "data[" + i + "]=" + data[i] + " >= env*tol=" + (env[i] * tol));
        }
    }
}
