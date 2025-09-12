package uk.gov.hmcts.juror.job.execution.config;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import uk.gov.hmcts.juror.standard.client.interceptor.JwtAuthenticationInterceptor;
import uk.gov.hmcts.juror.standard.config.WebConfig;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientConfigTest {

    private static RemoteConfig config;
    private static ClientConfig clientConfig;
    private static JwtService jwtService;

    @BeforeAll
    static void beforeAll() {
        config = mock(RemoteConfig.class);
        jwtService = mock(JwtService.class);
        clientConfig = new ClientConfig();

        WebConfig schedularServiceWebConfig = createWebConfigMock();
        WebConfig jurorServiceWebConfig = createWebConfigMock();
        WebConfig policeNationalComputerCheckServiceWebConfig = createWebConfigMock();

        when(config.getSchedulerService()).thenReturn(schedularServiceWebConfig);
        when(config.getJurorService()).thenReturn(jurorServiceWebConfig);
        when(config.getPoliceNationalComputerCheckService()).thenReturn(policeNationalComputerCheckServiceWebConfig);

    }

    private static WebConfig createWebConfigMock() {
        WebConfig webConfig = mock(WebConfig.class);
        when(webConfig.getHost()).thenReturn(RandomStringUtils.randomAlphabetic(10));
        when(webConfig.getPort()).thenReturn(RandomUtils.nextInt());
        when(webConfig.getScheme()).thenReturn(RandomStringUtils.randomAlphabetic(10));
        when(webConfig.getRequestFactory()).thenReturn(mock(ClientHttpRequestFactory.class));
        return webConfig;
    }


    @Test
    void validateSchedulerServiceRestTemplateBuilder() {
        validateRestTemplateBuilder(config.getSchedulerService(),
            clientConfig.schedulerServiceRestTemplateBuilder(config, jwtService));
    }

    @Test
    void validateJurorServiceRestTemplateBuilder() {
        validateRestTemplateBuilder(config.getJurorService(),
            clientConfig.jurorServiceRestTemplateBuilder(config, jwtService));
    }

    @Test
    void validatePoliceNationalCheckServiceRestTemplateBuilder() {
        validateRestTemplateBuilder(config.getPoliceNationalComputerCheckService(),
            clientConfig.policeNationalCheckServiceRestTemplateBuilder(config, jwtService));
    }


    private void validateRestTemplateBuilder(WebConfig config, RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.build();

        assertInstanceOf(DefaultUriBuilderFactory.class, restTemplate.getUriTemplateHandler(),
            "RestTemplateBuilder should be of type DefaultUriBuilderFactory");

        DefaultUriBuilderFactory handler = (DefaultUriBuilderFactory) restTemplate.getUriTemplateHandler();


        // Assert correct URI base
        URI builtUri = handler.uriString("/example").build();
        String baseUri = config.getScheme() + "://" + config.getHost() + ":" + config.getPort();

        String expectedUri = (config.getScheme() + "://" + config.getHost()
            + ":" + config.getPort() + "/example").toLowerCase();
        assertEquals(expectedUri, builtUri.toString().toLowerCase(),
                     "RestTemplateBuilder should build URI with correct base URL (case-insensitive scheme)");

        // Assert correct interceptor
        assertEquals(1, restTemplate.getInterceptors().size(),
                     "RestTemplateBuilder should have one interceptor configured");

        assertInstanceOf(JwtAuthenticationInterceptor.class, restTemplate.getInterceptors().get(0),
                         "Interceptor should be of type JwtAuthenticationInterceptor");

        verify(config, times(1)).getRequestFactory();
    }
}
