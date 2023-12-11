package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import uk.gov.hmcts.juror.job.execution.jobs.Job;

public class ErrorsOvernight extends DashboardDataEntry {
    protected ErrorsOvernight(DashboardData dashboardData) {
        super(dashboardData, "Errors Overnight", "Server Name", "Status");
    }
    @SuppressWarnings("PMD.LawOfDemeter")
    public Job.Result populate() {
        //SSUPVL03 gets populated by auto sys
        addRow("SSUPVL04", "None");
        return Job.Result.passed();
    }

    public void addRow(String serverName, String status) {
        this.addEntry(serverName, status);
    }
}