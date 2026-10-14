import pytest
import os
import csv
import tempfile
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock

from query_arxiv import (
    fetch_data,
    check_date,
    save_to_csv,
    construct_query_url,
    process_entries,
    print_results,
    get_args,
)

SAMPLE_XML = '''<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <entry>
    <title>Paper One</title>
    <published>{date1}</published>
    <summary>This is the abstract of paper one about machine learning.</summary>
    <id>http://arxiv.org/abs/2301.00001</id>
    <author><name>Alice Smith</name></author>
    <author><name>Bob Jones</name></author>
  </entry>
  <entry>
    <title>Paper Two</title>
    <published>{date2}</published>
    <summary>This is the abstract of paper two about deep learning.</summary>
    <id>http://arxiv.org/abs/2301.00002</id>
    <author><name>Charlie Brown</name></author>
  </entry>
</feed>'''


class TestCheckDate:
    def test_within_recent_days(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        date_str = "2024-01-14T10:00:00Z"
        assert check_date(date_str, 7, now) is True

    def test_outside_recent_days(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        date_str = "2024-01-01T10:00:00Z"
        assert check_date(date_str, 7, now) is False

    def test_exact_boundary(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        date_str = "2024-01-08T12:00:00Z"
        assert check_date(date_str, 7, now) is True

    def test_same_day(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        date_str = "2024-01-15T10:00:00Z"
        assert check_date(date_str, 1, now) is True


class TestConstructQueryUrl:
    def test_category_only(self):
        url = construct_query_url(category="cs.CL")
        assert "search_query=cat:cs.CL" in url
        assert "sortBy=submittedDate" in url
        assert "sortOrder=descending" in url

    def test_multiple_params(self):
        url = construct_query_url(category="cs.CL", title="attention")
        assert "cat:cs.CL" in url
        assert "ti:attention" in url
        assert "+AND+" in url

    def test_max_results(self):
        url = construct_query_url(category="cs.CL", max_results=50)
        assert "max_results=50" in url

    def test_no_params_raises(self):
        with pytest.raises(ValueError, match="You must specify at least one argument"):
            construct_query_url()

    def test_non_ascii_raises(self):
        with pytest.raises(ValueError):
            construct_query_url(title="café")

    def test_author_param(self):
        url = construct_query_url(author="Smith")
        assert "au:Smith" in url

    def test_abstract_param(self):
        url = construct_query_url(abstract="transformer")
        assert "abs:transformer" in url

    def test_default_max_results(self):
        url = construct_query_url(category="cs.AI")
        assert "max_results=100" in url


class TestProcessEntries:
    def _make_entries(self, date1, date2):
        xml = SAMPLE_XML.format(date1=date1, date2=date2)
        root = ET.fromstring(xml)
        namespace = {'default': 'http://www.w3.org/2005/Atom'}
        entries = root.findall('default:entry', namespace)
        return entries, namespace

    def test_all_recent(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        d1 = "2024-01-14T10:00:00Z"
        d2 = "2024-01-13T10:00:00Z"
        entries, ns = self._make_entries(d1, d2)
        papers = process_entries(entries, ns, now, 7)
        assert len(papers) == 2
        assert papers[0]["title"] == "Paper One"
        assert papers[1]["title"] == "Paper Two"

    def test_authors_joined(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        d1 = "2024-01-14T10:00:00Z"
        d2 = "2024-01-13T10:00:00Z"
        entries, ns = self._make_entries(d1, d2)
        papers = process_entries(entries, ns, now, 7)
        assert papers[0]["authors"] == "Alice Smith, Bob Jones"
        assert papers[1]["authors"] == "Charlie Brown"

    def test_breaks_on_old_entry(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        d1 = "2024-01-14T10:00:00Z"
        d2 = "2023-12-01T10:00:00Z"
        entries, ns = self._make_entries(d1, d2)
        papers = process_entries(entries, ns, now, 7)
        assert len(papers) == 1

    def test_no_recent_entries(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        d1 = "2023-12-01T10:00:00Z"
        d2 = "2023-11-01T10:00:00Z"
        entries, ns = self._make_entries(d1, d2)
        papers = process_entries(entries, ns, now, 7)
        assert len(papers) == 0

    def test_paper_has_link(self):
        now = datetime(2024, 1, 15, 12, 0, 0)
        d1 = "2024-01-14T10:00:00Z"
        d2 = "2024-01-13T10:00:00Z"
        entries, ns = self._make_entries(d1, d2)
        papers = process_entries(entries, ns, now, 7)
        assert papers[0]["link"] == "http://arxiv.org/abs/2301.00001"


class TestSaveToCsv:
    def test_save_basic(self, tmp_path):
        papers = [{"title": "Paper A", "link": "http://example.com"}]
        fpath = str(tmp_path / "out.csv")
        save_to_csv(papers, fpath)
        with open(fpath, "r") as f:
            reader = csv.DictReader(f)
            rows = list(reader)
        assert len(rows) == 1
        assert rows[0]["title"] == "Paper A"

    def test_save_empty_prints(self, capsys):
        save_to_csv([], "dummy.csv")
        captured = capsys.readouterr()
        assert "No papers to save" in captured.out

    def test_save_creates_dirs(self, tmp_path):
        papers = [{"title": "Paper B"}]
        fpath = str(tmp_path / "subdir" / "out.csv")
        save_to_csv(papers, fpath)
        assert os.path.exists(fpath)


class TestPrintResults:
    def test_print_output(self, capsys):
        papers = [{
            "title": "Test Paper",
            "authors": "Author A",
            "abstract": "Short abstract.",
            "published": "2024-01-14T10:00:00Z",
            "link": "http://arxiv.org/abs/0001"
        }]
        print_results(papers)
        captured = capsys.readouterr()
        assert "Test Paper" in captured.out
        assert "Author A" in captured.out
        assert "--------------------------" in captured.out


class TestGetArgs:
    def test_required_recent_days(self):
        with pytest.raises(SystemExit):
            get_args(["--category", "cs.CL"])

    def test_parse_basic(self):
        args = get_args(["--category", "cs.CL", "--recent_days", "7"])
        assert args.category == "cs.CL"
        assert args.recent_days == 7
        assert args.max_results == 10

    def test_verbose_flag(self):
        args = get_args(["--category", "cs.CL", "--recent_days", "7", "--verbose"])
        assert args.verbose is True

    def test_to_file(self):
        args = get_args(["--category", "cs.CL", "--recent_days", "7", "--to_file", "out.csv"])
        assert args.to_file == "out.csv"


class TestFetchData:
    def test_fetch_data_calls_urlopen(self):
        mock_response = MagicMock()
        mock_response.read.return_value = b"<xml>test</xml>"
        mock_response.__enter__ = MagicMock(return_value=mock_response)
        mock_response.__exit__ = MagicMock(return_value=False)
        with patch("query_arxiv.urllib.request.urlopen", return_value=mock_response) as mock_open:
            result = fetch_data("http://example.com/api")
            mock_open.assert_called_once_with("http://example.com/api")
            assert result == b"<xml>test</xml>"
