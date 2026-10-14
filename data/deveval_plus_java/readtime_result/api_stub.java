// API Stub — class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.time.Duration;

public class Result {

    public Result(int seconds, Integer wpm) { /* ... */ }
    public Result(int seconds) { /* ... */ }

    public Integer getWpm() { /* ... */ }
    public Duration getDelta() { /* ... */ }
    public int getSeconds() { /* returns total seconds as int */ }
    public int getMinutes() { /* ceiling division, min 1 */ }
    public String getText() { /* "{minutes} min" */ }

    @Override
    public String toString() { /* "{minutes} min read" */ }

    public Result add(Result other) { /* sum deltas, return new Result */ }
    public Result subtract(Result other) { /* diff deltas, return new Result */ }
}
