package uk.gov.hmcts.juror.job.execution.service.contracts;

import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.util.Collection;

public interface SftpService {
    Collection<File> upload(Class<? extends Sftp> sftpClass,Collection<File> filesToProcess);
    boolean upload(Class<? extends Sftp> sftpClass,File fileToProcess);
}
