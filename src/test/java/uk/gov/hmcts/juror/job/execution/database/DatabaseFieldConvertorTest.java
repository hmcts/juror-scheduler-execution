package uk.gov.hmcts.juror.job.execution.database;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DatabaseFieldConvertorTest {

    private DatabaseColumn createDatabaseColumnMock(String name, String setter) {
        return createDatabaseColumnMock(name, setter, false);
    }

    private DatabaseColumn createDatabaseColumnMock(String name, String setter, boolean isClob) {
        DatabaseColumn databaseColumn = mock(DatabaseColumn.class);
        when(databaseColumn.isClob()).thenReturn(isClob);
        when(databaseColumn.name()).thenReturn(name);
        when(databaseColumn.setter()).thenReturn(setter);
        return databaseColumn;
    }

    @Nested
    @DisplayName("Convertor Tests")
    class ConvertorTests {
        @Test
        void positiveStringClobConvertorTest() throws Exception {
            final String columnName = "testClob";
            Clob clob = mock(Clob.class);
            String expectedValue = "testClobValue";
            when(clob.length()).thenReturn(12L);
            when(clob.getSubString(1, (int) clob.length())).thenReturn(expectedValue);

            ResultSet resultSet = mock(ResultSet.class);

            when(resultSet.getObject(columnName, Clob.class)).thenReturn(clob);
            DatabaseColumn databaseColumn = createDatabaseColumnMock(columnName, "setTestClob", true);
            assertEquals(expectedValue, DatabaseFieldConvertor.CONVERTERS.get(String.class).apply(databaseColumn,
                resultSet), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, Clob.class);
            verifyNoMoreInteractions(resultSet);
        }


        @Test
        void negativeStringClobConvertorTestUnexpectedException() throws Exception {
            final String columnName = "testClob";
            DatabaseColumn databaseColumn = createDatabaseColumnMock("testClob", "setTestClob", true);
            Clob clob = mock(Clob.class);
            when(clob.length()).thenReturn(12L);
            RuntimeException cause = new RuntimeException("I am the cause");
            when(clob.getSubString(1, (int) clob.length())).thenThrow(cause);

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject("testClob", Clob.class)).thenReturn(clob);

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> DatabaseFieldConvertor.CONVERTERS.get(String.class).apply(databaseColumn, resultSet),
                "Should throw InternalServerException");

            assertEquals("Failed to convert clob to string", internalServerException.getMessage(),
                "Message should be the same");
            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");
            verify(resultSet, times(1)).getObject(columnName, Clob.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void positiveStringConvertorTest() throws Exception {
            final String columnName = "testString";
            String expectedValue = "testStringValue";
            DatabaseColumn databaseColumn = createDatabaseColumnMock(columnName, "setTestString");
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, String.class)).thenReturn(expectedValue);
            assertEquals(expectedValue, DatabaseFieldConvertor.CONVERTERS.get(String.class).apply(databaseColumn,
                resultSet), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, String.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void positiveIntegerConvertorTest() throws Exception {
            final String columnName = "testInteger";
            Integer expectedValue = 123;
            DatabaseColumn databaseColumn = createDatabaseColumnMock(columnName, "setTestInteger");
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, Integer.class)).thenReturn(expectedValue);
            assertEquals(expectedValue, DatabaseFieldConvertor.CONVERTERS.get(Integer.class).apply(databaseColumn,
                resultSet), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, Integer.class);
            verifyNoMoreInteractions(resultSet);

        }

        @Test
        void positiveBigDecimalConvertorTest() throws Exception {
            final String columnName = "testBigDecimal";
            BigDecimal expectedValue = new BigDecimal(1);
            DatabaseColumn databaseColumn = createDatabaseColumnMock(columnName, "setBigDecimal");
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, BigDecimal.class)).thenReturn(expectedValue);
            assertEquals(expectedValue, DatabaseFieldConvertor.CONVERTERS.get(BigDecimal.class).apply(databaseColumn,
                resultSet), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, BigDecimal.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void positiveLocalDateConvertorTest() throws Exception {
            final String columnName = "testLocalDate";
            Timestamp timestamp = mock(Timestamp.class);
            LocalDateTime expectedValue = LocalDateTime.now();
            when(timestamp.toLocalDateTime()).thenReturn(expectedValue);
            DatabaseColumn databaseColumn = createDatabaseColumnMock(columnName, "setLocalDate");
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, Timestamp.class)).thenReturn(timestamp);
            assertEquals(expectedValue.toLocalDate(), DatabaseFieldConvertor.CONVERTERS.get(LocalDate.class).apply(databaseColumn,
                resultSet), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, Timestamp.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void negativeLocalDateConvertorTestNotFound() throws Exception {
            final String columnName = "testLocalDate";
            DatabaseColumn databaseColumn = createDatabaseColumnMock(columnName, "setLocalDate");
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, Timestamp.class)).thenReturn(null);
            assertNull(DatabaseFieldConvertor.CONVERTERS.get(LocalDate.class).apply(databaseColumn,
                resultSet), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, Timestamp.class);
            verifyNoMoreInteractions(resultSet);
        }

    }

    @Nested
    @DisplayName("Static constructor test")
    class StaticConstructorTest {
        @Test
        void positiveConstructorTest() {
            assertEquals(4, DatabaseFieldConvertor.CONVERTERS.size(),
                "Converters should have 4 entries");
            assertTrue(DatabaseFieldConvertor.CONVERTERS.containsKey(String.class),
                "Converters should contain String.class");
            assertTrue(DatabaseFieldConvertor.CONVERTERS.containsKey(Integer.class),
                "Converters should contain Integer.class");
            assertTrue(DatabaseFieldConvertor.CONVERTERS.containsKey(BigDecimal.class),
                "Converters should contain BigDecimal.class");
            assertTrue(DatabaseFieldConvertor.CONVERTERS.containsKey(LocalDate.class),
                "Converters should contain LocalDate.class");
        }
    }

    @Nested
    @DisplayName("static <T> T getResultSetObject(ResultSet resultSet, String columnName, Class<T> type)")
    class GetResultSetObject {

        @Test
        void positiveTypical() throws Exception {
            final String columnName = "testColumnName";
            final String expectedValue = "testValue";
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, String.class)).thenReturn(expectedValue);
            assertEquals(expectedValue, DatabaseFieldConvertor.getResultSetObject(resultSet, columnName,
                String.class), "Should return expected value");
            verify(resultSet, times(1)).getObject(columnName, String.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void negativeUnexpectedException() throws Exception {
            final String columnName = "testColumnName";
            RuntimeException cause = new RuntimeException("I am the cause");
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject(columnName, String.class)).thenThrow(cause);

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> DatabaseFieldConvertor.getResultSetObject(resultSet, columnName, String.class),
                "Should throw InternalServerException");
            assertEquals("Failed to convert result set into: class java.lang.String",
                internalServerException.getMessage(), "Message should be the same");
            assertEquals(cause, internalServerException.getCause(), "Cause should be the same");
            verify(resultSet, times(1)).getObject(columnName, String.class);
            verifyNoMoreInteractions(resultSet);
        }
    }

    @Nested
    @DisplayName("public static <T> T convertToItem(Class<T> convertToClass, ResultSet resultSet)")
    class ConvertToItem {

        @Setter
        @Getter
        public static class SingleFieldConvert {

            @DatabaseColumn(name = "dbColumnName", setter = "setMyStringValue")
            private String myStringValue;
        }

        @Setter
        @Getter
        public static class MultipleFieldConvert {

            @DatabaseColumn(name = "dbColumnName", setter = "setMyStringValue")
            private String myStringValue;
            @DatabaseColumn(name = "owner", setter = "setOwner")
            private String owner;
            @DatabaseColumn(name = "count", setter = "setTotal")
            private Integer total;
        }

        @Setter
        @Getter
        public static class MixFieldConvert {

            @DatabaseColumn(name = "dbColumnName", setter = "setMyStringValue")
            private String myStringValue;
            private String owner;
            @DatabaseColumn(name = "count", setter = "setTotal")
            private Integer total;
        }


        public static class NoFieldConvert {
        }

        @Setter
        @Getter
        public static class NoValidFieldConvert {

            private String myStringValue;
            private String owner;
            private Integer total;
        }


        @Test
        void positiveTypicalSingleField() throws Exception {
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject("dbColumnName", String.class)).thenReturn("testValue");
            SingleFieldConvert result = DatabaseFieldConvertor.convertToItem(SingleFieldConvert.class, resultSet);

            assertNotNull(result, "Should return not null");
            assertEquals("testValue", result.getMyStringValue(), "Should return expected value");

            verify(resultSet, times(1)).getObject("dbColumnName", String.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void positiveTypicalMultipleFields() throws Exception {
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject("dbColumnName", String.class)).thenReturn("testValue");
            when(resultSet.getObject("owner", String.class)).thenReturn("testOwner");
            when(resultSet.getObject("count", Integer.class)).thenReturn(10);
            MultipleFieldConvert result = DatabaseFieldConvertor.convertToItem(MultipleFieldConvert.class, resultSet);

            assertNotNull(result, "Should return not null");
            assertEquals("testValue", result.getMyStringValue(), "Should return expected value");
            assertEquals("testOwner", result.getOwner(), "Should return expected value");
            assertEquals(10, result.getTotal(), "Should return expected value");

            verify(resultSet, times(1)).getObject("dbColumnName", String.class);
            verify(resultSet, times(1)).getObject("owner", String.class);
            verify(resultSet, times(1)).getObject("count", Integer.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void positiveNoFields() {
            ResultSet resultSet = mock(ResultSet.class);

            NoFieldConvert result = DatabaseFieldConvertor.convertToItem(NoFieldConvert.class, resultSet);

            assertNotNull(result, "Should return not null");
            verifyNoInteractions(resultSet);
        }

        @Test
        void positiveNoValidFields() {
            ResultSet resultSet = mock(ResultSet.class);
            NoValidFieldConvert result = DatabaseFieldConvertor.convertToItem(NoValidFieldConvert.class, resultSet);
            assertNotNull(result, "Should return not null");
            assertNull(result.getMyStringValue(), "Should return expected value");
            assertNull(result.getOwner(), "Should return expected value");
            assertNull(result.getTotal(), "Should return expected value");
            verifyNoInteractions(resultSet);
        }

        @Test
        void positiveMixOfFields() throws Exception {

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject("dbColumnName", String.class)).thenReturn("testValue");
            when(resultSet.getObject("count", Integer.class)).thenReturn(10);
            MixFieldConvert result = DatabaseFieldConvertor.convertToItem(MixFieldConvert.class, resultSet);

            assertNotNull(result, "Should return not null");
            assertEquals("testValue", result.getMyStringValue(), "Should return expected value");
            assertNull(result.getOwner(), "Should return expected value");
            assertEquals(10, result.getTotal(), "Should return expected value");

            verify(resultSet, times(1)).getObject("dbColumnName", String.class);
            verify(resultSet, times(1)).getObject("count", Integer.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void negativeUnexpectedException() {
            ResultSet resultSet = mock(ResultSet.class);

            InternalServerException internalServerException =
                assertThrows(InternalServerException.class, () -> DatabaseFieldConvertor.convertToItem(
                    null, resultSet), "Should throw InternalServerException");
            assertEquals("Failed to convert result set into: null", internalServerException.getMessage(),
                "Message should be the same");
            assertInstanceOf(NullPointerException.class, internalServerException.getCause(),
                "Cause should be the same");
            verifyNoInteractions(resultSet);
        }
    }

    @Nested
    @DisplayName("static Object convertField(Field field, ResultSet resultSet)")
    class ConvertField {

        public enum TestEnum {
            A, B, C, AB, AC, ABC;
        }

        @Test
        void positiveEnumConversion() throws Exception {
            Field field = mock(Field.class);
            doReturn(TestEnum.class).when(field).getType();
            DatabaseColumn databaseColumn = createDatabaseColumnMock("testEnum", "setTestEnum");
            when(field.getAnnotation(DatabaseColumn.class)).thenReturn(databaseColumn);

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject("testEnum", String.class)).thenReturn("A");

            assertEquals(TestEnum.A, DatabaseFieldConvertor.convertField(field, resultSet),
                "Should return expected value");
            verify(resultSet, times(1)).getObject("testEnum", String.class);
            verifyNoMoreInteractions(resultSet);
        }

        @Test
        void positiveUsingConvertors() throws Exception {
            final String expectedValue = "myStringValue";
            Field field = mock(Field.class);
            doReturn(String.class).when(field).getType();
            DatabaseColumn databaseColumn = createDatabaseColumnMock("testString", "setTestString");
            when(field.getAnnotation(DatabaseColumn.class)).thenReturn(databaseColumn);

            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getObject("testString", String.class)).thenReturn(expectedValue);

            assertEquals(expectedValue, DatabaseFieldConvertor.convertField(field, resultSet),
                "Should return expected value");
            verify(resultSet, times(1)).getObject("testString", String.class);
            verifyNoMoreInteractions(resultSet);
        }

        public static class NoneConvertableClass {
        }

        @Test
        void negativeNoConvertorsFound() {
            Field field = mock(Field.class);
            doReturn(NoneConvertableClass.class).when(field).getType();
            ResultSet resultSet = mock(ResultSet.class);

            InternalServerException internalServerException = assertThrows(InternalServerException.class,
                () -> DatabaseFieldConvertor.convertField(field, resultSet),
                "Should throw InternalServerException");
            assertEquals("Unsupported class type: class uk.gov.hmcts.juror.job.execution.database"
                    + ".DatabaseFieldConvertorTest$ConvertField$NoneConvertableClass",
                internalServerException.getMessage(), "Message should be the same");
            verifyNoInteractions(resultSet);
        }
    }

    @Nested
    @DisplayName("static <T extends Enum<T>> T getEnumInstance(final Class<T> enumClass, final String value)")
    class GetEnumInstance {
        enum TestEnum {
            A, B, C, AB, BC, ABC;
        }

        @ParameterizedTest
        @EnumSource(TestEnum.class)
        void positiveEnumConvertsSuccessfully(TestEnum testEnum) {
            assertEquals(testEnum, DatabaseFieldConvertor.getEnumInstance(TestEnum.class, testEnum.name()),
                "Enum should be the same");
        }

        @Test
        void negativeEnumNotFoundThrowsException() {
            IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> DatabaseFieldConvertor.getEnumInstance(TestEnum.class, "D"),
                "Should throw IllegalArgumentException");
            assertEquals(
                "No enum constant uk.gov.hmcts.juror.job.execution.database.DatabaseFieldConvertorTest"
                    + ".GetEnumInstance.TestEnum.D",
                illegalArgumentException.getMessage(), "Message should be the same");
        }

        @Test
        void positiveNullString() {
            assertNull(DatabaseFieldConvertor.getEnumInstance(TestEnum.class, null),
                "Enum should be null");
        }
    }

}
