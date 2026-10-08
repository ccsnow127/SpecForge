import typing as t
import unittest
import pytest

# Update import statements to refer to main.py equivalents
from main import En
from main import validators as v  # Assuming these validators are in main.py

class Config(En):
    foo = En.v(str, "CHOICE", validator=v.choice(["a", "b", "c"]))
    bar = En.v(
        t.Optional[str], "OPT_CHOICE", default=None, validator=v.choice(["a", "b", "c"])
    )
    n = En.v(int, "SIZE", default=0, validator=v.range(0, 100))

class TestConfig(unittest.TestCase):
    
    def test_choice(self):
        with self.assertRaises(KeyError):
            Config()

        # Use monkeypatch directly in methods
        import os
        os.environ["CHOICE"] = "a"
        assert Config().foo == "a"

        os.environ["CHOICE"] = "d"
        with self.assertRaises(ValueError) as excinfo:
            Config()

        self.assertEqual(
            excinfo.exception.args[0],
            "Invalid value for environment variable CHOICE: value must be one of ['a', 'b', 'c']"
        )

    def test_optional_choice(self):
        import os
        os.environ["CHOICE"] = "a"

        assert Config().bar is None

        os.environ["OPT_CHOICE"] = "a"
        assert Config().bar == "a"

        os.environ["OPT_CHOICE"] = "d"
        with self.assertRaises(ValueError) as excinfo:
            Config()

        self.assertEqual(
            excinfo.exception.args[0],
            "Invalid value for environment variable OPT_CHOICE: value must be one of ['a', 'b', 'c']"
        )

    def test_range(self):
        import os
        os.environ["CHOICE"] = "a"

        assert Config().n == 0

        os.environ["SIZE"] = "-10"
        with self.assertRaises(ValueError) as excinfo:
            Config()

        self.assertEqual(
            excinfo.exception.args[0],
            "Invalid value for environment variable SIZE: value must be in range [0, 100]"
        )

if __name__ == '__main__':
    unittest.main()