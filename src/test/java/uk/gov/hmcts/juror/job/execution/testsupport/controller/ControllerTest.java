package uk.gov.hmcts.juror.job.execution.testsupport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
    protected final HttpMethod method;
    protected final String url;
    @Getter
    protected final HttpStatus successStatus;

    public ControllerTest(HttpMethod method, String url, HttpStatus successStatus) {
        this.method = method;
        this.url = url;
        this.successStatus = successStatus;
    }

    protected abstract Stream<SuccessRequestArgument> getSuccessRequestArgument();

    @ParameterizedTest(name = "Positive: {0}")
    @MethodSource("getSuccessRequestArgument")
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert", "java:S2699"
    })
    //False positive - checked via inheritance
    void positiveValidTypical(SuccessRequestArgument requestArgument) throws Exception {
        callAndValidate(requestArgument);
    }

    protected <T> String createResponseStringFromObject(T apiJobDetailsResponses) throws JsonProcessingException {
        return objectMapper.writeValueAsString(apiJobDetailsResponses);
    }

    private MockHttpServletRequestBuilder buildRequest(RequestArgument requestArgument) {
        final MockHttpServletRequestBuilder requestBuilder;
        if (requestArgument.getPathParams() != null) {
            requestBuilder = request(method, url, (Object[]) requestArgument.getPathParams());
        } else {
            requestBuilder = request(method, url);
        }
        if (requestArgument.getRequestPayload() != null) {
            requestBuilder.content(requestArgument.getRequestPayload());
        }
        if (requestArgument.getContentType() != null) {
            requestBuilder.contentType(requestArgument.getContentType());
        }
        if (requestArgument.getQueryParams() != null) {
            requestArgument.getQueryParams().forEach(requestBuilder::queryParam);
        }
        if (requestArgument.getHeaders() != null) {
            requestArgument.getHeaders().forEach(requestBuilder::header);
        }
        return requestBuilder;
    }

    @SneakyThrows
    protected void callAndValidate(RequestArgument requestArgument) {
        MockHttpServletRequestBuilder requestBuilder = buildRequest(requestArgument);

        requestArgument.runPreActions(requestBuilder, this);
        ResultActions resultActions = this.mockMvc
            .perform(requestBuilder)
            .andDo(print())
            .andExpect(status().is(requestArgument.getExpectedStatus().value()));
        requestArgument.runPostActions(resultActions, this);
    }

    protected RequestArgument defaultRequestArgument() {
        return null;
    }
}
