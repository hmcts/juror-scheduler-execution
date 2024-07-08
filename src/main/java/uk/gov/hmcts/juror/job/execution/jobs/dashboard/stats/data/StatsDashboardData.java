package uk.gov.hmcts.juror.job.execution.jobs.dashboard.stats.data;

import lombok.Getter;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.Dashboard;

import java.util.List;

@Getter
public class StatsDashboardData extends Dashboard {
    private final CommunicationServiceInfo communicationServiceInfo;

    public StatsDashboardData(SchedulerServiceClient schedulerServiceClient) {
        this.communicationServiceInfo = new CommunicationServiceInfo(this, schedulerServiceClient);
        this.dashboardDataEntries = List.of(
            this.communicationServiceInfo
        );
    }
}
