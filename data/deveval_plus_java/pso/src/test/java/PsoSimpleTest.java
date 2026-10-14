import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PsoSimple — faithfully mirrors all Python tests in test_pso_simple.py.
 */
class PsoSimpleTest {

    /** Sphere function: sum of squares, minimum at origin. */
    static double sphere(List<Double> x) {
        double sum = 0;
        for (double xi : x) {
            sum += xi * xi;
        }
        return sum;
    }

    @BeforeEach
    void setUp() {
        // Reset the shared random to a fixed seed for reproducibility
        PsoSimple.random = new Random(42);
    }

    // ========== Particle.__init__ tests ==========

    @Test
    void test_Particle___init___position() {
        PsoSimple.numDimensions = 3;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(1.0, 2.0, 3.0));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), p.positionI);
    }

    @Test
    void test_Particle___init___velocity_length() {
        PsoSimple.numDimensions = 3;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(1.0, 2.0, 3.0));
        assertEquals(3, p.velocityI.size());
    }

    @Test
    void test_Particle___init___defaults() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(0.0, 0.0));
        assertTrue(p.posBestI.isEmpty());
        assertEquals(-1, p.errBestI);
        assertEquals(-1, p.errI);
    }

    @Test
    void test_Particle___init___velocity_range() {
        PsoSimple.numDimensions = 100;
        List<Double> zeros = new ArrayList<>();
        for (int i = 0; i < 100; i++) zeros.add(0.0);
        PsoSimple.Particle p = new PsoSimple.Particle(zeros);
        for (double v : p.velocityI) {
            assertTrue(v >= -1 && v <= 1, "Velocity " + v + " not in [-1, 1]");
        }
    }

    // ========== Particle.evaluate tests ==========

    @Test
    void test_Particle_evaluate_updates_error() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(3.0, 4.0));
        p.evaluate(PsoSimpleTest::sphere);
        assertEquals(25.0, p.errI);
    }

    @Test
    void test_Particle_evaluate_updates_best_on_first_call() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(3.0, 4.0));
        p.evaluate(PsoSimpleTest::sphere);
        assertEquals(25.0, p.errBestI);
        assertEquals(Arrays.asList(3.0, 4.0), p.posBestI);
    }

    @Test
    void test_Particle_evaluate_updates_best_on_improvement() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(3.0, 4.0));
        p.evaluate(PsoSimpleTest::sphere);
        // Move to a better position manually
        p.positionI = new ArrayList<>(Arrays.asList(1.0, 1.0));
        p.evaluate(PsoSimpleTest::sphere);
        assertEquals(2.0, p.errBestI);
        assertEquals(Arrays.asList(1.0, 1.0), p.posBestI);
    }

    @Test
    void test_Particle_evaluate_no_update_on_worse() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(1.0, 1.0));
        p.evaluate(PsoSimpleTest::sphere);
        assertEquals(2.0, p.errBestI);
        p.positionI = new ArrayList<>(Arrays.asList(5.0, 5.0));
        p.evaluate(PsoSimpleTest::sphere);
        assertEquals(2.0, p.errBestI);
        assertEquals(Arrays.asList(1.0, 1.0), p.posBestI);
    }

    // ========== Particle.update_velocity tests ==========

    @Test
    void test_Particle_update_velocity_changes_velocity() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(5.0, 5.0));
        p.posBestI = new ArrayList<>(Arrays.asList(5.0, 5.0));
        p.errBestI = 50.0;
        List<Double> oldVelocity = new ArrayList<>(p.velocityI);
        p.updateVelocity(Arrays.asList(0.0, 0.0));
        assertNotEquals(oldVelocity, p.velocityI);
    }

    @Test
    void test_Particle_update_velocity_length_preserved() {
        PsoSimple.numDimensions = 3;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(1.0, 2.0, 3.0));
        p.posBestI = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0));
        p.errBestI = 14.0;
        p.updateVelocity(Arrays.asList(0.0, 0.0, 0.0));
        assertEquals(3, p.velocityI.size());
    }

    @Test
    void test_Particle_update_velocity_at_optimum() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(0.0, 0.0));
        p.posBestI = new ArrayList<>(Arrays.asList(0.0, 0.0));
        p.errBestI = 0.0;
        // When particle is at best position and global best, cognitive and social terms are 0
        p.updateVelocity(Arrays.asList(0.0, 0.0));
        // Only inertia component remains: w * old_velocity
        for (int i = 0; i < 2; i++) {
            assertTrue(Math.abs(p.velocityI.get(i)) <= 1.0,
                    "Velocity component " + p.velocityI.get(i) + " exceeds 1.0");
        }
    }

    // ========== Particle.update_position tests ==========

    @Test
    void test_Particle_update_position_basic() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(0.0, 0.0));
        p.velocityI = new ArrayList<>(Arrays.asList(1.0, -1.0));
        double[][] bounds = {{-10, 10}, {-10, 10}};
        p.updatePosition(bounds);
        assertEquals(Arrays.asList(1.0, -1.0), p.positionI);
    }

    @Test
    void test_Particle_update_position_clamp_upper() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(9.0, 0.0));
        p.velocityI = new ArrayList<>(Arrays.asList(5.0, 0.0));
        double[][] bounds = {{-10, 10}, {-10, 10}};
        p.updatePosition(bounds);
        assertEquals(10.0, p.positionI.get(0));
    }

    @Test
    void test_Particle_update_position_clamp_lower() {
        PsoSimple.numDimensions = 2;
        PsoSimple.Particle p = new PsoSimple.Particle(Arrays.asList(-9.0, 0.0));
        p.velocityI = new ArrayList<>(Arrays.asList(-5.0, 0.0));
        double[][] bounds = {{-10, 10}, {-10, 10}};
        p.updatePosition(bounds);
        assertEquals(-10.0, p.positionI.get(0));
    }

    // ========== minimize tests ==========

    @Test
    void test_minimize_returns_tuple() {
        PsoSimple.random = new Random(42);
        PsoSimple.Result result = PsoSimple.minimize(
                PsoSimpleTest::sphere,
                Arrays.asList(5.0, 5.0),
                new double[][]{{-10, 10}, {-10, 10}},
                5, 10);
        assertNotNull(result);
        assertNotNull(result.position);
        assertEquals(2, result.position.size());
    }

    @Test
    void test_minimize_converges() {
        PsoSimple.random = new Random(42);
        PsoSimple.Result result = PsoSimple.minimize(
                PsoSimpleTest::sphere,
                Arrays.asList(5.0, 5.0),
                new double[][]{{-10, 10}, {-10, 10}},
                20, 100);
        assertTrue(result.error < 1.0, "Error " + result.error + " should be < 1.0");
    }

    @Test
    void test_minimize_respects_bounds() {
        PsoSimple.random = new Random(42);
        PsoSimple.Result result = PsoSimple.minimize(
                PsoSimpleTest::sphere,
                Arrays.asList(5.0),
                new double[][]{{-2, 2}},
                10, 50);
        assertTrue(result.position.get(0) >= -2 && result.position.get(0) <= 2,
                "Position " + result.position.get(0) + " not in [-2, 2]");
    }

    @Test
    void test_minimize_single_dimension() {
        PsoSimple.random = new Random(42);
        PsoSimple.Result result = PsoSimple.minimize(
                PsoSimpleTest::sphere,
                Arrays.asList(5.0),
                new double[][]{{-10, 10}},
                10, 50);
        assertEquals(1, result.position.size());
        assertTrue(result.error < 5.0, "Error " + result.error + " should be < 5.0");
    }
}
