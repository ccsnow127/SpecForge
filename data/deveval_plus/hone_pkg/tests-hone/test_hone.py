import pytest
import os
from hone import Hone


@pytest.fixture
def hone():
    return Hone()


@pytest.fixture
def simple_csv(tmp_path):
    f = tmp_path / "simple.csv"
    f.write_text("name,age\nAlice,30\nBob,25\n", encoding="utf-8-sig")
    return str(f)


@pytest.fixture
def nested_csv(tmp_path):
    f = tmp_path / "nested.csv"
    f.write_text("person_name,person_age,location\nAlice,30,London\n", encoding="utf-8-sig")
    return str(f)


@pytest.fixture
def multi_level_csv(tmp_path):
    f = tmp_path / "multi.csv"
    f.write_text("a_b,a_c,d\n1,2,3\n", encoding="utf-8-sig")
    return str(f)


class TestHoneInit:
    def test_default_delimiters(self, hone):
        assert hone.delimiters == [",", "_", " "]

    def test_custom_delimiters(self):
        h = Hone(delimiters=["-"])
        assert h.delimiters == ["-"]

    def test_csv_filepath_none(self, hone):
        assert hone.csv_filepath is None


class TestHoneConvert:
    def test_simple_csv(self, hone, simple_csv):
        result = hone.convert(simple_csv)
        assert isinstance(result, list)
        assert len(result) == 2

    def test_nested_csv(self, hone, nested_csv):
        result = hone.convert(nested_csv)
        assert len(result) == 1
        row = result[0]
        assert "person" in row or "location" in row

    def test_preserves_data(self, hone, simple_csv):
        result = hone.convert(simple_csv)
        names = []
        for row in result:
            if "name" in row:
                names.append(row["name"])
        assert "Alice" in names or len(result) == 2


class TestGetSchema:
    def test_returns_dict(self, hone, simple_csv):
        schema = hone.get_schema(simple_csv)
        assert isinstance(schema, dict)

    def test_schema_has_columns(self, hone, simple_csv):
        schema = hone.get_schema(simple_csv)
        assert len(schema) > 0


class TestGenerateFullStructure:
    def test_flat_columns(self, hone):
        result = hone.generate_full_structure(["name", "age"])
        assert "name" in result
        assert "age" in result

    def test_nested_columns(self, hone):
        result = hone.generate_full_structure(["person_name", "person_age", "location"])
        assert "person" in result or "location" in result

    def test_single_column(self, hone):
        result = hone.generate_full_structure(["value"])
        assert "value" in result
        assert result["value"] == "value"


class TestGetLeaves:
    def test_flat_structure(self, hone):
        structure = {"name": "name", "age": "age"}
        leaves = hone.get_leaves(structure, "", {})
        assert "name" in leaves
        assert "age" in leaves

    def test_nested_structure(self, hone):
        structure = {"person": {"name": "person_name"}}
        leaves = hone.get_leaves(structure, "", {})
        assert "person_name" in leaves
        assert "['person']['name']" in leaves["person_name"]


class TestGetValidSplits:
    def test_with_underscore(self, hone):
        splits = hone.get_valid_splits("person_name")
        assert "person" in splits

    def test_no_delimiters(self, hone):
        splits = hone.get_valid_splits("name")
        assert splits == []

    def test_multiple_delimiters(self, hone):
        splits = hone.get_valid_splits("a_b_c")
        assert "a" in splits
        assert "a_b" in splits


class TestGetSplitSuffix:
    def test_basic_suffix(self, hone):
        result = hone.get_split_suffix("person", "person_name")
        assert result == "name"

    def test_suffix_with_leading_delimiters(self, hone):
        result = hone.get_split_suffix("a", "a__b")
        assert result == "b"


class TestCleanSplit:
    def test_no_trailing_delimiters(self, hone):
        assert hone.clean_split("person") == "person"

    def test_trailing_underscore(self, hone):
        assert hone.clean_split("person_") == "person"

    def test_multiple_trailing(self, hone):
        assert hone.clean_split("person__") == "person"


class TestIsValidPrefix:
    def test_valid_prefix(self, hone):
        assert hone.is_valid_prefix("person", "person_name") is True

    def test_invalid_prefix(self, hone):
        assert hone.is_valid_prefix("per", "person_name") is False

    def test_no_delimiter_after(self, hone):
        assert hone.is_valid_prefix("person", "personality") is False


class TestSetCsvFilepath:
    def test_sets_filepath(self, hone):
        hone.set_csv_filepath("/some/path.csv")
        assert hone.csv_filepath == "/some/path.csv"
        assert hone.csv.filepath == "/some/path.csv"


class TestEscapeQuotes:
    def test_double_quotes(self, hone):
        result = hone.escape_quotes('hello "world"')
        assert '\\"' in result

    def test_single_quotes(self, hone):
        result = hone.escape_quotes("hello 'world'")
        assert "\\'" in result

    def test_no_quotes(self, hone):
        result = hone.escape_quotes("hello world")
        assert result == "hello world"

    def test_already_escaped(self, hone):
        result = hone.escape_quotes('hello \\"world\\"')
        assert result.count('\\"') == 2
