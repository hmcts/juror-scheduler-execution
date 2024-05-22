package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.job.execution.database.DatabaseColumn;

import java.math.BigDecimal;

@Getter
@Setter
@Accessors(chain = true)
public class BureauLettersAutomaticallyGeneratedDB {
    @DatabaseColumn(name = "withdrawal", setter = "setWithdrawal")
    public Long withdrawal;

    @DatabaseColumn(name = "confirmation", setter = "setConfirmation")
    public Long confirmation;
}
