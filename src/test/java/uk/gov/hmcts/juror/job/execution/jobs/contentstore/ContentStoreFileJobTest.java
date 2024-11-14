package uk.gov.hmcts.juror.job.execution.jobs.contentstore;

import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.util.FileSystemUtils;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.ContentStore;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;
import uk.gov.hmcts.juror.job.execution.util.FileSearch;
import uk.gov.hmcts.juror.job.execution.util.FileUtils;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.juror.job.execution.testsupport.TestConstants.REQUEST_PARAMS;
import static uk.gov.hmcts.juror.job.execution.testsupport.TestConstants.VALID_JOB_KEY;
import static uk.gov.hmcts.juror.job.execution.testsupport.TestConstants.VALID_TASK_ID;
import static uk.gov.hmcts.juror.job.execution.testsupport.TestConstants.VALID_TASK_ID_LONG;

@SuppressWarnings({"unchecked", "PMD.ExcessiveImports"})
@Getter
public class ContentStoreFileJobTest {

    protected SftpService sftpService;
    protected DatabaseConfig databaseConfig;
    protected DatabaseService databaseService;
    protected Connection connection;

    protected File ftpDirectory;
    protected String fileType;
    protected String procedureName;
    protected Object[] procedureArguments;
    protected String fileNameRegex;
    protected Class<? extends Sftp> sftpClass;

    protected ContentStoreFileJobTest() {

    }

    protected DatabaseConfig createDatabaseConfig() {
        return new DatabaseConfig();
    }

    protected ContentStoreFileJob getContentStoreFileJob() throws IOException {
        this.ftpDirectory = Files.createTempDirectory("ContentStoreFileJobTest").toFile();
        this.databaseConfig = createDatabaseConfig();

        this.fileType = "FileType123";
        this.procedureName = "ProcedureName";
        this.procedureArguments = new Object[0];
        this.fileNameRegex = "tbc";
        this.sftpClass = Sftp.class;

        return spy(new ContentStoreFileJobImpl(ftpDirectory,
            databaseConfig,
            fileType,
            procedureName,
            procedureArguments,
            fileNameRegex,
            sftpClass
        ));
    }

    @BeforeEach
    void beforeEach() {
        sftpService = mock(SftpService.class);
        databaseService = mock(DatabaseService.class);
        connection = mock(Connection.class);
        doAnswer(invocation -> {
            ((Consumer<Connection>) invocation.getArgument(1)).accept(connection);
            return null;
        }).when(databaseService).execute(any(), any());

    }

    @AfterEach
    void afterEach() {
        if (this.ftpDirectory != null && this.ftpDirectory.exists()) {
            FileSystemUtils.deleteRecursively(this.ftpDirectory);
        }
    }

    @DisplayName("protected Result generateFiles()")
    @Nested
    class GenerateFiles {
        private static final String SELECT_SQL_QUERY = "SELECT CS.REQUEST_ID, CS.DOCUMENT_ID, CS.DATA, "
            + "CS.FAILED_FILE_TRANSFER "
            + "FROM CONTENT_STORE CS "
            + "WHERE CS.FILE_TYPE=? "
            + "AND CS.DATE_SENT is NULL";

        private List<ContentStore> getStandardContentStoreList() {
            return List.of(
                new ContentStore().setRequestId(1L).setData("Data123")
                    .setDocumentId("DocId 1"),
                new ContentStore().setRequestId(2L).setData("New data")
                    .setDocumentId("DocId 2"),
                new ContentStore().setRequestId(3L).setData("A third piece of data")
                    .setDocumentId("DocId 3")
            );
        }

        private List<ContentStore> getFailedContentStoreList() {
            return List.of(
                new ContentStore().setRequestId(1L).setData("Data123")
                    .setDocumentId("DocId 1").setFailedFileTransfer(true),
                new ContentStore().setRequestId(2L).setData("New data")
                    .setDocumentId("DocId 2"),
                new ContentStore().setRequestId(3L).setData("A third piece of data")
                    .setDocumentId("DocId 3")
            );
        }

        @Test
        void positiveTypical() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                List<ContentStore> contentStoreList = getStandardContentStoreList();

