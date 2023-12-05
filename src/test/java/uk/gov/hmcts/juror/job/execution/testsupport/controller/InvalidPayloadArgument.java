package uk.gov.hmcts.juror.job.execution.testsupport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.function.Consumer;

public class InvalidPayloadArgument extends ErrorRequestArgument {
    public InvalidPayloadArgument(String requestPayload, String... expectedErrorMessages) {
        this(requestPayload, null, null, expectedErrorMessages);
    }

    public InvalidPayloadArgument(String requestPayload,
                                  Consumer<MockHttpServletRequestBuilder> preActions,
                                  Consumer<ResultActions> postActions,
                                  String... expectedErrorMessages) {
        super(HttpStatus.BAD_REQUEST, requestPayload, "INVALID_PAYLOAD",
            preActions, postActions,
            expectedErrorMessages);
    }

    public static InvalidPayloadArgument from(RequestArgument requestArgument, String requestPayload,
                                              String unableToReadPayloadContent) {
        InvalidPayloadArgument invalidPayloadArgument =
            new InvalidPayloadArgument(requestPayload, unableToReadPayloadContent);
        invalidPayloadArgument.merge(requestArgument);
        return invalidPayloadArgument;
    }
}
