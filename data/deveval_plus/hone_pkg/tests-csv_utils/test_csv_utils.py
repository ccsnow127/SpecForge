import pytest
import os
from csv_utils import CSVUtils


@pytest.fixture
def sample_csv(tmp_path):
    f = tmp_path / "sample.csv"
    f.write_text("name,age,city\nAlice,30,London\nBob,25,Paris\n", encoding="utf-8-sig")
    return str(f)


@pytest.fixture
def single_row_csv(tmp_path):
    f = tmp_path / "single.csv"
    f.write_text("col1,col2\nval1,val2\n", encoding="utf-8-sig")
    return str(f)


class TestCSVUtilsInit:
    def test_init_stores_filepath(self):
        cu = CSVUtils("/some/path.csv")
        assert cu.filepath == "/some/path.csv"

    def test_init_none_filepath(self):
        cu = CSVUtils(None)
        assert cu.filepath is None


class TestGetColumnNames:
    def test_basic_columns(self, sample_csv):
        cu = CSVUtils(sample_csv)
        cols = cu.get_column_names()
        assert cols == ["name", "age", "city"]

    def test_two_columns(self, single_row_csv):
        cu = CSVUtils(single_row_csv)
        cols = cu.get_column_names()
        assert cols == ["col1", "col2"]


class TestGetDataRows:
    def test_basic_rows(self, sample_csv):
        cu = CSVUtils(sample_csv)
        rows = cu.get_data_rows()
        assert len(rows) == 2
        assert rows[0] == ["Alice", "30", "London"]
        assert rows[1] == ["Bob", "25", "Paris"]

    def test_single_row(self, single_row_csv):
        cu = CSVUtils(single_row_csv)
        rows = cu.get_data_rows()
        assert len(rows) == 1
        assert rows[0] == ["val1", "val2"]

    def test_excludes_header(self, sample_csv):
        cu = CSVUtils(sample_csv)
        rows = cu.get_data_rows()
        assert ["name", "age", "city"] not in rows


class TestOpenCsv:
    def test_context_manager(self, sample_csv):
        cu = CSVUtils(sample_csv)
        with cu.open_csv() as f:
            lines = list(f)
            assert len(lines) > 0
