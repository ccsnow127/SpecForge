// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.
//
// Java port of the Python `schedule` module.  In-process job scheduler with a
// fluent builder API such as `every(10).minutes().doIt(fn)`.
//
// IMPORTANT JAVA NAMING NOTE:
//   The Python method `Job.do(...)` is renamed to `doIt(...)` in Java because
//   `do` is a reserved word.  All other method names follow camelCase
//   translation of their Python snake_case originals (e.g. `run_pending` ->
//   `runPending`, `cancel_job` -> `cancelJob`).  Python read-only `@property`
//   accessors that return `self` after mutating state (e.g. `seconds`,
//   `minutes`) are translated to no-arg methods in Java
//   (e.g. `seconds()`, `minutes()`).

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

public class Schedule {

    // ---------------------------------------------------------------
    // Exception hierarchy (mirrors the Python classes)
    // ---------------------------------------------------------------

    /** Base schedule exception. */
    public static class ScheduleError extends RuntimeException {
        public ScheduleError(String msg) { super(msg); }
    }

    /** Base schedule value error. */
    public static class ScheduleValueError extends ScheduleError {
        public ScheduleValueError(String msg) { super(msg); }
    }

    /** An improper interval was used. */
    public static class IntervalError extends ScheduleValueError {
        public IntervalError(String msg) { super(msg); }
    }

    // ---------------------------------------------------------------
    // CancelJob sentinel (returned from a job to unschedule itself)
    // ---------------------------------------------------------------
    /** Sentinel return value from a job that requests its own cancellation. */
    public static final Object CancelJob = new Object() {
        @Override public String toString() { return "CancelJob"; }
    };

    // ---------------------------------------------------------------
    // Default scheduler + module-level shortcuts
    // ---------------------------------------------------------------

    /** Default {@link Scheduler} instance used by the module-level helpers. */
    public static final Scheduler defaultScheduler = null; // initialised in impl

    /** {@code default_scheduler.jobs} — direct list of scheduled jobs. */
    public static List<Job> jobs() { /* ... */ return null; }

    /** Schedules a new job on the default scheduler (Python {@code schedule.every}). */
    public static Job every() { /* ... */ return null; }
    /** Schedules a new job with the given interval. */
    public static Job every(int interval) { /* ... */ return null; }

    /** Runs all pending jobs on the default scheduler. */
    public static void runPending() { /* ... */ }

    /** Runs all jobs on the default scheduler regardless of whether they are due. */
    public static void runAll() { /* ... */ }
    /** Runs all jobs with a delay between each. */
    public static void runAll(int delaySeconds) { /* ... */ }

    /** Returns all jobs (or only those tagged) on the default scheduler. */
    public static List<Job> getJobs() { /* ... */ return null; }
    public static List<Job> getJobs(Object tag) { /* ... */ return null; }

    /** Removes all jobs (or only those tagged) on the default scheduler. */
    public static void clear() { /* ... */ }
    public static void clear(Object tag) { /* ... */ }

    /** Cancels a single job on the default scheduler. */
    public static void cancelJob(Object job) { /* ... */ }

    /** Datetime when the next job should run, or null if no jobs are scheduled. */
    public static LocalDateTime nextRun() { /* ... */ return null; }
    public static LocalDateTime nextRun(Object tag) { /* ... */ return null; }

    /** Number of seconds until the next run, or null if no jobs are scheduled. */
    public static Double idleSeconds() { /* ... */ return null; }

    /** Decorator-style helper.  Adds {@code job} (an unconfigured Job) to the default scheduler. */
    public static Job repeat(Job job, Runnable function) { /* ... */ return null; }
    public static Job repeat(Job job, Supplier<Object> function) { /* ... */ return null; }

    /** Move {@code moment} forward to the next occurrence of the given weekday. */
    public static LocalDateTime moveToNextWeekday(LocalDateTime moment, String weekday) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // Scheduler
    // ---------------------------------------------------------------

    /**
     * Holds and executes a collection of {@link Job}s.
     * Mirrors Python {@code schedule.Scheduler}.
     */
    public static class Scheduler {

        public final List<Job> jobs = new ArrayList<>();

        /** Test-injectable wall-clock; defaults to {@link java.time.Clock#systemDefaultZone()}. */
        public Clock clock;

        public Scheduler() { /* ... */ }
        public Scheduler(Clock clock) { /* ... */ }

        /** Run all jobs that are scheduled to run. */
        public void runPending() { /* ... */ }

        /** Run every job regardless of schedule, with optional delay between jobs. */
        public void runAll() { /* ... */ }
        public void runAll(int delaySeconds) { /* ... */ }

        /** Get scheduled jobs marked with the given tag, or all jobs. */
        public List<Job> getJobs() { /* ... */ return null; }
        public List<Job> getJobs(Object tag) { /* ... */ return null; }

