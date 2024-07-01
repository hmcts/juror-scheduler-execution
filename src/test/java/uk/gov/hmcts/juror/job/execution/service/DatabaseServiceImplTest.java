package uk.gov.hmcts.juror.job.execution.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.DatabaseFieldConvertor;
import uk.gov.hmcts.juror.job.execution.database.model.Count;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DatabaseServiceImpl")
class DatabaseServiceImplTest {

    private DatabaseServiceImpl databaseService;
    private MockedStatic<DriverManager> driverManagerMockedStatic;
    private Connection connection;

    private DatabaseConfig config;
    private DatabaseConfig defaultDatabaseConfig;

    @BeforeEach
    void beforeEach() {
        connection = mock(Connection.class);
        defaultDatabaseConfig = new DatabaseConfig();
        defaultDatabaseConfig.setUsername("defaultDatabaseUsername");
        defaultDatabaseConfig.setPassword("defaultDatabasePassword");
        defaultDatabaseConfig.setUrl("defaultDatabaseUrl");
        defaultDatabaseConfig.setSchema("defaultDatabaseSchema");

        databaseService = new DatabaseServiceImpl(defaultDatabaseConfig);

        config = new DatabaseConfig();
        config.setUsername("databaseUsername");
        config.setPassword("databasePassword");
        config.setUrl("databaseUrl");
        config.setSchema("databaseSchema");
    }

    @AfterEach
    void afterEach() {
        if (driverManagerMockedStatic != null) {
            driverManagerMockedStatic.close();
        }
    }


    @Nested
    @DisplayName("public void execute(DatabaseConfig config, Consumer<Connection> connectionConsumer)")
    class Execute {
        @BeforeEach
        void beforeEach() {
            driverManagerMockedStatic = Mockito.mockStatic(DriverManager.class);
            driverManagerMockedStatic.when(() -> DriverManager.getConnection(config.getUrl(), config.getUsername(),
                    config.getPassword()
                ))
                .thenReturn(connection);
        }

        @Test
        void positiveExecutionTest() throws SQLException {
            AtomicBoolean hasRun = new AtomicBoolean(false);
            databaseService.execute(config, providedConnection -> {
                assertSame(connection, providedConnection,
                    "Connection should be the same");
                hasRun.set(true);
            });
            verify(connection, times(1))
                .setSchema(config.getSchema());
            driverManagerMockedStatic.verify(() -> DriverManager.getConnection(config.getUrl(), config.getUsername(),
                config.getPassword()
            ), times(1));
            assertTrue(hasRun.get(), "Consumer should have run");
            verify(connection, times(1)).setSchema(config.getSchema());
            verify(connection, times(1)).close();
            verifyNoMoreInteractions(connection);
        }

        @Test
        void negativeExecutionTestUnexpectedException() {
            RuntimeException cause = new RuntimeException("I am the cause");
            InternalServerException internalServerException = assertThrows(InternalServerException.class, () -> {
                databaseService.execute(config, connection -> {
                    throw cause;
                });
            }, "Should throw InternalServerException");
            assertEquals("Failed to execute connection sql consumer", internalServerException.getMessage(),
                "Message should be the same");
            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");
        }

        @Test
        void negativeExecutionTestUnexpectedExceptionBeforeConnection() {
            driverManagerMockedStatic.when(() -> DriverManager.getConnection(config.getUrl(), config.getUsername(),
                    config.getPassword()
                ))
                .thenReturn(null);
            InternalServerException internalServerException = assertThrows(InternalServerException.class, () -> {
                databaseService.execute(config, connection -> {
                    fail("should not get here");
                });
            }, "Should throw InternalServerException");
            assertEquals("Failed to execute connection sql consumer",
                internalServerException.getMessage(), "Message should be the same");
            assertEquals(NullPointerException.class, internalServerException.getCause().getClass(),
                "Cause should be the same");
        }

        @Test
        void negativeExecutionTestUnexpectedConnectionException() throws SQLException {
            RuntimeException cause = new RuntimeException("I am the cause");
            AtomicBoolean hasRun = new AtomicBoolean(false);

            doThrow(cause).when(connection).setSchema(config.getSchema());

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> databaseService.execute(config, connection -> hasRun.set(true)),
                "Should throw InternalServerException");

