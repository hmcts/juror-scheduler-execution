package uk.gov.hmcts.juror.job.execution.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

class FileUtilsTest {

    private MockedStatic<Files> filesMockedStatic;

    @BeforeEach
    void beforeEach() {
    }

    @AfterEach
    void afterEach() {
        if (filesMockedStatic != null) {
            filesMockedStatic.close();
        }
    }

    @Test
    void positiveCreateFileTest() throws IOException {
        File tmpDir = Files.createTempDirectory("test").toFile();
        File file = FileUtils.createFile(tmpDir.getAbsolutePath() + "/test.txt");

        assertTrue(file.exists(), "File should exist");
    }

    @Test
    void positiveCreateFileAlreadyExistsTest() throws IOException {
        File file = Files.createTempFile("test", ".txt").toFile();
        file.createNewFile();//Ensures the file exists
        File createdFile = FileUtils.createFile(file.getAbsolutePath());
        assertTrue(createdFile.exists(), "File should exist");
    }

    @Test
    void positiveDeleteFiles() throws IOException {
        File tmpFile1 = Files.createTempFile("test", "1.txt").toFile();
        File tmpFile2 = Files.createTempFile("test", "2.txt").toFile();
        File tmpFile3 = Files.createTempFile("test", "3.txt").toFile();
        assertTrue(tmpFile1.exists(), "File 1 should exist");
        assertTrue(tmpFile2.exists(), "File 2 should exist");
        assertTrue(tmpFile3.exists(), "File 3 should exist");
        FileUtils.deleteFiles(Set.of(tmpFile1, tmpFile2, tmpFile3));
        assertFalse(tmpFile1.exists(), "File 1 should be deleted");
        assertFalse(tmpFile2.exists(), "File 2 should be deleted");
        assertFalse(tmpFile3.exists(), "File 3 should be deleted");
    }

    @Test
    void positiveMoveTypical() throws IOException {
        File sourceFile = Files.createTempFile("source", ".txt").toFile();
        File destinationFile = new File("destination.txt");

        filesMockedStatic = Mockito.mockStatic(Files.class);
        FileUtils.move(sourceFile, destinationFile, false);
        filesMockedStatic.verify(() -> Files.move(sourceFile.toPath(), destinationFile.toPath()), times(1));
    }

    @Test
    void positiveMoveFileDoesNotExist() throws IOException {
        File sourceFile = new File("invalid");
        File destinationFile = new File("destination.txt");

        filesMockedStatic = Mockito.mockStatic(Files.class);
        FileUtils.move(sourceFile, destinationFile, false);
        filesMockedStatic.verifyNoInteractions();
    }

    @Test
    void negativeMoveFileDoesNotExistWithFailFlagSet() {
        File sourceFile = new File("invalid");
        File destinationFile = new File("destination.txt");

        InternalServerException internalServerException =
            assertThrows(InternalServerException.class, () -> FileUtils.move(sourceFile, destinationFile, true));
        assertEquals("File does not exist: " + sourceFile.getAbsolutePath(), internalServerException.getMessage());
    }

    @Test
    void positiveDoesFileExist() throws IOException {
        File tmpFile = Files.createTempFile("test", ".txt").toFile();
        assertTrue(FileUtils.doesFileExist(tmpFile),
            "File should exist");
    }

    @Test
    void negativeDoesFileExistNullFile() {
        assertFalse(FileUtils.doesFileExist(null),
            "File does not exist");
    }

    @Test
    void negativeDoesFileExistFalse() {
        File tmpFile = new File("test");
        assertFalse(FileUtils.doesFileExist(tmpFile),
            "File does not exist");
    }

    @Test
    void positiveWriteToFile() throws IOException {
        File tmpFile = Files.createTempFile("test", ".txt").toFile();
        FileUtils.writeToFile(tmpFile, "test");

        assertEquals("test", Files.readString(tmpFile.toPath()));
    }

    @Test
    void positiveGetLinesTypicalNoFilters() {
        List<String> lines = List.of("Line 1", "Line 2", "Line 3");
        File file = new File("test");

        filesMockedStatic = Mockito.mockStatic(Files.class);
        filesMockedStatic.when(() -> Files.lines(file.toPath())).thenReturn(lines.stream());

        List<String> returnedLines = FileUtils.getLines(new File("test"), null, null);
        assertEquals(lines, returnedLines);
    }

    @Test
    void positiveGetLinesTypicalWithMustMatchFilter() {
        List<String> lines = List.of("Line 1", "Line 2", "Line 3", "BadLine 1", "BadLine 2", "BadLine 3");
        File file = new File("test");

        filesMockedStatic = Mockito.mockStatic(Files.class);
        filesMockedStatic.when(() -> Files.lines(file.toPath())).thenReturn(lines.stream());

        List<String> returnedLines = FileUtils.getLines(new File("test"), "Line.*", null);
        assertEquals(List.of("Line 1", "Line 2", "Line 3"), returnedLines);
    }

    @Test
    void positiveGetLinesTypicalWithMustNotMatchFilter() {
        List<String> lines = List.of("Line 1", "Line 2", "Line 3", "BadLine 1", "BadLine 2", "BadLine 3");
        File file = new File("test");

        filesMockedStatic = Mockito.mockStatic(Files.class);
        filesMockedStatic.when(() -> Files.lines(file.toPath())).thenReturn(lines.stream());

        List<String> returnedLines = FileUtils.getLines(new File("test"), null, "Line.*");
        assertEquals(List.of("BadLine 1", "BadLine 2", "BadLine 3"), returnedLines);
    }

    @Test
    void positiveGetLinesTypicalWithBothFilters() {
        List<String> lines = List.of("Line 1", "Line 2", "Line 3",
            "BadLine 1", "BadLine 2", "BadLine 3", "BadLine 32");
        File file = new File("test");

        filesMockedStatic = Mockito.mockStatic(Files.class);
        filesMockedStatic.when(() -> Files.lines(file.toPath())).thenReturn(lines.stream());

        List<String> returnedLines = FileUtils.getLines(new File("test"), "BadLine.*", "BadLine.*2");
        assertEquals(List.of("BadLine 1", "BadLine 3"), returnedLines);
    }
}
