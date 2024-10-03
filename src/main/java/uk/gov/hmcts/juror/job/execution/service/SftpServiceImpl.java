package uk.gov.hmcts.juror.job.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.job.execution.service.contracts.SftpService;
import uk.gov.hmcts.juror.job.execution.util.Sftp;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SftpServiceImpl implements SftpService {

    final Map<Class<? extends Sftp>, Sftp.SftpServerGateway> sftpServerGatewaysByParentClass;

    @Autowired
    public SftpServiceImpl(List<Sftp.SftpServerGateway> sftpServerGateways) {
        this.sftpServerGatewaysByParentClass = sftpServerGateways.stream().collect(Collectors.toMap(
            Sftp.SftpServerGateway::getParent,
            Function.identity()
        ));
    }

    @Override
    public Collection<File> upload(Class<? extends Sftp> sftpClass, Collection<File> filesToProcess,
                                   long retryLimit, long retryDelay) {
        Set<File> filesFailedToUpload = new HashSet<>();
        Sftp.SftpServerGateway gateway = getGateway(sftpClass);
        for (File file : filesToProcess) {
            if (!upload(gateway, file, retryLimit, retryDelay)) {
                filesFailedToUpload.add(file);
            }
        }
        return filesFailedToUpload;
    }

    @Override
    public boolean upload(Class<? extends Sftp> sftpClass, File fileToProcess, long retryLimit, long retryDelay) {
        return upload(getGateway(sftpClass), fileToProcess, retryLimit, retryDelay);
    }


    private boolean upload(Sftp.SftpServerGateway gateway, File fileToProcess, long retryLimit, long retryDelay) {
        log.info("Uploading: {}", fileToProcess);
        int retryCount = 0;
        do {
            if (retryCount > 0) {
                log.info("Upload failed retrying ({}/{}): waiting {} ms", retryCount, retryLimit, fileToProcess);
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    log.error("Failed to sleep", e);
                    Thread.currentThread().interrupt();
                }
            }
            if (upload(gateway, fileToProcess)) {
                log.info("Upload successful: {}", fileToProcess);
                return true;
            }
            retryCount++;
        } while (retryLimit > 0 && retryCount <= retryLimit);
        return false;
    }

    private boolean upload(Sftp.SftpServerGateway gateway, File fileToProcess) {
        try {
            gateway.upload(fileToProcess);
            return true;
        } catch (Exception exception) {
            log.error("Failed to upload file: {}", fileToProcess, exception);
            return false;
        }
    }

    private Sftp.SftpServerGateway getGateway(Class<? extends Sftp> sftpClass) {
        return this.sftpServerGatewaysByParentClass.get(sftpClass);
    }
}
