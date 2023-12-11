package uk.gov.hmcts.juror.job.execution.service.contracts;

import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public interface DatabaseService {


    void execute(DatabaseConfig config, Consumer<Connection> connectionConsumer);

    void executeStoredProcedure(Connection connection, String procedureName, Object... arguments);

    void executeStoredProcedure(DatabaseConfig config, String procedureName, Object... arguments);

    <T> T executeStoredProcedureWithReturn(Connection connection, String procedureName,
                                           Class<T> returnClass,
                                           int returnSqlType,
                                           Object... arguments);

    void executeUpdate(Connection connection, String sql, Object... arguments) throws SQLException;

    <T> List<T> executePreparedStatement(Connection connection, Class<T> clazz, String sql, Object... arguments);
}
