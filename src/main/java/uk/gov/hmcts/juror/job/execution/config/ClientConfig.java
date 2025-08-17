package uk.gov.hmcts.juror.job.execution.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.util.DefaultUriBuilderFactory;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.client.interceptor.JwtAuthenticationInterceptor;
import uk.gov.hmcts.juror.standard.config.WebConfig;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

import java.util.List;

@Configuration
@Slf4j
public class ClientConfig {

    @Bean
    @ClientType("SchedulerService")
    public RestTemplateBuilder schedulerServiceRestTemplateBuilder(
        final RemoteConfig config,
        final JwtService jwtService
    ) {
        return restTemplateBuilder(config.getSchedulerService(), jwtService);
    }

    @Bean
    @ClientType("JurorService")
    public RestTemplateBuilder jurorServiceRestTemplateBuilder(
        final RemoteConfig config,
        final JwtService jwtService
    ) {
        return restTemplateBuilder(config.getJurorService(), jwtService);
    }

    @Bean
    @ClientType("PoliceNationalCheckService")
    public RestTemplateBuilder policeNationalCheckServiceRestTemplateBuilder(
        final RemoteConfig config,
        final JwtService jwtService
    ) {
        return restTemplateBuilder(config.getPoliceNationalComputerCheckService(), jwtService);
    }

    @SuppressWarnings("removal")
    private RestTemplateBuilder restTemplateBuilder(final WebConfig webConfig,
                                                    final JwtService jwtService) {
        final List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList =
            List.of(new JwtAuthenticationInterceptor(jwtService, webConfig.getSecurity()));

        // Construct URI builder with scheme, host, and port
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(
            webConfig.getScheme() + "://" + webConfig.getHost() + ":" + webConfig.getPort());
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

        // Return builder with explicit URI handler and request factory
        return new RestTemplateBuilder()
            .requestFactory(webConfig::getRequestFactory) // Avoids Spring's HttpClient5 auto-configuration
            .uriTemplateHandler(uriBuilderFactory)
            .additionalInterceptors(clientHttpRequestInterceptorList);
    }
}
