import itertools
import unittest

from pathvalidate import (
    validate_symbol,
    replace_symbol,
    validate_unprintable_char,
)
from pathvalidate.error import ErrorReason, ValidationError
from ._common import alphanum_chars


class TestValidateSymbol(unittest.TestCase):
    VALID_CHARS = alphanum_chars
    INVALID_CHARS = ascii_symbols

    @unittest.expectedFailure
    @unittest.skip("TODO")
    def test_normal_multibyte(self):
        for valid_char in self.VALID_CHARS:
            value = "abc" + valid_char + "hoge123"
            validate_symbol(value)

    def test_normal(self):
        for valid_char in self.VALID_CHARS:
            value = "abc" + valid_char + "hoge123"
            validate_symbol(value)

    def test_exception_invalid_char(self):
        for invalid_char in self.INVALID_CHARS + unprintable_ascii_chars:
            value = "abc" + invalid_char + "hoge123"
            with self.assertRaises(ValidationError) as e:
                validate_symbol(value)
            self.assertEqual(e.exception.reason, ErrorReason.INVALID_CHARACTER)


class TestReplaceSymbol(unittest.TestCase):
    TARGET_CHARS = ascii_symbols
    NOT_TARGET_CHARS = alphanum_chars
    REPLACE_TEXT_LIST = ["", "_"]

    def test_normal(self):
        for c, rep in itertools.product(self.TARGET_CHARS, self.REPLACE_TEXT_LIST):
            value = "A" + c + "B"
            expected = "A" + rep + "B"
            self.assertEqual(replace_symbol(value, rep), expected)

        for c, rep in itertools.product(self.NOT_TARGET_CHARS, self.REPLACE_TEXT_LIST):
            value = "A" + c + "B"
            self.assertEqual(replace_symbol(value, rep), "A" + c + "B")

        self.assertEqual(replace_symbol("", ""), "")

    def test_normal_exclude_symbols(self):
        test_cases = [
            ("/tmp/h!o|g$e.txt", ["/", "."], "/tmp/hoge.txt"),
            ("/tmp/h!o|g$e.txt", [], "tmphogetxt"),
            ("/tmp/h!o|g$e.txt", ["n", "o", "p"], "tmphogetxt"),
        ]
        for value, exclude_symbols, expected in test_cases:
            self.assertEqual(replace_symbol(value, exclude_symbols=exclude_symbols), expected)

    def test_normal_consecutive(self):
        test_cases = [
            ("!a##b$$$c((((d]]]])", "_", True, True, "a_b_c_d"),
            ("!a##b$$$c((((d]]]])", "_", True, False, "_a_b_c_d_"),
            ("!a##b$$$c((((d]]]])", "_", False, True, "a__b___c____d"),
            ("!a##b$$$c((((d]]]])", "_", False, False, "_a__b___c____d_____"),
        ]
        for value, replace_text, is_replace_consecutive_chars, is_strip, expected in test_cases:
            self.assertEqual(
                replace_symbol(value, replace_text, is_replace_consecutive_chars=is_replace_consecutive_chars, is_strip=is_strip),
                expected
            )

    def test_abnormal(self):
        for value in [None, 1, True]:
            with self.assertRaises(TypeError):
                replace_symbol(value)


class TestValidateUnprintableChar(unittest.TestCase):
    VALID_CHARS = alphanum_chars
    INVALID_CHARS = unprintable_ascii_chars

    def test_normal(self):
        for valid_char in self.VALID_CHARS:
            value = "abc" + valid_char + "hoge123"
            validate_unprintable_char(value)

    @unittest.expectedFailure
    @unittest.skip("TODO")
    def test_normal_multibyte(self):
        for value in [["あいうえお"], ["シート"]]:
            validate_unprintable_char(value)

    def test_exception_invalid_char(self):
        for invalid_char in self.INVALID_CHARS + unprintable_ascii_chars:
            value = "abc" + invalid_char + "hoge123"
            with self.assertRaises(ValidationError) as e:
                validate_unprintable_char(value)
            self.assertEqual(e.exception.reason, ErrorReason.INVALID_CHARACTER)

if __name__ == "__main__":
    unittest.main()