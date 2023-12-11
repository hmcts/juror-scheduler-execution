package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class TimestampsTest {
    private Timestamps timestamps;
    private DashboardData dashboardData;

    @BeforeEach
    void beforeEach() {
        this.dashboardData = mock(DashboardData.class);
        this.timestamps = new Timestamps(dashboardData);
    }

    @Test
    void positiveConstructorTest() {
        assertSame(dashboardData, timestamps.dashboardData,
            "DashboardData should be the same");

        assertEquals("Timestamps", timestamps.title,
            "Expected title to be Timestamps");
        assertEquals(1, timestamps.rows.size(),
            "Expected row size to be 1");
        assertEquals(2, timestamps.columCount,
            "Expected column count to be 3");
        assertEquals(2, timestamps.rows.get(0).length,
            "Expected row size to be 2");
        assertEquals("Section", timestamps.rows.get(0)[0],
            "Expected row 0 column 0 to be Section");
        assertEquals("Last Update Date", timestamps.rows.get(0)[1],
            "Expected row 0 column 1 to be Last Update Date");
    }

    @Test
    void negativeAddRowLocalDateTimeNullLastChangedDateTest() {
        assertEquals(1, timestamps.rows.size(),
            "Expected row size to be 1");
        timestamps.addRow("testName", (LocalDateTime) null);
        assertEquals(2, timestamps.rows.size(),
            "Expected row size to be 2");
        assertEquals(2, timestamps.rows.get(1).length,
            "Expected row size to be 2");
        assertEquals("testName", timestamps.rows.get(1)[0],
            "Expected row 1 column 0 to be testName");
        assertEquals("ERROR", timestamps.rows.get(1)[1],
            "Expected row 1 column 1 to be ERROR");
    }

    @Test
    void positiveAddRowLocalDateTimeTest() {
        assertEquals(1, timestamps.rows.size(),
            "Expected row size to be 1");
        timestamps.addRow("testName2", LocalDateTime.of(2023, 11, 13, 11, 8, 34));
        assertEquals(2, timestamps.rows.size(),
            "Expected row size to be 2");
        assertEquals(2, timestamps.rows.get(1).length,
            "Expected row size to be 2");
        assertEquals("testName2", timestamps.rows.get(1)[0],
            "Expected row 1 column 0 to be testName2");
        assertEquals("2023-11-13 11:08:34", timestamps.rows.get(1)[1],
            "Expected row 1 column 1 to be 2023-11-13 11:08:34");

    }

    @Test
    void positiveAddRowStringTest() {
        assertEquals(1, timestamps.rows.size(),
            "Expected row size to be 1");
        timestamps.addRow("testName2", "String date");
        assertEquals(2, timestamps.rows.size(),
            "Expected row size to be 2");
        assertEquals(2, timestamps.rows.get(1).length,
            "Expected row size to be 2");
        assertEquals("testName2", timestamps.rows.get(1)[0],
            "Expected row 1 column 0 to be testName2");
        assertEquals("String date", timestamps.rows.get(1)[1],
            "Expected row 1 column 1 to be String date");

    }
}
