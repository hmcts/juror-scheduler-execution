package uk.gov.hmcts.juror.job.execution.config.contracts;

import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;

public interface HasSmtpConfig {
    SmtpConfig getSmtp();

    void setSmtp(SmtpConfig smtpConfig);
}
