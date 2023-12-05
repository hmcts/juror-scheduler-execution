package uk.gov.hmcts.juror.job.execution.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.job.execution.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.standard.client.AbstractRemoteRestClient;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

@Slf4j
@Component
public class JurorServiceClientImpl extends AbstractRemoteRestClient implements JurorServiceClient {

    private final String url;

    @Autowired
    protected JurorServiceClientImpl(@ClientType("JurorService") RestTemplateBuilder restTemplateBuilder,
                                     @Value("${uk.gov.hmcts.juror.job.execution.remote.juror-service.url}") String url) {
        super(restTemplateBuilder);
        this.url = url;
    }

    @Override
    public void call(String jurorNumber, Payload payload) {
        ResponseEntity<Void> response;
        try {
            log.debug("Updating juror: " + jurorNumber + " pnc check result on juror service backend");
            HttpEntity<Payload> requestUpdate = new HttpEntity<>(payload);
            response =
                restTemplate.exchange(url, HttpMethod.PATCH, requestUpdate, Void.class, jurorNumber);
        } catch (Exception exception) {
            String message = "Failed to update juror pnc status";
            log.error(message, exception);
            throw new InternalServerException(message, exception);
        }
        final HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.equals(HttpStatus.ACCEPTED)) {
            throw new RemoteGatewayException("Call to JurorServiceClient failed status code was: " + statusCode);
        }
        log.debug("Successfully updated juror: " + jurorNumber + " pnc check result on juror service backend");
    }
}
