package uk.gov.hmcts.juror.job.execution.util;

import uk.gov.hmcts.juror.job.execution.config.SftpConfig;
import uk.gov.hmcts.juror.job.execution.config.contracts.HasSftpConfig;
import uk.gov.hmcts.juror.job.execution.testsupport.AbstractSftpTest;


class SftpTest extends AbstractSftpTest<Sftp, HasSftpConfig, Sftp.SftpServerGateway> {

    public static class TestSftp extends Sftp {

        protected TestSftp(SftpConfig config) {
            super(config);
        }
    }

    @Override
    protected Sftp createSftp(HasSftpConfig config) {
        return new TestSftp(config.getSftp());
    }

    @Override
    protected HasSftpConfig createConfig() {
        return new HasSftpConfig() {
            SftpConfig config;

            @Override
            public SftpConfig getSftp() {
                return config;
            }

            @Override
            public void setSftp(SftpConfig sftp) {
                this.config = sftp;
            }
        };
    }

    @Override
    protected String getName() {
        throw new UnsupportedOperationException("Not required for this test");
    }

    @Override
    protected Class<Sftp> getSftpClass() {
        return Sftp.class;
    }

    @Override
    protected Class<Sftp.SftpServerGateway> getSftpServerGatewayClass() {
        return Sftp.SftpServerGateway.class;
    }

    //Exclude tests


    @Override
    protected void verifySftpSessionFactoryBeanAnnotation() throws Exception {
    }

    @Override
    protected void verifyToSftpChannelPrintDestinationHandlerBeanAnnotation() throws Exception {

    }

    @Override
    protected void verifyToSftpChannelPrintDestinationHandlerServiceActivatorAnnotation() throws Exception {

    }

    @Override
    protected void verifyPrintSftpServerGatewayImplGetParent() {

    }

    @Override
    protected void verifyPrintSftpServerGatewayImplAnnotations() throws Exception {

    }
}
