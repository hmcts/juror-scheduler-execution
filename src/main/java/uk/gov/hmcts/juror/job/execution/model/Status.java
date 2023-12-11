package uk.gov.hmcts.juror.job.execution.model;

import lombok.Getter;

@Getter
public enum Status {
    VALIDATION_PASSED(1),
    VALIDATION_FAILED(2),
    PROCESSING(3),
    SUCCESS(4),
    PARTIAL_SUCCESS(5),
    PENDING(6),
    INDETERMINATE(7),
    FAILED(8),
    FAILED_UNEXPECTED_EXCEPTION(9);


    private final int priority;

    Status(int priority) {
        this.priority = priority;
    }
}
