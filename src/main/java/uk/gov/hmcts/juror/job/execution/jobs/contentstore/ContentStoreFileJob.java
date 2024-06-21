package uk.gov.hmcts.juror.job.execution.jobs.contentstore;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.ContentStore;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.rules.Rules;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;
import uk.gov.hmcts.juror.job.execution.util.FileSearch;
import uk.gov.hmcts.juror.job.execution.util.FileUtils;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
public abstract class ContentStoreFileJob extends LinearJob {
    private static final String SELECT_SQL_QUERY = "SELECT CS.REQUEST_ID, CS.DOCUMENT_ID, CS.DATA, "
        + "CS.FAILED_FILE_TRANSFER "
        + "FROM CONTENT_STORE CS "
        + "WHERE CS.FILE_TYPE=? "
        + "AND CS.DATE_SENT is NULL";

    private static final String UPDATE_SQL_QUERY = "UPDATE CONTENT_STORE "
        + "SET DATE_SENT=now(), FAILED_FILE_TRANSFER=false "
        + "WHERE DOCUMENT_ID=? AND FILE_TYPE=?";

    private static final String UPDATE_SQL_FAILED_QUERY = "UPDATE CONTENT_STORE "
        + "SET FAILED_FILE_TRANSFER=true "
        + "WHERE DOCUMENT_ID=? AND FILE_TYPE=?";

    private final SftpService sftpService;
    private final DatabaseService databaseService;
    private final Class<? extends Sftp> sftpClass;
    private final File ftpDirectory;
    private final DatabaseConfig databaseConfig;
    private final String procedureName;
    private final Object[] procedureArguments;
    private final String fileNameRegex;
    private final String fileType;

    protected ContentStoreFileJob(
        SftpService sftpService,
        DatabaseService databaseService,
        File ftpDirectory,
        DatabaseConfig databaseConfig,
        String fileType,
        String procedureName,
        Object[] procedureArguments,
        String fileNameRegex,
        Class<? extends Sftp> sftpClass
    ) {
        super();
        this.sftpService = sftpService;
        this.databaseService = databaseService;
        this.ftpDirectory = ftpDirectory;
        this.databaseConfig = databaseConfig;
        this.fileType = fileType;
        this.procedureName = procedureName;
        this.procedureArguments = procedureArguments.clone();
        this.fileNameRegex = fileNameRegex;
        this.sftpClass = sftpClass;
        new File(this.getFtpDirectory().getAbsolutePath()).mkdir();
        addRules(
            Rules.requireDirectory(this.getFtpDirectory())
        );
    }

    private boolean isRunFailedOnly(MetaData metaData) {
        if (metaData.getRequestParams().containsKey("onlyRunFailed")) {
            return Boolean.parseBoolean(metaData.getRequestParams().get("onlyRunFailed"));
        } else {
            return false;
        }
    }

