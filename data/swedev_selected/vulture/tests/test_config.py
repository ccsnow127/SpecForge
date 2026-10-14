"""
Unit tests for config file and CLI argument parsing.
"""

from io import BytesIO
import pathlib
from textwrap import dedent
import unittest
import pytest

# Update the imports to reference the objects from main.py
from main import (
    DEFAULTS,
    _parse_args,
    _parse_toml,
    make_config,
    InputError,
)

class TestConfigFileAndCLIParsing(unittest.TestCase):
    def get_toml_bytes(self, toml_str: str) -> BytesIO:
        """
        Wrap a string in BytesIO to play the role of the incoming config stream.
        """
        return BytesIO(bytes(toml_str, "utf-8"))

    def test_cli_args(self):
        """
        Ensure that CLI arguments are converted to a config object.
        """
        expected = dict(
            paths=["path1", "path2"],
            exclude=["file*.py", "dir/"],
            ignore_decorators=["deco1", "deco2"],
            ignore_names=["name1", "name2"],
            config="pyproject.toml",
            make_whitelist=True,
            min_confidence=10,
            sort_by_size=True,
            verbose=True,
        )
        result = _parse_args(
            [
                "--exclude=file*.py,dir/",
                "--ignore-decorators=deco1,deco2",
                "--ignore-names=name1,name2",
                "--make-whitelist",
                "--min-confidence=10",
                "--sort-by-size",
                "--verbose",
                "path1",
                "path2",
            ]
        )
        self.assertIsInstance(result, dict)
        self.assertEqual(result, expected)

    def test_toml_config(self):
        """
        Ensure parsing of TOML files results in a valid config object.
        """
        expected = dict(
            paths=["path1", "path2"],
            exclude=["file*.py", "dir/"],
            ignore_decorators=["deco1", "deco2"],
            ignore_names=["name1", "name2"],
            make_whitelist=True,
            min_confidence=10,
            sort_by_size=True,
            verbose=True,
        )
        data = self.get_toml_bytes(
            dedent(
                """\
            [tool.vulture]
            exclude = ["file*.py", "dir/"]
            ignore_decorators = ["deco1", "deco2"]
            ignore_names = ["name1", "name2"]
            make_whitelist = true
            min_confidence = 10
            sort_by_size = true
            verbose = true
            paths = ["path1", "path2"]
            """
            )
        )
        result = _parse_toml(data)
        self.assertIsInstance(result, dict)
        self.assertEqual(result, expected)

    def test_toml_config_with_heterogenous_array(self):
        """
        Ensure parsing of TOML files results in a valid config object, even if some
        other part of the file contains an array of mixed types.
        """
        expected = dict(
            paths=["path1", "path2"],
            exclude=["file*.py", "dir/"],
            ignore_decorators=["deco1", "deco2"],
            ignore_names=["name1", "name2"],
            make_whitelist=True,
            min_confidence=10,
            sort_by_size=True,
            verbose=True,
        )
        data = self.get_toml_bytes(
            dedent(
                """\
            [tool.foo]
            # comment for good measure
            problem_array = [{a = 1}, [2,3,4], "foo"]

            [tool.vulture]
            exclude = ["file*.py", "dir/"]
            ignore_decorators = ["deco1", "deco2"]
            ignore_names = ["name1", "name2"]
            make_whitelist = true
            min_confidence = 10
            sort_by_size = true
            verbose = true
            paths = ["path1", "path2"]
            """
            )
        )
        result = _parse_toml(data)
        self.assertIsInstance(result, dict)
        self.assertEqual(result, expected)

    def test_config_merging(self):
        """
        If we have both CLI args and a ``pyproject.toml`` file, the CLI args should
        have precedence.
        """
        toml = self.get_toml_bytes(
            dedent(
                """\
            [tool.vulture]
            exclude = ["toml_exclude"]
            ignore_decorators = ["toml_deco"]
            ignore_names = ["toml_name"]
            make_whitelist = false
            min_confidence = 10
            sort_by_size = false
            verbose = false
            paths = ["toml_path"]
            """
            )
        )
        cliargs = [
            "--exclude=cli_exclude",
            "--ignore-decorators=cli_deco",
            "--ignore-names=cli_name",
            "--make-whitelist",
            "--min-confidence=20",
            "--sort-by-size",
            "--verbose",
            "cli_path",
        ]
        result = make_config(cliargs, toml)
        expected = dict(
            paths=["cli_path"],
            exclude=["cli_exclude"],
            ignore_decorators=["cli_deco"],
            ignore_names=["cli_name"],
            config="pyproject.toml",
            make_whitelist=True,
            min_confidence=20,
            sort_by_size=True,
            verbose=True,
        )
        self.assertEqual(result, expected)

    def test_toml_config_custom_path(self):
        """
        Ensure that TOML pyproject.toml files can be read from a custom path,
        other than the current working directory.

        Test file is in tests/toml/mock_pyproject.toml
        """
        here = pathlib.Path(__file__).parent
        tomlfile_path = here.joinpath("toml", "mock_pyproject.toml")
        cliargs = [
            f"--config={tomlfile_path}",
            "cli_path",
        ]
        result = make_config(cliargs)
        self.assertEqual(result["ignore_names"], ["name_from_toml_file"])

    def test_config_merging_missing(self):
        """
        If we have set a boolean value in the TOML file, but not on the CLI, we
        want the TOML value to be taken.
        """
        toml = self.get_toml_bytes(
            dedent(
                """\
            [tool.vulture]
            verbose = true
            ignore_names = ["name1"]
            """
            )
        )
        cliargs = [
            "cli_path",
        ]
        result = make_config(cliargs, toml)
        self.assertTrue(result["verbose"])
        self.assertEqual(result["ignore_names"], ["name1"])

    def test_config_merging_toml_paths_only(self):
        """
        If we have paths in the TOML but not on the CLI, the TOML paths should be
        used.
        """
        toml = self.get_toml_bytes(
            dedent(
                """\
            [tool.vulture]
            paths = ["path1", "path2"]
            """
            )
        )
        cliargs = [
            "--exclude=test_*.py",
        ]
        result = make_config(cliargs, toml)
        self.assertEqual(result["paths"], ["path1", "path2"])
        self.assertEqual(result["exclude"], ["test_*.py"])

    def test_invalid_config_options_output(self):
        """
        If the config file contains unknown options we want to abort.
        """

        with self.assertRaises(InputError):
            _check_input_config({"unknown_key_1": 1})

    @pytest.mark.parametrize("key, value", list(DEFAULTS.items()))
    def test_incompatible_option_type(self, key, value):
        """
        If a config value has a different type from the default value we abort.
        """
        wrong_types = {int, str, list, bool} - {type(value)}
        for wrong_type in wrong_types:
            test_value = wrong_type()
            with self.assertRaises(InputError):
                _check_input_config({key: test_value})

    def test_missing_paths(self):
        """
        If the script is run without any paths, we want to abort.
        """
        with self.assertRaises(InputError):
            make_config([])

# To run the tests
if __name__ == "__main__":
    unittest.main()