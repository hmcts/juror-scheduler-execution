package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmsDashboardSendJobTest {

    private SftpService sftpService;
    private AmsDashboardConfig config;
    private AmsDashboardSendJob amsDashboardSendJob;

    @BeforeEach
    void beforeEach(){
        this.sftpService = mock(SftpService.class);
        this.config = AmsDashboardGenerateJobTest.createConfig();
        this.amsDashboardSendJob = spy(new AmsDashboardSendJob(sftpService, config));
    }

    @Test
    void positiveConstructorTest(){
        assertSame(sftpService, amsDashboardSendJob.getSftpService());
        assertSame(config, amsDashboardSendJob.getConfig());
    }

    @Test
    void positiveGetResultSupplierTest(){
        Job.ResultSupplier resultSupplier = amsDashboardSendJob.getResultSupplier();
        MetaData metaData = mock(MetaData.class);


        assertFalse(resultSupplier.isContinueOnFailure(), "Result supplier should not continue on failure");
        assertEquals(1, resultSupplier.getResultRunners().size(), "Result supplier should have 1 runner");


        Job.Result expectedResult = Job.Result.passed("Some Message");
        when(amsDashboardSendJob.sendDashboardFile()).thenReturn(expectedResult);
        Function<MetaData, Job.Result>
            resultSupplier2Runner = resultSupplier.getResultRunners().iterator().next();

        Job.Result result = resultSupplier2Runner.apply(metaData);
        verify(amsDashboardSendJob, times(1)).sendDashboardFile();
        assertSame(expectedResult, result, "Result should be the same as the expected result");
    }

    @Test
    void positiveSendDashboardFileSuccess(){
        when(sftpService.upload(AmsDashboardSftp.class, config.getDashboardCsvLocation()))
            .thenReturn(true);
        Job.Result result = amsDashboardSendJob.sendDashboardFile();
        assertEquals(Job.Result.passed(), result, "Result should be passed");
        verify(sftpService,times(1))
            .upload(AmsDashboardSftp.class, config.getDashboardCsvLocation());
    }
    @Test
    void negativeSendDashboardFileFail(){
        when(sftpService.upload(AmsDashboardSftp.class, config.getDashboardCsvLocation()))
            .thenReturn(false);
        Job.Result result = amsDashboardSendJob.sendDashboardFile();
        assertEquals(Job.Result.failed("Failed to upload dashboard file"), result,
            "Result should be failed with exception and message");
        verify(sftpService,times(1))
            .upload(AmsDashboardSftp.class, config.getDashboardCsvLocation());

    }

}
