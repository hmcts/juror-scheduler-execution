package uk.gov.hmcts.juror.job.execution.jobs.contentstore;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.model.ContentStore;
import uk.gov.hmcts.juror.job.execution.jobs.LinearJob;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.rules.Rules;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;
import uk.gov.hmcts.juror.job.execution.util.FileSearch;
import uk.gov.hmcts.juror.job.execution.util.FileUtils;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
public abstract class ContentStoreFileJob extends LinearJob {
    private static final String SELECT_SQL_QUERY = "SELECT CS.REQUEST_ID, CS.DOCUMENT_ID, CS.DATA "
        + "FROM CONTENT_STORE CS "
        + "WHERE CS.FILE_TYPE=? AND CS.DATE_SENT is NULL";

    private static final String UPDATE_SQL_QUERY = "UPDATE CONTENT_STORE CS "
        + "SET CS.DATE_SENT=SYSDATE "
        + "WHERE CS.REQUEST_ID=? AND CS.FILE_TYPE=?";

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
        addRules(
            Rules.requireDirectory(this.getFtpDirectory())
        );
    }


    protected Result generateFiles() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(0);

        databaseService.execute(getDatabaseConfig(), connection -> {
            log.info(fileType + ": Updating Content-Store");
            databaseService.executeStoredProcedure(connection, getProcedureName(), getProcedureArguments());
            log.info(fileType + ": Getting Items to generate");
            List<ContentStore> contentStoreList =
                databaseService.executePreparedStatement(connection, ContentStore.class, SELECT_SQL_QUERY,
                    fileType);

            totalFiles.set(contentStoreList.size());
            AtomicInteger count = new AtomicInteger(1);

            contentStoreList.forEach(contentStore -> {
                try {
                    log.info(fileType + ": Generating file " + count.getAndIncrement() + "/" + totalFiles.get() + ": "
                        + contentStore.getDocumentId());
                    File file = FileUtils.createFile(
                        this.getFtpDirectory().getAbsolutePath() + '/' + contentStore.getDocumentId());

                    FileUtils.writeToFile(file, contentStore.getData());

                    databaseService.executeUpdate(connection, UPDATE_SQL_QUERY, contentStore.getRequestId(), fileType);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error(fileType + ": Failed to generate file for: " + contentStore.getDocumentId(), e);
                    metaData.put("FAILED_TO_GENERATE_FILE_" + failureCount.incrementAndGet(),
                        contentStore.getDocumentId());
                }
            });
        });
        metaData.put("TOTAL_FILES_TO_GENERATED", String.valueOf(totalFiles));
        metaData.put("TOTAL_FILES_GENERATED_SUCCESS", successCount.toString());
        metaData.put("TOTAL_FILES_GENERATED_UNSUCCESSFULLY", failureCount.toString());
        if (failureCount.get() > 0) {
            return Result.partialSuccess(
                    failureCount.get() + " files failed to generate out of " + totalFiles.get() + ".")
                .addMetaData(metaData);
        } else {
            return Result.passed()
                .addMetaData(metaData);
        }
    }

    protected Result uploadFiles() {
        Map<String, String> metaData = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
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
                    FileUtils.deleteFile(file);
                    successCount.incrementAndGet();
                } else {
                    metaData.put("FAILED_TO_UPLOAD_FILE_" + failureCount.incrementAndGet(), file.getName());
                }
            });
            log.info(fileType + ": Job completed");

            if (failureCount.get() > 0) {
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
        return new Result(status, message).addMetaData(metaData);
    }

    @Override
    public ResultSupplier getResultSupplier() {
        return new ResultSupplier(false, Set.of(
            metaData -> generateFiles(),
            metaData -> uploadFiles()
        ));
    }
}
