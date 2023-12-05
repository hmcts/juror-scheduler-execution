package uk.gov.hmcts.juror.job.execution.testsupport.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Setter
@Accessors(chain = true)
public class RequestArgument implements Arguments {
    @Getter
    private final HttpStatus expectedStatus;
    private Consumer<MockHttpServletRequestBuilder> preActions = (builder) -> {
    };
    private Consumer<ResultActions> postActions = resultActions -> {
    };

    @Getter
    private final String requestPayload;

    @Getter
    private final Map<String, String> queryParams;

    @Getter
    private final Map<String, String> headers;

    @Getter
    private MediaType contentType = MediaType.APPLICATION_JSON;

    @Getter
    private String[] pathParams;

    public RequestArgument(HttpStatus expectedStatus,
                           Consumer<MockHttpServletRequestBuilder> preActions,
                           Consumer<ResultActions> postActions) {
        this(expectedStatus, preActions, postActions, null);
    }

    public RequestArgument(HttpStatus expectedStatus,
                           Consumer<MockHttpServletRequestBuilder> preActions,
                           Consumer<ResultActions> postActions,
                           String requestPayload) {
        if (preActions != null) {
            this.preActions = preActions;
        }
        if (postActions != null) {
            this.postActions = postActions;
        }
        this.requestPayload = requestPayload;
        this.expectedStatus = expectedStatus;
        this.queryParams = new HashMap<>();
        this.headers = new HashMap<>();
    }

    public void runPreActions(MockHttpServletRequestBuilder builder, ControllerTest controllerTest) throws Exception {
        this.preActions.accept(builder);
    }

    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        this.postActions.accept(resultActions);
    }


    @Override
    public final Object[] get() {
        return new Object[]{this};
    }

    protected RequestArgument merge(RequestArgument requestArgument) {
        if (requestArgument == null) {
            return this;
        }
        if (requestArgument.getHeaders() != null && !requestArgument.getHeaders().isEmpty()) {
            this.headers.putAll(requestArgument.getHeaders());
        }
        if (requestArgument.getQueryParams() != null && !requestArgument.getQueryParams().isEmpty()) {
            this.queryParams.putAll(requestArgument.getQueryParams());
        }
        if (requestArgument.getPathParams() != null && requestArgument.getPathParams().length > 0) {
            this.pathParams = requestArgument.getPathParams();
        }
        return this;
    }
}
