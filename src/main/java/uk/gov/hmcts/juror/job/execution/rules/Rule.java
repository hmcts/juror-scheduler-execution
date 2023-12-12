package uk.gov.hmcts.juror.job.execution.rules;

@SuppressWarnings("PMD.ShortClassName")
public interface Rule {
    boolean execute();

    String getMessage();
}
