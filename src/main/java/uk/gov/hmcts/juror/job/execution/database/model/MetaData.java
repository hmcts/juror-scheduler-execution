package uk.gov.hmcts.juror.job.execution.database.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class MetaData {
    private final String jobKey;
    private final Long taskId;

    private Map<String, String> queryParams;
}