                when(databaseService.executePreparedStatement(connection,
                    ContentStore.class,
                    SELECT_SQL_QUERY,
                    fileType)).thenReturn(contentStoreList);

                fileUtilsMock.when(() ->
                        FileUtils.createFile(any()))
                    .thenReturn(mock(File.class));

                Job.Result result = contentStoreFileJob.generateFiles(new MetaData(VALID_JOB_KEY,
                    VALID_TASK_ID_LONG, REQUEST_PARAMS));
                assertEquals(Status.SUCCESS, result.getStatus(), "Expect status to be SUCCESS");
                assertNull(result.getMessage(), "Expect no message");
                assertNull(result.getThrowable(), "Expect no throwable");
                assertEquals(3, result.getMetaData().size(), "Expect 3 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_GENERATED"),
                    "Expect 3 files to be generated");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_GENERATED_SUCCESS"),
                    "Expect 3 files to be generated successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_GENERATED_UNSUCCESSFULLY"),
                    "Expect 0 files to be generated unsuccessfully");
                verify(databaseService, times(1))
                    .execute(eq(databaseConfig), any());

                verify(databaseService, times(1))
                    .executeStoredProcedure(connection, procedureName, procedureArguments);

                verify(databaseService, times(1))
                    .executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY, fileType);

                for (ContentStore contentStore : contentStoreList) {
                    fileUtilsMock.verify(() -> FileUtils.writeToFile(any(File.class), eq(contentStore.getData())));
                }

                verifyNoMoreInteractions(databaseService);
                verifyNoInteractions(sftpService);
            }
        }

        @Test
        void onlyRunFailedIsTrue() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();

                when(databaseService.executePreparedStatement(connection,
                    ContentStore.class,
                    SELECT_SQL_QUERY,
                    fileType)).thenReturn(getFailedContentStoreList());

                fileUtilsMock.when(() -> FileUtils.createFile(any())).thenReturn(mock(File.class));

                Map<String, String> requestParams = Map.of("jobKey", VALID_JOB_KEY,
                    "taskId", VALID_TASK_ID,
                    "onlyRunFailed", "true");

                Job.Result result = contentStoreFileJob.generateFiles(new MetaData(VALID_JOB_KEY,
                    VALID_TASK_ID_LONG,
                    requestParams));

                assertEquals(Status.SUCCESS, result.getStatus(), "Expect status to be SUCCESS");
                assertNull(result.getMessage(), "Expect no message");
                assertNull(result.getThrowable(), "Expect no throwable");
                assertEquals(3, result.getMetaData().size(), "Expect 3 metadata entries");
                assertEquals("1", result.getMetaData().get("TOTAL_FILES_TO_GENERATED"),
                    "Expect 1 files to be generated");
                assertEquals("1", result.getMetaData().get("TOTAL_FILES_GENERATED_SUCCESS"),
                    "Expect 1 files to be generated successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_GENERATED_UNSUCCESSFULLY"),
                    "Expect 0 files to be generated unsuccessfully");
                verify(databaseService, times(1))
                    .execute(eq(databaseConfig), any());

                verify(databaseService, never())
                    .executeStoredProcedure(connection, procedureName, procedureArguments);

                verify(databaseService, times(1))
                    .executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY, fileType);

                fileUtilsMock.verify(() -> FileUtils.writeToFile(any(File.class), any(String.class)), times(1));

                verifyNoMoreInteractions(databaseService);
                verifyNoInteractions(sftpService);
            }
        }

        @Test
        void onlyRunFailedIsFalse() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();

                when(databaseService.executePreparedStatement(connection,
                    ContentStore.class,
                    SELECT_SQL_QUERY,
                    fileType)).thenReturn(getFailedContentStoreList());

                fileUtilsMock.when(() -> FileUtils.createFile(any())).thenReturn(mock(File.class));

                Map<String, String> requestParams = Map.of("jobKey", VALID_JOB_KEY,
                    "taskId", VALID_TASK_ID,
                    "onlyRunFailed", "false");

                Job.Result result = contentStoreFileJob.generateFiles(new MetaData(VALID_JOB_KEY,
                    VALID_TASK_ID_LONG, requestParams));

                assertEquals(Status.SUCCESS, result.getStatus(), "Expect status to be SUCCESS");
                assertNull(result.getMessage(), "Expect no message");
                assertNull(result.getThrowable(), "Expect no throwable");
                assertEquals(3, result.getMetaData().size(), "Expect 3 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_GENERATED"),
                    "Expect 3 files to be generated");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_GENERATED_SUCCESS"),
                    "Expect 3 files to be generated successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_GENERATED_UNSUCCESSFULLY"),
                    "Expect 0 files to be generated unsuccessfully");
                verify(databaseService, times(1))
                    .execute(eq(databaseConfig), any());

                verify(databaseService, times(1))
                    .executeStoredProcedure(connection, procedureName, procedureArguments);

                verify(databaseService, times(1))
                    .executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY, fileType);

                fileUtilsMock.verify(() -> FileUtils.writeToFile(any(File.class), any(String.class)), times(3));

                verifyNoMoreInteractions(databaseService);
                verifyNoInteractions(sftpService);
            }
        }

        @Test
        void onlyRunFailedIsBlank() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();

                when(databaseService.executePreparedStatement(connection,
                    ContentStore.class,
                    SELECT_SQL_QUERY,
                    fileType)).thenReturn(getFailedContentStoreList());

                fileUtilsMock.when(() -> FileUtils.createFile(any())).thenReturn(mock(File.class));

                Map<String, String> requestParams = Map.of("jobKey", VALID_JOB_KEY,
                    "taskId", VALID_TASK_ID,
                    "onlyRunFailed", "");

                Job.Result result = contentStoreFileJob.generateFiles(new MetaData(VALID_JOB_KEY,
                    VALID_TASK_ID_LONG,
                    requestParams));

                assertEquals(Status.SUCCESS, result.getStatus(), "Expect status to be SUCCESS");
                assertNull(result.getMessage(), "Expect no message");
                assertNull(result.getThrowable(), "Expect no throwable");
                assertEquals(3, result.getMetaData().size(), "Expect 3 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_GENERATED"),
                    "Expect 3 files to be generated");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_GENERATED_SUCCESS"),
                    "Expect 3 files to be generated successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_GENERATED_UNSUCCESSFULLY"),
                    "Expect 0 files to be generated unsuccessfully");
                verify(databaseService, times(1))
                    .execute(eq(databaseConfig), any());

                verify(databaseService, times(1))
                    .executeStoredProcedure(connection, procedureName, procedureArguments);

                verify(databaseService, times(1))
                    .executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY, fileType);

                fileUtilsMock.verify(() -> FileUtils.writeToFile(any(File.class), any(String.class)), times(3));

                verifyNoMoreInteractions(databaseService);
                verifyNoInteractions(sftpService);
            }
        }

        @Test
        void negativeHasErrors() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                List<ContentStore> contentStoreList = getStandardContentStoreList();

                RuntimeException expectedException = new RuntimeException("I am the cause");
                when(databaseService.executePreparedStatement(connection,
                    ContentStore.class,
                    SELECT_SQL_QUERY,
                    fileType)).thenReturn(contentStoreList);

                fileUtilsMock.when(() ->
                        FileUtils.createFile(any()))
                    .thenThrow(expectedException);

                Job.Result result = contentStoreFileJob.generateFiles(new MetaData(VALID_JOB_KEY,
                    VALID_TASK_ID_LONG, REQUEST_PARAMS));
                assertEquals(Status.PARTIAL_SUCCESS, result.getStatus());
                assertEquals("3 files failed to generate out of 3.", result.getMessage(),
                    "Expect message to contain the number of failed files");
                assertNull(result.getThrowable());
                assertEquals(6, result.getMetaData().size(), "Expect 6 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_GENERATED"),
                    "Expect 3 files to be generated");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_GENERATED_SUCCESS"),
                    "Expect 0 files to be generated successfully");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_GENERATED_UNSUCCESSFULLY"),
                    "Expect 3 files to be generated unsuccessfully");
                assertEquals("DocId 1", result.getMetaData().get("FAILED_TO_GENERATE_FILE_1"),
                    "Expect DocId 1 to be the first failed file");
                assertEquals("DocId 2", result.getMetaData().get("FAILED_TO_GENERATE_FILE_2"),
                    "Expect DocId 2 to be the second failed file");
                assertEquals("DocId 3", result.getMetaData().get("FAILED_TO_GENERATE_FILE_3"),
                    "Expect DocId 3 to be the third failed file");
                verify(databaseService, times(1))
                    .execute(eq(databaseConfig), any());

                verify(databaseService, times(1))
                    .executeStoredProcedure(connection, procedureName, procedureArguments);

                verify(databaseService, times(1))
                    .executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY, fileType);

                verifyNoMoreInteractions(databaseService);
                verifyNoInteractions(sftpService);
            }
        }
    }

    @DisplayName("protected Result uploadFiles()")
    @Nested
    class UploadFiles {
        private static final String UPDATE_SQL_QUERY = "UPDATE CONTENT_STORE "
            + "SET DATE_SENT=now(), FAILED_FILE_TRANSFER=false "
            + "WHERE DOCUMENT_ID=? AND FILE_TYPE=? AND DATE_SENT is NULL";

        private static final String UPDATE_SQL_FAILED_QUERY = "UPDATE CONTENT_STORE "
            + "SET FAILED_FILE_TRANSFER=true "
            + "WHERE DOCUMENT_ID=? AND FILE_TYPE=?";

        @Test
        void positiveTypical() throws IOException, SQLException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
                 MockedStatic<FileSearch> fileSearchMock = Mockito.mockStatic(FileSearch.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                FileSearch fileSearch = mock(FileSearch.class);
                fileSearchMock.when(() -> FileSearch.directory(ftpDirectory, true)).thenReturn(fileSearch);

                File file1 = mock(File.class);
                when(file1.getName()).thenReturn("sample1.txt");
                File file2 = mock(File.class);
                when(file2.getName()).thenReturn("sample2.txt");
                File file3 = mock(File.class);
                when(file3.getName()).thenReturn("sample3.txt");
                Set<File> files = new HashSet<>(Set.of(file1, file2, file3));

                when(fileSearch.search()).thenReturn(files);
                when(fileSearch.setFileNameRegexFilter(any())).thenReturn(fileSearch);
                when(sftpService.upload(eq(sftpClass), any(File.class), anyLong(), anyLong()))
                    .thenReturn(true);

                Job.Result result = contentStoreFileJob.uploadFiles();
                assertEquals(5, result.getMetaData().size(),
                    "Expect 5 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_UPLOAD"),
                    "Expect 3 files to be uploaded");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPLOADED_SUCCESS"),
                    "Expect 3 files to be uploaded successfully");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPDATED_SUCCESS"),
                    "Expect 3 files to be updated successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPDATED_UNSUCCESSFULLY"),
                    "Expect 0 files to be updated unsuccessfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY"),
                    "Expect 0 files to be uploaded unsuccessfully");

                verify(fileSearch, times(1)).setFileNameRegexFilter(fileNameRegex);
                for (File file : files) {
                    fileUtilsMock.verify(() -> FileUtils.deleteFile(eq(file)), times(1));
                    verify(sftpService, times(1)).upload(sftpClass, file, 0, 0);
                    verify(databaseService, times(1))
                        .executeUpdate(connection, UPDATE_SQL_QUERY, file.getName(), fileType);
                }

                assertEquals(Status.SUCCESS, result.getStatus(), "Expect status to be SUCCESS");
                assertEquals("Successfully uploaded 3 files", result.getMessage(), "Expect success message");
            }
        }

        @Test
        void updateSqlException() throws IOException, SQLException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
                 MockedStatic<FileSearch> fileSearchMock = Mockito.mockStatic(FileSearch.class)) {
                final ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                FileSearch fileSearch = mock(FileSearch.class);
                fileSearchMock.when(() -> FileSearch.directory(ftpDirectory, true)).thenReturn(fileSearch);

                File file1 = mock(File.class);
                when(file1.getName()).thenReturn("sample1.txt");
                File file2 = mock(File.class);
                when(file2.getName()).thenReturn("sample2.txt");
                File file3 = mock(File.class);
                when(file3.getName()).thenReturn("sample3.txt");
                Set<File> files = new HashSet<>(Set.of(file1, file2, file3));

                when(fileSearch.search()).thenReturn(files);
                when(fileSearch.setFileNameRegexFilter(any())).thenReturn(fileSearch);
                when(sftpService.upload(eq(sftpClass), any(File.class), anyLong(), anyLong())).thenReturn(true);

                SQLException sqlException = new SQLException("Forced sql exception");
                for (File file : files) {
                    fileUtilsMock.when(() ->
                            databaseService.executeUpdate(connection, UPDATE_SQL_QUERY, file.getName(), fileType))
                        .thenThrow(sqlException);
                }

                Job.Result result = contentStoreFileJob.uploadFiles();
                assertEquals(Status.PARTIAL_SUCCESS, result.getStatus());
                assertEquals("0 files failed to upload out of 3", result.getMessage(),
                    "0 files failed to upload out of 3");

                assertEquals(8, result.getMetaData().size(),
                    "Expect 8 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_UPLOAD"),
                    "Expect 3 files to be uploaded");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPLOADED_SUCCESS"),
                    "Expect 3 files to be uploaded successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPDATED_SUCCESS"),
                    "Expect 0 files to be updated successfully");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPDATED_UNSUCCESSFULLY"),
                    "Expect 3 files to be updated unsuccessfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY"),
                    "Expect 0 files to be uploaded unsuccessfully");

                for (File file : files) {
                    fileUtilsMock.verify(() -> FileUtils.deleteFile(eq(file)), times(1));
                    verify(sftpService, times(1)).upload(sftpClass, file, 0, 0);
                    verify(databaseService, times(1))
                        .executeUpdate(connection, UPDATE_SQL_QUERY, file.getName(), fileType);
                }
            }
        }

        @Test
        void setToFailedSqlException() throws IOException, SQLException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
                 MockedStatic<FileSearch> fileSearchMock = Mockito.mockStatic(FileSearch.class)) {
                final ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                FileSearch fileSearch = mock(FileSearch.class);
                fileSearchMock.when(() -> FileSearch.directory(ftpDirectory, true)).thenReturn(fileSearch);

                File file1 = mock(File.class);
                when(file1.getName()).thenReturn("sample1.txt");
                File file2 = mock(File.class);
                when(file2.getName()).thenReturn("sample2.txt");
                Set<File> files = new HashSet<>(Set.of(file1, file2));

                when(fileSearch.search()).thenReturn(files);
                when(fileSearch.setFileNameRegexFilter(any())).thenReturn(fileSearch);
                when(sftpService.upload(eq(sftpClass), any(File.class), anyLong(), anyLong())).thenReturn(false);

                SQLException sqlException = new SQLException("Forced sql exception");
                for (File file : files) {
                    fileUtilsMock.when(() -> databaseService.executeUpdate(connection,
                        UPDATE_SQL_FAILED_QUERY, file.getName(), fileType)).thenThrow(sqlException);
                }

                Job.Result result = contentStoreFileJob.uploadFiles();
                assertEquals(Status.FAILED, result.getStatus());
                assertEquals("2 files failed to upload out of 2", result.getMessage(),
                    "2 files failed to upload out of 2");

                assertEquals(7, result.getMetaData().size(),
                    "Expect 7 metadata entries");
                assertEquals("2", result.getMetaData().get("TOTAL_FILES_TO_UPLOAD"),
                    "Expect 2 files to be uploaded");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPLOADED_SUCCESS"),
                    "Expect 0 files to be uploaded successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPDATED_SUCCESS"),
                    "Expect 0 files to be updated successfully");
                assertEquals("2", result.getMetaData().get("TOTAL_FILES_UPDATED_UNSUCCESSFULLY"),
                    "Expect 2 files to be updated unsuccessfully");
                assertEquals("2", result.getMetaData().get("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY"),
                    "Expect 2 files to be uploaded unsuccessfully");

                for (File file : files) {
                    fileUtilsMock.verify(() -> FileUtils.deleteFile(eq(file)), times(1));
                    verify(sftpService, times(1)).upload(sftpClass, file, 0, 0);
                    verify(databaseService, times(1))
                        .executeUpdate(connection, UPDATE_SQL_FAILED_QUERY, file.getName(), fileType);
                }
            }
        }

        @Test
        void positiveNoFilesFound() throws IOException {
            try (MockedStatic<FileSearch> fileSearchMock = Mockito.mockStatic(FileSearch.class)) {
                FileSearch fileSearch = mock(FileSearch.class);
                when(fileSearch.setFileNameRegexFilter(any())).thenReturn(fileSearch);

                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                fileSearchMock.when(() -> FileSearch.directory(ftpDirectory, true))
                    .thenReturn(fileSearch);

                when(fileSearch.search()).thenReturn(Set.of());

                Job.Result result = contentStoreFileJob.uploadFiles();
                assertEquals(5, result.getMetaData().size(), "Expect 3 metadata entries");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_TO_UPLOAD"), "Expect 0 files to be uploaded");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPLOADED_SUCCESS"),
                    "Expect 0 files to be uploaded successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY"),
                    "Expect 0 files to be uploaded unsuccessfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPDATED_SUCCESS"),
                    "Expect 0 files to be updated successfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPDATED_UNSUCCESSFULLY"),
                    "Expect 0 files to be updated unsuccessfully");
                assertEquals(Status.SUCCESS, result.getStatus(), "Expect status to be SUCCESS");
                assertEquals("No files found", result.getMessage(), "Expect no files found message");
                verify(fileSearch, times(1)).setFileNameRegexFilter(fileNameRegex);
            }
        }

        @Test
        @SuppressWarnings("VariableDeclarationUsageDistance")//Required for mocks setup
        void negativeParticularSuccess() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
                 MockedStatic<FileSearch> fileSearchMock = Mockito.mockStatic(FileSearch.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                FileSearch fileSearch = mock(FileSearch.class);
                fileSearchMock.when(() -> FileSearch.directory(ftpDirectory, true)).thenReturn(fileSearch);

                File failedFile = mock(File.class);
                when(failedFile.getName()).thenReturn("test.txt");
                Set<File> workingFiles =
                    new HashSet<>(Set.of(mock(File.class), mock(File.class), mock(File.class), failedFile));
                Set<File> allFiles = new HashSet<>(workingFiles);
                allFiles.add(failedFile);

                when(fileSearch.search()).thenReturn(allFiles);
                when(fileSearch.setFileNameRegexFilter(any())).thenReturn(fileSearch);
                for (File file : workingFiles) {
                    when(sftpService.upload(sftpClass, file, 0, 0)).thenReturn(true);
                }
                when(sftpService.upload(sftpClass, failedFile, 0, 0)).thenReturn(false);


                Job.Result result = contentStoreFileJob.uploadFiles();
                assertEquals(6, result.getMetaData().size(), "Expect 6 metadata entries");
                assertEquals("test.txt", result.getMetaData().get("FAILED_TO_UPLOAD_FILE_1"),
                    "Expect failed file name");
                assertEquals("4", result.getMetaData().get("TOTAL_FILES_TO_UPLOAD"),
                    "Expect 4 files to be uploaded");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPLOADED_SUCCESS"),
                    "Expect 3 files to be uploaded successfully");

                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPDATED_SUCCESS"),
                    "Expect 3 files to be updated successfully");
                assertEquals("1", result.getMetaData().get("TOTAL_FILES_UPDATED_UNSUCCESSFULLY"),
                    "Expect 1 file to be updated unsuccessfully");

                assertEquals("1", result.getMetaData().get("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY"),
                    "Expect 1 files to be uploaded unsuccessfully");

                verify(fileSearch, times(1)).setFileNameRegexFilter(fileNameRegex);

                for (File file : workingFiles) {
                    verify(sftpService, times(1)).upload(sftpClass, file, 0, 0);
                    fileUtilsMock.verify(() -> FileUtils.deleteFile(any()), times(4));
                }
                verify(sftpService, times(1)).upload(sftpClass, failedFile, 0, 0);
                fileUtilsMock.verify(() -> FileUtils.deleteFile(failedFile), times(1));
                assertEquals(Status.PARTIAL_SUCCESS, result.getStatus());
                assertEquals("1 files failed to upload out of 4",
                    result.getMessage(), "Expect failure message");
            }
        }

        @Test
        void negativeAllFilesFailedToUpload() throws IOException {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class);
                 MockedStatic<FileSearch> fileSearchMock = Mockito.mockStatic(FileSearch.class)) {
                ContentStoreFileJob contentStoreFileJob = getContentStoreFileJob();
                FileSearch fileSearch = mock(FileSearch.class);
                fileSearchMock.when(() -> FileSearch.directory(ftpDirectory, true)).thenReturn(fileSearch);

                Function<String, File> createFileMock = name -> {
                    File file = mock(File.class);
                    when(file.getName()).thenReturn(name);
                    return file;
                };

                Set<File> files = Set.of(
                    createFileMock.apply("Test1.txt"),
                    createFileMock.apply("Test2.txt"),
                    createFileMock.apply("Test3.txt"));

                Set<File> uploadFiles = new HashSet<>(files);

                when(fileSearch.search()).thenReturn(uploadFiles);
                when(fileSearch.setFileNameRegexFilter(any())).thenReturn(fileSearch);
                when(sftpService.upload(sftpClass, files, 1, 1)).thenReturn(files);

                Job.Result result = contentStoreFileJob.uploadFiles();
                assertEquals(8, result.getMetaData().size(), "Expect 8 metadata entries");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_TO_UPLOAD"), "Expect 3 files to be uploaded");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPLOADED_SUCCESS"),
                    "Expect 0 files to be uploaded successfully");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY"),
                    "Expect 3 files to be uploaded unsuccessfully");
                assertEquals("0", result.getMetaData().get("TOTAL_FILES_UPDATED_SUCCESS"),
                    "Expect 0 files to be updated successfully");
                assertEquals("3", result.getMetaData().get("TOTAL_FILES_UPDATED_UNSUCCESSFULLY"),
                    "Expect 3 files to be updated unsuccessfully");

                verify(fileSearch, times(1)).setFileNameRegexFilter(fileNameRegex);

                for (File file : files) {
                    verify(sftpService, times(1)).upload(sftpClass, file, 0, 0);
                    fileUtilsMock.verify(() -> FileUtils.deleteFile(any()), times(3));
                }

                fileUtilsMock.verify(() -> FileUtils.deleteFiles(uploadFiles), never());
                assertEquals(Status.FAILED, result.getStatus());

                assertThat(result.getMessage(), startsWith("3 files failed to upload out of 3"));
                List<String> failedFilesSorted = Stream.of(
                    result.getMetaData().get("FAILED_TO_UPLOAD_FILE_1"),
                    result.getMetaData().get("FAILED_TO_UPLOAD_FILE_2"),
                    result.getMetaData().get("FAILED_TO_UPLOAD_FILE_3")).sorted().toList();

                assertEquals("Test1.txt", failedFilesSorted.get(0), "Expect Test1.txt to be the first failed file");
                assertEquals("Test2.txt", failedFilesSorted.get(1), "Expect Test2.txt to be the second failed file");
                assertEquals("Test3.txt", failedFilesSorted.get(2), "Expect Test3.txt to be the third failed file");
            }
        }
    }

    protected class ContentStoreFileJobImpl extends ContentStoreFileJob {

        protected ContentStoreFileJobImpl(File ftpDirectory,
                                          DatabaseConfig databaseConfig, String fileType, String procedureName,
                                          Object[] procedureArguments, String fileNameRegex,
                                          Class<? extends Sftp> sftpClass) {
            super(sftpService, databaseService, ftpDirectory, databaseConfig, fileType, procedureName,
                procedureArguments, fileNameRegex, sftpClass, 0, 0);
        }
    }
}
