package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;

@Getter
@Setter
@Accessors(chain = true)
public class BureauLettersToBePrintedDB {
    @DatabaseColumn(name = "type", setter = "setType")
    public String type;
    @DatabaseColumn(name = "description", setter = "setDescription")
    public String description;
    @DatabaseColumn(name = "count", setter = "setCount")
    public Long count;
}
