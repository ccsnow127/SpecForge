import pytest
from datetime import timedelta
from result import Result


class TestResultInit:
    def test_init_stores_wpm(self):
        r = Result(seconds=60, wpm=200)
        assert r.wpm == 200

    def test_init_creates_delta(self):
        r = Result(seconds=120, wpm=200)
        assert isinstance(r.delta, timedelta)
        assert r.delta == timedelta(seconds=120)

    def test_init_default_wpm_none(self):
        r = Result(seconds=30)
        assert r.wpm is None

    def test_init_zero_seconds(self):
        r = Result(seconds=0, wpm=100)
        assert r.delta == timedelta(seconds=0)


class TestResultSeconds:
    def test_seconds_basic(self):
        r = Result(seconds=90, wpm=200)
        assert r.seconds == 90

    def test_seconds_zero(self):
        r = Result(seconds=0, wpm=200)
        assert r.seconds == 0

    def test_seconds_large(self):
        r = Result(seconds=3600, wpm=200)
        assert r.seconds == 3600

    def test_seconds_returns_int(self):
        r = Result(seconds=90, wpm=200)
        assert isinstance(r.seconds, int)


class TestResultMinutes:
    def test_minutes_exact(self):
        r = Result(seconds=120, wpm=200)
        assert r.minutes == 2

    def test_minutes_rounds_up(self):
        r = Result(seconds=61, wpm=200)
        assert r.minutes == 2

    def test_minutes_minimum_one(self):
        r = Result(seconds=0, wpm=200)
        assert r.minutes == 1

    def test_minutes_one_second(self):
        r = Result(seconds=1, wpm=200)
        assert r.minutes == 1

    def test_minutes_59_seconds(self):
        r = Result(seconds=59, wpm=200)
        assert r.minutes == 1

    def test_minutes_60_seconds(self):
        r = Result(seconds=60, wpm=200)
        assert r.minutes == 1


class TestResultText:
    def test_text_one_min(self):
        r = Result(seconds=60, wpm=200)
        assert r.text == '1 min'

    def test_text_multiple_mins(self):
        r = Result(seconds=300, wpm=200)
        assert r.text == '5 min'

    def test_text_rounds_up(self):
        r = Result(seconds=121, wpm=200)
        assert r.text == '3 min'


class TestResultRepr:
    def test_repr_format(self):
        r = Result(seconds=60, wpm=200)
        assert repr(r) == '1 min read'

    def test_str_same_as_repr(self):
        r = Result(seconds=120, wpm=200)
        assert str(r) == repr(r)

    def test_repr_large_time(self):
        r = Result(seconds=600, wpm=200)
        assert repr(r) == '10 min read'


class TestResultOperators:
    def test_add_results(self):
        r1 = Result(seconds=60, wpm=200)
        r2 = Result(seconds=120, wpm=200)
        r3 = r1 + r2
        assert isinstance(r3, Result)
        assert r3.seconds == 180

    def test_subtract_results(self):
        r1 = Result(seconds=120, wpm=200)
        r2 = Result(seconds=60, wpm=200)
        r3 = r1 - r2
        assert isinstance(r3, Result)
