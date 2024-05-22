package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.config.SmtpConfig;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data.DashboardData;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmsDashboardGenerateJobTest {
    private SchedulerServiceClient schedulerServiceClient;
    private DatabaseService databaseService;
    private AmsDashboardConfig config;
    private Clock clock;

    private AmsDashboardGenerateJob amsDashboardGenerateJob;

    private MockedStatic<FileUtils> fileUtilsMock;

    @BeforeEach
    void beforeEach() {
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.databaseService = mock(DatabaseService.class);
        this.config = createConfig();
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.amsDashboardGenerateJob = spy(new AmsDashboardGenerateJob(schedulerServiceClient, databaseService, config,
            clock));
    }

    public static AmsDashboardConfig createConfig() {
        AmsDashboardConfig config = new AmsDashboardConfig();
        config.setDatabase(mock(DatabaseConfig.class));
        config.setSmtp(mock(SmtpConfig.class));
        config.setEmailRecipients(
            new String[]{
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10)});
        config.setPncCertificateLocation(mock(File.class));
        config.setDashboardCsvLocation(mock(File.class));
        config.setPncCertificatePassword(RandomStringUtils.randomAlphabetic(10));
        config.setPncCertificateAlias(RandomStringUtils.randomAlphabetic(10));
        return config;
    }

    @AfterEach
    void afterEach() {
        if (fileUtilsMock != null) {
            fileUtilsMock.close();
        }
    }

    @Test
    void positiveConstructorTest() {
        assertSame(schedulerServiceClient, amsDashboardGenerateJob.getSchedulerServiceClient());
        assertSame(databaseService, amsDashboardGenerateJob.getDatabaseService());
        assertSame(config, amsDashboardGenerateJob.getConfig());
        assertSame(clock, amsDashboardGenerateJob.getClock());
    }

    @Test
    void positiveGetResultSuppliers() {
        List<Job.ResultSupplier> resultSuppliers = amsDashboardGenerateJob.getResultSuppliers();
        assertEquals(2, resultSuppliers.size(), "There should be 2 result suppliers");
        Job.ResultSupplier resultSupplier1 = resultSuppliers.get(0);
        Job.ResultSupplier resultSupplier2 = resultSuppliers.get(1);


        assertTrue(resultSupplier1.isContinueOnFailure(), "First result supplier should continue on failure");
        assertEquals(8, resultSupplier1.getResultRunners().size(), "First result supplier should have 7 runners");

        assertFalse(resultSupplier2.isContinueOnFailure(), "Second result supplier should not continue on failure");
        assertEquals(1, resultSupplier2.getResultRunners().size(), "Second result supplier should have 1 runner");


        Job.Result expectedResult = Job.Result.passed("Some Message");
        when(amsDashboardGenerateJob.generateDashboardFile(any())).thenReturn(expectedResult);
        Function<MetaData, Job.Result>
            resultSupplier2Runner = resultSupplier2.getResultRunners().iterator().next();
        MetaData metaData = mock(MetaData.class);
        Job.Result result = resultSupplier2Runner.apply(metaData);
        verify(amsDashboardGenerateJob, times(1)).generateDashboardFile(any());
        assertSame(expectedResult, result, "Result should be the same as the expected result");


    }

    @Test
    void positiveGenerateDashboardFile() {
        fileUtilsMock = Mockito.mockStatic(FileUtils.class);
        final String testCsv = "test,test2,test3";
        DashboardData dashboardData = mock(DashboardData.class);
        when(dashboardData.toCsv(clock)).thenReturn(testCsv);

        Job.Result result = amsDashboardGenerateJob.generateDashboardFile(dashboardData);
        assertEquals(Job.Result.passed(), result, "Result should be passed");

        verify(dashboardData, times(1)).toCsv(clock);


        fileUtilsMock.verify(() -> {
            try {
                FileUtils.writeToFile(config.getDashboardCsvLocation(), testCsv);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, times(0));
    }

    @Test
    void negativeGenerateDashboardFileUnexpectedException() {
        RuntimeException cause = new RuntimeException("I am the cause");
        DashboardData dashboardData = mock(DashboardData.class);
        when(dashboardData.toCsv(clock)).thenThrow(cause);

        Job.Result result = amsDashboardGenerateJob.generateDashboardFile(dashboardData);
        assertEquals(Job.Result.failed("Failed to output dashboard csv", cause),
            result, "Result should be failed with exception and message");

        assertSame(cause, result.getThrowable(), "Exception should be the same as the cause");
        verify(dashboardData, times(1)).toCsv(clock);
    }
}
