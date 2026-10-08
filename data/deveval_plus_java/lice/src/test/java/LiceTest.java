import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JUnit 5 tests for {@link Lice} -- one-to-one mapping to every Python test in
 * lice/tests-core/test_core.py.
 */
public class LiceTest {

    /** Read a template directly from the classpath; the source-of-truth for comparisons. */
    private static String readTemplateResource(String name) throws IOException {
        try (InputStream in = Lice.class.getResourceAsStream("/templates/" + name)) {
            assertNotNull(in, "missing resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // =================================================================
    // test_paths -> clean_path
    // =================================================================
    @Test // test_paths -> clean_path
    void testPaths() {
        // clean_path(".") == cwd
        assertEquals(Paths.get("").toAbsolutePath().normalize().toString(),
                Lice.cleanPath("."));
        String home = System.getProperty("user.home");
        // clean_path("$HOME") expands when HOME is set; on systems without HOME we set
        // the system property as fallback. Use whichever is the user.home.
        String homeEnv = System.getenv("HOME");
        if (homeEnv != null) {
            assertEquals(Paths.get(homeEnv).toAbsolutePath().normalize().toString(),
                    Lice.cleanPath("$HOME"));
        }
        assertEquals(home, Lice.cleanPath("~"));
    }

    // =================================================================
    // test_file_template -> load_file_template
    // =================================================================
    @Test // test_file_template -> load_file_template
    void testFileTemplate(@TempDir Path tmp) throws Exception {
        for (String license : Lice.LICENSES) {
            String resourceName = "template-" + license + ".txt";
            String content = readTemplateResource(resourceName);
            // Stage to a real file so loadFileTemplate (which uses the FS) can read it.
            Path file = tmp.resolve(resourceName);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            String loaded = Lice.loadFileTemplate(file.toString()).toString();
            assertEquals(content.strip(), loaded.strip(),
                    "mismatch on license " + license);
        }
    }

    // =================================================================
    // test_package_template -> load_template
    // =================================================================
    @Test // test_package_template -> load_template
    void testPackageTemplate() throws Exception {
        for (String license : Lice.LICENSES) {
            String resourceContent = readTemplateResource("template-" + license + ".txt");
            String packaged = Lice.loadTemplate(license).toString();
            assertEquals(resourceContent.strip(), packaged.strip(),
                    "mismatch on license " + license);
        }
    }

    // =================================================================
    // test_extract_vars -> extract_vars
    // =================================================================
    @Test // test_extract_vars -> extract_vars
    void testExtractVars() {
        StringBuilder template = new StringBuilder();
        for (String license : Lice.LICENSES) {
            template.append("Oh hey, {{ this }} is a {{ template }} test.");
            List<String> varList = Lice.extractVars(template);
            assertEquals(Arrays.asList("template", "this"), varList);
        }
    }

    // =================================================================
    // test_license -> generate_license
    // =================================================================
    @Test // test_license -> generate_license
    void testLicense() throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("year", "1981");
        context.put("project", "lice");
        context.put("organization", "Awesome Co.");

        for (String license : Lice.LICENSES) {
            StringBuilder template = Lice.loadTemplate(license);
            String content = template.toString();
            content = content.replace("{{ year }}", context.get("year"));
            content = content.replace("{{ project }}", context.get("project"));
            content = content.replace("{{ organization }}", context.get("organization"));
            assertEquals(content,
                    Lice.generateLicense(template, context).toString(),
                    "mismatch on license " + license);
        }
    }

    // =================================================================
    // test_license_header -> generate_license (header templates)
    // =================================================================
    @Test // test_license_header -> generate_license
    void testLicenseHeader() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("year", "1981");
        context.put("project", "lice");
        context.put("organization", "Awesome Co.");

        for (String license : Lice.LICENSES) {
            try {
                StringBuilder template = Lice.loadTemplate(license, true);
                String content = template.toString();
                content = content.replace("{{ year }}", context.get("year"));
                content = content.replace("{{ project }}", context.get("project"));
                content = content.replace("{{ organization }}", context.get("organization"));
                assertEquals(content,
                        Lice.generateLicense(template, context).toString(),
                        "mismatch on header for " + license);
            } catch (IOException e) {
                // header template may not exist for this license — okay.
            }
        }
    }

    // =================================================================
    // TestFormatLicense
    // =================================================================
    @Test // test_format_license_c_language -> format_license
    void testFormatLicenseCLanguage() {
        StringBuilder licenseText = new StringBuilder("Example License Text");
        String formatted = Lice.formatLicense(licenseText, "c").toString();
        assertEquals("/*\n * Example License Text */\n", formatted);
    }

    @Test // test_format_license_python_language -> format_license
    void testFormatLicensePythonLanguage() {
        StringBuilder licenseText = new StringBuilder("Example License Text");
        String formatted = Lice.formatLicense(licenseText, "py").toString();
        assertEquals("\n# Example License Text\n", formatted);
    }

    // =================================================================
    // TestGetSuffix
    // =================================================================
    @Test // test_valid_suffix -> get_suffix
    void testValidSuffix() {
        assertEquals("py", Lice.getSuffix("example.py"));
    }

    @Test // test_no_suffix -> get_suffix
    void testNoSuffix() {
        // Python: assertFalse(get_suffix("example")) — None is falsy in our mapping.
        assertNull(Lice.getSuffix("example"));
    }

    @Test // test_unrecognized_suffix -> get_suffix
    void testUnrecognizedSuffix() {
        assertNull(Lice.getSuffix("example.xyz"));
    }

    // =================================================================
    // TestValidYear
    // =================================================================
    @Test // test_valid_year -> valid_year
    void testValidYear() {
        assertEquals("2024", Lice.validYear("2024"));
    }

    @Test // test_invalid_year_non_numeric -> valid_year
    void testInvalidYearNonNumeric() {
        assertThrows(Lice.ArgumentTypeException.class, () -> Lice.validYear("abc"));
    }

    @Test // test_invalid_year_not_four_digits -> valid_year
    void testInvalidYearNotFourDigits() {
        assertThrows(Lice.ArgumentTypeException.class, () -> Lice.validYear("202"));
    }

    @Test // test_invalid_year_extra_characters -> valid_year
    void testInvalidYearExtraCharacters() {
        assertThrows(Lice.ArgumentTypeException.class, () -> Lice.validYear("2024abc"));
    }

    // =================================================================
    // TestMainFunction
    // =================================================================

    @Test // test_help_message -> main
    void testHelpMessage() {
        PrintStream origOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            Lice.SystemExitException ex = assertThrows(Lice.SystemExitException.class,
                    () -> Lice.runMain(new String[]{"-h"}));
            assertEquals(0, ex.code);
        } finally {
            System.setOut(origOut);
        }
    }

