package uk.gov.hmcts.juror.job.execution.service.contracts;

import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.util.Collection;

public interface SftpService {
    default Collection<File> upload(Class<? extends Sftp> sftpClass, Collection<File> filesToProcess) {
        return upload(sftpClass, filesToProcess, 0, 0);
    }

    Collection<File> upload(Class<? extends Sftp> sftpClass, Collection<File> filesToProcess,
                            long retryLimit, long retryDelay);

    default boolean upload(Class<? extends Sftp> sftpClass, File fileToProcess) {
        return upload(sftpClass, fileToProcess, 0, 0);
    }

    boolean upload(Class<? extends Sftp> sftpClass, File fileToProcess, long retryLimit, long retryDelay);
}