        /** Delete scheduled jobs marked with the given tag, or all jobs. */
        public void clear() { /* ... */ }
        public void clear(Object tag) { /* ... */ }

        /** Delete a scheduled job. */
        public void cancelJob(Object job) { /* ... */ }

        /** Schedule a new periodic job with default interval=1. */
        public Job every() { /* ... */ return null; }
        /** Schedule a new periodic job with the given interval. */
        public Job every(int interval) { /* ... */ return null; }

        /** Datetime when the next job should run, or null if no jobs scheduled. */
        public LocalDateTime getNextRun() { /* ... */ return null; }
        public LocalDateTime getNextRun(Object tag) { /* ... */ return null; }

        /** Number of seconds until {@link #getNextRun()}, or null if no jobs scheduled. */
        public Double idleSeconds() { /* ... */ return null; }
    }

    // ---------------------------------------------------------------
    // Job
    // ---------------------------------------------------------------

    /**
     * A periodic job as used by {@link Scheduler}.
     * Mirrors Python {@code schedule.Job}.
     */
    public static class Job implements Comparable<Job> {
        public int interval;
        public Integer latest;
        public Runnable jobFunc;
        public Supplier<Object> jobFuncSupplier;
        public String jobFuncName;
        public Object[] jobArgs;
        public Map<String, Object> jobKwargs;
        public String unit;
        public LocalTime atTime;
        public ZoneId atTimeZone;
        public LocalDateTime lastRun;
        public LocalDateTime nextRun;
        public String startDay;
        public LocalDateTime cancelAfter;
        public final Set<Object> tags = new HashSet<>();
        public Scheduler scheduler;

        public Job(int interval) { /* ... */ }
        public Job(int interval, Scheduler scheduler) { /* ... */ }

        /** Sortable by next_run (Python {@code __lt__}). */
        @Override public int compareTo(Job other) { /* ... */ return 0; }

        // ---------- time-unit selectors (Python @property) ------------
        public Job second()    { /* ... */ return null; }
        public Job seconds()   { /* ... */ return null; }
        public Job minute()    { /* ... */ return null; }
        public Job minutes()   { /* ... */ return null; }
        public Job hour()      { /* ... */ return null; }
        public Job hours()     { /* ... */ return null; }
        public Job day()       { /* ... */ return null; }
        public Job days()      { /* ... */ return null; }
        public Job week()      { /* ... */ return null; }
        public Job weeks()     { /* ... */ return null; }
        public Job monday()    { /* ... */ return null; }
        public Job tuesday()   { /* ... */ return null; }
        public Job wednesday() { /* ... */ return null; }
        public Job thursday()  { /* ... */ return null; }
        public Job friday()    { /* ... */ return null; }
        public Job saturday()  { /* ... */ return null; }
        public Job sunday()    { /* ... */ return null; }

        /** Tag the job with one or more identifiers. */
        public Job tag(Object... tags) { /* ... */ return null; }

        /** Specify the time at which the job should run.  Format depends on the unit. */
        public Job at(String timeStr) { /* ... */ return null; }
        public Job at(String timeStr, String tz) { /* ... */ return null; }
        public Job at(String timeStr, ZoneId tz) { /* ... */ return null; }

        /** Schedule at a randomized interval between {@code interval} and {@code latest}. */
        public Job to(int latest) { /* ... */ return null; }

        /** Schedule until a specific moment / time / duration. */
        public Job until(LocalDateTime untilTime) { /* ... */ return null; }
        public Job until(LocalTime untilTime)     { /* ... */ return null; }
        public Job until(Duration untilTime)      { /* ... */ return null; }
        public Job until(String untilTime)        { /* ... */ return null; }

        /**
         * Specifies the function to call when the job runs.
         * Renamed from Python `do` (reserved word in Java).
         */
        public Job doIt(Runnable function) { /* ... */ return null; }
        public Job doIt(String name, Runnable function) { /* ... */ return null; }
        public Job doIt(Supplier<Object> function) { /* ... */ return null; }
        public Job doIt(String name, Supplier<Object> function) { /* ... */ return null; }
        /** Variant that accepts positional/keyword args for {@code __str__}/{@code __repr__}. */
        public Job doIt(String name, Runnable function, Object[] args, Map<String, Object> kwargs) { /* ... */ return null; }

        /** True if the job should be run now. */
        public boolean shouldRun() { /* ... */ return false; }

        /** Run the job and immediately reschedule it. */
        public Object run() { /* ... */ return null; }

        /** Compute when this job should run next (sets {@link #nextRun}). */
        public void scheduleNextRun() { /* ... */ }

        /** Internal: align an instant into a different UTC offset (DST handling). */
        public java.time.ZonedDateTime correctUtcOffset(java.time.ZonedDateTime moment, boolean fixateTime) { /* ... */ return null; }
    }
}
