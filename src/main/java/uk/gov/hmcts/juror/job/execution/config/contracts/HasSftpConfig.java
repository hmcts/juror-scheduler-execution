package uk.gov.hmcts.juror.job.execution.config.contracts;

import uk.gov.hmcts.juror.job.execution.config.SftpConfig;

public interface HasSftpConfig {

    SftpConfig getSftp();
    void setSftp(SftpConfig sftp);
}
