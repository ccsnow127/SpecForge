// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class PsoSimple {

    static Random random;
    static int numDimensions;

    public static class Particle {
        public List<Double> positionI;
        public List<Double> velocityI;
        public List<Double> posBestI;
        public double errBestI;
        public double errI;

        public Particle(List<Double> x0) { /* ... */ }
        public void evaluate(Function<List<Double>, Double> costFunc) { /* ... */ }
        public void updateVelocity(List<Double> posBestG) { /* ... */ }
        public void updatePosition(double[][] bounds) { /* ... */ }
    }

    public static class Result {
        public double error;
        public List<Double> position;

        public Result(double error, List<Double> position) { /* ... */ }
    }

    public static Result minimize(Function<List<Double>, Double> costFunc,
                                  List<Double> x0,
                                  double[][] bounds,
                                  int numParticles,
                                  int maxiter,
                                  boolean verbose) { /* ... */ }

    public static Result minimize(Function<List<Double>, Double> costFunc,
                                  List<Double> x0,
                                  double[][] bounds,
                                  int numParticles,
                                  int maxiter) { /* ... */ }
}
