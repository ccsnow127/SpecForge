import re
import unittest
from pathvalidate import ErrorReason, ValidationError
from pathvalidate.handler import (
    raise_error,
    NullValueHandler,
    ReservedNameHandler,
)

timestamp_regexp = re.compile(r"^\d+\.\d+$")


class TestRaiseError(unittest.TestCase):
    @unittest.skipUnless(True, 'Skipping for illustration purposes')
    def test_normal(self):
        exceptions = [
            ValidationError(
                description="hoge",
                reason=ErrorReason.INVALID_CHARACTER,
            ),
            ValidationError(
                description="foo",
                reason=ErrorReason.INVALID_AFTER_SANITIZE,
            ),
        ]
        
        for exception in exceptions:
            with self.assertRaises(ValidationError) as e:
                raise_error(exception)
            self.assertEqual(exception, e.exception)


class TestNullValueHandler(unittest.TestCase):
    @unittest.skipUnless(True, 'Skipping for illustration purposes')
    def test_return_null_string(self):
        exceptions = [
            ValidationError(
                description="hoge",
                reason=ErrorReason.INVALID_CHARACTER,
            ),
            ValidationError(
                description="foo",
                reason=ErrorReason.INVALID_AFTER_SANITIZE,
            ),
        ]
        
        for exception in exceptions:
            self.assertEqual(NullValueHandler.return_null_string(exception), "")

    @unittest.skipUnless(True, 'Skipping for illustration purposes')
    def test_return_timestamp(self):
        exceptions = [
            ValidationError(
                description="hoge",
                reason=ErrorReason.INVALID_CHARACTER,
            ),
            ValidationError(
                description="foo",
                reason=ErrorReason.INVALID_AFTER_SANITIZE,
            ),
        ]
        
        for exception in exceptions:
            self.assertIsNotNone(timestamp_regexp.search(NullValueHandler.return_timestamp(exception)))


class TestReservedNameHandler(unittest.TestCase):
    @unittest.skipUnless(True, 'Skipping for illustration purposes')
    def test_add_leading_underscore(self):
        test_cases = [
            (
                ValidationError(
                    description="not reusable reserved name",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name="hoge",
                ),
                "_hoge",
            ),
            (
                ValidationError(
                    description="do nothing to reusable reserved name",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=True,
                    reserved_name="hoge",
                ),
                "hoge",
            ),
            (
                ValidationError(
                    description="do nothing to dot",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name=".",
                ),
                ".",
            ),
            (
                ValidationError(
                    description="do nothing to double dot",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name="..",
                ),
                "..",
            ),
        ]

        for exception, expected in test_cases:
            self.assertEqual(ReservedNameHandler.add_leading_underscore(exception), expected)

    @unittest.skipUnless(True, 'Skipping for illustration purposes')
    def test_add_trailing_underscore(self):
        test_cases = [
            (
                ValidationError(
                    description="not reusable reserved name",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name="hoge",
                ),
                "hoge_",
            ),
            (
                ValidationError(
                    description="do nothing to reusable reserved name",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=True,
                    reserved_name="hoge",
                ),
                "hoge",
            ),
            (
                ValidationError(
                    description="do nothing to dot",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name=".",
                ),
                ".",
            ),
            (
                ValidationError(
                    description="do nothing to double dot",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name="..",
                ),
                "..",
            ),
        ]

        for exception, expected in test_cases:
            self.assertEqual(ReservedNameHandler.add_trailing_underscore(exception), expected)

    @unittest.skipUnless(True, 'Skipping for illustration purposes')
    def test_as_is(self):
        test_cases = [
            (
                ValidationError(
                    description="not reusable reserved name",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name="hoge",
                ),
                "hoge",
            ),
            (
                ValidationError(
                    description="reusable reserved name",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=True,
                    reserved_name="hoge",
                ),
                "hoge",
            ),
            (
                ValidationError(
                    description="dot",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name=".",
                ),
                ".",
            ),
            (
                ValidationError(
                    description="double dot",
                    reason=ErrorReason.RESERVED_NAME,
                    reusable_name=False,
                    reserved_name="..",
                ),
                "..",
            ),
        ]

        for exception, expected in test_cases:
            self.assertEqual(ReservedNameHandler.as_is(exception), expected)


if __name__ == '__main__':
    unittest.main()