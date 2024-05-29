package uk.gov.hmcts.juror.job.execution.jobs.pooltransfers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.jobs.StoredProcedureJob;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;

@Component
public class PoolTransferJob extends StoredProcedureJob {
    @Autowired
    public PoolTransferJob(DatabaseService databaseService, PoolTransferConfig config) {
        super(databaseService, config.getDatabase(), "juror_mod.transfer_pool_details");
    }
}
