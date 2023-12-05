package uk.gov.hmcts.juror.job.execution.testsupport;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestUtil {

    private static final Random RANDOM;

    private TestUtil() {

    }

    static {
        RANDOM = new Random();
    }

    public static ResultMatcher jsonMatcher(JSONCompareMode mode, String expectedPayload) {
        return result -> {
            String actual = result.getResponse().getContentAsString();
            JSONAssert.assertEquals(
                "",
                expectedPayload,
                actual,
                mode
            );
        };
    }

    @SneakyThrows
    public static String getTestDataAsStringFromFile(String fileName) {
        File resource = getTestDataFile(fileName);
        return new String(Files.readAllBytes(resource.toPath()));
    }

    @SneakyThrows
    public static File getTestDataFile(String fileName) {
        return new ClassPathResource("testdata/" + fileName).getFile();
    }

    public static String replaceJsonPath(String json, String jsonPath, Object replacement) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.set(jsonPath, replacement);
        return parsed.jsonString();
    }

    public static String addJsonPath(String json, String jsonPath, String key, Object value) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.put(jsonPath, key, value);
        return parsed.jsonString();
    }

    public static String deleteJsonPath(String json, String jsonPath) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.delete(jsonPath);
        return parsed.jsonString();
    }


    public static void isUnmodifiable(Collection<?> collection) {
        assertThrows(UnsupportedOperationException.class,
            () -> collection.add(null));
    }


    public static <T> T getFieldValue(Class<T> fieldType, String fieldName, Object objectToGetFrom) {
        return getFieldValue(fieldType, objectToGetFrom.getClass(), fieldName, objectToGetFrom);
    }

    @SuppressWarnings("unchecked")
    public static <T, R> T getFieldValue(Class<T> fieldType, Class<R> classFieldIsWithin, String fieldName,
                                         Object objectToGetFrom) {
        try {
            Field field = classFieldIsWithin.getDeclaredField(fieldName);
            field.setAccessible(true);

            return (T) field.get(objectToGetFrom);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends Enum<?>> T getRandomEnumValue(Class<T> enumClass) {
        T[] values = enumClass.getEnumConstants();
        return values[RANDOM.nextInt(values.length)];
    }
}
