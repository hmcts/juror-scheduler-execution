package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.jurordigital;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.testsupport.AbstractStoredProcedureJobTest;

@Slf4j
class JurorDigitalHouseKeepingJobTest
    extends AbstractStoredProcedureJobTest<JurorDigitalHouseKeepingJob, JurorDigitalHouseKeepingConfig> {

    private static final String PROCEDURE_NAME = "juror_mod.housekeeping_digital_process";

    protected JurorDigitalHouseKeepingJobTest() {
        super(PROCEDURE_NAME);
    }

    @Override
    public JurorDigitalHouseKeepingJob createStoredProcedureJob(DatabaseService databaseService,
                                                                JurorDigitalHouseKeepingConfig config) {
        log.info("Juror Digital Housekeeping timeout is set to: {}", config.getMaxTimeout());
        return new JurorDigitalHouseKeepingJob(databaseService, config);
    }

    @Override
    public JurorDigitalHouseKeepingConfig createConfig() {
        JurorDigitalHouseKeepingConfig config = new JurorDigitalHouseKeepingConfig();
        config.setMaxTimeout(RandomUtils.nextInt());
        return config;
    }

    @Override
    protected Object[] getProcedureArguments(JurorDigitalHouseKeepingConfig config) {
        return new Object[]{config.getMaxTimeout()};
    }
}
