package uk.gov.hmcts.juror.job.execution.rules;

public interface Rule {
    boolean execute();

    String getMessage();
}