    protected Result generateFiles(MetaData metaData) {
        Map<String, String> resultMetaData = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(0);

        boolean isRunFailedOnly = isRunFailedOnly(metaData);
        databaseService.execute(getDatabaseConfig(), connection -> {
            if (!isRunFailedOnly) {
                log.info(fileType + ": Updating Content-Store");
                databaseService.executeStoredProcedure(connection, getProcedureName(), getProcedureArguments());
            }
            log.info(fileType + ": Getting Items to generate");

            List<ContentStore> contentStoreList =
                databaseService.executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY, fileType);

            // check the flag - only run failed transfers, otherwise run all (failed and new)
            if (isRunFailedOnly) {
                contentStoreList = contentStoreList
                    .stream()
                    .filter(ContentStore::isFailedFileTransfer)
                    .toList();
            }

            totalFiles.set(contentStoreList.size());
            AtomicInteger count = new AtomicInteger(1);

            contentStoreList.forEach(contentStore -> {
                try {
                    log.info(fileType + ": Generating file " + count.getAndIncrement() + "/" + totalFiles.get() + ": "
                        + contentStore.getDocumentId());
                    File file = FileUtils.createFile(
                        this.getFtpDirectory().getAbsolutePath() + '/' + contentStore.getDocumentId());

                    FileUtils.writeToFile(file, contentStore.getData());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error(fileType + ": Failed to generate file for: " + contentStore.getDocumentId(), e);
                    resultMetaData.put("FAILED_TO_GENERATE_FILE_" + failureCount.incrementAndGet(),
                        contentStore.getDocumentId());
                }
            });
        });
        resultMetaData.put("TOTAL_FILES_TO_GENERATED", String.valueOf(totalFiles));
        resultMetaData.put("TOTAL_FILES_GENERATED_SUCCESS", successCount.toString());
        resultMetaData.put("TOTAL_FILES_GENERATED_UNSUCCESSFULLY", failureCount.toString());
        if (failureCount.get() > 0) {
            return Result.partialSuccess(
                    failureCount.get() + " files failed to generate out of " + totalFiles.get() + ".")
                .addMetaData(resultMetaData);
        } else {
            return Result.passed()
                .addMetaData(resultMetaData);
        }
    }

    protected Result uploadFiles() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger successUpdateCount = new AtomicInteger(0);
        AtomicInteger failedUpdateCount = new AtomicInteger(0);
        String message;
        Status status;

        log.info(fileType + ": Uploading generated files");
        Set<File> filesToProcess = FileSearch.directory(this.getFtpDirectory(), true)
            .setFileNameRegexFilter(fileNameRegex).search();
        int totalFilesToUpload = filesToProcess.size();

        if (filesToProcess.isEmpty()) {
            log.info(fileType + ": No files found");
            status = Status.SUCCESS;
            message = "No files found";
        } else {
            log.info(fileType + ": Attempting to upload files");
            filesToProcess.forEach(file -> {
                if (sftpService.upload(sftpClass, file)) {
                    updateDateSent(file, successUpdateCount, failedUpdateCount, metaData);
                    successCount.incrementAndGet();
                } else {
                    setToFailed(file);
                    metaData.put("FAILED_TO_UPLOAD_FILE_" + failureCount.incrementAndGet(), file.getName());
                    failedUpdateCount.incrementAndGet();
                }
                FileUtils.deleteFile(file);
            });
            log.info(fileType + ": Job completed");

            if (failureCount.get() > 0 || failedUpdateCount.get() > 0) {
                message = failureCount.get() + " files failed to upload out of " + filesToProcess.size();

                if (failureCount.get() == totalFilesToUpload) {
                    status = Status.FAILED;
                } else {
                    status = Status.PARTIAL_SUCCESS;
                }
            } else {
                status = Status.SUCCESS;
                message = "Successfully uploaded " + totalFilesToUpload + " files";
            }
        }

        metaData.put("TOTAL_FILES_TO_UPLOAD", String.valueOf(totalFilesToUpload));
        metaData.put("TOTAL_FILES_UPLOADED_SUCCESS", successCount.toString());
        metaData.put("TOTAL_FILES_UPLOADED_UNSUCCESSFULLY", failureCount.toString());
        metaData.put("TOTAL_FILES_UPDATED_SUCCESS", successUpdateCount.toString());
        metaData.put("TOTAL_FILES_UPDATED_UNSUCCESSFULLY", failedUpdateCount.toString());

        return new Result(status, message).addMetaData(metaData);
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(false, List.of(
            this::generateFiles,
            metaData -> uploadFiles()
        ));
    }

    private void updateDateSent(File file, AtomicInteger successUpdateCount, AtomicInteger failedUpdateCount,
                                Map<String, String> metaData) {
        databaseService.execute(getDatabaseConfig(), connection -> {
            try {
                databaseService.executeUpdate(connection, UPDATE_SQL_QUERY, file.getName(), fileType);
                successUpdateCount.incrementAndGet();
            } catch (SQLException e) {
                log.error(fileType + ": Failed to update file: " + file.getName() + " as uploaded", e);
                metaData.put("FAILED_TO_UPDATE_FILE_" + failedUpdateCount.incrementAndGet(), "file.getName()");
            }
        });
    }

    private void setToFailed(File file) {
        databaseService.execute(getDatabaseConfig(), connection -> {
            try {
                databaseService.executeUpdate(connection, UPDATE_SQL_FAILED_QUERY, file.getName(), fileType);
            } catch (SQLException e) {
                log.error(fileType + ": Failed to set failed file transfer flag: " + file.getName(), e);
            }
        });
    }
}