            assertEquals("Failed to execute connection sql consumer", internalServerException.getMessage(),
                "Message should be the same");

            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");

            assertFalse(hasRun.get(), "Consumer should not have run as exception occurred when getting connection");

            verify(connection, times(1)).setSchema(config.getSchema());
            verify(connection, times(1)).close();
            verifyNoMoreInteractions(connection);
        }
    }

    @Nested
    @DisplayName("public void executeStoredProcedure(Connection connection, String procedureName, Object... arguments)")
    class ExecuteStoredProcedure {
        @Test
        void positiveTypicalWithArguments() throws Exception {
            CallableStatement callableStatement = mock(CallableStatement.class);
            when(connection.prepareCall("CALL procedureForTestPurposes(?,?)")).thenReturn(callableStatement);

            databaseService.executeStoredProcedure(connection, "procedureForTestPurposes", "argument1", "argument2");

            verify(connection, times(1)).prepareCall("CALL procedureForTestPurposes(?,?)");
            verify(callableStatement, times(1)).setObject(1, "argument1");
            verify(callableStatement, times(1)).setObject(2, "argument2");
            verify(callableStatement, times(1)).execute();
            verify(callableStatement, times(1)).close();
            verifyNoMoreInteractions(connection, callableStatement);
        }

        @Test
        void positiveTypicalWithOutArguments() throws Exception {
            CallableStatement callableStatement = mock(CallableStatement.class);
            when(connection.prepareCall("CALL procedureForTestPurposes()")).thenReturn(callableStatement);

            databaseService.executeStoredProcedure(connection, "procedureForTestPurposes");

            verify(connection, times(1)).prepareCall("CALL procedureForTestPurposes()");
            verify(callableStatement, times(1)).execute();
            verify(callableStatement, times(1)).close();
            verifyNoMoreInteractions(connection, callableStatement);
        }

        @Test
        void negativeUnexpectedException() throws Exception {
            RuntimeException cause = new RuntimeException("I am the cause");
            CallableStatement callableStatement = mock(CallableStatement.class);
            when(connection.prepareCall("CALL procedureForTestPurposes(?)")).thenReturn(callableStatement);
            doThrow(cause).when(callableStatement).execute();

            InternalServerException internalServerException =
                assertThrows(InternalServerException.class, () -> databaseService.executeStoredProcedure(connection,
                        "procedureForTestPurposes",
                        "Argument 1"),
                    "Should throw InternalServerException");

            assertEquals("Failed to execute stored procedure: procedureForTestPurposes",
                internalServerException.getMessage(), "Message should be the same");
            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");

            verify(connection, times(1)).prepareCall("CALL procedureForTestPurposes(?)");
            verify(callableStatement, times(1)).setObject(1, "Argument 1");
            verify(callableStatement, times(1)).execute();
            verify(callableStatement, times(1)).close();
            verifyNoMoreInteractions(connection, callableStatement);
        }
    }

    @Test
    @DisplayName(" public void executeStoredProcedure(DatabaseConfig config, String procedureName, Object... "
        + "arguments)")
    void positiveVerifyExecuteStoredProcedureOverloadedMethod() throws Exception {
        driverManagerMockedStatic = Mockito.mockStatic(DriverManager.class);
        driverManagerMockedStatic.when(() -> DriverManager.getConnection(config.getUrl(), config.getUsername(),
                config.getPassword()
            ))
            .thenReturn(connection);
        CallableStatement callableStatement = mock(CallableStatement.class);
        when(connection.prepareCall("CALL procedureForTestPurposes(?,?)")).thenReturn(callableStatement);

        DatabaseServiceImpl databaseServiceImplSpy = Mockito.spy(databaseService);
        databaseServiceImplSpy.executeStoredProcedure(config, "procedureForTestPurposes", "Argument 1", "Argument 2");

        verify(databaseServiceImplSpy, times(1))
            .executeStoredProcedure(connection,
                "procedureForTestPurposes",
                "Argument 1", "Argument 2");
    }


    @Nested
    @DisplayName("""
         public <T> T executeStoredProcedureWithReturn(Connection connection, 
                        String procedureName, Class<T> returnClass,
                        int returnSqlType,
                        Object... arguments)
        """)
    class ExecuteStoredProcedureWithReturn {

        @Test
        void positiveTypicalWithArguments() throws Exception {
            CallableStatement callableStatement = mock(CallableStatement.class);
            when(connection.prepareCall("{? = CALL procedureForTestPurposes(?,?)}")).thenReturn(callableStatement);

            final String expectedReturnValue = "I am the expected value";
            when(callableStatement.getObject(1, String.class)).thenReturn(expectedReturnValue);

            String returnValue = databaseService.executeStoredProcedureWithReturn(
                connection,
                "procedureForTestPurposes",
                String.class,
                Types.VARCHAR,
                "argument1", "argument2");

            assertEquals(expectedReturnValue, returnValue, "Return value should be the same");

            verify(connection, times(1)).prepareCall("{? = CALL procedureForTestPurposes(?,?)}");
            verify(callableStatement, times(1)).registerOutParameter(1, Types.VARCHAR);
            verify(callableStatement, times(1)).setObject(2, "argument1");
            verify(callableStatement, times(1)).setObject(3, "argument2");
            verify(callableStatement, times(1)).getObject(1, String.class);
            verify(callableStatement, times(1)).execute();
            verify(callableStatement, times(1)).close();

            verifyNoMoreInteractions(connection, callableStatement);
        }

        @Test
        void positiveTypicalWithOutArguments() throws Exception {
            CallableStatement callableStatement = mock(CallableStatement.class);
            when(connection.prepareCall("{? = CALL procedureForTestPurposes()}")).thenReturn(callableStatement);

            final String expectedReturnValue = "I am the expected value";
            when(callableStatement.getObject(1, String.class)).thenReturn(expectedReturnValue);

            String returnValue = databaseService.executeStoredProcedureWithReturn(
                connection,
                "procedureForTestPurposes",
                String.class,
                Types.VARCHAR);

            assertEquals(expectedReturnValue, returnValue, "Return value should be the same");

            verify(connection, times(1)).prepareCall("{? = CALL procedureForTestPurposes()}");
            verify(callableStatement, times(1)).registerOutParameter(1, Types.VARCHAR);
            verify(callableStatement, times(1)).getObject(1, String.class);
            verify(callableStatement, times(1)).execute();
            verify(callableStatement, times(1)).close();

            verifyNoMoreInteractions(connection, callableStatement);
        }

        @Test
        void negativeUnexpectedException() throws Exception {
            RuntimeException cause = new RuntimeException("I am the cause");

            CallableStatement callableStatement = mock(CallableStatement.class);
            when(connection.prepareCall("{? = CALL procedureForTestPurposes(?)}")).thenReturn(callableStatement);

            final String expectedReturnValue = "I am the expected value";
            when(callableStatement.getObject(1, String.class)).thenReturn(expectedReturnValue);
            doThrow(cause).when(callableStatement).execute();

            InternalServerException internalServerException =
                assertThrows(InternalServerException.class, () -> databaseService.executeStoredProcedureWithReturn(
                        connection,
                        "procedureForTestPurposes",
                        String.class,
                        Types.VARCHAR,
                        "argument1"),
                    "Should throw InternalServerException");

            assertEquals("Failed to execute stored procedure with return: procedureForTestPurposes",
                internalServerException.getMessage(), "Message should be the same");
            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");


            verify(connection, times(1)).prepareCall("{? = CALL procedureForTestPurposes(?)}");
            verify(callableStatement, times(1)).registerOutParameter(1, Types.VARCHAR);
            verify(callableStatement, times(1)).setObject(2, "argument1");
            verify(callableStatement, times(1)).execute();
            verify(callableStatement, times(1)).close();
            verifyNoMoreInteractions(connection, callableStatement);
        }
    }

    @Nested
    @DisplayName("public void executeUpdate(Connection connection, String sql, Object... arguments)")
    class ExecuteUpdate {
        @Test
        void positiveTypicalWithArguments() throws Exception {
            final String sql = "UPDATE USER SET CS.password=? WHERE userId=?";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

            databaseService.executeUpdate(connection, sql, "password", "userId");

            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).executeUpdate();
            verify(preparedStatement, times(1)).setObject(1, "password");
            verify(preparedStatement, times(1)).setObject(2, "userId");
            verify(preparedStatement, times(1)).close();
            verifyNoMoreInteractions(connection, preparedStatement);
        }

        @Test
        void positiveTypicalWithoutArguments() throws Exception {
            final String sql = "UPDATE USER SET CS.password=password404";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

            databaseService.executeUpdate(connection, sql);

            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).executeUpdate();
            verify(preparedStatement, times(1)).close();
            verifyNoMoreInteractions(connection, preparedStatement);
        }

        @Test
        void negativeUnexpectedException() throws Exception {
            RuntimeException cause = new RuntimeException("I am the cause");

            final String sql = "UPDATE USER SET CS.password=password404";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
            doThrow(cause).when(preparedStatement).executeUpdate();

            RuntimeException thrownException = assertThrows(RuntimeException.class,
                () -> databaseService.executeUpdate(connection, sql),
                "Should throw exception that was thrown");
            assertSame(cause, thrownException, "Exception should be the same");
            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).executeUpdate();
            verify(preparedStatement, times(1)).close();//Ensures that connection is closed even on exception
            verifyNoMoreInteractions(connection, preparedStatement);
        }
    }

    @Nested
    @DisplayName("""
        public <T> List<T> executePreparedStatement(Connection connection, Class<T> convertToClass, String sql, 
        Object... arguments)
        """)
    class ExecutePreparedStatement {
        private MockedStatic<DatabaseFieldConvertor> databaseFieldConvertorMockedStatic;

        @BeforeEach
        void beforeEach() {
            databaseFieldConvertorMockedStatic = Mockito.mockStatic(DatabaseFieldConvertor.class);
        }

        @AfterEach
        void afterEach() {
            if (databaseFieldConvertorMockedStatic != null) {
                databaseFieldConvertorMockedStatic.close();
            }
        }

        @Test
        void positiveWithArguments() throws Exception {
            final String sql = "SELECT count(1) as count FROM USER WHERE userId=?";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

            ResultSet resultSet = mock(ResultSet.class);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.isBeforeFirst()).thenReturn(true);
            databaseFieldConvertorMockedStatic.when(
                    () -> DatabaseFieldConvertor.convertToItem(Count.class, resultSet))
                .thenReturn(new Count().setValue(1), new Count().setValue(2));
            when(resultSet.next()).thenReturn(true, true, false);

            List<Count> counts = databaseService.executePreparedStatement(connection, Count.class, sql, "userId");

            assertEquals(2, counts.size(), "Should have 2 counts");
            assertEquals(1, counts.get(0).getValue(), "First value should have 1 count");
            assertEquals(2, counts.get(1).getValue(), "Second value should have 2 count");

            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).setObject(1, "userId");
            verify(preparedStatement, times(1)).executeQuery();
            verify(resultSet, times(1)).isBeforeFirst();
            verify(resultSet, times(3)).next();
            verify(resultSet, times(1)).close();
            verify(preparedStatement, times(1)).close();
            verifyNoMoreInteractions(connection, preparedStatement, resultSet);
        }

        @Test
        void positiveWithoutArguments() throws Exception {
            final String sql = "SELECT count(1) as count FROM USER";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

            ResultSet resultSet = mock(ResultSet.class);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.isBeforeFirst()).thenReturn(true);
            databaseFieldConvertorMockedStatic.when(
                    () -> DatabaseFieldConvertor.convertToItem(Count.class, resultSet))
                .thenReturn(new Count().setValue(1));
            when(resultSet.next()).thenReturn(true, false);

            List<Count> counts = databaseService.executePreparedStatement(connection, Count.class, sql);

            assertEquals(1, counts.size(), "Should have 1 counts");
            assertEquals(1, counts.get(0).getValue(), "First value should have 1 count");

            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).executeQuery();
            verify(resultSet, times(1)).isBeforeFirst();
            verify(resultSet, times(2)).next();
            verify(resultSet, times(1)).close();
            verify(preparedStatement, times(1)).close();
            verifyNoMoreInteractions(connection, preparedStatement, resultSet);
        }

        @Test
        void positiveNoResponse() throws Exception {
            final String sql = "SELECT count(1) as count FROM USER";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

            ResultSet resultSet = mock(ResultSet.class);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.isBeforeFirst()).thenReturn(false);

            List<Count> counts = databaseService.executePreparedStatement(connection, Count.class, sql);

            assertEquals(0, counts.size(), "Should have 0 counts");

            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).executeQuery();
            verify(resultSet, times(1)).isBeforeFirst();
            verify(resultSet, times(1)).close();
            verify(preparedStatement, times(1)).close();
            verifyNoMoreInteractions(connection, preparedStatement, resultSet);
        }

        @Test
        void negativeUnexpectedException() throws Exception {
            final String sql = "SELECT count(1) as count FROM USER";
            PreparedStatement preparedStatement = mock(PreparedStatement.class);
            when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

            RuntimeException cause = new RuntimeException("I am the cause");
            when(preparedStatement.executeQuery()).thenThrow(cause);

            InternalServerException internalServerException =
                assertThrows(InternalServerException.class, () -> databaseService.executePreparedStatement(connection,
                    Count.class, sql), "Should throw InternalServerException");

            assertEquals("Failed to get result set", internalServerException.getMessage(),
                "Message should be the same");
            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");

            verify(connection, times(1)).prepareStatement(sql);
            verify(preparedStatement, times(1)).executeQuery();
            verify(preparedStatement, times(1)).close();
            verifyNoMoreInteractions(connection, preparedStatement);
        }
    }

    @Nested
    @DisplayName("DatabaseConfig getEffectiveDatabaseConfig(DatabaseConfig config)")
    class GetEffectiveDatabaseConfig {

        void assertDefaultConfig(DatabaseConfig actual, DatabaseConfig expected) {
            assertThat(actual.getSchema()).isEqualTo(expected.getSchema());
            assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
            assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
            assertThat(actual.getPassword()).isEqualTo(expected.getPassword());
        }

        @Test
        void positiveProvidedIsNull() {
            assertThat(databaseService.getEffectiveDatabaseConfig(null)).isEqualTo(defaultDatabaseConfig);
        }

        @Test
        void positiveProvidedIsEmpty() {
            assertDefaultConfig(databaseService.getEffectiveDatabaseConfig(new DatabaseConfig()),
                defaultDatabaseConfig);
        }

        @Test
        void positiveMissingSchema() {
            config.setSchema(null);
            assertDefaultConfig(databaseService.getEffectiveDatabaseConfig(config), DatabaseConfig.builder()
                .schema(defaultDatabaseConfig.getSchema())
                .url(config.getUrl())
                .username(config.getUsername())
                .password(config.getPassword())
                .build());
        }

        @Test
        void positiveMissingUrl() {
            config.setUrl(null);
            assertDefaultConfig(databaseService.getEffectiveDatabaseConfig(config), DatabaseConfig.builder()
                .schema(config.getSchema())
                .url(defaultDatabaseConfig.getUrl())
                .username(config.getUsername())
                .password(config.getPassword())
                .build());
        }

        @Test
        void positiveMissingUsername() {
            config.setUsername(null);
            assertDefaultConfig(databaseService.getEffectiveDatabaseConfig(config), DatabaseConfig.builder()
                .schema(config.getSchema())
                .url(config.getUrl())
                .username(defaultDatabaseConfig.getUsername())
                .password(config.getPassword())
                .build());
        }

        @Test
        void positiveMissingPassword() {
            config.setPassword(null);
            assertDefaultConfig(databaseService.getEffectiveDatabaseConfig(config), DatabaseConfig.builder()
                .schema(config.getSchema())
                .url(config.getUrl())
                .username(config.getUsername())
                .password(defaultDatabaseConfig.getPassword())
                .build());
        }
    }
}