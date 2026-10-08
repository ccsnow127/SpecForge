import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Port of every test in dataset/python/hone_pkg/tests-hone/test_hone.py
 */
class HoneTest {

    // ------------------------------------------------------------ //
    //  Shared fixtures                                               //
    // ------------------------------------------------------------ //

    @TempDir
    Path tmpDir;

    private Hone hone;

    @BeforeEach
    void setUp() {
        hone = new Hone();
    }

    /** Write a CSV file with UTF-8-BOM encoding (matching Python test fixtures). */
    private String writeCsv(String name, String content) throws IOException {
        Path p = tmpDir.resolve(name);
        try (OutputStream os = Files.newOutputStream(p)) {
            // write BOM
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return p.toString();
    }

    private String simpleCsv() throws IOException {
        return writeCsv("simple.csv", "name,age\nAlice,30\nBob,25\n");
    }

    private String nestedCsv() throws IOException {
        return writeCsv("nested.csv", "person_name,person_age,location\nAlice,30,London\n");
    }

    private String multiLevelCsv() throws IOException {
        return writeCsv("multi.csv", "a_b,a_c,d\n1,2,3\n");
    }

    // ------------------------------------------------------------ //
    //  TestHoneInit                                                  //
    // ------------------------------------------------------------ //

    @Nested
    class TestHoneInit {
        @Test
        void test_default_delimiters() {
            assertEquals(Arrays.asList(",", "_", " "), hone.getDelimiters());
        }

        @Test
        void test_custom_delimiters() {
            Hone h = new Hone(Arrays.asList("-"));
            assertEquals(Arrays.asList("-"), h.getDelimiters());
        }

        @Test
        void test_csv_filepath_none() {
            assertNull(hone.getCsvFilepath());
        }
    }

    // ------------------------------------------------------------ //
    //  TestHoneConvert                                               //
    // ------------------------------------------------------------ //

    @Nested
    class TestHoneConvert {
        @Test
        void test_simple_csv() throws IOException {
            List<Map<String, Object>> result = hone.convert(simpleCsv());
            assertInstanceOf(List.class, result);
            assertEquals(2, result.size());
        }

        @Test
        void test_nested_csv() throws IOException {
            List<Map<String, Object>> result = hone.convert(nestedCsv());
            assertEquals(1, result.size());
            Map<String, Object> row = result.get(0);
            assertTrue(row.containsKey("person") || row.containsKey("location"));
        }

        @Test
        void test_preserves_data() throws IOException {
            List<Map<String, Object>> result = hone.convert(simpleCsv());
            List<String> names = new ArrayList<>();
            for (Map<String, Object> row : result) {
                if (row.containsKey("name")) {
                    names.add((String) row.get("name"));
                }
            }
            assertTrue(names.contains("Alice") || result.size() == 2);
        }
    }

    // ------------------------------------------------------------ //
    //  TestGetSchema                                                 //
    // ------------------------------------------------------------ //

    @Nested
    class TestGetSchema {
        @Test
        void test_returns_dict() throws IOException {
            Map<String, Object> schema = hone.getSchema(simpleCsv());
            assertInstanceOf(Map.class, schema);
        }

        @Test
        void test_schema_has_columns() throws IOException {
            Map<String, Object> schema = hone.getSchema(simpleCsv());
            assertFalse(schema.isEmpty());
        }
    }

    // ------------------------------------------------------------ //
    //  TestGenerateFullStructure                                     //
    // ------------------------------------------------------------ //

    @Nested
    class TestGenerateFullStructure {
        @Test
        void test_flat_columns() {
            Map<String, Object> result =
                    hone.generateFullStructure(Arrays.asList("name", "age"));
            assertTrue(result.containsKey("name"));
            assertTrue(result.containsKey("age"));
        }

        @Test
        void test_nested_columns() {
            Map<String, Object> result =
                    hone.generateFullStructure(
                            Arrays.asList("person_name", "person_age", "location"));
            assertTrue(result.containsKey("person") || result.containsKey("location"));
        }

        @Test
        void test_single_column() {
            Map<String, Object> result =
                    hone.generateFullStructure(Arrays.asList("value"));
            assertTrue(result.containsKey("value"));
            assertEquals("value", result.get("value"));
        }
    }

    // ------------------------------------------------------------ //
    //  TestGetLeaves                                                 //
    // ------------------------------------------------------------ //

    @Nested
    class TestGetLeaves {
        @Test
        void test_flat_structure() {
            Map<String, Object> structure = new LinkedHashMap<>();
            structure.put("name", "name");
            structure.put("age", "age");
            Map<String, String> leaves = hone.getLeaves(structure, "", new LinkedHashMap<>());
            assertTrue(leaves.containsKey("name"));
            assertTrue(leaves.containsKey("age"));
        }

        @Test
        void test_nested_structure() {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("name", "person_name");
            Map<String, Object> structure = new LinkedHashMap<>();
            structure.put("person", inner);
            Map<String, String> leaves = hone.getLeaves(structure, "", new LinkedHashMap<>());
            assertTrue(leaves.containsKey("person_name"));
            assertTrue(leaves.get("person_name").contains("['person']['name']"));
        }
    }

    // ------------------------------------------------------------ //
    //  TestGetValidSplits                                            //
    // ------------------------------------------------------------ //

    @Nested
    class TestGetValidSplits {
        @Test
        void test_with_underscore() {
            List<String> splits = hone.getValidSplits("person_name");
            assertTrue(splits.contains("person"));
        }

        @Test
        void test_no_delimiters() {
            List<String> splits = hone.getValidSplits("name");
            assertTrue(splits.isEmpty());
        }

        @Test
        void test_multiple_delimiters() {
            List<String> splits = hone.getValidSplits("a_b_c");
            assertTrue(splits.contains("a"));
            assertTrue(splits.contains("a_b"));
        }
    }

    // ------------------------------------------------------------ //
    //  TestGetSplitSuffix                                            //
    // ------------------------------------------------------------ //

    @Nested
    class TestGetSplitSuffix {
        @Test
        void test_basic_suffix() {
            assertEquals("name", hone.getSplitSuffix("person", "person_name"));
        }

        @Test
        void test_suffix_with_leading_delimiters() {
            assertEquals("b", hone.getSplitSuffix("a", "a__b"));
        }
    }

    // ------------------------------------------------------------ //
    //  TestCleanSplit                                                //
    // ------------------------------------------------------------ //

    @Nested
    class TestCleanSplit {
        @Test
        void test_no_trailing_delimiters() {
            assertEquals("person", hone.cleanSplit("person"));
        }

        @Test
        void test_trailing_underscore() {
            assertEquals("person", hone.cleanSplit("person_"));
        }

        @Test
        void test_multiple_trailing() {
            assertEquals("person", hone.cleanSplit("person__"));
        }
    }

    // ------------------------------------------------------------ //
    //  TestIsValidPrefix                                             //
    // ------------------------------------------------------------ //

    @Nested
    class TestIsValidPrefix {
        @Test
        void test_valid_prefix() {
            assertTrue(hone.isValidPrefix("person", "person_name"));
        }

        @Test
        void test_invalid_prefix() {
            assertFalse(hone.isValidPrefix("per", "person_name"));
        }

        @Test
        void test_no_delimiter_after() {
            assertFalse(hone.isValidPrefix("person", "personality"));
        }
    }

    // ------------------------------------------------------------ //
    //  TestSetCsvFilepath                                            //
    // ------------------------------------------------------------ //

    @Nested
    class TestSetCsvFilepath {
        @Test
        void test_sets_filepath() {
            hone.setCsvFilepath("/some/path.csv");
            assertEquals("/some/path.csv", hone.getCsvFilepath());
            assertEquals("/some/path.csv", hone.getCsv().filepath);
        }
    }

    // ------------------------------------------------------------ //
    //  TestEscapeQuotes                                              //
    // ------------------------------------------------------------ //

    @Nested
    class TestEscapeQuotes {
        @Test
        void test_double_quotes() {
            String result = hone.escapeQuotes("hello \"world\"");
            assertTrue(result.contains("\\\""));
        }

        @Test
        void test_single_quotes() {
            String result = hone.escapeQuotes("hello 'world'");
            assertTrue(result.contains("\\'"));
        }

        @Test
        void test_no_quotes() {
            assertEquals("hello world", hone.escapeQuotes("hello world"));
        }

        @Test
        void test_already_escaped() {
            String result = hone.escapeQuotes("hello \\\"world\\\"");
            // count occurrences of \"
            int count = 0;
            int idx = 0;
            while ((idx = result.indexOf("\\\"", idx)) != -1) {
                count++;
                idx += 2;
            }
            assertEquals(2, count);
        }
    }
}
