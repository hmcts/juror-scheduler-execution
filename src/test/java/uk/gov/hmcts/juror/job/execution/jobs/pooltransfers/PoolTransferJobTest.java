package uk.gov.hmcts.juror.job.execution.jobs.pooltransfers;

import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.testsupport.AbstractStoredProcedureJobTest;

public class PoolTransferJobTest extends AbstractStoredProcedureJobTest<PoolTransferJob, PoolTransferConfig> {
    private static final String PROCEDURE_NAME = "pool_transfer.transfer_pool_details";


    protected PoolTransferJobTest() {
        super(PROCEDURE_NAME);
    }

    @Override
    public PoolTransferJob createStoredProcedureJob(DatabaseService databaseService, PoolTransferConfig config) {
        return new PoolTransferJob(databaseService, config);
    }

    @Override
    public PoolTransferConfig createConfig() {
        return new PoolTransferConfig();
    }
}
