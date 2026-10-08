import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for {@link Schedule} — port of the Python test_schedule.py.
 *
 * <p>Time-mocking mirrors the Python {@code mock_datetime} context manager: a
 * {@link Clock#fixed(java.time.Instant, ZoneId)} is installed on the
 * {@link Schedule.Scheduler} so that {@code now()} is deterministic.
 *
 * <p>Tests that depend on {@code pytz} timezones in Python use
 * {@code java.time.ZoneId} here (which uses TZDB and gives equivalent results
 * for IANA names like "Europe/Berlin", "America/New_York", etc.).</p>
 */
public class ScheduleTest {

    /** Convert "naive" local datetime to a Clock fixed at that instant in given zone. */
    private static Clock fixedClock(int y, int mo, int d, int h, int mi, int s, ZoneId zone) {
        LocalDateTime ldt = LocalDateTime.of(y, mo, d, h, mi, s);
        return Clock.fixed(ldt.atZone(zone).toInstant(), zone);
    }
    private static Clock fixedClock(int y, int mo, int d, int h, int mi) {
        return fixedClock(y, mo, d, h, mi, 0, ZoneId.of("Europe/Berlin"));
    }
    private static Clock fixedClock(int y, int mo, int d, int h, int mi, int s) {
        return fixedClock(y, mo, d, h, mi, s, ZoneId.of("Europe/Berlin"));
    }
    private static Clock fixedClock(int y, int mo, int d, int h, int mi, int s, String tz) {
        return fixedClock(y, mo, d, h, mi, s, ZoneId.of(tz));
    }

    /** Pin the default scheduler's clock for the duration of one test. */
    private void setClock(Clock c) { Schedule.defaultScheduler.clock = c; }

    @BeforeEach
    void setUp() {
        Schedule.clear();
        // tests use Berlin TZ as the "system" timezone for parity with Python
        setClock(Clock.systemDefaultZone());
    }

    @AfterEach
    void tearDown() {
        Schedule.clear();
        Schedule.defaultScheduler.clock = Clock.systemDefaultZone();
    }

    private static Schedule.MockJob makeMockJob() { return new Schedule.MockJob(); }
    private static Schedule.MockJob makeMockJob(String name) { return new Schedule.MockJob(name); }

    // =================================================================
    // test_time_units
    // =================================================================
    @Test
    void testTimeUnits() {
        assertEquals("seconds", Schedule.every().seconds().unit);
        assertEquals("minutes", Schedule.every().minutes().unit);
        assertEquals("hours",   Schedule.every().hours().unit);
        assertEquals("days",    Schedule.every().days().unit);
        assertEquals("weeks",   Schedule.every().weeks().unit);

        Schedule.Job ji = new Schedule.Job(2);
        assertThrows(Schedule.IntervalError.class, ji::minute);
        assertThrows(Schedule.IntervalError.class, ji::hour);
        assertThrows(Schedule.IntervalError.class, ji::day);
        assertThrows(Schedule.IntervalError.class, ji::week);
        assertThrows(Schedule.IntervalError.class, ji::monday);
        assertThrows(Schedule.IntervalError.class, ji::tuesday);
        assertThrows(Schedule.IntervalError.class, ji::wednesday);
        assertThrows(Schedule.IntervalError.class, ji::thursday);
        assertThrows(Schedule.IntervalError.class, ji::friday);
        assertThrows(Schedule.IntervalError.class, ji::saturday);
        assertThrows(Schedule.IntervalError.class, ji::sunday);

        // invalid unit
        ji.unit = "foo";
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("1:0:0"));
        assertThrows(Schedule.ScheduleValueError.class, ji::scheduleNextRun);

        // start_day exists but unit != weeks
        ji.unit = "days";
        ji.startDay = "monday";
        assertThrows(Schedule.ScheduleValueError.class, ji::scheduleNextRun);

        // weeks with invalid start_day
        ji.unit = "weeks";
        ji.startDay = "bar";
        assertThrows(Schedule.ScheduleValueError.class, ji::scheduleNextRun);

        // valid unit but invalid hours/minutes/seconds (start_day still set; at() doesn't touch it)
        ji.unit = "days";
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("25:00:00"));
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("00:61:00"));
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("00:00:61"));

        // invalid time format
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("25:0:0"));
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("0:61:0"));
        assertThrows(Schedule.ScheduleValueError.class, () -> ji.at("0:0:61"));

        // latest >= interval (latest=1, interval=2 -> ScheduleError)
        ji.latest = 1;
        assertThrows(Schedule.ScheduleError.class, ji::scheduleNextRun);
        // latest=3, interval=2 passes the latest check, but unit=weeks/start_day=bar still fails
        ji.latest = 3;
        ji.unit = "weeks";
        assertThrows(Schedule.ScheduleError.class, ji::scheduleNextRun);
    }

    @Test
    void testNextRunWithTag() {
        setClock(fixedClock(2014, 6, 28, 12, 0));
        Schedule.Job j1 = Schedule.every(5).seconds().doIt("job1", makeMockJob()).tag("tag1");
        Schedule.Job j2 = Schedule.every(2).hours().doIt("job2", makeMockJob()).tag("tag1", "tag2");
        Schedule.Job j3 = Schedule.every(1).minutes().doIt("job3", makeMockJob()).tag("tag1", "tag3", "tag2");

        assertEquals(j1.nextRun, Schedule.nextRun("tag1"));
        assertEquals(j3.nextRun, Schedule.defaultScheduler.getNextRun("tag2"));
        assertEquals(j3.nextRun, Schedule.nextRun("tag3"));
        assertNull(Schedule.nextRun("tag4"));
    }

    @Test
    void testSingularTimeUnitsMatchPluralUnits() {
        assertEquals(Schedule.every().seconds().unit, Schedule.every().second().unit);
        assertEquals(Schedule.every().minutes().unit, Schedule.every().minute().unit);
        assertEquals(Schedule.every().hours().unit,   Schedule.every().hour().unit);
        assertEquals(Schedule.every().days().unit,    Schedule.every().day().unit);
        assertEquals(Schedule.every().weeks().unit,   Schedule.every().week().unit);
    }

    @Test
    void testTimeRange() {
        setClock(fixedClock(2014, 6, 28, 12, 0));
        Schedule.MockJob mock = makeMockJob();
        Set<Integer> minutes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int m = Schedule.every(5).to(30).minutes().doIt(mock).nextRun.getMinute();
            minutes.add(m);
        }
        assertTrue(minutes.size() > 1);
        assertTrue(Collections.min(minutes) >= 5);
        assertTrue(Collections.max(minutes) <= 30);
    }

    @Test
    void testTimeRangeRepr() {
        setClock(fixedClock(2014, 6, 28, 12, 0));
        Schedule.MockJob mock = makeMockJob();
        String rep = Schedule.every(5).to(30).minutes().doIt(mock).repr();
        assertTrue(rep.startsWith("Every 5 to 30 minutes do "));
    }

    @Test
    void testAtTime() {
        Schedule.MockJob mock = makeMockJob();
        assertEquals(10, Schedule.every().day().at("10:30").doIt(mock).nextRun.getHour());
        assertEquals(30, Schedule.every().day().at("10:30").doIt(mock).nextRun.getMinute());
        assertEquals(59, Schedule.every().day().at("20:59").doIt(mock).nextRun.getMinute());
        assertEquals(50, Schedule.every().day().at("10:30:50").doIt(mock).nextRun.getSecond());

        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at("2:30:000001"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at("::2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at(".2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at("2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at(":2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at(" 2:30:00"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().at("59:59"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().doIt(() -> null));

        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).second());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).minute());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).hour());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).day());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).week());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).monday());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).tuesday());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).wednesday());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).thursday());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).friday());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).saturday());
        assertThrows(Schedule.IntervalError.class, () -> Schedule.every(2).sunday());
    }

    @Test
    void testUntilTime() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2020, 1, 1, 10, 0, 0));
        LocalDateTime m = LocalDateTime.of(2020, 1, 1, 10, 0, 0);

        assertEquals(LocalDateTime.of(3000, 1, 1, 20, 30, 0),
            Schedule.every().day().until(LocalDateTime.of(3000, 1, 1, 20, 30)).doIt(mock).cancelAfter);
        assertEquals(LocalDateTime.of(3000, 1, 1, 20, 30, 50),
            Schedule.every().day().until(LocalDateTime.of(3000, 1, 1, 20, 30, 50)).doIt(mock).cancelAfter);
        assertEquals(m.withHour(12).withMinute(30).withSecond(0).withNano(0),
            Schedule.every().day().until(LocalTime.of(12, 30)).doIt(mock).cancelAfter);
        assertEquals(m.withHour(12).withMinute(30).withSecond(50).withNano(0),
            Schedule.every().day().until(LocalTime.of(12, 30, 50)).doIt(mock).cancelAfter);
        assertEquals(LocalDateTime.of(2020, 2, 10, 15, 12, 42),
            Schedule.every().day().until(Duration.ofDays(40).plusHours(5).plusMinutes(12).plusSeconds(42))
                .doIt(mock).cancelAfter);
        assertEquals(m.withHour(10).withMinute(30).withSecond(0).withNano(0),
            Schedule.every().day().until("10:30").doIt(mock).cancelAfter);
        assertEquals(m.withHour(10).withMinute(30).withSecond(50).withNano(0),
            Schedule.every().day().until("10:30:50").doIt(mock).cancelAfter);
        assertEquals(LocalDateTime.of(3000, 1, 1, 10, 30, 0),
            Schedule.every().day().until("3000-01-01 10:30").doIt(mock).cancelAfter);
        assertEquals(LocalDateTime.of(3000, 1, 1, 10, 30, 50),
            Schedule.every().day().until("3000-01-01 10:30:50").doIt(mock).cancelAfter);
        assertEquals(LocalDateTime.of(3000, 1, 1, 10, 30, 50),
            Schedule.every().day().until(LocalDateTime.of(3000, 1, 1, 10, 30, 50)).doIt(mock).cancelAfter);

        // Invalid string formats
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().until("123"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().day().until("01-01-3000"));

        // Past moments
        assertThrows(Schedule.ScheduleValueError.class,
            () -> Schedule.every().day().until(LocalDateTime.of(2019, 12, 31, 23, 59)));
        assertThrows(Schedule.ScheduleValueError.class,
            () -> Schedule.every().day().until(Duration.ofMinutes(-1)));
        // one_hour_ago is now() - 1h which (with our fixed clock) is well before now
        assertThrows(Schedule.ScheduleValueError.class,
            () -> Schedule.every().day().until(LocalDateTime.of(2020, 1, 1, 9, 0)));

        // Unschedule when next_run passes the deadline
        Schedule.clear();
        setClock(fixedClock(2020, 1, 1, 11, 35, 10));
        Schedule.MockJob mock2 = makeMockJob();
        Schedule.Job runJob = Schedule.every(5).seconds().until(LocalTime.of(11, 35, 20)).doIt(mock2);
        // (run_pending at 11:35:15 -> count 1, jobs still 1)
        setClock(fixedClock(2020, 1, 1, 11, 35, 15));
        Schedule.runPending();
        assertEquals(1, mock2.callCount);
        assertEquals(1, Schedule.jobs().size());
        // run_all at 11:35:20 -> count 2, jobs cleared
        setClock(fixedClock(2020, 1, 1, 11, 35, 20));
        Schedule.runAll();
        assertEquals(2, mock2.callCount);
        assertEquals(0, Schedule.jobs().size());

        // Unschedule because current execution time has passed deadline
        Schedule.clear();
        setClock(fixedClock(2020, 1, 1, 11, 35, 10));
        Schedule.MockJob mock3 = makeMockJob();
        Schedule.every(5).seconds().until(LocalTime.of(11, 35, 20)).doIt(mock3);
        setClock(fixedClock(2020, 1, 1, 11, 35, 50));
        Schedule.runPending();
        assertEquals(0, mock3.callCount);
        assertEquals(0, Schedule.jobs().size());
    }

    @Test
    void testWeekdayAtToday() {
        Schedule.MockJob mock = makeMockJob();
        // 2020-11-25 is a wednesday
        setClock(fixedClock(2020, 11, 25, 22, 38, 5));
        Schedule.Job job = Schedule.every().wednesday().at("22:38:10").doIt(mock);
        assertEquals(22, job.nextRun.getHour());
        assertEquals(38, job.nextRun.getMinute());
        assertEquals(10, job.nextRun.getSecond());
        assertEquals(2020, job.nextRun.getYear());
        assertEquals(11, job.nextRun.getMonthValue());
        assertEquals(25, job.nextRun.getDayOfMonth());

        Schedule.Job job2 = Schedule.every().wednesday().at("22:39").doIt(mock);
        assertEquals(22, job2.nextRun.getHour());
        assertEquals(39, job2.nextRun.getMinute());
        assertEquals(0, job2.nextRun.getSecond());
        assertEquals(25, job2.nextRun.getDayOfMonth());
    }

    @Test
    void testAtTimeHour() {
        setClock(fixedClock(2010, 1, 6, 12, 20));
        Schedule.MockJob mock = makeMockJob();
        assertEquals(12, Schedule.every().hour().at(":30").doIt(mock).nextRun.getHour());
        assertEquals(30, Schedule.every().hour().at(":30").doIt(mock).nextRun.getMinute());
        assertEquals(0,  Schedule.every().hour().at(":30").doIt(mock).nextRun.getSecond());
        assertEquals(13, Schedule.every().hour().at(":10").doIt(mock).nextRun.getHour());
        assertEquals(10, Schedule.every().hour().at(":10").doIt(mock).nextRun.getMinute());
        assertEquals(0,  Schedule.every().hour().at(":10").doIt(mock).nextRun.getSecond());
        assertEquals(13, Schedule.every().hour().at(":00").doIt(mock).nextRun.getHour());
        assertEquals(0,  Schedule.every().hour().at(":00").doIt(mock).nextRun.getMinute());
        assertEquals(0,  Schedule.every().hour().at(":00").doIt(mock).nextRun.getSecond());

        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at("2:30:00"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at("::2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at(".2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at("2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at(" 2:30"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at("61:00"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at("00:61"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().hour().at("01:61"));

        assertEquals(12, Schedule.every().hour().at("30:05").doIt(mock).nextRun.getHour());
        assertEquals(30, Schedule.every().hour().at("30:05").doIt(mock).nextRun.getMinute());
        assertEquals(5,  Schedule.every().hour().at("30:05").doIt(mock).nextRun.getSecond());
        assertEquals(13, Schedule.every().hour().at("10:25").doIt(mock).nextRun.getHour());
        assertEquals(10, Schedule.every().hour().at("10:25").doIt(mock).nextRun.getMinute());
        assertEquals(25, Schedule.every().hour().at("10:25").doIt(mock).nextRun.getSecond());
        assertEquals(13, Schedule.every().hour().at("00:40").doIt(mock).nextRun.getHour());
        assertEquals(0,  Schedule.every().hour().at("00:40").doIt(mock).nextRun.getMinute());
        assertEquals(40, Schedule.every().hour().at("00:40").doIt(mock).nextRun.getSecond());
    }

    @Test
    void testAtTimeMinute() {
        setClock(fixedClock(2010, 1, 6, 12, 20, 30));
        Schedule.MockJob mock = makeMockJob();
        assertEquals(12, Schedule.every().minute().at(":40").doIt(mock).nextRun.getHour());
        assertEquals(20, Schedule.every().minute().at(":40").doIt(mock).nextRun.getMinute());
        assertEquals(40, Schedule.every().minute().at(":40").doIt(mock).nextRun.getSecond());
        assertEquals(12, Schedule.every().minute().at(":10").doIt(mock).nextRun.getHour());
        assertEquals(21, Schedule.every().minute().at(":10").doIt(mock).nextRun.getMinute());
        assertEquals(10, Schedule.every().minute().at(":10").doIt(mock).nextRun.getSecond());

        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().minute().at("::2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().minute().at(".2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().minute().at("2"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().minute().at("2:30:00"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().minute().at("2:30"));
        assertThrows(Schedule.ScheduleValueError.class, () -> Schedule.every().minute().at(" :30"));
    }

    @Test
    void testNextRunTime() {
        setClock(fixedClock(2010, 1, 6, 12, 15));
        Schedule.MockJob mock = makeMockJob();
        assertNull(Schedule.nextRun());
        assertEquals(16, Schedule.every().minute().doIt(mock).nextRun.getMinute());
        assertEquals(20, Schedule.every(5).minutes().doIt(mock).nextRun.getMinute());
        assertEquals(13, Schedule.every().hour().doIt(mock).nextRun.getHour());
        assertEquals(7,  Schedule.every().day().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(7,  Schedule.every().day().at("09:00").doIt(mock).nextRun.getDayOfMonth());
        assertEquals(6,  Schedule.every().day().at("12:30").doIt(mock).nextRun.getDayOfMonth());
        assertEquals(13, Schedule.every().week().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(11, Schedule.every().monday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(12, Schedule.every().tuesday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(13, Schedule.every().wednesday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(7,  Schedule.every().thursday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(8,  Schedule.every().friday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(9,  Schedule.every().saturday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(10, Schedule.every().sunday().doIt(mock).nextRun.getDayOfMonth());
        assertEquals(16, Schedule.every().minute().until(LocalTime.of(12, 17)).doIt(mock).nextRun.getMinute());
    }

    @Test
    void testNextRunTimeDayEnd() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2010, 12, 1, 23, 0, 0));
        Schedule.Job job = Schedule.every().day().at("23:30").doIt(mock);
        assertEquals(1, job.nextRun.getDayOfMonth());
        assertEquals(23, job.nextRun.getHour());

        setClock(fixedClock(2010, 12, 2, 1, 0, 0));
        job.run();
        assertEquals(2, job.nextRun.getDayOfMonth());
        assertEquals(23, job.nextRun.getHour());

        setClock(fixedClock(2010, 12, 2, 23, 30, 0));
        job.run();
        assertEquals(3, job.nextRun.getDayOfMonth());
        assertEquals(23, job.nextRun.getHour());
    }

    @Test
    void testNextRunTimeMinuteEnd() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2010, 10, 10, 10, 10, 0));
        Schedule.Job job = Schedule.every().minute().at(":15").doIt(mock);
        assertEquals(10, job.nextRun.getMinute());
        assertEquals(15, job.nextRun.getSecond());

        setClock(fixedClock(2010, 10, 10, 10, 10, 59));
        job.run();
        assertEquals(11, job.nextRun.getMinute());
        assertEquals(15, job.nextRun.getSecond());

        setClock(fixedClock(2010, 10, 10, 10, 12, 14));
        job.run();
        assertEquals(12, job.nextRun.getMinute());
        assertEquals(15, job.nextRun.getSecond());

        setClock(fixedClock(2010, 10, 10, 10, 12, 16));
        job.run();
        assertEquals(13, job.nextRun.getMinute());
        assertEquals(15, job.nextRun.getSecond());
    }

    // -----------------------------------------------------------------
    // Timezone tests (using java.time.ZoneId / TZDB instead of pytz)
    // -----------------------------------------------------------------
    @Test
    void testTzDailyKolkata() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2022, 2, 1, 23, 15));
        // Berlin: feb-1 23:15  ->  Kolkata: feb-2 03:45
        // Expected next-run Kolkata: 06:30 -> Berlin: 02:00
        LocalDateTime next = Schedule.every().day().at("06:30", "Asia/Kolkata").doIt(mock).nextRun;
        assertEquals(2, next.getDayOfMonth());
        assertEquals(2, next.getHour());
        assertEquals(0, next.getMinute());
    }

    @Test
    void testTzDailyMidnight() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2023, 4, 14, 4, 50));
        LocalDateTime next = Schedule.every().day().at("00:00", "US/Central").doIt(mock).nextRun;
        assertEquals(14, next.getDayOfMonth());
        assertEquals(7, next.getHour());
        assertEquals(0, next.getMinute());
    }

    @Test
    void testTzDailyHalfHourOffset() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2022, 4, 8, 10, 0));
        LocalDateTime next = Schedule.every().day().at("10:30", "America/New_York").doIt(mock).nextRun;
        assertEquals(16, next.getHour());
        assertEquals(30, next.getMinute());
    }

    @Test
    void testTzDailyDst() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2022, 3, 20, 10, 0));
        // Berlin not yet on DST, NY already on DST -> NY 10:30 = Berlin 15:30
        LocalDateTime next = Schedule.every().day().at("10:30", ZoneId.of("America/New_York")).doIt(mock).nextRun;
        assertEquals(15, next.getHour());
        assertEquals(30, next.getMinute());
    }

    @Test
    void testTzInvalidTimezoneExceptions() {
        Schedule.MockJob mock = makeMockJob();
        assertThrows(Schedule.ScheduleValueError.class,
            () -> Schedule.every().day().at("10:30", "FakeZone").doIt(mock));
    }

    // -----------------------------------------------------------------
    // _move_to_next_weekday helpers
    // -----------------------------------------------------------------
    @Test
    void testMoveToNextWeekdayToday() {
        LocalDateTime monday = LocalDateTime.of(2024, 5, 13, 10, 27, 54);
        LocalDateTime out = Schedule.moveToNextWeekday(monday, "monday");
        assertEquals(13, out.getDayOfMonth());
        assertEquals(10, out.getHour());
        assertEquals(27, out.getMinute());
    }

    @Test
    void testMoveToNextWeekdayTomorrow() {
        LocalDateTime monday = LocalDateTime.of(2024, 5, 13, 10, 27, 54);
        LocalDateTime out = Schedule.moveToNextWeekday(monday, "tuesday");
        assertEquals(14, out.getDayOfMonth());
        assertEquals(10, out.getHour());
        assertEquals(27, out.getMinute());
    }

    @Test
    void testMoveToNextWeekdayNextWeek() {
        LocalDateTime wed = LocalDateTime.of(2024, 5, 15, 10, 27, 54);
        LocalDateTime out = Schedule.moveToNextWeekday(wed, "tuesday");
        assertEquals(21, out.getDayOfMonth());
    }

    // -----------------------------------------------------------------
    // run_all / repeat
    // -----------------------------------------------------------------
    @Test
    void testRunAll() {
        Schedule.MockJob mock = makeMockJob();
        Schedule.every().minute().doIt(mock);
        Schedule.every().hour().doIt(mock);
        Schedule.every().day().at("11:00").doIt(mock);
        Schedule.runAll();
        assertEquals(3, mock.callCount);
    }

    @Test
    void testRunAllWithDecorator() {
        Schedule.MockJob mock = makeMockJob();
        Schedule.repeat(Schedule.every().minute(), (Runnable) mock::run);
        Schedule.repeat(Schedule.every().hour(),   (Runnable) mock::run);
        Schedule.repeat(Schedule.every().day().at("11:00"), (Runnable) mock::run);
        Schedule.runAll();
        assertEquals(3, mock.callCount);
    }

    @Test
    void testJobFuncArgsArePassedOn() {
        // Java port: do this by passing a lambda that captures args directly.
        Object[] args = {1, 2, "three"};
        Map<String, Object> kwargs = new LinkedHashMap<>();
        kwargs.put("foo", 23);
        kwargs.put("bar", new HashMap<>());
        final List<Object[]> received = new ArrayList<>();
        final List<Map<String, Object>> receivedKw = new ArrayList<>();
        Schedule.every().second().doIt("job", () -> {
            received.add(args);
            receivedKw.add(kwargs);
        }, args, kwargs);
        Schedule.runAll();
        assertEquals(1, received.size());
        assertArrayEquals(new Object[]{1, 2, "three"}, received.get(0));
        assertEquals(kwargs, receivedKw.get(0));
    }

    @Test
    void testToString() {
        Object[] args = {"foo"};
        Map<String, Object> kwargs = new LinkedHashMap<>();
        kwargs.put("bar", 23);
        Schedule.Job j = Schedule.every().minute().doIt("job_fun", () -> {}, args, kwargs);
        String s = j.toString();
        assertEquals("Job(interval=1, unit=minutes, do=job_fun, args=('foo',), kwargs={'bar': 23})", s);
        assertTrue(s.contains("job_fun"));
        assertTrue(s.contains("foo"));
        assertTrue(s.contains("{'bar': 23}"));
    }

    @Test
    void testToRepr() {
        Object[] args = {"foo"};
        Map<String, Object> kwargs = new LinkedHashMap<>();
        kwargs.put("bar", 23);
        String s = Schedule.every().minute().doIt("job_fun", () -> {}, args, kwargs).repr();
        assertTrue(s.startsWith("Every 1 minute do job_fun('foo', bar=23) (last run: [never], next run: "));
        assertTrue(s.contains("job_fun"));
        assertTrue(s.contains("foo"));
        assertTrue(s.contains("bar=23"));

        String s2 = Schedule.every().day().at("00:00").doIt("job_fun", () -> {}, args, kwargs).repr();
        assertTrue(s2.startsWith("Every 1 day at 00:00 do job_fun('foo', bar=23) (last run: [never], next run: "));

        // Partially-composed Job
        String s3 = new Schedule.Job(10).toString();
        // Python: "Every 10 None do [None] (last run: [never], next run: [never])"
        // Our toString format: Job(interval=10, unit=null, do=None, args=(), kwargs={})
        assertTrue(s3.contains("interval=10"));
    }

    @Test
    void testToStringLambda() {
        assertTrue(Schedule.every().minute().doIt(() -> null).toString().length() > 1);
        assertTrue(Schedule.every().day().at("10:30").doIt(() -> null).toString().length() > 1);
    }

    // -----------------------------------------------------------------
    // run_pending core
    // -----------------------------------------------------------------
    @Test
    void testRunPending() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2010, 1, 6, 12, 15));
        Schedule.every().minute().doIt(mock);
        Schedule.every().hour().doIt(mock);
        Schedule.every().day().doIt(mock);
        Schedule.every().sunday().doIt(mock);
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 6, 12, 16));
        Schedule.runPending();
        assertEquals(1, mock.callCount);

        setClock(fixedClock(2010, 1, 6, 13, 16));
        mock.reset();
        Schedule.runPending();
        assertEquals(2, mock.callCount);

        setClock(fixedClock(2010, 1, 7, 13, 16));
        mock.reset();
        Schedule.runPending();
        assertEquals(3, mock.callCount);

        setClock(fixedClock(2010, 1, 10, 13, 16));
        mock.reset();
        Schedule.runPending();
        assertEquals(4, mock.callCount);
    }

    @Test
    void testRunEveryWeekdayAtSpecificTimeToday() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2010, 1, 6, 13, 16));
        Schedule.every().wednesday().at("14:12").doIt(mock);
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 6, 14, 16));
        Schedule.runPending();
        assertEquals(1, mock.callCount);
    }

    @Test
    void testRunEveryWeekdayAtSpecificTimePastToday() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2010, 1, 6, 13, 16));
        Schedule.every().wednesday().at("13:15").doIt(mock);
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 13, 13, 14));
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 13, 13, 16));
        Schedule.runPending();
        assertEquals(1, mock.callCount);
    }

    @Test
    void testRunEveryNDaysAtSpecificTime() {
        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2010, 1, 6, 11, 29));
        Schedule.every(2).days().at("11:30").doIt(mock);
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 6, 11, 31));
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 7, 11, 31));
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 8, 11, 29));
        Schedule.runPending();
        assertEquals(0, mock.callCount);

        setClock(fixedClock(2010, 1, 8, 11, 31));
        Schedule.runPending();
        assertEquals(1, mock.callCount);

        setClock(fixedClock(2010, 1, 10, 11, 31));
        Schedule.runPending();
        assertEquals(2, mock.callCount);
    }

    @Test
    void testNextRunProperty() {
        setClock(fixedClock(2010, 1, 6, 13, 16));
        Schedule.MockJob hourly = makeMockJob("hourly");
        Schedule.MockJob daily  = makeMockJob("daily");
        Schedule.every().day().doIt(daily);
        Schedule.every().hour().doIt(hourly);
        assertEquals(2, Schedule.jobs().size());
        assertEquals(LocalDateTime.of(2010, 1, 6, 14, 16), Schedule.nextRun());
    }

    @Test
    void testIdleSeconds() {
        assertNull(Schedule.defaultScheduler.getNextRun());
        assertNull(Schedule.idleSeconds());

        Schedule.MockJob mock = makeMockJob();
        setClock(fixedClock(2020, 12, 9, 21, 46));
        Schedule.Job job = Schedule.every().hour().doIt(mock);
        assertEquals(60.0 * 60.0, Schedule.idleSeconds(), 0.001);
        Schedule.cancelJob(job);
        assertNull(Schedule.nextRun());
        assertNull(Schedule.idleSeconds());
    }

    @Test
    void testCancelJob() {
        Schedule.every().second().doIt(() -> Schedule.CancelJob);
        Schedule.MockJob mock = makeMockJob();
        Schedule.Job mj = Schedule.every().second().doIt(mock);
        assertEquals(2, Schedule.jobs().size());

        Schedule.runAll();
        assertEquals(1, Schedule.jobs().size());
        assertSame(mj, Schedule.jobs().get(0));

        Schedule.cancelJob("Not a job");
        assertEquals(1, Schedule.jobs().size());
        Schedule.defaultScheduler.cancelJob("Not a job");
        assertEquals(1, Schedule.jobs().size());

        Schedule.cancelJob(mj);
        assertEquals(0, Schedule.jobs().size());
    }

    @Test
    void testCancelJobs() {
        Schedule.every().second().doIt(() -> Schedule.CancelJob);
        Schedule.every().second().doIt(() -> Schedule.CancelJob);
        Schedule.every().second().doIt(() -> Schedule.CancelJob);
        assertEquals(3, Schedule.jobs().size());
        Schedule.runAll();
        assertEquals(0, Schedule.jobs().size());
    }

    @Test
    void testTagTypeEnforcement() {
        Schedule.Job j1 = Schedule.every().second().doIt("job1", makeMockJob());
        assertThrows(ClassCastException.class, () -> j1.tag(new HashMap<>()));
        assertThrows(ClassCastException.class, () -> j1.tag(1, "a", new ArrayList<>()));
        j1.tag(0, "a", true);
        assertEquals(3, j1.tags.size());
    }

    @Test
    void testGetByTag() {
        Schedule.every().second().doIt(makeMockJob()).tag("job1", "tag1");
        Schedule.every().second().doIt(makeMockJob()).tag("job2", "tag2", "tag4");
        Schedule.every().second().doIt(makeMockJob()).tag("job3", "tag3", "tag4");

        // None input -> all 3
        List<Schedule.Job> all = Schedule.getJobs();
        assertEquals(3, all.size());

        // Each 1:1 tag/job
        List<Schedule.Job> tag1 = Schedule.getJobs("tag1");
        assertEquals(1, tag1.size());
        assertTrue(tag1.get(0).tags.contains("job1"));

        // Multiple jobs found
        List<Schedule.Job> tag4 = Schedule.getJobs("tag4");
        assertEquals(2, tag4.size());

        // No match
        assertEquals(0, Schedule.getJobs("tag5").size());
        Schedule.clear();
        assertEquals(0, Schedule.jobs().size());
    }

    @Test
    void testClearByTag() {
        Schedule.every().second().doIt("job1", makeMockJob("job1")).tag("tag1");
        Schedule.every().second().doIt("job2", makeMockJob("job2")).tag("tag1", "tag2");
        Schedule.every().second().doIt("job3", makeMockJob("job3")).tag("tag3", "tag3", "tag3", "tag2");
        assertEquals(3, Schedule.jobs().size());
        Schedule.runAll();
        assertEquals(3, Schedule.jobs().size());
        Schedule.clear("tag3");
        assertEquals(2, Schedule.jobs().size());
        Schedule.clear("tag1");
        assertEquals(0, Schedule.jobs().size());
        Schedule.every().second().doIt("job1", makeMockJob("job1"));
        Schedule.every().second().doIt("job2", makeMockJob("job2"));
        Schedule.every().second().doIt("job3", makeMockJob("job3"));
        Schedule.clear();
        assertEquals(0, Schedule.jobs().size());
    }

    @Test
    void testMisconfiguredJobWontBreakScheduler() {
        Schedule.Scheduler scheduler = new Schedule.Scheduler();
        scheduler.every();
        scheduler.every(10).seconds();
        scheduler.runPending();
    }

    // -----------------------------------------------------------------
    // Daylight savings smoke test
    // -----------------------------------------------------------------
    @Test
    void testDaylightSavingTime() {
        Schedule.MockJob mock = makeMockJob();
        // 27 March 2022 02:00 -> +1h DST in Berlin
        setClock(fixedClock(2022, 3, 27, 0, 0));
        assertEquals(4, Schedule.every(4).hours().doIt(mock).nextRun.getHour());
        Schedule.clear();

        // 30 October 2022 03:00 -> -1h
        setClock(fixedClock(2022, 10, 30, 0, 0));
        assertEquals(4, Schedule.every(4).hours().doIt(mock).nextRun.getHour());
    }
}
