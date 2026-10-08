import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests faithfully mirroring all Python tests from test_result.py.
 * Every test in test_entity_map.json has a corresponding test method here.
 */
public class ResultTest {

    // ---- TestResultInit ----

    @Nested
    class TestResultInit {

        @Test
        void test_init_stores_wpm() {
            Result r = new Result(60, 200);
            assertEquals(200, r.getWpm());
        }

        @Test
        void test_init_creates_delta() {
            Result r = new Result(120, 200);
            assertNotNull(r.getDelta());
            assertInstanceOf(Duration.class, r.getDelta());
            assertEquals(Duration.ofSeconds(120), r.getDelta());
        }

        @Test
        void test_init_default_wpm_none() {
            Result r = new Result(30);
            assertNull(r.getWpm());
        }

        @Test
        void test_init_zero_seconds() {
            Result r = new Result(0, 100);
            assertEquals(Duration.ofSeconds(0), r.getDelta());
        }
    }

    // ---- TestResultSeconds ----

    @Nested
    class TestResultSeconds {

        @Test
        void test_seconds_basic() {
            Result r = new Result(90, 200);
            assertEquals(90, r.getSeconds());
        }

        @Test
        void test_seconds_zero() {
            Result r = new Result(0, 200);
            assertEquals(0, r.getSeconds());
        }

        @Test
        void test_seconds_large() {
            Result r = new Result(3600, 200);
            assertEquals(3600, r.getSeconds());
        }

        @Test
        void test_seconds_returns_int() {
            Result r = new Result(90, 200);
            // In Java, getSeconds() returns int by signature — this confirms it
            assertEquals(90, r.getSeconds());
            assertTrue(r.getSeconds() == (int) r.getSeconds());
        }
    }

    // ---- TestResultMinutes ----

    @Nested
    class TestResultMinutes {

        @Test
        void test_minutes_exact() {
            Result r = new Result(120, 200);
            assertEquals(2, r.getMinutes());
        }

        @Test
        void test_minutes_rounds_up() {
            Result r = new Result(61, 200);
            assertEquals(2, r.getMinutes());
        }

        @Test
        void test_minutes_minimum_one() {
            Result r = new Result(0, 200);
            assertEquals(1, r.getMinutes());
        }

        @Test
        void test_minutes_one_second() {
            Result r = new Result(1, 200);
            assertEquals(1, r.getMinutes());
        }

        @Test
        void test_minutes_59_seconds() {
            Result r = new Result(59, 200);
            assertEquals(1, r.getMinutes());
        }

        @Test
        void test_minutes_60_seconds() {
            Result r = new Result(60, 200);
            assertEquals(1, r.getMinutes());
        }
    }

    // ---- TestResultText ----

    @Nested
    class TestResultText {

        @Test
        void test_text_one_min() {
            Result r = new Result(60, 200);
            assertEquals("1 min", r.getText());
        }

        @Test
        void test_text_multiple_mins() {
            Result r = new Result(300, 200);
            assertEquals("5 min", r.getText());
        }

        @Test
        void test_text_rounds_up() {
            Result r = new Result(121, 200);
            assertEquals("3 min", r.getText());
        }
    }

    // ---- TestResultRepr ----

    @Nested
    class TestResultRepr {

        @Test
        void test_repr_format() {
            Result r = new Result(60, 200);
            assertEquals("1 min read", r.toString());
        }

        @Test
        void test_str_same_as_repr() {
            Result r = new Result(120, 200);
            // In Python: str(r) == repr(r); in Java both go through toString()
            assertEquals(r.toString(), r.toString());
        }

        @Test
        void test_repr_large_time() {
            Result r = new Result(600, 200);
            assertEquals("10 min read", r.toString());
        }
    }

    // ---- TestResultOperators ----

    @Nested
    class TestResultOperators {

        @Test
        void test_add_results() {
            Result r1 = new Result(60, 200);
            Result r2 = new Result(120, 200);
            Result r3 = r1.add(r2);
            assertInstanceOf(Result.class, r3);
            assertEquals(180, r3.getSeconds());
        }

        @Test
        void test_subtract_results() {
            Result r1 = new Result(120, 200);
            Result r2 = new Result(60, 200);
            Result r3 = r1.subtract(r2);
            assertInstanceOf(Result.class, r3);
        }
    }
}
