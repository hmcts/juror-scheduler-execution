package uk.gov.hmcts.juror.job.execution.jobs.checks.pnc.batch;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import uk.gov.hmcts.juror.job.execution.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.job.execution.client.contracts.PoliceNationalCheckServiceClient;
import uk.gov.hmcts.juror.job.execution.client.contracts.SchedulerServiceClient;
import uk.gov.hmcts.juror.job.execution.database.model.MetaData;
import uk.gov.hmcts.juror.job.execution.database.model.RequirePncCheck;
import uk.gov.hmcts.juror.job.execution.jobs.Job;
import uk.gov.hmcts.juror.job.execution.model.Status;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.job.execution.testsupport.TestConstants;
import uk.gov.hmcts.juror.job.execution.testsupport.TestUtil;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PncBatchJobTest {

    private DatabaseService databaseService;
    private PncBatchConfig config;
    private PoliceNationalCheckServiceClient policeNationalCheckServiceClient;
    private SchedulerServiceClient schedulerServiceClient;
    private JurorServiceClient jurorServiceClient;
    private PncBatchJob pncBatchJob;

    @BeforeEach
    void beforeEach() {
        databaseService = mock(DatabaseService.class);
        policeNationalCheckServiceClient = mock(PoliceNationalCheckServiceClient.class);
        schedulerServiceClient = mock(SchedulerServiceClient.class);
        jurorServiceClient = mock(JurorServiceClient.class);
        config = new PncBatchConfig();

        pncBatchJob = spy(new PncBatchJob(databaseService, config, policeNationalCheckServiceClient,
            schedulerServiceClient, jurorServiceClient));
    }

    @Test
    void positiveStaticConstructorTest() {

        assertEquals("TOTAL_CHECKS_REQUESTED", PncBatchJob.TOTAL_CHECKS_REQUESTED_KEY,
            "TOTAL_CHECKS_REQUESTED_KEY is not as expected");
        assertEquals("TOTAL_CHECKS_IN_BATCH", PncBatchJob.TOTAL_CHECKS_IN_BATCH_KEY,
            "TOTAL_CHECKS_IN_BATCH_KEY is not as expected");
        assertEquals("TOTAL_BATCHES_RESPONSES", PncBatchJob.TOTAL_BATCHES_RESPONSES_KEY,
            "TOTAL_BATCHES_RESPONSES_KEY is not as expected");

        assertEquals("TOTAL_BATCHES_REQUESTED", PncBatchJob.TOTAL_BATCHES_REQUESTED_KEY,
            "TOTAL_BATCHES_REQUESTED_KEY is not as expected");

        assertEquals("select * from juror_mod.require_pnc_check_view",
            PncBatchJob.GET_JURORS_THAT_REQUIRE_PNC_CHECK_SQL,
            "GET_JURORS_THAT_REQUIRE_PNC_CHECK_SQL is not as expected");

        assertEquals(PoliceCheck.values().length + 3, PncBatchJob.META_DATA_KEYS_TO_COMBINE.size());
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            assertTrue(PncBatchJob.META_DATA_KEYS_TO_COMBINE.contains("TOTAL_WITH_STATUS_" + policeCheck.name()),
                "TOTAL_WITH_STATUS_" + policeCheck.name() + " is not in META_DATA_KEYS_TO_COMBINE");
        }
        assertTrue(PncBatchJob.META_DATA_KEYS_TO_COMBINE.contains("TOTAL_CHECKS_REQUESTED"),
            "TOTAL_CHECKS_REQUESTED is not in META_DATA_KEYS_TO_COMBINE");
        assertTrue(PncBatchJob.META_DATA_KEYS_TO_COMBINE.contains("TOTAL_CHECKS_IN_BATCH"),
            "TOTAL_CHECKS_IN_BATCH is not in META_DATA_KEYS_TO_COMBINE");
        assertTrue(PncBatchJob.META_DATA_KEYS_TO_COMBINE.contains("TOTAL_NULL_RESULTS"),
            "TOTAL_NULL_RESULTS is not in META_DATA_KEYS_TO_COMBINE");
        TestUtil.isUnmodifiable(PncBatchJob.META_DATA_KEYS_TO_COMBINE);
    }

    @Test
    void positiveConstructorTest() {
        assertSame(databaseService, pncBatchJob.getDatabaseService(),
            "databaseService is not as expected");
        assertSame(config, pncBatchJob.getConfig(), "config is not as expected");
        assertSame(policeNationalCheckServiceClient, pncBatchJob.getPoliceNationalCheckServiceClient(),
            "policeNationalCheckServiceClient is not as expected");
        assertSame(schedulerServiceClient, pncBatchJob.getSchedulerServiceClient(),
            "schedulerServiceClient is not as expected");
        assertSame(jurorServiceClient, pncBatchJob.getJurorServiceClient(),
            "jurorServiceClient is not as expected");
    }

    @Test
    void positiveGetResultSupplierTestEmptyChecks() {
        Job.ResultSupplier resultSupplier = pncBatchJob.getResultSupplier();
        assertFalse(resultSupplier.isContinueOnFailure(),
            "isContinueOnFailure is not as expected");
        assertEquals(1, resultSupplier.getResultRunners().size(),
            "results size is not as expected");
        assertNull(resultSupplier.getPostRunChecks(),
            "postRunChecks should be null");

        MetaData metaData = mock(MetaData.class);
        List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequestList
            = List.of();
        when(pncBatchJob.getJurorsToCheck())
            .thenReturn(jurorCheckRequestList);


        Job.Result result = resultSupplier.getResultRunners().iterator().next().apply(metaData);
        assertEquals(Status.SUCCESS, result.getStatus(),
            "result status is not as expected");
        assertEquals("0 jurors need PNC checks. As such none were sent to the PNC service", result.getMessage(),
            "result message is not as expected");
        assertEquals(2, result.getMetaData().size(),
            "result metaData size is not as expected");
        assertEquals("0", result.getMetaData().get("TOTAL_BATCHES_REQUESTED"),
            "result metaData TOTAL_BATCHES_REQUESTED_KEY is not as expected");
        assertEquals("0", result.getMetaData().get("TOTAL_CHECKS_REQUESTED"),
            "result metaData TOTAL_CHECKS_REQUESTED_KEY is not as expected");

        verify(pncBatchJob, times(1))
            .getJurorsToCheck();
        verify(pncBatchJob, never())
            .triggerPncCheck(any(), any());
    }

    @Test
    void positiveGetResultSupplierTest() {
        Job.ResultSupplier resultSupplier = pncBatchJob.getResultSupplier();
        assertFalse(resultSupplier.isContinueOnFailure(),
            "isContinueOnFailure is not as expected");
        assertEquals(1, resultSupplier.getResultRunners().size(),
            "results size is not as expected");
        assertNull(resultSupplier.getPostRunChecks(),
            "postRunChecks should be null");

        MetaData metaData = mock(MetaData.class);
        List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequestList
            = List.of(mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class),
            mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class));

        when(pncBatchJob.getJurorsToCheck())
            .thenReturn(jurorCheckRequestList);
        doReturn(3).when(pncBatchJob)
            .triggerPncCheck(jurorCheckRequestList, metaData);


        Job.Result result = resultSupplier.getResultRunners().iterator().next().apply(metaData);
        assertEquals(Status.PROCESSING, result.getStatus(),
            "result status is not as expected");
        assertEquals("2 jurors sent to the PNC service to be checked. In 3 batches", result.getMessage(),
            "result message is not as expected");
        assertEquals(2, result.getMetaData().size(),
            "result metaData size is not as expected");
        assertEquals("3", result.getMetaData().get("TOTAL_BATCHES_REQUESTED"),
            "result metaData TOTAL_BATCHES_REQUESTED_KEY is not as expected");
        assertEquals("2", result.getMetaData().get("TOTAL_CHECKS_REQUESTED"),
            "result metaData TOTAL_CHECKS_REQUESTED_KEY is not as expected");

        verify(pncBatchJob, times(1))
            .getJurorsToCheck();
        verify(pncBatchJob, times(1))
            .triggerPncCheck(jurorCheckRequestList, metaData);
    }


    @Test
    void positiveTriggerPncCheck() {
        config.setBatchSize(2);
        List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequestList
            = List.of(mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class),
            mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class));
        MetaData metaData = TestConstants.VALID_META_DATA;

        assertEquals(1, pncBatchJob.triggerPncCheck(jurorCheckRequestList, metaData),
            "triggerPncCheck should return 1");

        PoliceNationalCheckServiceClient.JurorCheckRequestBulk expectedRequest =
            PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().checks(jurorCheckRequestList).metaData(
                new PoliceNationalCheckServiceClient.JurorCheckRequestBulk.MetaData(metaData.getJobKey(),
                    metaData.getTaskId())).build();

        verify(policeNationalCheckServiceClient, times(1))
            .checkJurors(expectedRequest);


        verifyNoMoreInteractions(policeNationalCheckServiceClient);
    }

    @Test
    void positiveTriggerPncCheckBatchSplit() {
        config.setBatchSize(2);
        List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequestList
            = List.of(mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class),
            mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class),
            mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class),
            mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class),
            mock(PoliceNationalCheckServiceClient.JurorCheckRequest.class)
        );
        MetaData metaData = TestConstants.VALID_META_DATA;

        assertEquals(3, pncBatchJob.triggerPncCheck(jurorCheckRequestList, metaData),
            "triggerPncCheck should return 3");


        verify(policeNationalCheckServiceClient, times(1))
            .checkJurors(PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().checks(List.of(
                jurorCheckRequestList.get(0), jurorCheckRequestList.get(1)
            )).metaData(
                new PoliceNationalCheckServiceClient.JurorCheckRequestBulk.MetaData(metaData.getJobKey(),
                    metaData.getTaskId())).build());

        verify(policeNationalCheckServiceClient, times(1))
            .checkJurors(PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().checks(List.of(
                jurorCheckRequestList.get(2), jurorCheckRequestList.get(3)
            )).metaData(
                new PoliceNationalCheckServiceClient.JurorCheckRequestBulk.MetaData(metaData.getJobKey(),
                    metaData.getTaskId())).build());

        verify(policeNationalCheckServiceClient, times(1))
            .checkJurors(PoliceNationalCheckServiceClient.JurorCheckRequestBulk.builder().checks(List.of(
                jurorCheckRequestList.get(4)
            )).metaData(
                new PoliceNationalCheckServiceClient.JurorCheckRequestBulk.MetaData(metaData.getJobKey(),
                    metaData.getTaskId())).build());

        verifyNoMoreInteractions(policeNationalCheckServiceClient);
    }

    @Nested
    @DisplayName("private List<PoliceNationalCheckServiceClient.JurorCheckRequest> getJurorsToCheck()")
    class GetJurorsToCheck {
        @Test
        void positiveGetJurorsToCheckTypical() {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());


            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(null),
                createRequirePncCheck(PoliceCheck.NOT_CHECKED),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(0).toJurorCheckRequest(),
                requirePncCheck.get(1).toJurorCheckRequest(),
                requirePncCheck.get(2).toJurorCheckRequest()
            );
            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");

            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.IN_PROGRESS));
            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(1).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.IN_PROGRESS));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);
        }

        @Test
        void positiveGetJurorsToCheckInsufficientInformationMissingPostCode() {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());


            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(null).setPostcode(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS).setPostcode(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(2).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");


            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));
            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(1).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);
        }


        @Test
        void positiveGetJurorsToCheckInsufficientInformationMissingDateOfBirth() {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());


            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(null).setDateOfBirth(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS).setDateOfBirth(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                                                          "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(2).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                         "jurorCheckRequests is not as expected");


            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                      new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));
            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(1).getJurorNumber(),
                      new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                                          "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);

        }

        @Test
        void positiveGetJurorsToCheckInsufficientInformationMissingFirstName() {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());


            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(null).setFirstName(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS).setFirstName(null)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(1).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");


            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));
            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(2).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);

        }

        @Test
        void positiveGetJurorsToCheckInsufficientInformationMissingBothPostCodeAndDateOfBirth() {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());


            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(null).setDateOfBirth(null).setPostcode(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS).setDateOfBirth(null).setPostcode(null),
                createRequirePncCheck(PoliceCheck.IN_PROGRESS)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(2).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");


            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));
            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(1).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.INSUFFICIENT_INFORMATION));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);

        }

        @Test
        void positiveGetJurorsToCheckNullPoliceCheck() {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());

            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(null)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(0).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");


            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.IN_PROGRESS));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);
        }

        @ParameterizedTest
        @EnumSource(value = PoliceCheck.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"NOT_CHECKED"})
        @NullSource
        void positiveGetJurorsToCheckPoliceCheckUpdatedToInProgress(PoliceCheck policeCheck) {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());

            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(policeCheck)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(0).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");


            verify(jurorServiceClient, times(1))
                .call(requirePncCheck.get(0).getJurorNumber(),
                    new JurorServiceClient.Payload(PoliceCheck.IN_PROGRESS));

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(jurorServiceClient, databaseService);
        }

        @ParameterizedTest
        @EnumSource(value = PoliceCheck.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"NOT_CHECKED"})
        void positiveGetJurorsToCheckPoliceCheckOtherPoliceCheckStatus(PoliceCheck policeCheck) {
            Connection connection = mock(Connection.class);

            doAnswer(invocation -> {
                Consumer<Connection> connectionConsumer = invocation.getArgument(1);
                connectionConsumer.accept(connection);
                return null;
            }).when(databaseService).execute(eq(config.getDatabase()), any());

            List<RequirePncCheck> requirePncCheck = List.of(
                createRequirePncCheck(policeCheck)
            );
            when(databaseService.executePreparedStatement(connection, RequirePncCheck.class,
                "select * from juror_mod.require_pnc_check_view"))
                .thenReturn(requirePncCheck);


            List<PoliceNationalCheckServiceClient.JurorCheckRequest> jurorCheckRequests
                = pncBatchJob.getJurorsToCheck();

            List<PoliceNationalCheckServiceClient.JurorCheckRequest> expectedJurorChecks = List.of(
                requirePncCheck.get(0).toJurorCheckRequest()
            );

            assertEquals(expectedJurorChecks, jurorCheckRequests,
                "jurorCheckRequests is not as expected");

            verify(databaseService, times(1))
                .execute(eq(config.getDatabase()), any());

            verify(databaseService, times(1))
                .executePreparedStatement(connection, RequirePncCheck.class,
                    "select * from juror_mod.require_pnc_check_view");
            verifyNoMoreInteractions(databaseService);
            verifyNoInteractions(jurorServiceClient);

        }

        private RequirePncCheck createRequirePncCheck(PoliceCheck policeCheck) {
            return createRequirePncCheck(LocalDate.now(),
                RandomStringUtils.randomAlphabetic(5),
                policeCheck,
                RandomStringUtils.randomAlphabetic(10));
        }


        private RequirePncCheck createRequirePncCheck(
            LocalDate dataOfBirth, String postCode, PoliceCheck policeCheck, String middleName
        ) {
            RequirePncCheck requirePncCheck = new RequirePncCheck();
            requirePncCheck.setJurorNumber(RandomStringUtils.randomNumeric(9));
            requirePncCheck.setDateOfBirth(dataOfBirth);
            requirePncCheck.setPostcode(postCode);
            requirePncCheck.setPoliceCheck(policeCheck);
            requirePncCheck.setFirstName(RandomStringUtils.randomAlphabetic(10));
            requirePncCheck.setMiddleName(middleName);
            requirePncCheck.setLastName(RandomStringUtils.randomAlphabetic(10));
            return requirePncCheck;
        }

    }

    @Nested
    @DisplayName("public void updateResult(SchedulerServiceClient.StatusUpdatePayload payload, String jobKey, Long "
        + "taskId)")
    class UpdateResult {


        @Test
        void positiveUpdateResult() {

        }

        @Test
        void negativeNullPayload() {
            pncBatchJob.updateResult(null, TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);
            verifyNoInteractions(schedulerServiceClient, databaseService);
        }

        @Test
        void negativeTaskNotFound() {

            when(schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG))
                .thenReturn(null);

            InternalServerException exception = assertThrows(InternalServerException.class,
                () -> pncBatchJob.updateResult(createStatusUpdatePayload(),
                    TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG),
                "updateResult should throw InternalServerException");
            assertEquals("Task with jobKey: " + TestConstants.VALID_JOB_KEY + " taskId: "
                    + TestConstants.VALID_TASK_ID_LONG + " was not found. Failed to update status",
                exception.getMessage(),
                "exception message is not as expected");
            assertNull(exception.getCause(),
                "exception cause should be null");
        }

        @Test
        void positiveCombineMetaData() {
            SchedulerServiceClient.TaskResponse oldTaskResponse = new SchedulerServiceClient.TaskResponse();
            oldTaskResponse.setMetaData(Map.of(
                "TOTAL_BATCHES_REQUESTED", "2",
                "TOTAL_CHECKS_REQUESTED", "6"
            ));

            when(schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG))
                .thenReturn(oldTaskResponse);
            SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
            payload.setStatus(Status.SUCCESS);
            payload.setMessage("Hey I am a message");
            payload.addMetaData("TOTAL_WITH_STATUS_INELIGIBLE", "1");
            payload.addMetaData("TOTAL_WITH_STATUS_ELIGIBLE", "3");
            payload.addMetaData("TOTAL_CHECKS_IN_BATCH", "4");

            doNothing().when(pncBatchJob).combineMetaData(payload, oldTaskResponse);
            pncBatchJob.updateResult(payload,
                TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);

            verify(schedulerServiceClient, times(1))
                .getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);
            verify(schedulerServiceClient, times(1))
                .updateStatus(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG, payload);


            verify(pncBatchJob, times(1)).combineMetaData(payload, oldTaskResponse);

            assertEquals(4, payload.getMetaData().size());
            assertEquals("1",
                payload.getMetaDataValue("TOTAL_BATCHES_RESPONSES"),
                "TOTAL_BATCHES_RESPONSES is not as expected");
            assertEquals("1",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_INELIGIBLE"),
                "TOTAL_WITH_STATUS_INELIGIBLE is not as expected");
            assertEquals("3",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_ELIGIBLE"),
                "TOTAL_WITH_STATUS_ELIGIBLE is not as expected");
            assertEquals("4",
                payload.getMetaDataValue("TOTAL_CHECKS_IN_BATCH"),
                "TOTAL_CHECKS_IN_BATCH is not as expected");

            assertEquals("Hey I am a message", payload.getMessage(),
                "payload message is not as expected");

            verifyNoMoreInteractions(schedulerServiceClient);
            verifyNoInteractions(databaseService);
        }

        @Test
        void positiveTotalBatchesRespondEqualsTotalBatchesRequested() {
            SchedulerServiceClient.TaskResponse oldTaskResponse = new SchedulerServiceClient.TaskResponse();
            oldTaskResponse.setMetaData(Map.of(
                "TOTAL_BATCHES_REQUESTED", "2",
                "TOTAL_CHECKS_REQUESTED", "4",
                "TOTAL_BATCHES_RESPONSES", "1"
            ));

            when(schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG))
                .thenReturn(oldTaskResponse);
            SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
            payload.setStatus(Status.SUCCESS);
            payload.setMessage("Hey I am a message");
            payload.addMetaData("TOTAL_WITH_STATUS_INELIGIBLE", "1");
            payload.addMetaData("TOTAL_WITH_STATUS_ELIGIBLE", "3");
            payload.addMetaData("TOTAL_CHECKS_IN_BATCH", "4");
            payload.addMetaData("TOTAL_CHECKS_REQUESTED", "4");

            doNothing().when(pncBatchJob).combineMetaData(payload, oldTaskResponse);
            pncBatchJob.updateResult(payload,
                TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);

            verify(schedulerServiceClient, times(1))
                .getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);
            verify(schedulerServiceClient, times(1))
                .updateStatus(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG, payload);


            verify(pncBatchJob, times(1)).combineMetaData(payload, oldTaskResponse);

            assertEquals(5, payload.getMetaData().size());
            assertEquals("2",
                payload.getMetaDataValue("TOTAL_BATCHES_RESPONSES"),
                "TOTAL_BATCHES_RESPONSES is not as expected");
            assertEquals("1",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_INELIGIBLE"),
                "TOTAL_WITH_STATUS_INELIGIBLE is not as expected");
            assertEquals("3",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_ELIGIBLE"),
                "TOTAL_WITH_STATUS_ELIGIBLE is not as expected");
            assertEquals("4",
                payload.getMetaDataValue("TOTAL_CHECKS_IN_BATCH"),
                "TOTAL_CHECKS_IN_BATCH is not as expected");
            assertEquals("4",
                payload.getMetaDataValue("TOTAL_CHECKS_REQUESTED"),
                "TOTAL_CHECKS_REQUESTED is not as expected");


            assertEquals("All batches have successfully processed", payload.getMessage(),
                "payload message is not as expected");
            assertEquals(Status.SUCCESS, payload.getStatus(),
                "payload status is not as expected");
            verifyNoMoreInteractions(schedulerServiceClient);
            verifyNoInteractions(databaseService);
        }

        @Test
        void positiveTotalBatchesRespondEqualsTotalBatchesRequestedWithErrors() {
            SchedulerServiceClient.TaskResponse oldTaskResponse = new SchedulerServiceClient.TaskResponse();
            oldTaskResponse.setMetaData(Map.of(
                "TOTAL_BATCHES_REQUESTED", "2",
                "TOTAL_CHECKS_REQUESTED", "4",
                "TOTAL_BATCHES_RESPONSES", "1"
            ));

            when(schedulerServiceClient.getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG))
                .thenReturn(oldTaskResponse);
            SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
            payload.setStatus(Status.SUCCESS);
            payload.setMessage("Hey I am a message");
            payload.addMetaData("TOTAL_WITH_STATUS_INELIGIBLE", "1");
            payload.addMetaData("TOTAL_WITH_STATUS_ELIGIBLE", "2");
            payload.addMetaData("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR", "1");
            payload.addMetaData("TOTAL_CHECKS_IN_BATCH", "4");
            payload.addMetaData("TOTAL_CHECKS_REQUESTED", "4");

            doNothing().when(pncBatchJob).combineMetaData(payload, oldTaskResponse);
            pncBatchJob.updateResult(payload,
                TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);

            verify(schedulerServiceClient, times(1))
                .getTask(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG);
            verify(schedulerServiceClient, times(1))
                .updateStatus(TestConstants.VALID_JOB_KEY, TestConstants.VALID_TASK_ID_LONG, payload);


            verify(pncBatchJob, times(1)).combineMetaData(payload, oldTaskResponse);

            assertEquals(6, payload.getMetaData().size());
            assertEquals("2",
                payload.getMetaDataValue("TOTAL_BATCHES_RESPONSES"),
                "TOTAL_BATCHES_RESPONSES is not as expected");
            assertEquals("1",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_INELIGIBLE"),
                "TOTAL_WITH_STATUS_INELIGIBLE is not as expected");
            assertEquals("2",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_ELIGIBLE"),
                "TOTAL_WITH_STATUS_ELIGIBLE is not as expected");
            assertEquals("1",
                payload.getMetaDataValue("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR"),
                "TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR is not as expected");
            assertEquals("4",
                payload.getMetaDataValue("TOTAL_CHECKS_IN_BATCH"),
                "TOTAL_CHECKS_IN_BATCH is not as expected");
            assertEquals("4",
                payload.getMetaDataValue("TOTAL_CHECKS_REQUESTED"),
                "TOTAL_CHECKS_REQUESTED is not as expected");


            assertEquals("All batches have processed but some checks did not process", payload.getMessage(),
                "payload message is not as expected");
            assertEquals(Status.PARTIAL_SUCCESS, payload.getStatus(),
                "payload status is not as expected");
            verifyNoMoreInteractions(schedulerServiceClient);
            verifyNoInteractions(databaseService);
        }
    }

    @Test
    void positiveCombineMetaData() {
        int minValue = 0;
        SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
        Map<String, String> oldTaskMetaData = new ConcurrentHashMap<>();
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            payload.addMetaData("TOTAL_WITH_STATUS_" + policeCheck.name(),
                Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
            oldTaskMetaData.put("TOTAL_WITH_STATUS_" + policeCheck.name(),
                Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
            minValue += 10;
        }
        payload.addMetaData("TOTAL_CHECKS_REQUESTED",
            Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        oldTaskMetaData.put("TOTAL_CHECKS_REQUESTED",
            Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        payload.addMetaData("TOTAL_CHECKS_IN_BATCH",
            Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        oldTaskMetaData.put("TOTAL_CHECKS_IN_BATCH",
            Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        payload.addMetaData("TOTAL_NULL_RESULTS", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        oldTaskMetaData.put("TOTAL_NULL_RESULTS", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        //Values that should not be merged
        payload.addMetaData("TOTAL_123", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        oldTaskMetaData.put("TOTAL_123", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        payload.addMetaData("TOTAL_321", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        oldTaskMetaData.put("TOTAL_321", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        payload.addMetaData("MetaValue", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        oldTaskMetaData.put("MetaValue", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));

        SchedulerServiceClient.TaskResponse oldTaskResponse = new SchedulerServiceClient.TaskResponse();
        oldTaskResponse.setMetaData(oldTaskMetaData);


        doAnswer(invocation -> invocation.getArgument(1)).when(pncBatchJob)
            .combineMetaData(any(String.class), any(String.class));

        pncBatchJob.combineMetaData(payload, oldTaskResponse);

        verify(pncBatchJob, times(1))
            .combineMetaData(payload, oldTaskResponse);//Required for verify no more interactions
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            verify(pncBatchJob, times(1))
                .combineMetaData(
                    oldTaskMetaData.get("TOTAL_WITH_STATUS_" + policeCheck.name()),
                    payload.getMetaDataValue("TOTAL_WITH_STATUS_" + policeCheck.name()));
        }

        verify(pncBatchJob, times(1))
            .combineMetaData(
                oldTaskMetaData.get("TOTAL_CHECKS_REQUESTED"),
                payload.getMetaDataValue("TOTAL_CHECKS_REQUESTED"));
        verify(pncBatchJob, times(1))
            .combineMetaData(
                oldTaskMetaData.get("TOTAL_CHECKS_IN_BATCH"),
                payload.getMetaDataValue("TOTAL_CHECKS_IN_BATCH"));

        verify(pncBatchJob, times(1))
            .combineMetaData(
                oldTaskMetaData.get("TOTAL_NULL_RESULTS"),
                payload.getMetaDataValue("TOTAL_NULL_RESULTS"));
        verifyNoMoreInteractions(pncBatchJob);

    }

    @Test
    void positiveCombineMetaDataNullTaskResponse() {
        int minValue = 0;
        SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            payload.addMetaData("TOTAL_WITH_STATUS_" + policeCheck.name(),
                Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10))
            );
            minValue += 10;
        }
        //Values that should not be merged
        payload.addMetaData("TOTAL_123", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        payload.addMetaData("TOTAL_321", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));
        minValue += 10;

        payload.addMetaData("MetaValue", Integer.toString(RandomUtils.nextInt(minValue + 1, minValue + 10)));

        assertEquals(PoliceCheck.values().length + 3, payload.getMetaData().size(),
            "payload metaData size is not as expected");
        pncBatchJob.combineMetaData(payload, null);

        assertEquals(PoliceCheck.values().length + 6, payload.getMetaData().size(),
            "payload metaData size is not as expected");
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            assertEquals(payload.getMetaDataValue("TOTAL_WITH_STATUS_" + policeCheck.name()),
                payload.getMetaDataValue("TOTAL_WITH_STATUS_" + policeCheck.name()),
                "TOTAL_WITH_STATUS_" + policeCheck.name() + " is not as expected");
        }
        assertEquals("0", payload.getMetaDataValue("TOTAL_CHECKS_REQUESTED"),
            "TOTAL_CHECKS_REQUESTED is not as expected");
        assertEquals("0", payload.getMetaDataValue("TOTAL_CHECKS_IN_BATCH"),
            "TOTAL_CHECKS_IN_BATCH is not as expected");

        assertEquals("0", payload.getMetaDataValue("TOTAL_NULL_RESULTS"),
            "TOTAL_NULL_RESULTS is not as expected");


        verify(pncBatchJob, never()).combineMetaData(any(String.class), any(String.class));

        verify(pncBatchJob, times(1))
            .combineMetaData(payload, null); //Required for verifyNoMore interactions
        verifyNoMoreInteractions(pncBatchJob);
    }


    public static Stream<Arguments> combineMetaDataArgumentProvider() {
        return Stream.of(
            Arguments.arguments(
                "Old Value is null and new value has value. New value returned", null, "5", "5"),
            Arguments.arguments(
                "Old Value has value and new value is null. Old value returned", "6", null, "6"),
            Arguments.arguments(
                "Both new and old values are null. 0 returned", null, null, "0"),
            Arguments.arguments(
                "Old Value not numeric. New value returned", "ABC", "13", "13"),
            Arguments.arguments(
                "Old value is numeric but New Value not numeric. Old value returned", "12", "ABC", "12"),
            Arguments.arguments(
                "Both old and new values are numeric. Sum of both returned", "12", "12", "24")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("combineMetaDataArgumentProvider")
    @DisplayName("String combineMetaData(String oldValue, String newValue)")
    void validate(String title, String oldValue, String newValue, String expectedValue) {
        assertEquals(expectedValue, pncBatchJob.combineMetaData(oldValue, newValue),
            "combineMetaData is not as expected");
    }


    @Test
    void positiveSumMetaData() {
        SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            payload.addMetaData("TOTAL_WITH_STATUS_" + policeCheck.name(),
                Long.toString((long) Math.pow(10, policeCheck.ordinal())));
        }
        long expectedValue = sum(PoliceCheck.ELIGIBLE, PoliceCheck.INELIGIBLE);
        long actualValue = pncBatchJob.sumMetaData(payload, PoliceCheck.ELIGIBLE, PoliceCheck.INELIGIBLE);
        assertEquals(expectedValue, actualValue,
            "sumMetaData is not as expected");
    }

    @Test
    void positiveSumMetaDataAll() {
        SchedulerServiceClient.StatusUpdatePayload payload = createStatusUpdatePayload();
        for (PoliceCheck policeCheck : PoliceCheck.values()) {
            payload.addMetaData("TOTAL_WITH_STATUS_" + policeCheck.name(),
                Long.toString((long) Math.pow(10, policeCheck.ordinal())));
        }
        long expectedValue = sum(PoliceCheck.values());
        long actualValue = pncBatchJob.sumMetaData(payload, PoliceCheck.values());
        assertEquals(expectedValue, actualValue,
            "sumMetaData is not as expected");
    }

    private long sum(PoliceCheck... policeChecks) {
        long value = 0;
        for (PoliceCheck policeCheck : policeChecks) {
            value += (long) Math.pow(10, policeCheck.ordinal());
        }
        return value;
    }


    private SchedulerServiceClient.StatusUpdatePayload createStatusUpdatePayload() {
        return new SchedulerServiceClient.StatusUpdatePayload(
            Status.SUCCESS,
            "message",
            new ConcurrentHashMap<>()
        );
    }
}
