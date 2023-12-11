package uk.gov.hmcts.juror.job.execution.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.job.execution.testsupport.TestUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FileSearchTest {


    private File searchDirectory;
    private Path searchPath;

    private File directory;
    private File file;

    private MockedStatic<Files> filesMockedStatic;

    @BeforeEach
    void beforeEach() {
        searchDirectory = mock(File.class);
        searchPath = mock(Path.class);
        when(searchDirectory.toPath()).thenReturn(searchPath);

        filesMockedStatic = Mockito.mockStatic(Files.class);

        directory = mock(File.class);
        when(directory.isDirectory()).thenReturn(true);
        when(directory.isFile()).thenReturn(false);


        file = mock(File.class);
        when(file.isDirectory()).thenReturn(false);
        when(file.isFile()).thenReturn(true);
    }

    @AfterEach
    void afterEach() {
        if (filesMockedStatic != null) {
            filesMockedStatic.close();
        }
    }


    private FileSearch createFileSearch() {
        return FileSearch.directory(searchDirectory, false);
    }

    @ParameterizedTest(name = "directory constructor test: is Recursive {0}")
    @ValueSource(booleans = {true, false})
    void directoryConstructorTest(boolean recursive) {
        FileSearch fileSearch = FileSearch.directory(searchDirectory, recursive);

        assertNotNull(fileSearch.streamSupplier, "The streamSupplier should not be null");
        assertEquals(0, fileSearch.pathPredicates.size(),
            "The pathPredicates list should be empty");
        assertEquals(1, fileSearch.filePredicates.size(),
            "The filePredicates list should contain one predicate");

        Predicate<File> isFilePredicate = fileSearch.filePredicates.get(0);
        assertTrue(isFilePredicate.test(file), "The file should be a file");
        assertFalse(isFilePredicate.test(directory), "The file should not be a directory");

        verify(file, times(1)).isFile();
        verify(directory, times(1)).isFile();

        fileSearch.streamSupplier.get();//Required to trigger supplier

        verify(searchDirectory, times(1)).toPath();
        if (recursive) {
            filesMockedStatic.verify(() -> {
                try {
                    Files.walk(searchPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, times(1));
        } else {
            filesMockedStatic.verify(() -> {
                try {
                    Files.list(searchPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, times(1));
        }
        filesMockedStatic.verifyNoMoreInteractions();

        verifyNoMoreInteractions(file, directory, searchDirectory, searchPath);
    }


    @Test
    @DisplayName("setFileNameRegexFilter(...): Setting a non-null file name regex filter should add a predicate")
    void positiveSetFileNameRegexFilter() {
        FileSearch fileSearch = createFileSearch();
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());
        assertSame(fileSearch, fileSearch.setFileNameRegexFilter(".*\\.txt"),
            "The setFileNameRegexFilter method should return the same FileSearch instance");
        assertEquals(1, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());

        Predicate<Path> pathPredicate = fileSearch.pathPredicates.get(0);

        Path path = mock(Path.class);
        when(path.getFileName()).thenReturn(path);
        when(path.toString()).thenReturn("test.txt");
        assertTrue(pathPredicate.test(path), "The path should match the regex");
        when(path.toString()).thenReturn("test");
        assertFalse(pathPredicate.test(path), "The path should not match the regex");
    }

    @Test
    @DisplayName("setFileNameRegexFilter(...): Setting a null file name regex filter should not add a predicate")
    void negativeSetFileNameRegexFilter() {
        FileSearch fileSearch = createFileSearch();
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());
        assertSame(fileSearch, fileSearch.setFileNameRegexFilter(null),
            "The setFileNameRegexFilter method should return the same FileSearch instance");
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());
    }

    @Test
    @DisplayName("setMaxAge(...): File should pass if age is younger then max age")
    void positiveSetMaxAge() {
        FileSearch fileSearch = createFileSearch();
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());
        assertSame(fileSearch, fileSearch.setMaxAge(20),
            "The setMaxAge method should return the same FileSearch instance");
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(2, fileSearch.filePredicates.size());

        Predicate<File> filePredicate = fileSearch.filePredicates.get(1);

        File file = mock(File.class);
        when(file.lastModified()).thenReturn(19L);
        assertTrue(filePredicate.test(file), "The file should be younger than 20 milliseconds");
        when(file.lastModified()).thenReturn(21L);
        assertFalse(filePredicate.test(file), "The file should be younger than 20 milliseconds");
    }

    @Test
    @DisplayName("setMaxAge(...): File should fail if age is equal to or older then max age")
    void negativeSetMaxAge() {
        FileSearch fileSearch = createFileSearch();
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());
        assertSame(fileSearch, fileSearch.setMaxAge(20),
            "The setMaxAge method should return the same FileSearch instance");
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(2, fileSearch.filePredicates.size());

        Predicate<File> filePredicate = fileSearch.filePredicates.get(1);

        when(file.lastModified()).thenReturn(21L);
        assertFalse(filePredicate.test(file), "The file should be younger than 20 milliseconds");

        when(file.lastModified()).thenReturn(20L);
        assertFalse(filePredicate.test(file), "The file should be younger than 20 milliseconds");
    }


    private void setOwnerTest(String ownerToSend) {
        final String owner = "Owner123";
        FileSearch fileSearch = createFileSearch();
        assertEquals(0, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());
        assertSame(fileSearch, fileSearch.setOwner(owner),
            "The setOwner method should return the same FileSearch instance");
        assertEquals(1, fileSearch.pathPredicates.size());
        assertEquals(1, fileSearch.filePredicates.size());

        Predicate<Path> pathPredicate = fileSearch.pathPredicates.get(0);

        Path path = mock(Path.class);
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getName()).thenReturn(ownerToSend);

        filesMockedStatic.when(
            () -> Files.getOwner(path)).thenReturn(userPrincipal);

        if (owner.equals(ownerToSend)) {
            assertTrue(pathPredicate.test(path), "The path owner should match");
        } else {
            assertFalse(pathPredicate.test(path), "The path owner should not match");
        }
    }

    @Test
    @DisplayName("setOwner(...): Should pass when provided same owner")
    void positiveSetOwner() {
        setOwnerTest("Owner123");
    }

    @Test
    @DisplayName("setOwner(...): Should fail when provided different owner")
    void negativeSetOwner() {
        setOwnerTest("WrongOwner123");
    }


    @Test
    @DisplayName("search(): Single file match")
    void positiveSearch() throws Exception {
        List<Path> paths = List.of(
            createPath("test1.txt", "Owner", 10L),
            createPath("test2.txt", "Owner3", 15L),
            createPath("test3.txt", "Owner", 20L)
        );
        filesMockedStatic.when(() -> Files.list(searchPath)).thenReturn(paths.stream());
        FileSearch fileSearch = createFileSearch();

        fileSearch.setFileNameRegexFilter(".*\\.txt")
            .setMaxAge(19L)
            .setOwner("Owner");

        Set<File> files = fileSearch.search();
        assertEquals(1, files.size());
        TestUtil.isUnmodifiable(files);
        assertSame(paths.get(0).toFile(), files.iterator().next());
    }

    @Test
    @DisplayName("search(): All file match")
    void positiveSearchAllFilesMatch() throws Exception {
        List<Path> paths = List.of(
            createPath("test1.txt", "Owner", 10L),
            createPath("test2.txt", "Owner", 15L),
            createPath("test3.txt", "Owner", 20L)
        );
        filesMockedStatic.when(() -> Files.list(searchPath)).thenReturn(paths.stream());
        FileSearch fileSearch = createFileSearch();

        fileSearch
            .setMaxAge(25L);

        Set<File> files = fileSearch.search();
        assertEquals(3, files.size());
        TestUtil.isUnmodifiable(files);
    }


    @Test
    @DisplayName("search(): No file match")
    void negativeSearch() throws Exception {
        List<Path> paths = List.of(
            createPath("test1.txt", "Owner", 10L),
            createPath("test2.txt", "Owner3", 15L),
            createPath("test3.txt", "Owner", 20L)
        );
        filesMockedStatic.when(() -> Files.list(searchPath)).thenReturn(paths.stream());
        FileSearch fileSearch = createFileSearch();

        fileSearch.setFileNameRegexFilter(".*\\.txt")
            .setMaxAge(19L)
            .setOwner("RandomOwner");

        Set<File> files = fileSearch.search();
        filesMockedStatic.verify(() -> Files.list(searchPath), times(1));

        assertEquals(0, files.size());
        TestUtil.isUnmodifiable(files);

    }

    private Path createPath(String fileName, String owner, long age) throws Exception {
        Path path = mock(Path.class);
        File file = mock(File.class);
        when(path.getFileName()).thenReturn(path);
        when(path.toString()).thenReturn(fileName);

        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getName()).thenReturn(owner);
        filesMockedStatic.when(() -> Files.getOwner(path)).thenReturn(userPrincipal);

        when(path.toFile()).thenReturn(file);
        when(file.isFile()).thenReturn(true);
        when(file.lastModified()).thenReturn(age);

        return path;
    }


}
