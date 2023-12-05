package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams;

import uk.gov.hmcts.juror.job.execution.testsupport.AbstractSftpTest;

public class AmsDashboardSftpTest extends AbstractSftpTest<AmsDashboardSftp,AmsDashboardConfig, AmsDashboardSftp.AmsDashboardSftpServerGatewayImpl> {
    @Override
    protected AmsDashboardSftp createSftp(AmsDashboardConfig config) {
        return new AmsDashboardSftp(config);
    }

    @Override
    protected AmsDashboardConfig createConfig() {
        return new AmsDashboardConfig();
    }

    @Override
    protected String getName() {
        return "AmsDashboard";
    }

    @Override
    protected Class<AmsDashboardSftp> getSftpClass() {
        return AmsDashboardSftp.class;
    }

    @Override
    protected Class<AmsDashboardSftp.AmsDashboardSftpServerGatewayImpl> getSftpServerGatewayClass() {
        return AmsDashboardSftp.AmsDashboardSftpServerGatewayImpl.class;
    }
}
