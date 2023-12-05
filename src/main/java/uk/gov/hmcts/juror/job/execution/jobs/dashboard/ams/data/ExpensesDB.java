package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;

@Getter
@Setter
@Accessors(chain = true)
public class ExpensesDB {

    @DatabaseColumn(name = "date", setter = "setDate")
    public String date;
    @DatabaseColumn(name = "amount", setter = "setAmount")
    public String amount;
}
