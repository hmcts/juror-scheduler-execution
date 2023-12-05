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
import uk.gov.hmcts.juror.job.execution.client.contracts.PoliceNationalCheckServiceClient;
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
@DisplayName("Client: Police National Computer Check")
class PoliceNationalComputerCheckClientImplTest {
    private PoliceNationalComputerCheckClientImpl policeNationalCheckServiceClient;

    private RestTemplate restTemplate;

    private ResponseEntity<Void> response;
    private static final String URL = "/jurors/check/bulk";

    @BeforeEach
    void beforeEach() {
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        restTemplate = mock(RestTemplate.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        policeNationalCheckServiceClient = new PoliceNationalComputerCheckClientImpl(restTemplateBuilder, URL);
        response = mock(ResponseEntity.class);
        when(restTemplate.exchange(eq(URL), eq(HttpMethod.POST), any(), eq(Void.class))).thenReturn(response);
    }

    @DisplayName("Call: " + URL)
    @Nested
    class CheckJurors {
        @Test
        void positiveTypical() {
            PoliceNationalCheckServiceClient.JurorCheckRequestBulk jurorCheckRequestBulk =
                PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().build();

            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            policeNationalCheckServiceClient.checkJurors(jurorCheckRequestBulk);

            ArgumentCaptor<HttpEntity<PoliceNationalCheckServiceClient.JurorCheckRequestBulk>> argumentCaptor =
                ArgumentCaptor.forClass(
                    HttpEntity.class);

            verify(restTemplate, times(1)).exchange(eq(URL), eq(HttpMethod.POST), argumentCaptor.capture(), eq(Void.class));
            assertEquals(jurorCheckRequestBulk, argumentCaptor.getValue().getBody(), "Payloads should match");
        }

        @Test
        void negativeIncorrectStatusCode() {
            PoliceNationalCheckServiceClient.JurorCheckRequestBulk jurorCheckRequestBulk =
                PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().build();

            when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

            RemoteGatewayException remoteGatewayException = assertThrows(RemoteGatewayException.class,
                () -> policeNationalCheckServiceClient.checkJurors(jurorCheckRequestBulk),
                "Should throw RemoteGatewayException");
            assertEquals("Call to PoliceNationalCheckServiceClientImpl check bulk jurors failed status code was: "
                    + HttpStatus.NOT_FOUND,
                remoteGatewayException.getMessage());
            ArgumentCaptor<HttpEntity<PoliceNationalCheckServiceClient.JurorCheckRequestBulk>> argumentCaptor =
                ArgumentCaptor.forClass(
                    HttpEntity.class);


            verify(restTemplate, times(1)).exchange(eq(URL), eq(HttpMethod.POST), argumentCaptor.capture(), eq(Void.class));

            assertEquals(jurorCheckRequestBulk, argumentCaptor.getValue().getBody(), "Payloads should match");
        }

        @Test
        void negativeUnexpectedException() {
            RuntimeException cause = new RuntimeException("I am the cause");
            PoliceNationalCheckServiceClient.JurorCheckRequestBulk jurorCheckRequestBulk =
                PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().build();

            when(restTemplate.exchange(eq(URL), eq(HttpMethod.POST), any(), eq(Void.class))).thenThrow(cause);


            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> policeNationalCheckServiceClient.checkJurors(jurorCheckRequestBulk),
                "Should throw InternalServerException");
            assertEquals("Failed to run bulk juror pnc checks",
                internalServerException.getMessage());
            ArgumentCaptor<HttpEntity<PoliceNationalCheckServiceClient.JurorCheckRequestBulk>> argumentCaptor =
                ArgumentCaptor.forClass(
                    HttpEntity.class);


            verify(restTemplate, times(1)).exchange(eq(URL), eq(HttpMethod.POST), argumentCaptor.capture(), eq(Void.class));

            assertEquals(jurorCheckRequestBulk, argumentCaptor.getValue().getBody(), "Payloads should match");
        }
    }
}
