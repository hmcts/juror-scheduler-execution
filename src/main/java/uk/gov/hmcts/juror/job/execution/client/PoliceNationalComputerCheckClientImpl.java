package uk.gov.hmcts.juror.job.execution.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.PoliceNationalCheckServiceClient;
import uk.gov.hmcts.juror.standard.client.AbstractRemoteRestClient;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

@Slf4j
@Component
public class PoliceNationalComputerCheckClientImpl extends AbstractRemoteRestClient implements PoliceNationalCheckServiceClient {
    private final String bulkUpdateUrl;

    protected PoliceNationalComputerCheckClientImpl(
        @ClientType("PoliceNationalCheckService") RestTemplateBuilder restTemplateBuilder,
        @Value("${uk.gov.hmcts.juror.job.execution.remote.police-national-computer-check-service.url}") String bulkUpdateUrl) {
        super(restTemplateBuilder);
        this.bulkUpdateUrl = bulkUpdateUrl;
    }


    @Override
    public void checkJurors(JurorCheckRequestBulk jurorCheckRequestBulk) {
        ResponseEntity<Void> response;
        try {
            HttpEntity<JurorCheckRequestBulk>
                requestUpdate = new HttpEntity<>(jurorCheckRequestBulk);

           response =
                restTemplate.exchange(bulkUpdateUrl, HttpMethod.POST, requestUpdate, Void.class);
        } catch (Exception throwable) {
            String message = "Failed to run bulk juror pnc checks";
            log.error(message, throwable);
            throw new InternalServerException(message, throwable);
        }
        final HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.equals(HttpStatus.OK)) {
            throw new RemoteGatewayException(
                "Call to PoliceNationalCheckServiceClientImpl check bulk jurors failed status code was: " + statusCode);
        }
    }
}
