// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

public class Croniter {

    // ---------------------------------------------------------------
    // Exceptions (mirror the Python class hierarchy)
    // ---------------------------------------------------------------

    /** Top-level Croniter base exception. */
    public static class CroniterError extends RuntimeException {
        public CroniterError(String msg) { super(msg); }
    }

    /** Type-range error (raised when start_time is wrong type). */
    public static class CroniterBadTypeRangeError extends RuntimeException {
        public CroniterBadTypeRangeError(String msg) { super(msg); }
    }

    /** Syntax / unknown value / range error within a cron expression. */
    public static class CroniterBadCronError extends CroniterError {
        public CroniterBadCronError(String msg) { super(msg); }
    }

    /** Valid cron syntax, but likely to produce inaccurate results. */
    public static class CroniterUnsupportedSyntaxError extends CroniterBadCronError {
        public CroniterUnsupportedSyntaxError(String msg) { super(msg); }
    }

    /** Unable to find next/prev timestamp match. */
    public static class CroniterBadDateError extends CroniterError {
        public CroniterBadDateError(String msg) { super(msg); }
    }

    /** Cron syntax contains an invalid day or month abbreviation. */
    public static class CroniterNotAlphaError extends CroniterBadCronError {
        public CroniterNotAlphaError(String msg) { super(msg); }
    }

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /**
     * Construct a Croniter for the given cron expression starting at the
     * current system time.
     *
     * @param exprFormat the cron expression (5, 6, or 7 fields)
     */
    public Croniter(String exprFormat) { /* ... */ }

    /**
     * Construct a Croniter for the given cron expression starting at the
     * specified base time.
     *
     * @param exprFormat the cron expression
     * @param startTime the starting LocalDateTime
     */
    public Croniter(String exprFormat, LocalDateTime startTime) { /* ... */ }

    /**
     * Construct a Croniter for the given cron expression starting at the
     * specified base time (timezone-aware).
     *
     * @param exprFormat the cron expression
     * @param startTime the starting ZonedDateTime
     */
    public Croniter(String exprFormat, ZonedDateTime startTime) { /* ... */ }

    /**
     * Full constructor supporting all options.
     *
     * @param exprFormat the cron expression
     * @param startTime the starting time (LocalDateTime, ZonedDateTime, or Double epoch)
     * @param dayOr if true (default), DAY_OF_MONTH and DAY_OF_WEEK form a union; if false an intersection
     * @param maxYearsBetweenMatches maximum number of years to search before raising CroniterBadDateError; null for default
     * @param isPrev if true, default direction is backwards
     * @param implementCronBug if true, implement Vixie cron's DAY-OR/AND bug
     * @param secondAtBeginning if true, second is the leading field rather than the trailing field
     * @param expandFromStartTime if true, anchor */
     * step expansion at the start time
     */
    public Croniter(String exprFormat, Object startTime, boolean dayOr,
                    Integer maxYearsBetweenMatches, boolean isPrev,
                    boolean implementCronBug, boolean secondAtBeginning,
                    boolean expandFromStartTime) { /* ... */ }

    // ---------------------------------------------------------------
    // Iteration
    // ---------------------------------------------------------------

    /**
     * Return the next firing time as a Unix epoch double.
     */
    public double getNext() { /* ... */ return 0.0; }

    /**
     * Return the next firing time as a LocalDateTime (or ZonedDateTime if start was zoned).
     */
    public Object getNext(Class<?> retType) { /* ... */ return null; }

    /**
     * Return the next firing time, with explicit start time (resets internal current).
     */
    public Object getNext(Class<?> retType, Object startTime) { /* ... */ return null; }

    /**
     * Return the next firing time. If updateCurrent is false the internal cursor is not advanced.
     */
    public Object getNext(Class<?> retType, Object startTime, boolean updateCurrent) { /* ... */ return null; }

    /**
     * Return the previous firing time as a Unix epoch double.
     */
    public double getPrev() { /* ... */ return 0.0; }

    /** Return the previous firing time as the requested type. */
    public Object getPrev(Class<?> retType) { /* ... */ return null; }
    public Object getPrev(Class<?> retType, Object startTime) { /* ... */ return null; }
    public Object getPrev(Class<?> retType, Object startTime, boolean updateCurrent) { /* ... */ return null; }

    /** Return the current cursor value. */
    public Object getCurrent(Class<?> retType) { /* ... */ return null; }
    public double getCurrent() { /* ... */ return 0.0; }

    /** Update the internal cursor; returns the new cursor value (epoch seconds). */
    public double setCurrent(Object startTime, boolean force) { /* ... */ return 0.0; }

    /** Iterator yielding successive next firing times. */
    public Iterator<Object> allNext(Class<?> retType) { /* ... */ return null; }

    /** Iterator yielding successive previous firing times. */
    public Iterator<Object> allPrev(Class<?> retType) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // Static helpers
    // ---------------------------------------------------------------

    /** Validate a cron expression. */
    public static boolean isValid(String expression) { /* ... */ return false; }
    public static boolean isValid(String expression, String hashId) { /* ... */ return false; }
    public static boolean isValid(String expression, String hashId, boolean secondAtBeginning,
                                   boolean strict, Object strictYear) { /* ... */ return false; }

    /**
     * Returns true if {@code testdate} matches the cron expression (within precision).
     */
    public static boolean match(String cronExpression, LocalDateTime testdate) { /* ... */ return false; }
    public static boolean match(String cronExpression, LocalDateTime testdate, boolean dayOr) { /* ... */ return false; }
    public static boolean match(String cronExpression, LocalDateTime testdate, boolean dayOr,
                                 boolean secondAtBeginning, Integer precisionInSeconds) { /* ... */ return false; }

    /** Returns true if the cron expression has at least one match in [from, to]. */
    public static boolean matchRange(String cronExpression, LocalDateTime fromDt, LocalDateTime toDt) { /* ... */ return false; }
    public static boolean matchRange(String cronExpression, LocalDateTime fromDt, LocalDateTime toDt,
                                      boolean dayOr, boolean secondAtBeginning, Integer precisionInSeconds) { /* ... */ return false; }

    /**
     * Expand a cron expression into a normalized form used internally.
     *
     * @return the expanded fields (each element a sorted list of ints, or {@code "*"} / {@code "l"})
     */
    public static Object[] expand(String exprFormat) { /* ... */ return null; }
    public static Object[] expand(String exprFormat, String hashId, boolean secondAtBeginning,
                                   Double fromTimestamp, boolean strict, Object strictYear) { /* ... */ return null; }

    /** Convert a LocalDateTime to a Unix timestamp (UTC epoch seconds). */
    public static double datetimeToTimestamp(LocalDateTime d) { /* ... */ return 0.0; }

    /** Convert a Unix epoch timestamp to a LocalDateTime in the given zone. */
    public LocalDateTime timestampToDatetime(double timestamp) { /* ... */ return null; }

    /** Returns the nth-weekday-of-month tuple (1st, 2nd, …) for the given (year, month, dayOfWeek). */
    public static int[] getNthWeekdayOfMonth(int year, int month, int dayOfWeek) { /* ... */ return null; }

    /** Returns the nearest weekday (Mon-Fri) to the given day-of-month in the given year/month. */
    public static int getNearestWeekday(int year, int month, int day) { /* ... */ return 0; }

    /** Public accessor for the expanded fields (after construction). */
    public List<List<Object>> getExpanded() { /* ... */ return null; }
}
