package uk.gov.hmcts.juror.job.execution.database.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class MetaData {
    private final String jobKey;
    private final Long taskId;
}
