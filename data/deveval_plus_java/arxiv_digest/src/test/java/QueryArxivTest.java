import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JUnit 5 tests for QueryArxiv -- one-to-one mapping to every Python test in
 * test_query_arxiv.py.
 */
public class QueryArxivTest {

    // ---------------------------------------------------------------
    // Shared sample XML template (mirrors Python SAMPLE_XML)
    // ---------------------------------------------------------------
    private static final String SAMPLE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
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
            </feed>""";

    /** Parse the template XML into a DOM and return entry NodeList. */
    private NodeList makeEntries(String date1, String date2) throws Exception {
        String xml = SAMPLE_XML.replace("{date1}", date1).replace("{date2}", date2);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        return doc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
    }

    // ===================  TestCheckDate  ===================

    @Test // test_within_recent_days -> check_date
    void testWithinRecentDays() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        assertTrue(QueryArxiv.checkDate("2024-01-14T10:00:00Z", 7, now));
    }

    @Test // test_outside_recent_days -> check_date
    void testOutsideRecentDays() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        assertFalse(QueryArxiv.checkDate("2024-01-01T10:00:00Z", 7, now));
    }

    @Test // test_exact_boundary -> check_date
    void testExactBoundary() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        assertTrue(QueryArxiv.checkDate("2024-01-08T12:00:00Z", 7, now));
    }

    @Test // test_same_day -> check_date
    void testSameDay() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        assertTrue(QueryArxiv.checkDate("2024-01-15T10:00:00Z", 1, now));
    }

    // ===================  TestConstructQueryUrl  ===================

    @Test // test_category_only -> construct_query_url
    void testCategoryOnly() {
        String url = QueryArxiv.constructQueryUrl("cs.CL", null, null, null);
        assertTrue(url.contains("search_query=cat:cs.CL"));
        assertTrue(url.contains("sortBy=submittedDate"));
        assertTrue(url.contains("sortOrder=descending"));
    }

    @Test // test_multiple_params -> construct_query_url
    void testMultipleParams() {
        String url = QueryArxiv.constructQueryUrl("cs.CL", "attention", null, null);
        assertTrue(url.contains("cat:cs.CL"));
        assertTrue(url.contains("ti:attention"));
        assertTrue(url.contains("+AND+"));
    }

    @Test // test_max_results -> construct_query_url
    void testMaxResults() {
        String url = QueryArxiv.constructQueryUrl("cs.CL", null, null, null, 50);
        assertTrue(url.contains("max_results=50"));
    }

    @Test // test_no_params_raises -> construct_query_url
    void testNoParamsRaises() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> QueryArxiv.constructQueryUrl(null, null, null, null));
        assertTrue(ex.getMessage().contains("You must specify at least one argument"));
    }

    @Test // test_non_ascii_raises -> construct_query_url
    void testNonAsciiRaises() {
        assertThrows(IllegalArgumentException.class,
                () -> QueryArxiv.constructQueryUrl(null, "caf\u00e9", null, null));
    }

    @Test // test_author_param -> construct_query_url
    void testAuthorParam() {
        String url = QueryArxiv.constructQueryUrl(null, null, "Smith", null);
        assertTrue(url.contains("au:Smith"));
    }

    @Test // test_abstract_param -> construct_query_url
    void testAbstractParam() {
        String url = QueryArxiv.constructQueryUrl(null, null, null, "transformer");
        assertTrue(url.contains("abs:transformer"));
    }

    @Test // test_default_max_results -> construct_query_url
    void testDefaultMaxResults() {
        String url = QueryArxiv.constructQueryUrl("cs.AI", null, null, null);
        assertTrue(url.contains("max_results=100"));
    }

    // ===================  TestProcessEntries  ===================

    @Test // test_all_recent -> process_entries
    void testAllRecent() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        NodeList entries = makeEntries("2024-01-14T10:00:00Z", "2024-01-13T10:00:00Z");
        List<Map<String, String>> papers = QueryArxiv.processEntries(entries, now, 7);
        assertEquals(2, papers.size());
        assertEquals("Paper One", papers.get(0).get("title"));
        assertEquals("Paper Two", papers.get(1).get("title"));
    }

    @Test // test_authors_joined -> process_entries
    void testAuthorsJoined() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        NodeList entries = makeEntries("2024-01-14T10:00:00Z", "2024-01-13T10:00:00Z");
        List<Map<String, String>> papers = QueryArxiv.processEntries(entries, now, 7);
        assertEquals("Alice Smith, Bob Jones", papers.get(0).get("authors"));
        assertEquals("Charlie Brown", papers.get(1).get("authors"));
    }

    @Test // test_breaks_on_old_entry -> process_entries
    void testBreaksOnOldEntry() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        NodeList entries = makeEntries("2024-01-14T10:00:00Z", "2023-12-01T10:00:00Z");
        List<Map<String, String>> papers = QueryArxiv.processEntries(entries, now, 7);
        assertEquals(1, papers.size());
    }

    @Test // test_no_recent_entries -> process_entries
    void testNoRecentEntries() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        NodeList entries = makeEntries("2023-12-01T10:00:00Z", "2023-11-01T10:00:00Z");
        List<Map<String, String>> papers = QueryArxiv.processEntries(entries, now, 7);
        assertEquals(0, papers.size());
    }

    @Test // test_paper_has_link -> process_entries
    void testPaperHasLink() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        NodeList entries = makeEntries("2024-01-14T10:00:00Z", "2024-01-13T10:00:00Z");
        List<Map<String, String>> papers = QueryArxiv.processEntries(entries, now, 7);
        assertEquals("http://arxiv.org/abs/2301.00001", papers.get(0).get("link"));
    }

    // ===================  TestSaveToCsv  ===================

    @Test // test_save_basic -> save_to_csv
    void testSaveBasic(@TempDir Path tmpDir) throws Exception {
        List<Map<String, String>> papers = new ArrayList<>();
        Map<String, String> p = new LinkedHashMap<>();
        p.put("title", "Paper A");
        p.put("link", "http://example.com");
        papers.add(p);

        String fpath = tmpDir.resolve("out.csv").toString();
        QueryArxiv.saveToCsv(papers, fpath);

        List<String> lines = Files.readAllLines(Paths.get(fpath), StandardCharsets.UTF_8);
        assertEquals(2, lines.size()); // header + 1 row
        assertEquals("title,link", lines.get(0));
        assertEquals("Paper A,http://example.com", lines.get(1));
    }

    @Test // test_save_empty_prints -> save_to_csv
    void testSaveEmptyPrints() throws Exception {
        PrintStream orig = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            QueryArxiv.saveToCsv(new ArrayList<>(), "dummy.csv");
        } finally {
            System.setOut(orig);
        }
        String output = baos.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("No papers to save"));
    }

    @Test // test_save_creates_dirs -> save_to_csv
    void testSaveCreatesDirs(@TempDir Path tmpDir) throws Exception {
        List<Map<String, String>> papers = new ArrayList<>();
        Map<String, String> p = new LinkedHashMap<>();
        p.put("title", "Paper B");
        papers.add(p);

        Path fpath = tmpDir.resolve("subdir").resolve("out.csv");
        QueryArxiv.saveToCsv(papers, fpath.toString());
        assertTrue(Files.exists(fpath));
    }

    // ===================  TestPrintResults  ===================

    @Test // test_print_output -> print_results
    void testPrintOutput() {
        List<Map<String, String>> papers = new ArrayList<>();
        Map<String, String> p = new LinkedHashMap<>();
        p.put("title", "Test Paper");
        p.put("authors", "Author A");
        p.put("abstract", "Short abstract.");
        p.put("published", "2024-01-14T10:00:00Z");
        p.put("link", "http://arxiv.org/abs/0001");
        papers.add(p);

        PrintStream orig = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            QueryArxiv.printResults(papers);
        } finally {
            System.setOut(orig);
        }
        String output = baos.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Test Paper"));
        assertTrue(output.contains("Author A"));
        assertTrue(output.contains("--------------------------"));
    }

    // ===================  TestGetArgs  ===================

    @Test // test_required_recent_days -> get_args
    void testRequiredRecentDays() {
        assertThrows(IllegalArgumentException.class,
                () -> QueryArxiv.getArgs(new String[]{"--category", "cs.CL"}));
    }

    @Test // test_parse_basic -> get_args
    void testParseBasic() {
        QueryArxiv.Args args = QueryArxiv.getArgs(
                new String[]{"--category", "cs.CL", "--recent_days", "7"});
        assertEquals("cs.CL", args.category);
        assertEquals(7, args.recentDays);
        assertEquals(10, args.maxResults);
    }

    @Test // test_verbose_flag -> get_args
    void testVerboseFlag() {
        QueryArxiv.Args args = QueryArxiv.getArgs(
                new String[]{"--category", "cs.CL", "--recent_days", "7", "--verbose"});
        assertTrue(args.verbose);
    }

    @Test // test_to_file -> get_args
    void testToFile() {
        QueryArxiv.Args args = QueryArxiv.getArgs(
                new String[]{"--category", "cs.CL", "--recent_days", "7", "--to_file", "out.csv"});
        assertEquals("out.csv", args.toFile);
    }

    // ===================  TestFetchData  ===================

    @Test // test_fetch_data_calls_urlopen -> fetch_data
    void testFetchDataCallsUrlopen() throws Exception {
        final boolean[] called = {false};
        final String[] capturedUrl = {null};

        QueryArxiv.UrlFetcher mockFetcher = url -> {
            called[0] = true;
            capturedUrl[0] = url;
            return "<xml>test</xml>".getBytes(StandardCharsets.UTF_8);
        };

        byte[] result = QueryArxiv.fetchData("http://example.com/api", mockFetcher);
        assertTrue(called[0]);
        assertEquals("http://example.com/api", capturedUrl[0]);
        assertArrayEquals("<xml>test</xml>".getBytes(StandardCharsets.UTF_8), result);
    }
}
