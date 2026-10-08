import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for Croniter -- selected port of test_croniter.py.
 *
 * <p>Mirrors a representative subset of the Python test suite covering
 * the next/prev iteration, alpha conversion, last-day-of-month, error
 * handling, expansion semantics, and helper utilities.
 */
public class CroniterTest {

    // ---- helpers ----
    private static LocalDateTime dt(int y, int M, int d) { return LocalDateTime.of(y, M, d, 0, 0); }
    private static LocalDateTime dt(int y, int M, int d, int h, int m) { return LocalDateTime.of(y, M, d, h, m); }
    private static LocalDateTime dt(int y, int M, int d, int h, int m, int s) { return LocalDateTime.of(y, M, d, h, m, s); }

    // ============================================================
    // test_second_sec
    // ============================================================
    @Test
    public void testSecondSec() {
        LocalDateTime base = dt(2012, 4, 6, 13, 26, 10);
        Croniter itr = new Croniter("* * * * * 15,25", base);
        LocalDateTime n = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(15, n.getSecond());
        n = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(25, n.getSecond());
        n = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(15, n.getSecond());
        assertEquals(27, n.getMinute());
    }

    // ============================================================
    // test_second
    // ============================================================
    @Test
    public void testSecond() {
        LocalDateTime base = dt(2012, 4, 6, 13, 26, 10);
        Croniter itr = new Croniter("*/1 * * * * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(base.getYear(), n1.getYear());
        assertEquals(base.getMonthValue(), n1.getMonthValue());
        assertEquals(base.getDayOfMonth(), n1.getDayOfMonth());
        assertEquals(base.getHour(), n1.getHour());
        assertEquals(base.getMinute(), n1.getMinute());
        assertEquals(base.getSecond() + 1, n1.getSecond());
    }

    // ============================================================
    // test_second_repeat
    // ============================================================
    @Test
    public void testSecondRepeat() {
        LocalDateTime base = dt(2012, 4, 6, 13, 26, 36);
        Croniter itr = new Croniter("* * * * * */15", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        LocalDateTime n3 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(45, n1.getSecond());
        assertEquals(base.getMinute() + 1, n2.getMinute());
        assertEquals(0, n2.getSecond());
        assertEquals(base.getMinute() + 1, n3.getMinute());
        assertEquals(15, n3.getSecond());
    }

    // ============================================================
    // test_minute
    // ============================================================
    @Test
    public void testMinute() {
        LocalDateTime base = dt(2010, 1, 23, 12, 18);
        Croniter itr = new Croniter("*/1 * * * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(base.getMinute(), n1.getMinute() - 1);
        for (int i = 0; i < 39; i++) itr.getNext();
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(59, n2.getMinute());
        LocalDateTime n3 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(0, n3.getMinute());
        assertEquals(13, n3.getHour());
    }

    // ============================================================
    // test_hour
    // ============================================================
    @Test
    public void testHour() {
        LocalDateTime base = dt(2010, 1, 24, 12, 2);
        Croniter itr = new Croniter("0 */3 * * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(15, n1.getHour());
        assertEquals(0, n1.getMinute());
        for (int i = 0; i < 2; i++) itr.getNext();
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(0, n2.getHour());
        assertEquals(25, n2.getDayOfMonth());
    }

    // ============================================================
    // test_day
    // ============================================================
    @Test
    public void testDay() {
        LocalDateTime base = dt(2010, 2, 24, 12, 9);
        Croniter itr = new Croniter("0 0 */3 * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(25, n1.getDayOfMonth());
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(28, n2.getDayOfMonth());
        LocalDateTime n3 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(1, n3.getDayOfMonth());
        assertEquals(3, n3.getMonthValue());

        // leap year
        base = dt(1996, 2, 27);
        itr = new Croniter("0 0 * * *", base);
        n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(28, n1.getDayOfMonth());
        n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(29, n2.getDayOfMonth());
        assertEquals(2, n2.getMonthValue());
    }

    // ============================================================
    // test_weekday
    // ============================================================
    @Test
    public void testWeekday() {
        LocalDateTime base = dt(2010, 2, 25);
        Croniter itr = new Croniter("0 0 * * sat", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(DayOfWeek.SATURDAY, n1.getDayOfWeek());
        assertEquals(27, n1.getDayOfMonth());
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(DayOfWeek.SATURDAY, n2.getDayOfWeek());
        assertEquals(6, n2.getDayOfMonth());
        assertEquals(3, n2.getMonthValue());
    }

    // ============================================================
    // test_nth_weekday
    // ============================================================
    @Test
    public void testNthWeekday() {
        LocalDateTime base = dt(2010, 2, 25);
        Croniter itr = new Croniter("0 0 * * sat#1", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(DayOfWeek.SATURDAY, n1.getDayOfWeek());
        assertEquals(6, n1.getDayOfMonth());
        assertEquals(3, n1.getMonthValue());
    }

    // ============================================================
    // test_weekday_day_and
    // ============================================================
    @Test
    public void testWeekdayDayAnd() {
        LocalDateTime base = dt(2010, 1, 25);
        Croniter itr = new Croniter("0 0 1 * mon", base, false);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(2, n1.getMonthValue());
        assertEquals(1, n1.getDayOfMonth());
    }

    // ============================================================
    // test_month
    // ============================================================
    @Test
    public void testMonth() {
        LocalDateTime base = dt(2010, 1, 25);
        Croniter itr = new Croniter("0 0 1 * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(2, n1.getMonthValue());
        assertEquals(1, n1.getDayOfMonth());
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(3, n2.getMonthValue());
        for (int i = 0; i < 8; i++) itr.getNext();
        LocalDateTime n3 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(12, n3.getMonthValue());
        LocalDateTime n4 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(1, n4.getMonthValue());
        assertEquals(2011, n4.getYear());
    }

    // ============================================================
    // test_last_day_of_month
    // ============================================================
    @Test
    public void testLastDayOfMonth() {
        LocalDateTime base = dt(2015, 9, 4);
        Croniter itr = new Croniter("0 0 l * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(9, n1.getMonthValue());
        assertEquals(30, n1.getDayOfMonth());
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(10, n2.getMonthValue());
        assertEquals(31, n2.getDayOfMonth());
    }

    // ============================================================
    // test_range_with_uppercase_last_day_of_month
    // ============================================================
    @Test
    public void testRangeWithUppercaseLastDayOfMonth() {
        LocalDateTime base = dt(2015, 9, 4);
        Croniter itr = new Croniter("0 0 29-L * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(9, n1.getMonthValue());
        assertEquals(29, n1.getDayOfMonth());
    }

    // ============================================================
    // test_prev_last_day_of_month
    // ============================================================
    @Test
    public void testPrevLastDayOfMonth() {
        LocalDateTime base = dt(2009, 12, 31, 20, 0);
        Croniter itr = new Croniter("0 0 l * *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(12, n1.getMonthValue());
        assertEquals(31, n1.getDayOfMonth());

        base = dt(2010, 1, 5);
        itr = new Croniter("0 0 l * *", base);
        n1 = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(12, n1.getMonthValue());
        assertEquals(31, n1.getDayOfMonth());
    }

    // ============================================================
    // test_error
    // ============================================================
    @Test
    public void testError() {
        Croniter itr = new Croniter("* * * * *");
        assertThrows(RuntimeException.class, () -> itr.getNext(String.class));
        assertThrows(RuntimeException.class, () -> new Croniter("* * * *"));
        assertThrows(RuntimeException.class, () -> new Croniter("-90 * * * *"));
        assertThrows(RuntimeException.class, () -> new Croniter("a * * * *"));
        assertThrows(RuntimeException.class, () -> new Croniter("* * * janu-jun *"));
        assertThrows(RuntimeException.class, () -> new Croniter("0-10/error * * * *"));
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("0-1& * * * *"));
        assertThrows(RuntimeException.class, () -> new Croniter("* * 5-100 * *"));
    }

    // ============================================================
    // test_sunday_to_thursday_with_alpha_conversion
    // ============================================================
    @Test
    public void testSundayToThursdayWithAlphaConversion() {
        LocalDateTime base = dt(2010, 8, 25, 15, 56);
        Croniter itr = new Croniter("30 22 * * sun-thu", base);
        LocalDateTime next = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(base.getYear(), next.getYear());
        assertEquals(base.getMonthValue(), next.getMonthValue());
        assertEquals(base.getDayOfMonth(), next.getDayOfMonth());
        assertEquals(22, next.getHour());
        assertEquals(30, next.getMinute());
    }

    // ============================================================
    // test_optimize_cron_expressions
    // ============================================================
    @SuppressWarnings("unchecked")
    @Test
    public void testOptimizeCronExpressions() {
        List<Object> wildcard = Arrays.asList((Object) "*");
        int M = 0, H = 1, D = 2, MON = 3, DOW = 4, S = 5;
        assertEquals(wildcard, new Croniter("0-59 0 1 1 0").getExpanded().get(M));
        assertEquals(wildcard, new Croniter("0 0-23 1 1 0").getExpanded().get(H));
        assertEquals(Arrays.asList((Object) 0), new Croniter("0 0 1-31 1 0").getExpanded().get(DOW));
        assertEquals(wildcard, new Croniter("0 0 1-31 1 *").getExpanded().get(D));
        assertEquals(wildcard, new Croniter("0 0 1 1-12 0").getExpanded().get(MON));
        assertEquals(Arrays.asList((Object) 0, 1, 2, 3, 4, 5, 6),
                new Croniter("0 0 1 1 0-6").getExpanded().get(DOW));
        assertEquals(wildcard, new Croniter("0 0 * 1 0-6").getExpanded().get(DOW));
    }

    // ============================================================
    // test_block_dup_ranges
    // ============================================================
    @SuppressWarnings("unchecked")
    @Test
    public void testBlockDupRanges() {
        int H = 1, DOW = 4, S = 5;
        assertEquals(Arrays.asList((Object) 1, 2, 3, 4, 5, 6),
                new Croniter("* 5,5,1-6 * * *").getExpanded().get(H));
        assertEquals(Arrays.asList((Object) 2, 3, 4, 5),
                new Croniter("* * * * 2-3,4-5,3,3,3").getExpanded().get(DOW));
        assertEquals(Arrays.asList((Object) 1, 2, 3, 4, 5),
                new Croniter("* 4,1-4,5,4 * * *").getExpanded().get(H));
    }

    // ============================================================
    // test_prev_minute
    // ============================================================
    @Test
    public void testPrevMinute() {
        LocalDateTime base = dt(2010, 8, 25, 15, 56);
        Croniter itr = new Croniter("*/1 * * * *", base);
        LocalDateTime prev = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(base.getMinute(), prev.getMinute() + 1);

        base = dt(2010, 8, 25, 15, 0);
        itr = new Croniter("*/1 * * * *", base);
        prev = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(base.getHour(), prev.getHour() + 1);
        assertEquals(59, prev.getMinute());
    }

    // ============================================================
    // test_prev_day_of_month_with_crossing
    // ============================================================
    @Test
    public void testPrevDayOfMonthWithCrossing() {
        LocalDateTime base = dt(2012, 3, 15, 0, 0);
        Croniter itr = new Croniter("0 0 22 * *", base);
        LocalDateTime prev = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(2012, prev.getYear());
        assertEquals(2, prev.getMonthValue());
        assertEquals(22, prev.getDayOfMonth());
    }

    // ============================================================
    // test_prev_weekday
    // ============================================================
    @Test
    public void testPrevWeekday() {
        LocalDateTime base = dt(2010, 8, 25, 15, 56);
        Croniter itr = new Croniter("0 0 * * sat,sun", base);
        LocalDateTime p1 = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(22, p1.getDayOfMonth());
        LocalDateTime p2 = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(21, p2.getDayOfMonth());
        LocalDateTime p3 = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(15, p3.getDayOfMonth());
    }

    // ============================================================
    // test_iso_weekday
    // ============================================================
    @Test
    public void testIsoWeekday() {
        LocalDateTime base = dt(2010, 2, 25);
        Croniter itr = new Croniter("0 0 * * 6", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(DayOfWeek.SATURDAY, n1.getDayOfWeek());
        assertEquals(27, n1.getDayOfMonth());
    }

    // ============================================================
    // test_bug2 (every hour in March)
    // ============================================================
    @Test
    public void testBug2() {
        LocalDateTime base = dt(2012, 1, 1, 0, 0);
        Croniter itr = new Croniter("0 * * 3 *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(2012, n1.getYear());
        assertEquals(3, n1.getMonthValue());
        assertEquals(1, n1.getDayOfMonth());
        assertEquals(0, n1.getHour());
        LocalDateTime n2 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(3, n2.getMonthValue());
        assertEquals(1, n2.getHour());
    }

    // ============================================================
    // test_bug34 -- Feb 31 -> never
    // ============================================================
    @Test
    public void testBug34() {
        LocalDateTime base = dt(2012, 2, 24, 0, 0, 0);
        Croniter itr = new Croniter("* * 31 2 *", base);
        Croniter.CroniterBadDateError ex = assertThrows(Croniter.CroniterBadDateError.class,
                () -> itr.getNext(LocalDateTime.class));
        assertEquals("failed to find next date", ex.getMessage());
    }

    // ============================================================
    // test_bug3 -- 16th and 30th
    // ============================================================
    @Test
    public void testBug3() {
        LocalDateTime base = LocalDateTime.of(2013, 3, 1, 12, 17, 34, 257_877_000);
        Croniter c = new Croniter("00 03 16,30 * *", base);
        LocalDateTime n1 = (LocalDateTime) c.getNext(LocalDateTime.class);
        assertEquals(3, n1.getMonthValue());
        assertEquals(16, n1.getDayOfMonth());
        LocalDateTime n2 = (LocalDateTime) c.getNext(LocalDateTime.class);
        assertEquals(30, n2.getDayOfMonth());
        LocalDateTime n3 = (LocalDateTime) c.getNext(LocalDateTime.class);
        assertEquals(4, n3.getMonthValue());
        assertEquals(16, n3.getDayOfMonth());
    }

    // ============================================================
    // test_multiple_months -- "0 0 1 3,6,9,12 *"
    // ============================================================
    @Test
    public void testMultipleMonths() {
        LocalDateTime base = dt(2016, 3, 1, 0, 0, 0);
        Croniter itr = new Croniter("0 0 1 3,6,9,12 *", base);
        LocalDateTime n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(0, n1.getHour());
        assertEquals(6, n1.getMonthValue());
        assertEquals(1, n1.getDayOfMonth());

        base = dt(2016, 12, 3, 10, 0, 0);
        itr = new Croniter("0 0 1 3,6,9,12 *", base);
        n1 = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(3, n1.getMonthValue());
        assertEquals(2017, n1.getYear());

        // Get_prev across boundary
        base = dt(2016, 3, 1, 0, 0, 0);
        itr = new Croniter("0 0 1 3,6,9,12 *", base);
        LocalDateTime p1 = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(12, p1.getMonthValue());
        assertEquals(2015, p1.getYear());
    }

    // ============================================================
    // test_range_generator -- "1-9/2 0 1 * *"
    // ============================================================
    @Test
    public void testRangeGenerator() {
        LocalDateTime base = dt(2013, 3, 4, 0, 0);
        Croniter itr = new Croniter("1-9/2 0 1 * *", base);
        int[] expectedMin = {1, 3, 5, 7, 9};
        for (int i = 0; i < 5; i++) {
            LocalDateTime n = (LocalDateTime) itr.getNext(LocalDateTime.class);
            assertEquals(expectedMin[i], n.getMinute());
        }
    }

    // ============================================================
    // test_previous_hour
    // ============================================================
    @Test
    public void testPreviousHour() {
        LocalDateTime base = dt(2012, 6, 23, 17, 41);
        Croniter itr = new Croniter("* 10 * * *", base);
        LocalDateTime prev = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(10, prev.getHour());
        assertEquals(59, prev.getMinute());
    }

    // ============================================================
    // test_previous_day
    // ============================================================
    @Test
    public void testPreviousDay() {
        LocalDateTime base = dt(2012, 6, 27, 0, 15);
        Croniter itr = new Croniter("* * 26 * *", base);
        LocalDateTime prev = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(26, prev.getDayOfMonth());
        assertEquals(23, prev.getHour());
        assertEquals(59, prev.getMinute());
    }

    // ============================================================
    // test_previous_dow
    // ============================================================
    @Test
    public void testPreviousDow() {
        LocalDateTime base = dt(2012, 5, 13, 18, 48);
        Croniter itr = new Croniter("* * * * sat", base);
        LocalDateTime prev = (LocalDateTime) itr.getPrev(LocalDateTime.class);
        assertEquals(12, prev.getDayOfMonth());
        assertEquals(23, prev.getHour());
        assertEquals(59, prev.getMinute());
    }

    // ============================================================
    // test_get_current
    // ============================================================
    @Test
    public void testGetCurrent() {
        LocalDateTime base = dt(2012, 9, 25, 11, 24);
        Croniter itr = new Croniter("* * * * *", base);
        LocalDateTime res = (LocalDateTime) itr.getCurrent(LocalDateTime.class);
        assertEquals(base.getYear(), res.getYear());
        assertEquals(base.getMonthValue(), res.getMonthValue());
        assertEquals(base.getDayOfMonth(), res.getDayOfMonth());
        assertEquals(base.getHour(), res.getHour());
        assertEquals(base.getMinute(), res.getMinute());
    }

    // ============================================================
    // test_first_of_march -- issue #1
    // ============================================================
    @Test
    public void testFirstOfMarch() {
        Croniter it = new Croniter("0 0 */10 * *", dt(2025, 2, 22));
        assertEquals("2025-03-01T00:00",
                ((LocalDateTime) it.getNext(LocalDateTime.class)).toString());
    }

    // ============================================================
    // test_init_no_start_time
    // ============================================================
    @Test
    public void testInitNoStartTime() throws InterruptedException {
        Croniter itr = new Croniter("* * * * *");
        Thread.sleep(15);
        Croniter itr2 = new Croniter("* * * * *");
        assertTrue(itr2.getCurrent() > itr.getCurrent());
    }

    // ============================================================
    // test_error_alpha_cron
    // ============================================================
    @Test
    public void testErrorAlphaCron() {
        assertThrows(Croniter.CroniterNotAlphaError.class,
                () -> Croniter.expand("* * * janu-jun *"));
    }

    // ============================================================
    // test_error_bad_cron
    // ============================================================
    @Test
    public void testErrorBadCron() {
        assertThrows(Croniter.CroniterBadCronError.class,
                () -> Croniter.expand("* * * *"));
        assertThrows(Croniter.CroniterBadCronError.class,
                () -> Croniter.expand("* * * * * * * *"));
    }

    // ============================================================
    // test_is_valid
    // ============================================================
    @Test
    public void testIsValid() {
        assertTrue(Croniter.isValid("0 * * * *"));
        assertFalse(Croniter.isValid("0 * *"));
        assertFalse(Croniter.isValid("* * * janu-jun *"));
        assertTrue(Croniter.isValid("H 0 * * *", "abc"));
    }

    // ============================================================
    // test_is_valid_strict
    // ============================================================
    @Test
    public void testIsValidStrict() {
        assertTrue(Croniter.isValid("0 0 31 2 *"));
        assertFalse(Croniter.isValid("0 0 31 2 *", null, false, true, null));
        assertFalse(Croniter.isValid("0 0 30 2 *", null, false, true, null));
        assertFalse(Croniter.isValid("0 0 31 4 *", null, false, true, null));
        assertTrue(Croniter.isValid("0 0 29 2 *", null, false, true, null));
        assertTrue(Croniter.isValid("0 0 31 1 *", null, false, true, null));
        assertTrue(Croniter.isValid("0 0 31 * *", null, false, true, null));
        assertTrue(Croniter.isValid("0 0 * 2 *", null, false, true, null));
        assertTrue(Croniter.isValid("0 0 l * *", null, false, true, null));
        assertTrue(Croniter.isValid("0 * * * *", null, false, true, null));
        assertTrue(Croniter.isValid("*/5 * * * *", null, false, true, null));
        assertThrows(Croniter.CroniterBadCronError.class,
                () -> Croniter.expand("0 0 31 2 *", null, false, null, true, null));
    }

    // ============================================================
    // test_is_valid_strict_year_parameter
    // ============================================================
    @Test
    public void testIsValidStrictYearParameter() {
        assertTrue(Croniter.isValid("0 0 29 2 *", null, false, true, 2024));
        assertTrue(Croniter.isValid("0 0 29 2 *", null, false, true, 2000));
        assertFalse(Croniter.isValid("0 0 29 2 *", null, false, true, 2023));
        assertFalse(Croniter.isValid("0 0 29 2 *", null, false, true, 1900));
        assertFalse(Croniter.isValid("0 0 31 2 *", null, false, true, 2024));
        assertTrue(Croniter.isValid("0 0 29 2 *", null, false, true, Arrays.asList(2023, 2024)));
        assertFalse(Croniter.isValid("0 0 29 2 *", null, false, true, Arrays.asList(2023, 2025)));
    }

    // ============================================================
    // test_nearest_weekday_basic
    // ============================================================
    @Test
    public void testNearestWeekdayBasic() {
        LocalDateTime base = dt(2024, 1, 1);
        Croniter itr = new Croniter("0 9 15W * *", base);
        assertEquals(dt(2024, 1, 15, 9, 0), itr.getNext(LocalDateTime.class));
        assertEquals(dt(2024, 2, 15, 9, 0), itr.getNext(LocalDateTime.class));
        assertEquals(dt(2024, 3, 15, 9, 0), itr.getNext(LocalDateTime.class));
    }

    // ============================================================
    // test_nearest_weekday_saturday
    // ============================================================
    @Test
    public void testNearestWeekdaySaturday() {
        LocalDateTime base = dt(2024, 6, 1);
        Croniter itr = new Croniter("0 9 15W * *", base);
        assertEquals(dt(2024, 6, 14, 9, 0), itr.getNext(LocalDateTime.class));
    }

    // ============================================================
    // test_nearest_weekday_sunday
    // ============================================================
    @Test
    public void testNearestWeekdaySunday() {
        LocalDateTime base = dt(2024, 9, 1);
        Croniter itr = new Croniter("0 9 15W * *", base);
        assertEquals(dt(2024, 9, 16, 9, 0), itr.getNext(LocalDateTime.class));
    }

    // ============================================================
    // test_nearest_weekday_first_saturday
    // ============================================================
    @Test
    public void testNearestWeekdayFirstSaturday() {
        LocalDateTime base = dt(2024, 5, 31);
        Croniter itr = new Croniter("0 9 1W * *", base);
        assertEquals(dt(2024, 6, 3, 9, 0), itr.getNext(LocalDateTime.class));
    }

    // ============================================================
    // test_nearest_weekday_is_valid
    // ============================================================
    @Test
    public void testNearestWeekdayIsValid() {
        assertTrue(Croniter.isValid("0 9 15W * *"));
        assertTrue(Croniter.isValid("0 9 W15 * *"));
        assertTrue(Croniter.isValid("0 9 1W * *"));
        assertTrue(Croniter.isValid("0 9 31W * *"));
        assertFalse(Croniter.isValid("0 9 15W,16 * *"));
        assertFalse(Croniter.isValid("0 9 1,15W * *"));
        assertFalse(Croniter.isValid("0 9 0W * *"));
        assertFalse(Croniter.isValid("0 9 32W * *"));
    }

    // ============================================================
    // test_invalid_zerorepeat
    // ============================================================
    @Test
    public void testInvalidZerorepeat() {
        assertFalse(Croniter.isValid("*/0 * * * *"));
    }

    // ============================================================
    // test_match
    // ============================================================
    @Test
    public void testMatch() {
        assertTrue(Croniter.match("0 0 * * *", dt(2019, 1, 14, 0, 0, 0)));
        assertFalse(Croniter.match("0 0 * * *", dt(2019, 1, 14, 0, 1, 0)));
        assertTrue(Croniter.match("31 * * * *", dt(2019, 1, 14, 1, 31, 0)));
        assertTrue(Croniter.match("0 0 10 * wed", dt(2020, 6, 10, 0, 0, 0), true));
        assertTrue(Croniter.match("0 0 10 * fri", dt(2020, 6, 10, 0, 0, 0), true));
        assertTrue(Croniter.match("0 0 10 * fri", dt(2020, 6, 12, 0, 0, 0), true));
        assertFalse(Croniter.match("0 0 10 * fri", dt(2020, 6, 10, 0, 0, 0), false));
    }

    // ============================================================
    // test_invalid_question_mark
    // ============================================================
    @Test
    public void testInvalidQuestionMark() {
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("? * * * *"));
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("* ? * * *"));
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("* * ?,* * *"));
    }

    // ============================================================
    // test_question_mark
    // ============================================================
    @Test
    public void testQuestionMark() {
        LocalDateTime base = dt(2010, 8, 25, 15, 56);
        Croniter itr = new Croniter("0 0 1 * ?", base);
        LocalDateTime n = (LocalDateTime) itr.getNext(LocalDateTime.class);
        assertEquals(base.getYear(), n.getYear());
        assertEquals(9, n.getMonthValue());
        assertEquals(1, n.getDayOfMonth());
    }

    // ============================================================
    // test_nth_wday_simple
    // ============================================================
    @Test
    public void testNthWdaySimple() {
        // Sun=0, Mon=1, Tue=2, ...
        assertArrayEquals(new int[]{3, 10, 17, 24, 31}, Croniter.getNthWeekdayOfMonth(2000, 1, 1));  // Mon
        assertArrayEquals(new int[]{1, 8, 15, 22, 29}, Croniter.getNthWeekdayOfMonth(2000, 2, 2));   // Tue (leap)
        assertArrayEquals(new int[]{6, 13, 20, 27}, Croniter.getNthWeekdayOfMonth(2000, 4, 4));      // Thu
    }

    // ============================================================
    // test_nth_as_last_wday_simple
    // ============================================================
    @Test
    public void testNthAsLastWdaySimple() {
        int[] feb2000Tue = Croniter.getNthWeekdayOfMonth(2000, 2, 2);
        assertEquals(29, feb2000Tue[feb2000Tue.length - 1]);
        int[] feb2000Sun = Croniter.getNthWeekdayOfMonth(2000, 2, 0);
        assertEquals(27, feb2000Sun[feb2000Sun.length - 1]);
        int[] feb2000Sat = Croniter.getNthWeekdayOfMonth(2000, 2, 6);
        assertEquals(26, feb2000Sat[feb2000Sat.length - 1]);
    }

    // ============================================================
    // test_nth_out_of_range
    // ============================================================
    @Test
    public void testNthOutOfRange() {
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("0 0 * * 1#7"));
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("0 0 * * 1#0"));
    }

    // ============================================================
    // test_overflow -- huge numbers
    // ============================================================
    @Test
    public void testOverflow() {
        assertThrows(Croniter.CroniterBadCronError.class,
                () -> new Croniter("0-10000000 * * * *"));
    }

    // ============================================================
    // test_revert_issue_90_aka_support_dow7
    // ============================================================
    @Test
    public void testRevertIssue90AkaSupportDow7() {
        assertTrue(Croniter.isValid("* * * * 1-7"));
        assertTrue(Croniter.isValid("* * * * 7"));
    }

    // ============================================================
    // test_get_prev_leap_year_feb29
    // ============================================================
    @Test
    public void testGetPrevLeapYearFeb29() {
        for (LocalDateTime start : new LocalDateTime[]{
                dt(2024, 3, 2), dt(2024, 3, 15), dt(2024, 3, 28)}) {
            LocalDateTime ret = (LocalDateTime) new Croniter("0 0 29 * *", start).getPrev(LocalDateTime.class);
            assertEquals(dt(2024, 2, 29), ret);
        }
        LocalDateTime ret = (LocalDateTime) new Croniter("0 0 29 * *", dt(2024, 1, 15)).getPrev(LocalDateTime.class);
        assertEquals(dt(2023, 12, 29), ret);
    }

    // ============================================================
    // test_bug_62_leap
    // ============================================================
    @Test
    public void testBug62Leap() {
        LocalDateTime ret = (LocalDateTime) new Croniter("15 22 29 2 *", dt(2024, 2, 29))
                .getPrev(LocalDateTime.class);
        assertEquals(dt(2020, 2, 29, 22, 15), ret);
    }

    // ============================================================
    // test_invalid_year
    // ============================================================
    @Test
    public void testInvalidYear() {
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("0 0 1 * * 0 1000"));
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("0 0 1 * * 0 99999"));
        assertThrows(Croniter.CroniterBadCronError.class, () -> new Croniter("0 0 1 * * 0 2070#3"));
    }

    // ============================================================
    // test_get_next_fails_with_expand_from_start_time_true
    // ============================================================
    @Test
    public void testGetNextFailsWithExpandFromStartTimeTrue() {
        Croniter c = new Croniter("0 0 */5 * *", null, true, null, false, false, false, true);
        assertThrows(IllegalArgumentException.class,
                () -> c.getNext(LocalDateTime.class, dt(2024, 7, 12)));
    }

    // ============================================================
    // test_expand_from_start_time_minute
    // ============================================================
    @Test
    public void testExpandFromStartTimeMinute() {
        Croniter c1 = new Croniter("*/7 * * * *", (Object) dt(2024, 7, 11, 10, 11),
                true, null, false, false, false, true);
        assertEquals(dt(2024, 7, 11, 10, 18), c1.getNext(LocalDateTime.class));
    }

    // ============================================================
    // test_year_with_other_field
    // ============================================================
    @Test
    public void testYearWithOtherField() {
        Croniter itr1 = new Croniter("0 0 31 11-12 * 0 2023", dt(2000, 1, 30));
        LocalDateTime n1 = (LocalDateTime) itr1.getNext(LocalDateTime.class);
        assertEquals(2023, n1.getYear());
        assertEquals(12, n1.getMonthValue());
        assertEquals(31, n1.getDayOfMonth());
    }

    // ============================================================
    // test_year (subset)
    // ============================================================
    @Test
    public void testYear() {
        Croniter itr1 = new Croniter("0 0 11 * * 0 2060", dt(2050, 1, 1));
        LocalDateTime n1 = (LocalDateTime) itr1.getNext(LocalDateTime.class);
        assertEquals(2060, n1.getYear());
        assertEquals(1, n1.getMonthValue());
        assertEquals(11, n1.getDayOfMonth());
    }

    // ============================================================
    // test_year_bad_date_error
    // ============================================================
    @Test
    public void testYearBadDateError() {
        assertThrows(Croniter.CroniterBadDateError.class, () -> {
            Croniter c = new Croniter("* * * * * * 2020", dt(2030, 1, 1));
            c.getNext();
        });
        assertThrows(Croniter.CroniterBadDateError.class, () -> {
            Croniter c = new Croniter("* * * * * * 2020", dt(2000, 1, 1));
            c.getPrev();
        });
    }

    // ============================================================
    // test_get_next_update_current
    // ============================================================
    @Test
    public void testGetNextUpdateCurrent() {
        Croniter cron = new Croniter("* * * * * *");
        cron.setCurrent(dt(2024, 7, 12), true);
        for (int i = 0; i < 3; i++) {
            LocalDateTime n = (LocalDateTime) cron.getNext(LocalDateTime.class);
            LocalDateTime cur = (LocalDateTime) cron.getCurrent(LocalDateTime.class);
            assertEquals(dt(2024, 7, 12, 0, 0, i + 1), n);
            assertEquals(dt(2024, 7, 12, 0, 0, i + 1), cur);
        }

        // update_current=false
        cron.setCurrent(dt(2024, 7, 12), true);
        for (int i = 0; i < 3; i++) {
            LocalDateTime n = (LocalDateTime) cron.getNext(LocalDateTime.class, null, false);
            assertEquals(dt(2024, 7, 12, 0, 0, 1), n);
            assertEquals(dt(2024, 7, 12), cron.getCurrent(LocalDateTime.class));
        }
    }

    // ============================================================
    // test_milliseconds (Python: microseconds; Java: nanos)
    // ============================================================
    @Test
    public void testMilliseconds() {
        LocalDateTime d = LocalDateTime.of(2018, 1, 2, 10, 0, 0, 500_000);  // 500us
        Croniter c1 = new Croniter("0 10 * * *", d);
        assertEquals(dt(2018, 1, 2, 10, 0), c1.getPrev(LocalDateTime.class));

        Croniter c2 = new Croniter("0 10 * * *", d);
        assertEquals(dt(2018, 1, 3, 10, 0), c2.getNext(LocalDateTime.class));
    }

    // ============================================================
    // test_match_precision
    // ============================================================
    @Test
    public void testMatchPrecision() {
        assertTrue(Croniter.match("0 0 * * *", dt(2019, 1, 14, 0, 0, 59)));
        assertFalse(Croniter.match("0 0 * * *", dt(2019, 1, 14, 0, 1, 1)));
        assertTrue(Croniter.match("0 0 * * *", dt(2019, 1, 14, 0, 0, 0), 1));
        assertFalse(Croniter.match("0 0 * * *", dt(2019, 1, 14, 0, 0, 59), 1));
    }
}
