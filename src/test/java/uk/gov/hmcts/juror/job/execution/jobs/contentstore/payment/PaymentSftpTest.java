package uk.gov.hmcts.juror.job.execution.jobs.contentstore.payment;

import uk.gov.hmcts.juror.job.execution.testsupport.AbstractSftpTest;

class PaymentSftpTest
    extends AbstractSftpTest<PaymentSftp, PaymentConfig, PaymentSftp.PaymentSftpServerGatewayImpl> {
    @Override
    protected PaymentSftp createSftp(PaymentConfig config) {
        return new PaymentSftp(config);
    }

    @Override
    protected PaymentConfig createConfig() {
        return new PaymentConfig();
    }

    @Override
    protected String getName() {
        return "Payment";
    }

    @Override
    protected Class<PaymentSftp> getSftpClass() {
        return PaymentSftp.class;
    }

    @Override
    protected Class<PaymentSftp.PaymentSftpServerGatewayImpl> getSftpServerGatewayClass() {
        return PaymentSftp.PaymentSftpServerGatewayImpl.class;
    }


}
