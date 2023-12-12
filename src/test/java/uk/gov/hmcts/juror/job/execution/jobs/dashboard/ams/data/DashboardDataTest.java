package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardConfig;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.AmsDashboardGenerateJobTest;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.service.contracts.SmtpService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class DashboardDataTest {


    private DashboardData dashboardData;
    private SchedulerServiceClient schedulerServiceClient;
    private DatabaseService databaseService;
    private SmtpService smtpService;
    private AmsDashboardConfig config;
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        this.clock = Clock.fixed(Instant.ofEpochMilli(1699889730353L), ZoneId.systemDefault());
        this.schedulerServiceClient = mock(SchedulerServiceClient.class);
        this.databaseService = mock(DatabaseService.class);
        this.smtpService = mock(SmtpService.class);
        this.config = AmsDashboardGenerateJobTest.createConfig();
        this.dashboardData = new DashboardData(schedulerServiceClient, databaseService, smtpService, config, clock);
    }

    @Test
    void positiveConstructorTest() {
        BureauLettersAutomaticallyGenerated bureauLettersAutomaticallyGenerated =
            dashboardData.getBureauLettersAutomaticallyGenerated();
        assertSame(dashboardData, bureauLettersAutomaticallyGenerated.dashboardData,
            "DashboardData should be the same");
        assertSame(schedulerServiceClient, bureauLettersAutomaticallyGenerated.schedulerServiceClient,
            "schedulerServiceClient should be the same");


        BureauLettersToBePrinted bureauLettersToBePrinted = dashboardData.getBureauLettersToBePrinted();
        assertSame(dashboardData, bureauLettersToBePrinted.dashboardData,
            "DashboardData should be the same");
        assertSame(databaseService, bureauLettersToBePrinted.databaseService,
            "databaseService should be the same");
        assertSame(config.getDatabase(), bureauLettersToBePrinted.databaseConfig,
            "databaseConfig should be the same");
        assertSame(clock, bureauLettersToBePrinted.clock,
            "clock should be the same");

        PncCheck pncCheck = dashboardData.getPncCheck();
        assertSame(dashboardData, pncCheck.dashboardData,
            "DashboardData should be the same");
        assertSame(schedulerServiceClient, pncCheck.schedulerServiceClient,
            "schedulerServiceClient should be the same");

        Expenses expenses = dashboardData.getExpenses();
        assertSame(dashboardData, expenses.dashboardData,
            "DashboardData should be the same");
        assertSame(databaseService, expenses.databaseService,
            "databaseService should be the same");
        assertSame(config.getDatabase(), expenses.databaseConfig,
            "databaseConfig should be the same");
        assertSame(clock, expenses.clock,
            "clock should be the same");


        Certificates certificates = dashboardData.getCertificates();
        assertSame(dashboardData, certificates.dashboardData,
            "DashboardData should be the same");
        assertSame(config, certificates.config,
            "config should be the same");
        assertSame(smtpService, certificates.smtpService,
            "smtpService should be the same");
        assertSame(clock, certificates.clock,
            "clock should be the same");

        AutoSys autoSys = dashboardData.getAutoSys();
        assertSame(dashboardData, autoSys.dashboardData,
            "DashboardData should be the same");
        assertSame(schedulerServiceClient, autoSys.schedulerServiceClient,
            "schedulerServiceClient should be the same");
        assertSame(clock, autoSys.clock,
            "clock should be the same");

        ErrorsOvernight errorsOvernight = dashboardData.getErrorsOvernight();
        assertSame(dashboardData, errorsOvernight.dashboardData,
            "DashboardData should be the same");

        HouseKeeping houseKeeping = dashboardData.getHouseKeeping();
        assertSame(dashboardData, houseKeeping.dashboardData,
            "DashboardData should be the same");
        assertSame(schedulerServiceClient, houseKeeping.schedulerServiceClient,
            "schedulerServiceClient should be the same");
        assertSame(clock, houseKeeping.clock,
            "clock should be the same");

        Timestamps timestamps = dashboardData.getTimestamps();
        assertSame(dashboardData, timestamps.dashboardData,
            "DashboardData should be the same");


        List<DashboardDataEntry> dashboardDataEntries = dashboardData.getDashboardDataEntries();
        assertEquals(9, dashboardDataEntries.size(), "dashboardDataEntries should be the same");
        assertTrue(dashboardDataEntries.contains(bureauLettersAutomaticallyGenerated),
            "dashboardDataEntries should contain bureauLettersAutomaticallyGenerated");
        assertTrue(dashboardDataEntries.contains(bureauLettersToBePrinted),
            "dashboardDataEntries should contain bureauLettersToBePrinted");
        assertTrue(dashboardDataEntries.contains(pncCheck),
            "dashboardDataEntries should contain pncCheck");
        assertTrue(dashboardDataEntries.contains(expenses),
            "dashboardDataEntries should contain expenses");
        assertTrue(dashboardDataEntries.contains(certificates),
            "dashboardDataEntries should contain certificates");
        assertTrue(dashboardDataEntries.contains(autoSys),
            "dashboardDataEntries should contain autoSys");
        assertTrue(dashboardDataEntries.contains(errorsOvernight),
            "dashboardDataEntries should contain errorsOvernight");
        assertTrue(dashboardDataEntries.contains(houseKeeping),
            "dashboardDataEntries should contain houseKeeping");
        assertTrue(dashboardDataEntries.contains(timestamps),
            "dashboardDataEntries should contain timestamps");
    }

    @Test
    void positiveToCsv() {
        dashboardData = spy(dashboardData);
        List<DashboardDataEntry> dashboardDataEntries = List.of(
            createDashboardDataEntryMock("TestTitle"),
            createDashboardDataEntryMock("SomeOtherTitle"),
            createDashboardDataEntryMock("NewTitle")
        );

        when(dashboardData.getDashboardDataEntries()).thenReturn(dashboardDataEntries);

        String csv = dashboardData.toCsv(clock);
        assertEquals("13/11/2023 03:35:30\n"
            + "TestTitle,Test,123\n"
            + "SomeOtherTitle,Test,123\n"
            + "NewTitle,Test,123", csv, "csv should be the same");


    }

    private DashboardDataEntry createDashboardDataEntryMock(String testTitle) {
        DashboardDataEntry dashboardDataEntry = mock(DashboardDataEntry.class);
        when(dashboardDataEntry.toCsv()).thenReturn(new StringBuilder(testTitle).append(",Test").append(",123"));
        return dashboardDataEntry;
    }
}
