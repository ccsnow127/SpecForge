// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.util.function.BiFunction;

public class Peak {

    /** Numerical precision constant (eps). */
    public static final double EPS = Math.ulp(1.0);

    // ---------------------------------------------------------------
    // indexes
    // ---------------------------------------------------------------
    /**
     * Peak detection routine.
     * Finds the numeric index of the peaks in {@code y} by taking its first order difference.
     *
     * @param y         1D amplitude data to search for peaks (must be signed).
     * @param thres     Normalized threshold in [0, 1] (or absolute value if {@code thresAbs}).
     * @param minDist   Minimum distance between detected peaks.
     * @param thresAbs  If true, {@code thres} is interpreted as an absolute value.
     * @return Indexes of the detected peaks.
     */
    public static int[] indexes(double[] y, double thres, int minDist, boolean thresAbs) { /* ... */ return null; }

    /** Convenience overload: thres=0.3, minDist=1, thresAbs=false. */
    public static int[] indexes(double[] y) { /* ... */ return null; }

    /** Convenience overload: thresAbs=false. */
    public static int[] indexes(double[] y, double thres, int minDist) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // centroid
    // ---------------------------------------------------------------
    /**
     * Computes the centroid for the specified data.
     *
     * @param x Data on the x axis.
     * @param y Data on the y axis.
     * @return Centroid of the data.
     */
    public static double centroid(double[] x, double[] y) { /* ... */ return 0; }

    // ---------------------------------------------------------------
    // centroid2
    // ---------------------------------------------------------------
    /**
     * Computes the centroid and standard deviation using Simpson's rule.
     *
     * @param y Array whose centroid is computed.
     * @param x The points at which y is sampled (may be null, then arange(y.length)*dx).
     * @param dx Spacing if x is null.
     * @return [centroid, sd] pair.
     */
    public static double[] centroid2(double[] y, double[] x, double dx) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // gaussian
    // ---------------------------------------------------------------
    /**
     * Evaluates a Gaussian.
     *
     * @param x      Point to evaluate.
     * @param ampl   Amplitude.
     * @param center Center.
     * @param dev    Width.
     * @return Gaussian value at {@code x}.
     */
    public static double gaussian(double x, double ampl, double center, double dev) { /* ... */ return 0; }

    /** Vector form of {@link #gaussian(double, double, double, double)}. */
    public static double[] gaussian(double[] x, double ampl, double center, double dev) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // gaussian_fit
    // ---------------------------------------------------------------
    /**
     * Performs a Gaussian fit.
     *
     * @param x          Data on the x axis.
     * @param y          Data on the y axis.
     * @param centerOnly If true, returns just the center; otherwise returns full params.
     * @return Either a single-element array [center] or full [ampl, center, dev].
     */
    public static double[] gaussianFit(double[] x, double[] y, boolean centerOnly) { /* ... */ return null; }

    /** Convenience overload returning just the center. */
    public static double gaussianFit(double[] x, double[] y) { /* ... */ return 0; }

    // ---------------------------------------------------------------
    // interpolate
    // ---------------------------------------------------------------
    /**
     * Refines previously detected peaks using a fitting function around each peak.
     *
     * @param x      Data on the x dimension.
     * @param y      Data on the y dimension.
     * @param ind    Indexes of previously detected peaks (may be null -> uses indexes()).
     * @param width  Number of points before/after each peak to pass to {@code func}.
     * @param func   Function (x[], y[]) -> peak position.
     * @return Refined peak positions in {@code x}.
     */
    public static double[] interpolate(double[] x, double[] y, int[] ind, int width,
                                       BiFunction<double[], double[], Double> func) { /* ... */ return null; }

    /** Convenience overload using gaussianFit center as func. */
    public static double[] interpolate(double[] x, double[] y, int[] ind, int width) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // baseline
    // ---------------------------------------------------------------
    /**
     * Computes the baseline of given data via iterative polynomial fitting.
     *
     * @param y      Data to detect the baseline.
     * @param deg    Polynomial degree (default 3).
     * @param maxIt  Maximum iterations (default 100).
     * @param tol    Convergence tolerance (default 1e-3).
     * @return Baseline amplitude per point in y.
     */
    public static double[] baseline(double[] y, int deg, int maxIt, double tol) { /* ... */ return null; }

    /** Defaults: deg=3, maxIt=100, tol=1e-3. */
    public static double[] baseline(double[] y) { /* ... */ return null; }

    /** Defaults: maxIt=100, tol=1e-3. */
    public static double[] baseline(double[] y, int deg) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // envelope
    // ---------------------------------------------------------------
    /**
     * Computes the upper envelope using {@link #baseline(double[], int, int, double)}.
     */
    public static double[] envelope(double[] y, int deg, int maxIt, double tol) { /* ... */ return null; }

    /** Convenience overload (defaults). */
    public static double[] envelope(double[] y, int deg) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // scale
    // ---------------------------------------------------------------
    /** Result of {@link #scale}. */
    public static class ScaleResult {
        public final double[] scaled;
        public final double[] oldRange; // length 2: [min, max]
        public ScaleResult(double[] scaled, double[] oldRange) {
            this.scaled = scaled;
            this.oldRange = oldRange;
        }
    }

    /**
     * Scales the array to {@code newRange}, returning the scaled array and previous range.
     */
    public static ScaleResult scale(double[] x, double[] newRange, double eps) { /* ... */ return null; }

    /** Convenience: eps=1e-9. */
    public static ScaleResult scale(double[] x, double[] newRange) { /* ... */ return null; }
}
