package uk.gov.hmcts.juror.job.execution.database.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;

@Getter
@Setter
@Accessors(chain = true)
public class Count {
    @DatabaseColumn(name = "count", setter = "setValue")
    private Integer value;
}
