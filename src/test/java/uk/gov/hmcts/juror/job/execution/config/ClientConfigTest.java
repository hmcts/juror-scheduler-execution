package uk.gov.hmcts.juror.job.execution.config;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.juror.standard.client.interceptor.JwtAuthenticationInterceptor;
import uk.gov.hmcts.juror.standard.config.WebConfig;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

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

        assertInstanceOf(RootUriTemplateHandler.class, restTemplate.getUriTemplateHandler(),
            "RestTemplateBuilder should be of type DefaultUriBuilderFactory");


        RootUriTemplateHandler rootUriTemplateHandler = (RootUriTemplateHandler) restTemplate.getUriTemplateHandler();

        assertEquals(config.getScheme() + "://" + config.getHost() + ":" + config.getPort(),
            rootUriTemplateHandler.getRootUri(),
            "RestTemplateBuilder should have been configured with the correct root uri");

        assertEquals(1, restTemplate.getInterceptors().size(),
            "RestTemplateBuilder should have been configured with the correct number of interceptors");
        assertInstanceOf(JwtAuthenticationInterceptor.class, restTemplate.getInterceptors().get(0),
            "RestTemplateBuilder should have been configured with the correct interceptor");
        verify(config, times(1)).getRequestFactory();
    }
}
