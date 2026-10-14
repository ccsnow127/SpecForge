import itertools
import unittest

from main import sanitize_ltsv_label, validate_ltsv_label
from main.error import ErrorReason, ValidationError

from ._common import INVALID_WIN_FILENAME_CHARS, alphanum_chars


VALID_LABEL_CHARS = alphanum_chars + ("_", ".", "-")
INVALID_LABEL_CHARS = INVALID_WIN_FILENAME_CHARS + (
    "!",
    "#",
    "$",
    "&",
    "'",
    "=",
    "~",
    "^",
    "@",
    "`",
    "[",
    "]",
    "+",
    ";",
    "{",
    "}",
    ",",
    "(",
    ")",
    "%",
    " ",
    "\t",
    "\n",
    "\r",
    "\f",
    "\v",
)


class TestValidateLTSVLabel(unittest.TestCase):
    VALID_CHARS = alphanum_chars
    INVALID_CHARS = INVALID_LABEL_CHARS

    @unittest.expectedFailure
    @unittest.skip("Example of expected failure and skip usage")
    def test_normal(self):
        for valid_char in self.VALID_CHARS:
            with self.subTest(valid_char=valid_char):
                validate_ltsv_label("abc" + valid_char + "hoge123")

    def test_exception_invalid_char(self):
        for invalid_char in self.INVALID_CHARS:
            with self.subTest(invalid_char=invalid_char):
                with self.assertRaises(ValidationError) as e:
                    validate_ltsv_label("abc" + invalid_char + "hoge123")
                self.assertEqual(e.exception.reason, ErrorReason.INVALID_CHARACTER)

        # Testing multibyte characters
        for multibyte in ["あいうえお", "ラベル"]:
            with self.subTest(multibyte=multibyte):
                with self.assertRaises(ValidationError) as e:
                    validate_ltsv_label(multibyte)
                self.assertEqual(e.exception.reason, ErrorReason.INVALID_CHARACTER)


class TestSanitizeLTSVLabel(unittest.TestCase):
    TARGET_CHARS = INVALID_LABEL_CHARS
    NOT_TARGET_CHARS = alphanum_chars
    REPLACE_TEXT_LIST = ["", "_"]

    def test_normal(self):
        for c, rep in itertools.product(self.TARGET_CHARS, self.REPLACE_TEXT_LIST):
            with self.subTest(c=c, rep=rep):
                self.assertEqual(sanitize_ltsv_label("A" + c + "B", rep), "A" + rep + "B")

        for c, rep in itertools.product(self.NOT_TARGET_CHARS, self.REPLACE_TEXT_LIST):
            with self.subTest(c=c, rep=rep):
                self.assertEqual(sanitize_ltsv_label("A" + c + "B", rep), "A" + c + "B")

    def test_normal_multibyte(self):
        value = "aあいbうえcお"
        expected = "abc"
        self.assertEqual(sanitize_ltsv_label(value), expected)

    def test_abnormal(self):
        abnormal_cases = [
            ("", ValidationError),
            (None, ValidationError),
            (1, TypeError),
            (True, TypeError),
        ]
        for value, expected in abnormal_cases:
            with self.subTest(value=value, expected=expected):
                with self.assertRaises(expected):
                    sanitize_ltsv_label(value)

if __name__ == "__main__":
    unittest.main()