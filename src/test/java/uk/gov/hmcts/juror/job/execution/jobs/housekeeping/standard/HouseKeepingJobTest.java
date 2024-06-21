package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import org.apache.commons.lang3.RandomUtils;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.testsupport.AbstractStoredProcedureJobTest;

class HouseKeepingJobTest
    extends AbstractStoredProcedureJobTest<HouseKeepingJob, HouseKeepingConfig> {

    private static final String PROCEDURE_NAME = "juror_mod.housekeeping_process";

    protected HouseKeepingJobTest() {
        super(PROCEDURE_NAME);
    }

    @Override
    public HouseKeepingJob createStoredProcedureJob(DatabaseService databaseService,
                                                    HouseKeepingConfig config) {
        return new HouseKeepingJob(databaseService, config);
    }

    @Override
    public HouseKeepingConfig createConfig() {
        HouseKeepingConfig config = new HouseKeepingConfig();
        config.setMaxTimeout(RandomUtils.nextInt());
        return config;
    }

    @Override
    protected Object[] getProcedureArguments(HouseKeepingConfig config) {
        return new Object[]{config.getMaxTimeout(), config.getOwnerRestrict()};
    }
}
