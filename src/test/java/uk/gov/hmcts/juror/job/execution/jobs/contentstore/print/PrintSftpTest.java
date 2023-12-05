package uk.gov.hmcts.juror.job.execution.jobs.contentstore.print;

import uk.gov.hmcts.juror.job.execution.testsupport.AbstractSftpTest;

public class PrintSftpTest extends AbstractSftpTest<PrintSftp, PrintConfig, PrintSftp.PrintSftpServerGatewayImpl> {
    @Override
    protected PrintSftp createSftp(PrintConfig config) {
        return new PrintSftp(config);
    }

    @Override
    protected PrintConfig createConfig() {
        return new PrintConfig();
    }

    @Override
    protected String getName() {
        return "Print";
    }

    @Override
    protected Class<PrintSftp> getSftpClass() {
        return PrintSftp.class;
    }

    @Override
    protected Class<PrintSftp.PrintSftpServerGatewayImpl> getSftpServerGatewayClass() {
        return PrintSftp.PrintSftpServerGatewayImpl.class;
    }
}
