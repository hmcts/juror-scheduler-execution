package uk.gov.hmcts.juror.job.execution.jobs.housekeeping.standard;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.testsupport.AbstractStoredProcedureJobTest;

import java.sql.Connection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class HouseKeepingJobTest
    extends AbstractStoredProcedureJobTest<HouseKeepingJob, HouseKeepingConfig> {

    private static final int INACTIVE_USER_THRESHOLD_MONTHS = 6;
    private static final String PROCEDURE_NAME = HouseKeepingJob.HOUSEKEEPING_PROCEDURE;

    protected HouseKeepingJobTest() {
        super(PROCEDURE_NAME);
    }

    @Override
    public HouseKeepingJob createStoredProcedureJob(DatabaseService databaseService,
                                                    HouseKeepingConfig config) {
        log.info("Juror Housekeeping timeout is set to: {}", config.getMaxTimeout());
        log.info("Juror Housekeeping owner restrict is set to: {}", config.getOwnerRestrict());
        return new HouseKeepingJob(databaseService, config);
    }

    @Override
    public HouseKeepingConfig createConfig() {
        HouseKeepingConfig config = new HouseKeepingConfig();
        config.setMaxTimeout(RandomUtils.nextInt());
        config.setInactiveUserThresholdMonths(INACTIVE_USER_THRESHOLD_MONTHS);
        return config;
    }

    @Override
    protected Object[] getProcedureArguments(HouseKeepingConfig config) {
        return new Object[]{config.getMaxTimeout(), config.getOwnerRestrict()};
    }

    @Override
    protected void verifyAdditionalStoredProcedures(DatabaseService databaseService, Connection connection,
                                                    HouseKeepingJob job, HouseKeepingConfig config) {
        verify(databaseService, times(1))
            .executeStoredProcedure(connection, HouseKeepingJob.DEACTIVATE_INACTIVE_USERS_PROCEDURE,
                config.getInactiveUserThresholdMonths());
    }

    @Test
    @SuppressWarnings("unchecked")
    void positiveExecuteStoredProcedureRunsHousekeepingAndInactiveUserDeactivation() {
        DatabaseService databaseService = mock(DatabaseService.class);
        DatabaseConfig databaseConfig = mock(DatabaseConfig.class);
        HouseKeepingConfig config = createConfig();
        config.setDatabase(databaseConfig);
        HouseKeepingJob job = createStoredProcedureJob(databaseService, config);

        Job.Result result = job.executeStoredProcedure();

        assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
        assertNull(result.getMessage(), "ErrorMessage should be null");
        assertNull(result.getThrowable(), "Exception should be null");
        assertEquals(0, result.getMetaData().size(), "MetaData should be empty");

        ArgumentCaptor<Consumer<Connection>> connectionConsumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(databaseService, times(1)).execute(eq(databaseConfig), connectionConsumerCaptor.capture());

        Connection connection = mock(Connection.class);
        connectionConsumerCaptor.getValue().accept(connection);

        verify(databaseService, times(1))
            .executeStoredProcedure(connection, HouseKeepingJob.HOUSEKEEPING_PROCEDURE, job.getProcedureArguments());
        verify(databaseService, times(1))
            .executeStoredProcedure(connection, HouseKeepingJob.DEACTIVATE_INACTIVE_USERS_PROCEDURE,
                config.getInactiveUserThresholdMonths());
    }
}
