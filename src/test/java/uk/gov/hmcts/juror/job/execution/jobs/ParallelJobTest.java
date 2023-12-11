package uk.gov.hmcts.juror.job.execution.jobs;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.model.Status;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class ParallelJobTest {

    @Test
    void positiveSingleResultSupplier() {
        AtomicInteger postActionCalled = new AtomicInteger(0);

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
                postActionCalled.incrementAndGet();
            }
        );
        ParallelJob parallelJob = new TestParallelJob(resultSupplier);
        assertEquals(expectedResult, parallelJob.executeRunners(metaData));
        assertEquals(1, postActionCalled.get(), "Post action should be called");
    }

    @Test
    void positiveMultipleResultSuppliers() {
        AtomicInteger postActionCalled = new AtomicInteger(0);
        MetaData metaData = mock(MetaData.class);
        Job.ResultSupplier resultSupplier1 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 11");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 12");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 13");
                }),
            result -> {
                assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
                assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
                assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier2 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 21");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 22");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 23");
                }),
            result -> {

                assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
                assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
                assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier3 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 33");
                }
            ),
            result -> {
                assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 33"), "Message should contain Success 33");
                assertEquals(10, result.getMessage().length(), "Message should be 10 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        ParallelJob parallelJob = new TestParallelJob(resultSupplier1, resultSupplier2, resultSupplier3);
        Job.Result result = parallelJob.executeRunners(metaData);
        assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
        assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
        assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
        assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
        assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
        assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
        assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
        assertTrue(result.getMessage().contains("Success 33"), "Message should contain Success 33");
        assertEquals(82, result.getMessage().length(), "Message should be 82 characters long");
        assertEquals(3, postActionCalled.get(), "Post action should be called");
    }

    @Test
    void positiveMultipleResultSuppliersContinueOnFail() {
        AtomicInteger postActionCalled = new AtomicInteger(0);
        MetaData metaData = mock(MetaData.class);
        Job.ResultSupplier resultSupplier1 = new Job.ResultSupplier(
            true,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 11");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 12");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 13");
                }),
            result -> {
                assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
                assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
                assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier2 = new Job.ResultSupplier(
            true,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 21");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.FAILED, "Success 22");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 23");
                }),
            result -> {
                assertEquals(Status.FAILED, result.getStatus(), "Status should be FAILED");
                assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
                assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
                assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier3 = new Job.ResultSupplier(
            true,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 33");
                }
            ),
            result -> {
                assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 33"), "Message should contain Success 33");
                assertEquals(10, result.getMessage().length(), "Message should be 10 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        ParallelJob parallelJob = new TestParallelJob(resultSupplier1, resultSupplier2, resultSupplier3);
        Job.Result result = parallelJob.executeRunners(metaData);
        assertEquals(Status.FAILED, result.getStatus(), "Status should be FAILED");
        assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
        assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
        assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
        assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
        assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
        assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
        assertTrue(result.getMessage().contains("Success 33"), "Message should contain Success 33");
        assertEquals(82, result.getMessage().length(), "Message should be 82 characters long");
        assertEquals(3, postActionCalled.get(), "Post action should be called");
    }

    @Test
    void positiveMultipleResultSuppliersDontContinueOnFail() {
        AtomicInteger postActionCalled = new AtomicInteger(0);
        MetaData metaData = mock(MetaData.class);
        Job.ResultSupplier resultSupplier1 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 11");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 12");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 13");
                }),
            result -> {
                assertEquals(Status.SUCCESS, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
                assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
                assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier2 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 21");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.FAILED, "Success 22");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 23");
                }),
            result -> {
                assertEquals(Status.FAILED, result.getStatus(), "Status should be FAILED");
                assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
                assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
                assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier3 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 33");
                }
            ),
            result -> {
                postActionCalled.incrementAndGet();
                fail("Post action should not be called");
            }
        );
        ParallelJob parallelJob = new TestParallelJob(resultSupplier1, resultSupplier2, resultSupplier3);
        Job.Result result = parallelJob.executeRunners(metaData);
        assertEquals(Status.FAILED, result.getStatus(), "Status should be FAILED");
        assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
        assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
        assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
        assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
        assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
        assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
        assertEquals(70, result.getMessage().length(), "Message should be 82 characters long");
        assertEquals(2, postActionCalled.get(), "Post action should be called");
    }

    @Test
    void positiveMultipleResultSuppliersContinueOnFailMix() {
        AtomicInteger postActionCalled = new AtomicInteger(0);
        MetaData metaData = mock(MetaData.class);
        Job.ResultSupplier resultSupplier1 = new Job.ResultSupplier(
            true,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 11");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.FAILED, "Success 12");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 13");
                }),
            result -> {
                assertEquals(Status.FAILED, result.getStatus(), "Status should be SUCCESS");
                assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
                assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
                assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier2 = new Job.ResultSupplier(
            false,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 21");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.FAILED, "Success 22");
                },
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 23");
                }),
            result -> {
                assertEquals(Status.FAILED, result.getStatus(), "Status should be FAILED");
                assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
                assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
                assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
                assertEquals(34, result.getMessage().length(), "Message should be 34 characters long");
                postActionCalled.incrementAndGet();
            }
        );
        Job.ResultSupplier resultSupplier3 = new Job.ResultSupplier(
            true,
            List.of(
                md -> {
                    assertSame(metaData, md, "Metadata should be passed to the runner");
                    return new Job.Result(Status.SUCCESS, "Success 33");
                }
            ),
            result -> {
                postActionCalled.incrementAndGet();
                fail("Post action should not be called");
            }
        );
        ParallelJob parallelJob = new TestParallelJob(resultSupplier1, resultSupplier2, resultSupplier3);
        Job.Result result = parallelJob.executeRunners(metaData);
        assertEquals(Status.FAILED, result.getStatus(), "Status should be FAILED");
        assertTrue(result.getMessage().contains("Success 11"), "Message should contain Success 11");
        assertTrue(result.getMessage().contains("Success 12"), "Message should contain Success 12");
        assertTrue(result.getMessage().contains("Success 13"), "Message should contain Success 13");
        assertTrue(result.getMessage().contains("Success 21"), "Message should contain Success 21");
        assertTrue(result.getMessage().contains("Success 22"), "Message should contain Success 22");
        assertTrue(result.getMessage().contains("Success 23"), "Message should contain Success 23");
        assertEquals(70, result.getMessage().length(), "Message should be 82 characters long");
        assertEquals(2, postActionCalled.get(), "Post action should be called");
    }

    static class TestParallelJob extends ParallelJob {

        private List<ResultSupplier> resultSupplier;

        public TestParallelJob(ResultSupplier... resultSupplier) {
            super();
            this.resultSupplier = List.of(resultSupplier);
        }

        @Override
        public List<ResultSupplier> getResultSuppliers() {
            return resultSupplier;
        }
    }
}
