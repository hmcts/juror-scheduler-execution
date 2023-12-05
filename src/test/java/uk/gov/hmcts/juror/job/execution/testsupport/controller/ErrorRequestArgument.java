package uk.gov.hmcts.juror.job.execution.testsupport.controller;

import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.juror.job.execution.testsupport.TestUtil;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ErrorRequestArgument extends RequestArgument {
    private final String code;
    private final String[] expectedErrorMessages;

    public ErrorRequestArgument(HttpStatus expectedStatus, String requestPayload,
                                String code, String... expectedErrorMessages) {
        this(expectedStatus, requestPayload, code, null, null, expectedErrorMessages);
    }

    public ErrorRequestArgument(HttpStatus expectedStatus,
                                String requestPayload,
                                String code,
                                Consumer<MockHttpServletRequestBuilder> preActions,
                                Consumer<ResultActions> postActions,
                                String... expectedErrorMessages) {
        super(expectedStatus, preActions, postActions, requestPayload);
        this.code = code;
        this.expectedErrorMessages = expectedErrorMessages.clone();
    }

    @Override
    public String toString() {
        return Arrays.toString(this.expectedErrorMessages);
    }

    @Override
    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        resultActions.andExpect(status().is(getExpectedStatus().value()))
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(TestUtil.jsonMatcher(JSONCompareMode.NON_EXTENSIBLE,
                createErrorResponseString(this.code, this.expectedErrorMessages)));
        super.runPostActions(resultActions, controllerTest);
    }

    protected static String createErrorResponseString(String errorCode, String... messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"code\":\"").append(errorCode).append('\"');

        if (messages != null && messages.length > 0) {
            builder.append(",\"messages\": [");
            builder.append(Arrays.stream(messages).map(s -> "\"" + s + "\"").collect(Collectors.joining(",")));
            builder.append(']');
        }
        builder.append('}');
        return builder.toString();
    }
}
