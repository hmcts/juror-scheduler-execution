package uk.gov.hmcts.juror.job.execution.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.config.SftpConfig;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SftpServiceImplTest {

    private final List<Sftp.SftpServerGateway> gateways = List.of(
        new TestSftpServerGateway(),
        new TestSftpServerGatewayError());

    private SftpServiceImpl sftpService;
    private AtomicInteger uploadCount;

    @BeforeEach
    void beforeEach() {
        this.sftpService = new SftpServiceImpl(gateways);
        this.uploadCount = new AtomicInteger(0);
    }

    @Test
    void positiveConstructorTest() {
        assertEquals(2, this.sftpService.sftpServerGatewaysByParentClass.size());
        assertEquals(gateways.get(0), this.sftpService.sftpServerGatewaysByParentClass.get(TestSftp.class));
        assertEquals(gateways.get(1), this.sftpService.sftpServerGatewaysByParentClass.get(TestSftpError.class));
    }


    @Test
    void uploadCollectionAllPass() {
        Collection<File> filesFailedToUpload = sftpService
            .upload(TestSftp.class, List.of(new File("test1"), new File("test2")));
        assertEquals(0, filesFailedToUpload.size(), "No files should have failed to upload");
        assertEquals(2, uploadCount.get(), "Both files should have been uploaded");
    }

    @Test
    void uploadCollectionNonePass() {
        List<File> files = List.of(new File("test1"), new File("test2"));
        Collection<File> filesFailedToUpload = sftpService
            .upload(TestSftpError.class, files);
        assertEquals(2, filesFailedToUpload.size(), "All files should have failed to upload");
        assertTrue(filesFailedToUpload.containsAll(files), "All files should have failed to upload");
        assertEquals(2, uploadCount.get(), "Both files should have been uploaded");
    }

    @Test
    void uploadCollectionSomePass() {
        List<File> files = List.of(new File("FAIL 1"), new File("test2"), new File("FAIL 2"), new File("test3"));
        Collection<File> filesFailedToUpload = sftpService
            .upload(TestSftp.class, files);
        assertEquals(2, filesFailedToUpload.size(), "2 files should have failed to upload");
        assertTrue(filesFailedToUpload.contains(files.get(0)), "File 1 should have failed to upload");
        assertTrue(filesFailedToUpload.contains(files.get(2)), "File 3 should have failed to upload");
        assertEquals(4, uploadCount.get(), "All files should have attempted to upload");
    }

    @Test
    void uploadSinglePass() {
        assertTrue(sftpService.upload(TestSftp.class, new File("test1")), "File should have uploaded");
    }

    @Test
    void uploadSingleFail() {
        assertFalse(sftpService.upload(TestSftpError.class, new File("test1")), "File should have failed to upload");
    }

    class TestSftp extends Sftp {

        protected TestSftp(SftpConfig config) {
            super(config);
        }
    }

    class TestSftpServerGateway implements Sftp.SftpServerGateway {
        @Override
        public void upload(File file) {
            uploadCount.incrementAndGet();
            if (file.getName().startsWith("FAIL")) {
                throw new RuntimeException("Some unexpected error");
            }
        }

        @Override
        public Class<? extends Sftp> getParent() {
            return TestSftp.class;
        }
    }

    class TestSftpError extends Sftp {

        protected TestSftpError(SftpConfig config) {
            super(config);
        }
    }

    private class TestSftpServerGatewayError implements Sftp.SftpServerGateway {

        @Override
        public void upload(File file) {
            uploadCount.incrementAndGet();
            throw new RuntimeException("Some unexpected error");
        }

        @Override
        public Class<? extends Sftp> getParent() {
            return TestSftpError.class;
        }
    }

}
