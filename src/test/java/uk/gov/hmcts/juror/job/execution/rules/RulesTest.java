package uk.gov.hmcts.juror.job.execution.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.testsupport.TestUtil;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesTest {
    private static final String TEST_DATA_DIRECTORY = "rulesTest/";

    private static final String TEST_DIRECTORY = TEST_DATA_DIRECTORY;
    private static final String TEST_FILE = TEST_DATA_DIRECTORY + "/testFile.txt";


    @Test
    @DisplayName("notNull() should return true for non-null values")
    void positiveNotNull() {
        assertTrue(Rules.notNull().test("test"),
            "notNull should return true for non-null values");
    }

    @Test
    @DisplayName("notNull() should return false for null values")
    void negativeNotNull() {
        assertFalse(Rules.notNull().test(null),
            "notNull should return false for null values");
    }

    @Test
    @DisplayName("notNull(Class) should return true for non-null values")
    void positiveNotNullWithClass() {
        assertTrue(Rules.notNull(String.class).test("test"),
            "notNull should return true for non-null values");
    }

    @Test
    @DisplayName("notNull(Class) should return false for null values")
    void negativeNotNullWithClass() {
        assertFalse(Rules.notNull(String.class).test(null),
            "notNull should return false for null values");
    }

    @Test
    @DisplayName("isDirectory() should return true for directories")
    void positiveIsDirectory() {
        assertTrue(Rules.isDirectory().test(TestUtil.getTestDataFile(TEST_DIRECTORY)),
            "isDirectory should return true for directories");
    }

    @Test
    @DisplayName("isDirectory() should return false for files")
    void negativeIsDirectory() {
        assertFalse(Rules.isDirectory().test(new File("Invalid")),
            "isDirectory should return false for non-existent directories");
    }

    @Test
    @DisplayName("fileExists() should return true for files that exist")
    void positiveFileExists() {
        assertTrue(Rules.fileExists().test(TestUtil.getTestDataFile(TEST_FILE)),
            "fileExists should return true for files that exist");
    }

    @Test
    @DisplayName("fileExists() should return false for files that do not exist")
    void negativeFileExists() {
        assertFalse(Rules.fileExists().test(new File("Invalid")),
            "fileExists should return false for non-existent files");

    }

    @Test
    @DisplayName("requireDirectory() should return true for valid directories")
    void positiveRequireDirectory() {
        Rule rule = Rules.requireDirectory(TestUtil.getTestDataFile(TEST_DIRECTORY));
        assertTrue(rule.execute(), "Rule should execute");
        assertNull(rule.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("requireDirectory() should return false for invalid directories (Null directory)")
    void negativeRequireDirectoryNull() {
        Rule rule = Rules.requireDirectory(null);
        assertFalse(rule.execute(), "Rule should not execute");
        assertEquals("Directory must not be null", rule.getMessage(),
            "Message should be 'Directory must not be null'");
    }

    @Test
    @DisplayName("requireDirectory() should return false for invalid directories (Not found directory)")
    void negativeRequireDirectoryNotFound() {
        File file = new File("Invalid");
        Rule rule = Rules.requireDirectory(file);
        assertFalse(rule.execute(), "Rule should not execute");
        assertEquals(file.getAbsolutePath() + " must exist and be a directory", rule.getMessage(),
            "Message should be '" + file.getAbsolutePath() + " must exist and be a directory'");
    }

    @Test
    @DisplayName("requireDirectory() should return false for invalid directories (Is File not directory)")
    void negativeRequireDirectoryIsFile() {
        File file = TestUtil.getTestDataFile(TEST_FILE);
        Rule rule = Rules.requireDirectory(file);
        assertFalse(rule.execute(), "Rule should not execute");
        assertEquals(file.getAbsolutePath() + " must exist and be a directory", rule.getMessage(),
            "Message should be '" + file.getAbsolutePath() + " must exist and be a directory'");
    }
}
