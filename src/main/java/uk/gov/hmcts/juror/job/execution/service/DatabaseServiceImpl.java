package uk.gov.hmcts.juror.job.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.job.execution.config.DatabaseConfig;
import uk.gov.hmcts.juror.job.execution.database.DatabaseFieldConvertor;
import uk.gov.hmcts.juror.job.execution.service.contracts.DatabaseService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {

    @Override
    public void execute(DatabaseConfig config, Consumer<Connection> connectionConsumer) {
        try (Connection connection = getConnection(config)) {
            connectionConsumer.accept(connection);
        } catch (Exception e) {
            log.error("Failed to execute connection sql consumer", e);
            throw new InternalServerException("Failed to execute connection sql consumer", e);
        }
    }

    private Connection getConnection(DatabaseConfig databaseConfig) throws SQLException {
        Connection connection = DriverManager.getConnection(databaseConfig.getUrl(),
            databaseConfig.getUsername(),
            databaseConfig.getPassword());
        try {
            connection.setSchema(databaseConfig.getSchema());
        } catch (Exception exception) {
            connection.close();
            log.error("Failed to set database schema", exception);
            throw exception;//Close connection and throw original error we do not want to use try with resource here
            // as we need the connection to remain open for consuming sources
        }
        return connection;
    }


    @Override
    public void executeStoredProcedure(Connection connection, String procedureName, Object... arguments) {
        final String sql = "{CALL " + procedureName + "(" + StringUtils.chop("?,".repeat(arguments.length)) + ")}";
        log.debug("Attempting to run sql: '" + sql + "' with parameters: " + Arrays.toString(arguments));
        try (CallableStatement callableStatement = connection.prepareCall(sql)) {
            for (int i = 0; i < arguments.length; i++) {
                callableStatement.setObject(i + 1, arguments[i]);
            }
            callableStatement.execute();
            log.debug("Call to " + procedureName + " Successful");
        } catch (Exception e) {
            log.error("Failed to execute stored procedure: " + procedureName, e);
            throw new InternalServerException("Failed to execute stored procedure: " + procedureName, e);
        }
    }

    @Override
    public void executeStoredProcedure(DatabaseConfig config, String procedureName, Object... arguments) {
        this.execute(config, connection -> executeStoredProcedure(connection, procedureName, arguments));
    }

    @Override
    public <T> T executeStoredProcedureWithReturn(Connection connection, String procedureName, Class<T> returnClass,
                                                  int returnSqlType,
                                                  Object... arguments) {
        final String sql = "{? = CALL " + procedureName + "(" + StringUtils.chop("?,".repeat(arguments.length)) + ")}";
        log.debug("Attempting to run sql: '" + sql + "' with parameters: " + Arrays.toString(arguments));
        try (CallableStatement callableStatement = connection.prepareCall(sql)) {

            callableStatement.registerOutParameter(1, returnSqlType);
            for (int i = 0; i < arguments.length; i++) {
                callableStatement.setObject(i + 2, arguments[i]);
            }
            callableStatement.execute();
            log.debug("Call to " + procedureName + " Successful");
            return callableStatement.getObject(1, returnClass);
        } catch (Exception e) {
            log.error("Failed to execute stored procedure: " + procedureName, e);
            throw new InternalServerException("Failed to execute stored procedure: " + procedureName, e);
        }
    }

    @Override
    public void executeUpdate(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < arguments.length; i++) {
                ps.setObject(i + 1, arguments[i]);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public <T> List<T> executePreparedStatement(Connection connection, Class<T> convertToClass, String sql, Object...
        arguments) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < arguments.length; i++) {
                ps.setObject(i + 1, arguments[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    return Collections.emptyList();
                }
                List<T> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(DatabaseFieldConvertor.convertToItem(convertToClass, rs));
                }
                return items;
            }
        } catch (Exception e) {
            log.error("Failed to get result set", e);
            throw new InternalServerException("Failed to get result set", e);
        }
    }
}