    @Test // test_invalid_license -> main
    void testInvalidLicense() {
        PrintStream origErr = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        try {
            Lice.SystemExitException ex = assertThrows(Lice.SystemExitException.class,
                    () -> Lice.runMain(new String[]{"invalid_license"}));
            assertNotEquals(0, ex.code);
        } finally {
            System.setErr(origErr);
        }
    }

    @Test // test_valid_license -> main
    void testValidLicense() {
        PrintStream origOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try {
            // Should run to completion without throwing (other than potentially success).
            try {
                Lice.runMain(new String[]{"bsd3"});
            } catch (Lice.SystemExitException e) {
                assertEquals(0, e.code);
            }
        } finally {
            System.setOut(origOut);
        }
    }

    @Test // test_list_languages_option -> main
    void testListLanguagesOption() {
        PrintStream origOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            Lice.SystemExitException ex = assertThrows(Lice.SystemExitException.class,
                    () -> Lice.runMain(new String[]{"--languages"}));
            assertEquals(0, ex.code);
            String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("cpp"), "output should list cpp");
        } finally {
            System.setOut(origOut);
        }
    }

    @Test // test_template_path_not_exist -> main
    void testTemplatePathNotExist() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Lice.runMain(new String[]{"-t", "nonexistent_path"}));
        assertEquals("path does not exist: nonexistent_path", ex.getMessage());
    }

    @Test // test_output_to_file -> main
    void testOutputToFile(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("output.txt");
        // Run from tmp dir context: pass the absolute path so we control the location.
        Lice.runMain(new String[]{"bsd3", "-f", out.toString()});
        assertTrue(Files.exists(out), "expected file " + out + " to exist");
        String content = Files.readString(out, StandardCharsets.UTF_8);
        // Output should contain at least a copyright line from the bsd3 template.
        assertTrue(content.contains("Copyright"), "output file should contain license text");
    }

    @Test // test_list_template_vars -> main
    void testListTemplateVars() {
        PrintStream origOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            assertThrows(Lice.SystemExitException.class,
                    () -> Lice.runMain(new String[]{"--vars"}));
            String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("The bsd3 license template contains the following variables"),
                    "expected vars header in: " + output);
        } finally {
            System.setOut(origOut);
        }
    }

    @Test // test_list_template_vars_with_custom_template -> main
    void testListTemplateVarsWithCustomTemplate(@TempDir Path tmp) throws Exception {
        // Stage a custom template on disk and point -t at it.
        Path tmpl = tmp.resolve("custom_template_path.txt");
        Files.writeString(tmpl, "{{ variable1 }}\n{{ variable2 }}", StandardCharsets.UTF_8);

        PrintStream origOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            assertThrows(Lice.SystemExitException.class,
                    () -> Lice.runMain(new String[]{"--vars", "-t", tmpl.toString()}));
            String output = baos.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("variable1"), "should mention variable1");
            assertTrue(output.contains("variable2"), "should mention variable2");
        } finally {
            System.setOut(origOut);
        }
    }

    @Test // test_custom_organization -> main
    void testCustomOrganization() {
        PrintStream origOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            try {
                Lice.runMain(new String[]{"-o", "CustomOrg"});
            } catch (Lice.SystemExitException ignored) {
                // okay, may exit with 0
            }
            assertTrue(baos.toString(StandardCharsets.UTF_8).contains("CustomOrg"));
        } finally {
            System.setOut(origOut);
        }
    }

    @Test // test_custom_project -> main
    void testCustomProject() {
        PrintStream origOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            try {
                Lice.runMain(new String[]{"-p", "CustomProject"});
            } catch (Lice.SystemExitException ignored) {
                // okay, may exit with 0
            }
            assertTrue(baos.toString(StandardCharsets.UTF_8).contains("CustomProject"));
        } finally {
            System.setOut(origOut);
        }
    }
}
