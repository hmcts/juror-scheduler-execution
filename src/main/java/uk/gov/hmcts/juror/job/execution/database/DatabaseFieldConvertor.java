package uk.gov.hmcts.juror.job.execution.database;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Slf4j
public final class DatabaseFieldConvertor {

    static final Map<Class<?>, BiFunction<DatabaseColumn, ResultSet, ?>> CONVERTERS = new ConcurrentHashMap<>();

    private DatabaseFieldConvertor() {

    }

    static {
        CONVERTERS.put(String.class, (databaseColumn, resultSet) -> {
            if (databaseColumn.isClob()) {
                Clob clob = getResultSetObject(resultSet, databaseColumn.name(), Clob.class);
                try {
                    return clob.getSubString(1, (int) clob.length());
                } catch (Exception e) {
                    throw new InternalServerException("Failed to convert clob to string", e);
                }
            } else {
                return getResultSetObject(resultSet, databaseColumn.name(), String.class);
            }
        });
        CONVERTERS.put(Integer.class,
            (databaseColumn, resultSet) -> getResultSetObject(resultSet, databaseColumn.name(), Integer.class));

        CONVERTERS.put(BigDecimal.class,
            (databaseColumn, resultSet) -> getResultSetObject(resultSet, databaseColumn.name(), BigDecimal.class));

        CONVERTERS.put(LocalDate.class,
            (databaseColumn, resultSet) -> {
                Date date = getResultSetObject(resultSet, databaseColumn.name(), Date.class);
                return date == null ? null : date.toLocalDate();
            });
    }

    static <T> T getResultSetObject(ResultSet resultSet, String columnName, Class<T> type) {
        try {
            return resultSet.getObject(columnName, type);
        } catch (Exception e) {
            throw new InternalServerException("Failed to convert result set into: " + type, e);
        }
    }


    public static <T> T convertToItem(Class<T> convertToClass, ResultSet resultSet) {
        try {
            T dto = convertToClass.getConstructor().newInstance();

            for (Field field : convertToClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(DatabaseColumn.class)) {
                    continue;
                }
                DatabaseColumn databaseColumn = field.getAnnotation(DatabaseColumn.class);
                Method method = convertToClass.getMethod(databaseColumn.setter(), field.getType());
                method.invoke(dto, convertField(field, resultSet));
            }
            return dto;
        } catch (Exception exception) {
            log.error("Failed to convert result set into: " + convertToClass, exception);
            throw new InternalServerException("Failed to convert result set into: " + convertToClass, exception);
        }
    }

    @SuppressWarnings("unchecked")//This is checked via the .isEnum() method
    static Object convertField(Field field, ResultSet resultSet) {
        final DatabaseColumn databaseColumn = field.getAnnotation(DatabaseColumn.class);
        if (field.getType().isEnum()) {
            return getEnumInstance(field.getType().asSubclass(Enum.class),
                getResultSetObject(resultSet, databaseColumn.name(), String.class)
            );
        }
        if (!CONVERTERS.containsKey(field.getType())) {
            throw new InternalServerException("Unsupported class type: " + field.getType());
        }
        return CONVERTERS.get(field.getType()).apply(databaseColumn, resultSet);
    }

    static <T extends Enum<T>> T getEnumInstance(final Class<T> enumClass, final String value) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumClass, value);
    }
}
