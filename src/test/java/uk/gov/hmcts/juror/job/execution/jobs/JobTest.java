package uk.gov.hmcts.juror.job.execution.jobs;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.rules.Rule;
import uk.gov.hmcts.juror.job.execution.rules.RulesEngine;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobTest {
    private MockedStatic<RulesEngine> rulesEngineMockedStatic;

    @AfterEach
    void afterEach() {
        if (rulesEngineMockedStatic != null) {
            rulesEngineMockedStatic.close();
        }
    }

    @Test
    void positiveAddRulesTest() {
        Rule rule1 = mock(Rule.class);
        Rule rule2 = mock(Rule.class);
        Rule rule3 = mock(Rule.class);

        Job job = new TestJob();
        assertEquals(0, job.getRules().size());
        job.addRules(rule1, rule2, rule3);
        assertEquals(3, job.getRules().size());
        assertEquals(Set.of(rule1, rule2, rule3), job.getRules());

        Rule rule4 = mock(Rule.class);
        Rule rule5 = mock(Rule.class);
        job.addRules(rule4, rule5);
        assertEquals(5, job.getRules().size());
        assertEquals(Set.of(rule1, rule2, rule3, rule4, rule5), job.getRules());
    }

    @Test
    void negativeAddRulesDuplicateTest() {
        Rule rule1 = mock(Rule.class);

        Job job = new TestJob();
        assertEquals(0, job.getRules().size());
        job.addRules(rule1, rule1, rule1);
        assertEquals(1, job.getRules().size());
        assertEquals(Set.of(rule1), job.getRules());
    }

    @Nested
    @DisplayName("public final Result runRules()")
    class RunRules {
        @Test
        void positiveRunRules() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);
            Set<Rule> rules = Set.of(mock(Rule.class), mock(Rule.class), mock(Rule.class));
            Job job = new TestJob();
            job.addRules(rules.toArray(new Rule[0]));
            when(RulesEngine.fire(rules)).thenReturn(Collections.emptyList());

            assertNull(job.runRules(), "Expected null result");
        }

        @Test
        void negativeRunRulesWithSingleFailures() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);
            Set<Rule> rules = Set.of(mock(Rule.class), mock(Rule.class), mock(Rule.class));
            Job job = new TestJob();
            job.addRules(rules.toArray(new Rule[0]));
            when(RulesEngine.fire(rules)).thenReturn(List.of("Error reason 1"));

            Job.Result result = job.runRules();
            assertNotNull(result, "Expected non-null result");
            assertEquals(Status.VALIDATION_FAILED, result.getStatus(), "Expected failed status");
            assertEquals("Error reason 1", result.getMessage(), "Expected failed "
                + "message");
        }

        @Test
        void negativeRunRulesWithMultipleFailures() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);
            Set<Rule> rules = Set.of(mock(Rule.class), mock(Rule.class), mock(Rule.class));
            Job job = new TestJob();
            job.addRules(rules.toArray(new Rule[0]));
            when(RulesEngine.fire(rules)).thenReturn(List.of(
                "Error reason 1",
                "Error reason 2",
                "Error reason 3"));

            Job.Result result = job.runRules();
            assertNotNull(result, "Expected non-null result");
            assertEquals(Status.VALIDATION_FAILED, result.getStatus(), "Expected failed status");
            assertEquals("Error reason 1\\nError reason 2\\nError reason 3", result.getMessage(), "Expected failed "
                + "message");

        }
    }

    @Nested
    @DisplayName("public final Result execute(MetaData metaData)")
    class Execute {

        @Test
        void positiveExecuteSuccess() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);
            when(RulesEngine.fire(any())).thenReturn(Collections.emptyList());
            Job.Result expectedResult = Job.Result.passed();
            Job job = new TestJob(expectedResult);
            Job.Result result = job.execute(mock(MetaData.class));
            assertSame(expectedResult, result, "Expected same result");
        }

        @Test
        void negativeExecuteFailedRules() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);
            when(RulesEngine.fire(any())).thenReturn(List.of("Error message 123"));
            Job job = new TestJob();
            Job.Result result = job.execute(mock(MetaData.class));
            assertEquals(Status.VALIDATION_FAILED, result.getStatus(), "Expected same result");
            assertEquals("Error message 123", result.getMessage(), "Expected same result");
            assertNull(result.getThrowable(), "No throwables should be given");
            assertEquals(0, result.getMetaData().size(), "No meta data should be given");
        }

        @Test
        void negativeExecuteFailedRunners() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);
            when(RulesEngine.fire(any())).thenReturn(Collections.emptyList());
            Job.Result expectedResult = Job.Result.failed("Failure message");
            Job job = new TestJob(expectedResult);
            Job.Result result = job.execute(mock(MetaData.class));
            assertSame(expectedResult, result, "Expected same result");
        }

        @Test
        void negativeExecuteUnexpectedException() {
            rulesEngineMockedStatic = Mockito.mockStatic(RulesEngine.class);

            RuntimeException cause = new RuntimeException("I am the cause");
            when(RulesEngine.fire(any())).thenThrow(cause);
            Job job = new TestJob();
            Job.Result result = job.execute(mock(MetaData.class));
            assertEquals(Status.FAILED_UNEXPECTED_EXCEPTION, result.getStatus(), "Expected same result");
            assertEquals("Unexpected exception raised: java.lang.RuntimeException", result.getMessage(),
                "Expected same result");
            assertSame(cause, result.getThrowable(), "Expected same throwable");
            assertEquals(0, result.getMetaData().size(), "No meta data should be given");
        }
    }

    @Nested
    @DisplayName("protected final Result runJobStep(Function<MetaData, Result> resultFunction, MetaData metaData)")
    class RunJobStep {
        @Test
        void positiveSuccess() {
            Job job = new TestJob();
            MetaData metaData = mock(MetaData.class);
            Job.Result expectedResult = Job.Result.passed();
            Job.Result result = job.runJobStep(
                md -> {
                    assertSame(metaData, md,
                        "Expected same meta data");
                    return expectedResult;
                },
                metaData
            );
            assertEquals(expectedResult, result, "Expected same result");
        }

        @Test
        void negativeUnexpectedException() {
            Job job = new TestJob();
            RuntimeException cause = new RuntimeException("I am the cause");
            Job.Result result = job.runJobStep(
                md -> {
                    throw cause;
                },
                mock(MetaData.class)
            );
            assertEquals(Status.FAILED_UNEXPECTED_EXCEPTION, result.getStatus(), "Expected same result");
            assertEquals("Unexpected exception raised: java.lang.RuntimeException", result.getMessage(),
                "Expected same result");
            assertSame(cause, result.getThrowable(), "Expected same throwable");
            assertEquals(0, result.getMetaData().size(), "No meta data should be given");
        }

        @Test
        void negativeUnexpectedInternalServerException() {
            Job job = new TestJob();
            InternalServerException cause = new InternalServerException("I am the cause");
            Job.Result result = job.runJobStep(
                md -> {
                    throw cause;
                },
                mock(MetaData.class)
            );
            assertEquals(Status.FAILED_UNEXPECTED_EXCEPTION, result.getStatus(), "Expected same result");
            assertEquals("Internal Server exception raised: InternalServerException: I am the cause",
                result.getMessage(),
                "Expected same result");
            assertSame(cause, result.getThrowable(), "Expected same throwable");
            assertEquals(0, result.getMetaData().size(), "No meta data should be given");

        }
    }


    @Test
    void positiveGetName() {
        assertEquals("TestJob", new TestJob().getName(), "Expected same name");
    }

    @Nested
    @DisplayName("public static class Result")
    class ResultTest {

        @Test
        void positiveConstructorStatusOnly() {
            Job.Result result = new Job.Result(Status.VALIDATION_FAILED);
            assertEquals(Status.VALIDATION_FAILED, result.getStatus(), "Expected same status");
            assertNull(result.getMessage(), "Expected null message");
            assertNull(result.getThrowable(), "Expected null throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Test
        void positiveConstructorStatusAndMessageOnly() {
            final String message = RandomStringUtils.randomAlphabetic(10);
            Job.Result result = new Job.Result(Status.VALIDATION_FAILED, message);
            assertEquals(Status.VALIDATION_FAILED, result.getStatus(), "Expected same status");
            assertEquals(message, result.getMessage(), "Expected same message");
            assertNull(result.getThrowable(), "Expected null throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Test
        void positiveConstructorStatusMessageAndThrowable() {
            Throwable throwable = mock(Throwable.class);
            final String message = RandomStringUtils.randomAlphabetic(10);
            Job.Result result = new Job.Result(Status.VALIDATION_FAILED, message, throwable);
            assertEquals(Status.VALIDATION_FAILED, result.getStatus(), "Expected same status");
            assertEquals(message, result.getMessage(), "Expected same message");
            assertEquals(throwable, result.getThrowable(), "Expected same throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");

        }

        @Test
        void positiveAddMetaDataMap() {
            Map<String, String> entriesToAdd = Map.of(
                "Key", "Value",
                "Key123", "Value123",
                "Key321", "Value321"
            );
            Job.Result result = new Job.Result(Status.VALIDATION_FAILED);
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
            result.addMetaData(entriesToAdd);

            Map<String, String> expectedEntries = new ConcurrentHashMap<>(entriesToAdd);
            assertEquals(3, result.getMetaData().size(), "Expected 3 entry in meta data");
            assertEquals(expectedEntries, result.getMetaData(), "Expected same meta data");

            Map<String, String> additionalEntries = Map.of(
                "newKey", "newValue",
                "status", "123"
            );
            result.addMetaData(additionalEntries);
            expectedEntries.putAll(additionalEntries);
            assertEquals(5, result.getMetaData().size(), "Expected 5 entry in meta data");
            assertEquals(expectedEntries, result.getMetaData(), "Expected same meta data");

        }

        @Test
        void positiveAddMetaDataKeyValue() {
            Job.Result result = new Job.Result(Status.VALIDATION_FAILED);
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
            result.addMetaData("Key", "Value");
            assertEquals(1, result.getMetaData().size(), "Expected 1 entry in meta data");
            assertEquals("Value", result.getMetaData().get("Key"), "Expected same meta data");
            result.addMetaData("Key321", "Value123");
            assertEquals(2, result.getMetaData().size(), "Expected 2 entry in meta data");
            assertEquals("Value123", result.getMetaData().get("Key321"), "Expected same meta data");
        }

        @Test
        void positivePassed() {
            Job.Result result = Job.Result.passed();
            assertEquals(Status.SUCCESS, result.getStatus(), "Expected same status");
            assertNull(result.getMessage(), "Expected null message");
            assertNull(result.getThrowable(), "Expected null throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Test
        void positivePassedWithMessage() {
            final String message = RandomStringUtils.random(10);
            Job.Result result = Job.Result.passed(message);
            assertEquals(Status.SUCCESS, result.getStatus(), "Expected same status");
            assertEquals(message, result.getMessage(), "Expected same message");
            assertNull(result.getThrowable(), "Expected null throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Test
        void positiveFailed() {
            final String message = RandomStringUtils.random(10);
            Job.Result result = Job.Result.failed(message);
            assertEquals(Status.FAILED, result.getStatus(), "Expected same status");
            assertEquals(message, result.getMessage(), "Expected same message");
            assertNull(result.getThrowable(), "Expected null throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Test
        void positiveFailedWithThrowable() {
            Throwable cause = mock(Throwable.class);

            final String message = RandomStringUtils.random(10);
            Job.Result result = Job.Result.failed(message, cause);
            assertEquals(Status.FAILED, result.getStatus(), "Expected same status");
            assertEquals(message, result.getMessage(), "Expected same message");
            assertSame(cause, result.getThrowable(), "Expected same throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Test
        void positivePartialSuccess() {
            final String message = RandomStringUtils.random(10);
            Job.Result result = Job.Result.partialSuccess(message);
            assertEquals(Status.PARTIAL_SUCCESS, result.getStatus(), "Expected same status");
            assertEquals(message, result.getMessage(), "Expected same message");
            assertNull(result.getThrowable(), "Expected null throwable");
            assertEquals(0, result.getMetaData().size(), "Expected empty meta data");
        }

        @Nested
        @DisplayName("public static Result merge(Collection<Result> results)")
        class Merge {
            @Test
            void positiveOneResult() {
                Job.Result result = Job.Result.passed();
                assertSame(result, Job.Result.merge(List.of(result)),
                    "Expected same result");
            }

            @Test
            void positiveMultipleResult() {
                Job.Result result1 = Job.Result.failed("Message 1");
                Job.Result result2 = Job.Result.passed("Message 2");
                Job.Result result3 = Job.Result.partialSuccess("Message 3");
                Job.Result expectedResult = Job.Result.failed("Message 1\\nMessage 2\\nMessage 3");
                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2, result3)),
                    "Expected same result");
            }

            @Test
            void positiveNullStatus() {
                Job.Result result1 = new Job.Result(null, "Message 1");
                Job.Result result2 = new Job.Result(null, "Message 2");
                Job.Result expectedResult = new Job.Result(null, "Message 1\\nMessage 2");

                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2)),
                    "Expected same result");
            }

            @Test
            void positiveNullSecondStatus() {
                Job.Result result1 = new Job.Result(Status.SUCCESS, "Message 1");
                Job.Result result2 = new Job.Result(null, "Message 2");
                Job.Result expectedResult = new Job.Result(Status.SUCCESS, "Message 1\\nMessage 2");

                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2)),
                    "Expected same result");
            }

            @Test
            void positiveStatusPriorityHigher() {
                Job.Result result1 = new Job.Result(Status.FAILED, "Message 1");
                Job.Result result2 = new Job.Result(Status.SUCCESS, "Message 2");
                Job.Result expectedResult = new Job.Result(Status.FAILED, "Message 1\\nMessage 2");

                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2)),
                    "Expected same result");
            }

            @Test
            void positiveWithThrowable() {
                Throwable throwable = mock(Throwable.class);
                Job.Result result1 = new Job.Result(Status.SUCCESS, "Message 1");
                Job.Result result2 = new Job.Result(Status.FAILED, "Message 2", throwable);
                Job.Result expectedResult = new Job.Result(Status.FAILED, "Message 1\\nMessage 2", throwable);

                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2)),
                    "Expected same result");
            }

            @Test
            void positiveWithMetaData() {
                Job.Result result1 = new Job.Result(Status.SUCCESS, "Message 1")
                    .addMetaData("Key1", "Value1")
                    .addMetaData("Key2", "Value2");
                Job.Result result2 = new Job.Result(Status.FAILED, "Message 2")
                    .addMetaData("additionalKey1", "abc123")
                    .addMetaData("additionalKey2", "someKey");
                Job.Result expectedResult = new Job.Result(Status.FAILED, "Message 1\\nMessage 2")
                    .addMetaData("Key1", "Value1")
                    .addMetaData("Key2", "Value2")
                    .addMetaData("additionalKey1", "abc123")
                    .addMetaData("additionalKey2", "someKey");

                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2)),
                    "Expected same result");
            }

            @Test
            void positiveWithoutMessage() {
                Job.Result result1 = new Job.Result(Status.SUCCESS);
                Job.Result result2 = new Job.Result(Status.FAILED);
                Job.Result expectedResult = new Job.Result(Status.FAILED, null);

                assertEquals(expectedResult, Job.Result.merge(List.of(result1, result2)),
                    "Expected same result");
            }
        }
    }

    @Nested
    @DisplayName("public static class ResultSupplier")
    class ResultSupplierTest {


        @Test
        void positiveConstructorTestSimplified() {
            Collection<Function<MetaData, Job.Result>> resultRunners = List.of(
                (metaData) -> Job.Result.passed(),
                (metaData) -> Job.Result.passed()
            );

            Job.ResultSupplier resultSupplier = new Job.ResultSupplier(true,
                resultRunners
            );
            assertTrue(resultSupplier.isContinueOnFailure(), "Expected continue on failure");
            assertSame(resultRunners, resultSupplier.getResultRunners(), "Expected same result runners");
            assertNull(resultSupplier.getPostRunChecks(), "Expected null post run checks");
        }

        @Test
        void positiveConstructorTestFull() {
            Collection<Function<MetaData, Job.Result>> resultRunners = List.of(
                (metaData) -> Job.Result.passed(),
                (metaData) -> Job.Result.passed()
            );
            Consumer<Job.Result> postRunChecks = (result) -> {
            };


            Job.ResultSupplier resultSupplier = new Job.ResultSupplier(true,
                resultRunners,
                postRunChecks
            );
            assertTrue(resultSupplier.isContinueOnFailure(), "Expected continue on failure");
            assertSame(resultRunners, resultSupplier.getResultRunners(), "Expected same result runners");
            assertSame(postRunChecks, resultSupplier.getPostRunChecks(), "Expected same post run checks");
        }

        @Test
        void positiveRunPostChecksPopulated() {
            AtomicBoolean postRunChecksCalled = new AtomicBoolean(false);
            Job.Result expectedResult = Job.Result.passed();

            Consumer<Job.Result> postRunChecks = (result) -> {
                assertSame(expectedResult, result, "Expected same result");
                postRunChecksCalled.set(true);
            };


            Job.ResultSupplier resultSupplier = new Job.ResultSupplier(true,
                List.of(),
                postRunChecks
            );
            resultSupplier.runPostActions(expectedResult);
            assertTrue(postRunChecksCalled.get(), "Expected post run checks to be called");
        }

        @Test
        void positiveRunPostChecksNull() {
            Job.ResultSupplier resultSupplier = new Job.ResultSupplier(true,
                List.of(),
                null
            );
            assertDoesNotThrow(() -> resultSupplier.runPostActions(Job.Result.passed()), "Expected no exception");
        }
    }


    static class TestJob extends Job {


        private final Result executeRunnersResult;

        public TestJob() {
            this(null);
        }

        public TestJob(Result executeRunnersResult) {
            super();
            this.executeRunnersResult = executeRunnersResult;
        }

        @Override
        public Result executeRunners(MetaData metaData) {
            return executeRunnersResult;
        }
    }
}
