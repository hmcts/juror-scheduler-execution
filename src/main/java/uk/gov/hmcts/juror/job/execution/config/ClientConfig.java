package uk.gov.hmcts.juror.job.execution.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
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

    private RestTemplateBuilder restTemplateBuilder(final WebConfig webConfig,
                                                    final JwtService jwtService) {
        final List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList =
            List.of(new JwtAuthenticationInterceptor(jwtService, webConfig.getSecurity()));
        return new RestTemplateBuilder()
            .uriTemplateHandler(new RootUriTemplateHandler(
                webConfig.getScheme() + "://" + webConfig.getHost() + ":" + webConfig.getPort()))
            .additionalInterceptors(clientHttpRequestInterceptorList)
            .requestFactory(webConfig::getRequestFactory);
    }
}
