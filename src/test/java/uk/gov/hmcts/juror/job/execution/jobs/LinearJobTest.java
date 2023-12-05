package uk.gov.hmcts.juror.job.execution.jobs;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LinearJobTest {


    @Test
    void positiveSingleStep() {
        AtomicBoolean postActionCalled = new AtomicBoolean(false);
        MetaData metaData = mock(MetaData.class);
        Job.Result expectedResult = new Job.Result(Status.SUCCESS, "Success");

        Job.ResultSupplier resultSupplier = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success");
                }),
            result -> {
                assertEquals(expectedResult, result, "Result should be passed to the post action");
                postActionCalled.set(true);
            }
        );
        LinearJob linearJob = new TestLinearJob(resultSupplier);
        assertEquals(expectedResult, linearJob.executeRunners(metaData));
        assertTrue(postActionCalled.get(), "Post action should be called");
    }

    @Test
    void positiveMultipleSteps() {
        AtomicBoolean postActionCalled = new AtomicBoolean(false);
        MetaData metaData = mock(MetaData.class);
        Job.Result expectedResult = new Job.Result(Status.SUCCESS, "Success 1\\nSuccess 2\\nSuccess 3");

        Job.ResultSupplier resultSupplier = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 1");
                }, md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 2");
                }, md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 3");
                }),
            result -> {
                assertEquals(expectedResult, result, "Result should be passed to the post action");
                postActionCalled.set(true);
            }
        );
        LinearJob linearJob = new TestLinearJob(resultSupplier);
        assertEquals(expectedResult, linearJob.executeRunners(metaData));
        assertTrue(postActionCalled.get(), "Post action should be called");
    }

    @Test
    void negativeTwoPassOneFailVerifyMerge() {
        MetaData metaData = mock(MetaData.class);

        Job.ResultSupplier resultSupplier = new Job.ResultSupplier(
            true,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 1");
                }, md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.FAILED, "Success 2");
                }, md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 3");
                })
        );
        LinearJob linearJob = new TestLinearJob(resultSupplier);
        Job.Result expectedResult = new Job.Result(Status.FAILED, "Success 1\\nSuccess 2\\nSuccess 3");
        assertEquals(expectedResult, linearJob.executeRunners(metaData));
    }

    @Test
    void negativeOnePassTwoFailVerifyMergeWithoutContinueOnFail() {
        MetaData metaData = mock(MetaData.class);

        Job.ResultSupplier resultSupplier = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 1");
                }, md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.FAILED, "Success 2");
                }, md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 3");
                })
        );
        LinearJob linearJob = new TestLinearJob(resultSupplier);
        Job.Result expectedResult = new Job.Result(Status.FAILED, "Success 1\\nSuccess 2");
        assertEquals(expectedResult, linearJob.executeRunners(metaData));
    }


    static class TestLinearJob extends LinearJob {

        private ResultSupplier resultSupplier;

        public TestLinearJob(ResultSupplier resultSupplier) {
            super();
            this.resultSupplier = resultSupplier;
        }

        @Override
        public ResultSupplier getResultSupplier() {
            return this.resultSupplier;
        }
    }
}
