package uk.gov.hmcts.juror.job.execution.service.contracts;

import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;

public interface SmtpService {

    void sendEmail(SmtpConfig config, String subject, String text, String... recipients);
}
