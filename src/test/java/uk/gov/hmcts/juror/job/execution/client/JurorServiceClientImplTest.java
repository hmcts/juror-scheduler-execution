package uk.gov.hmcts.juror.job.execution.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.juror.job.execution.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch.PoliceCheck;
import uk.gov.hmcts.juror.job.execution.testsupport.TestConstants;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@DisplayName("Client: Juror Service")
class JurorServiceClientImplTest {

    private JurorServiceClientImpl jurorServiceClient;

    private RestTemplate restTemplate;

    private ResponseEntity<Void> response;
    private static final String SCHEME = "https";
    private static final String HOST = "localhost";
    private static final String PORT = "8080";
    private static final String URL_PREFIX = SCHEME + "://" + HOST + ":" + PORT;
    private static final String URL_SUFFIX = "/api/v1/moj/juror-record/pnc/{jurorNumber}";

    private static final String URL = URL_PREFIX + URL_SUFFIX;

    @BeforeEach
    void beforeEach() {
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        jurorServiceClient = new JurorServiceClientImpl(restTemplateBuilder,
            SCHEME, HOST, PORT, URL_SUFFIX);
        response = mock(ResponseEntity.class);
        when(restTemplate.exchange(eq(URL), eq(HttpMethod.PATCH), any(), eq(Void.class),
            eq(TestConstants.VALID_JUROR_NUMBER))).thenReturn(response);
    }

    @DisplayName("Call: " + URL)
    @Nested
    class Call {
        @Test
        void positiveTypical() {
            JurorServiceClient.Payload payload = new JurorServiceClient.Payload(PoliceCheck.ELIGIBLE);
            when(response.getStatusCode()).thenReturn(HttpStatus.ACCEPTED);
            jurorServiceClient.call(TestConstants.VALID_JUROR_NUMBER, payload);

            ArgumentCaptor<HttpEntity<JurorServiceClient.Payload>> argumentCaptor = ArgumentCaptor.forClass(
                HttpEntity.class);


            verify(restTemplate, times(1)).exchange(eq(URL), eq(HttpMethod.PATCH), argumentCaptor.capture(),
                eq(Void.class),
                eq(TestConstants.VALID_JUROR_NUMBER));

            assertEquals(payload, argumentCaptor.getValue().getBody(), "Payloads should match");
        }

        @Test
        void negativeIncorrectStatusCode() {
            JurorServiceClient.Payload payload = new JurorServiceClient.Payload(PoliceCheck.ELIGIBLE);
            when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

            RemoteGatewayException remoteGatewayException = assertThrows(RemoteGatewayException.class,
                () -> jurorServiceClient.call(TestConstants.VALID_JUROR_NUMBER, payload),
                "Should throw RemoteGatewayException");
            assertEquals("Call to JurorServiceClient failed status code was: 404 NOT_FOUND",
                remoteGatewayException.getMessage());
            ArgumentCaptor<HttpEntity<JurorServiceClient.Payload>> argumentCaptor = ArgumentCaptor.forClass(
                HttpEntity.class);


            verify(restTemplate, times(1)).exchange(eq(URL), eq(HttpMethod.PATCH), argumentCaptor.capture(),
                eq(Void.class),
                eq(TestConstants.VALID_JUROR_NUMBER));

            assertEquals(payload, argumentCaptor.getValue().getBody(), "Payloads should match");
        }

        @Test
        void negativeUnexpectedException() {
            JurorServiceClient.Payload payload = new JurorServiceClient.Payload(PoliceCheck.ELIGIBLE);

            RuntimeException cause = new RuntimeException("I am the cause");
            when(restTemplate.exchange(eq(URL), eq(HttpMethod.PATCH), any(), eq(Void.class),
                eq(TestConstants.VALID_JUROR_NUMBER))).thenThrow(cause);

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> jurorServiceClient.call(TestConstants.VALID_JUROR_NUMBER, payload),
                "Should throw InternalServerException");
            assertEquals("Failed to update juror pnc status",
                internalServerException.getMessage());
            assertEquals(cause, internalServerException.getCause(), "Cause should match");
            ArgumentCaptor<HttpEntity<JurorServiceClient.Payload>> argumentCaptor = ArgumentCaptor.forClass(
                HttpEntity.class);


            verify(restTemplate, times(1)).exchange(eq(URL), eq(HttpMethod.PATCH), argumentCaptor.capture(),
                eq(Void.class),
                eq(TestConstants.VALID_JUROR_NUMBER));

            assertEquals(payload, argumentCaptor.getValue().getBody(), "Payloads should match");
        }
    }
}
