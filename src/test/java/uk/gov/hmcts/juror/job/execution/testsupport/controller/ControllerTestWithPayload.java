package uk.gov.hmcts.juror.job.execution.testsupport.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

public abstract class ControllerTestWithPayload extends ControllerTest {

    public ControllerTestWithPayload(HttpMethod method, String url, HttpStatus successStatus) {
        super(method, url, successStatus);
    }

    protected Stream<InvalidPayloadArgument> getStandardInvalidPayloads() {
        return Stream.of(
            (InvalidPayloadArgument) new InvalidPayloadArgument(null, "Unable to read payload content")
                .merge(defaultRequestArgument()),
            (InvalidPayloadArgument) new InvalidPayloadArgument("Non-Json", "Unable to read payload content")
                .merge(defaultRequestArgument())
        );
    }

    @ParameterizedTest(name = "Expect error message: {0}")
    @MethodSource({"getInvalidPayloadArgumentSource", "getStandardInvalidPayloads"})
    @DisplayName("Invalid Payload")
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert", "java:S2699"
    })
    //False positive - checked via inheritance
    void callAndExpectErrorResponse(ErrorRequestArgument errorRequestArgument) throws Exception {
        callAndValidate(errorRequestArgument);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert", "java:S2699"
    })
    //False positive - checked via inheritance
    void negativeInvalidContentType() throws Exception {
        callAndValidate(new ErrorRequestArgument(HttpStatus.UNSUPPORTED_MEDIA_TYPE, getTypicalPayload(),
            "INVALID_CONTENT_TYPE", "Content Type must be application/json").setContentType(MediaType.TEXT_PLAIN)
            .merge(defaultRequestArgument()));
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert", "java:S2699"
    })
    void negativeMissingContentType() throws Exception {
        callAndValidate(new ErrorRequestArgument(HttpStatus.UNSUPPORTED_MEDIA_TYPE, getTypicalPayload(),
            "INVALID_CONTENT_TYPE", "Content Type must be application/json").setContentType(null)
            .merge(defaultRequestArgument()));
    }

    protected abstract String getTypicalPayload();

    protected abstract Stream<InvalidPayloadArgument> getInvalidPayloadArgumentSource();

}
