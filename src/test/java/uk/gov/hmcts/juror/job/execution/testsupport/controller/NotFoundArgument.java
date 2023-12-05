package uk.gov.hmcts.juror.job.execution.testsupport.controller;

import org.springframework.http.HttpStatus;

public class NotFoundArgument  extends ErrorRequestArgument {
    public NotFoundArgument(String requestPayload) {
        super(HttpStatus.NOT_FOUND, requestPayload, "NOT_FOUND", "The requested resource could not be located.");
    }
    public NotFoundArgument() {
        super(HttpStatus.NOT_FOUND, null, "NOT_FOUND", "The requested resource could not be located.");
    }
}
