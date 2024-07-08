package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.jobs.dashboard.DashboardDataEntry;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DashboardDataEntryTest {


    private DashboardData dashboardData;

    @BeforeEach
    void beforeEach() {
        this.dashboardData = mock(DashboardData.class);
    }

    @Test
    void positiveConstructorTest() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");

        assertSame(dashboardData, dashboardDataEntry.dashboardData, "DashboardData should be the same");
        assertSame("SomeTitle", dashboardDataEntry.title, "Title should be the same");
        assertSame(1, dashboardDataEntry.rows.size(), "Rows size should be 1");
        assertSame(3, dashboardDataEntry.rows.get(0).length, "Row size should be 3");
        assertSame(3, dashboardDataEntry.columCount, "Column count should be 3");
        assertEquals("RowName1", dashboardDataEntry.rows.get(0)[0], "RowName1 should be the same");
        assertEquals("SomeOtherRowName", dashboardDataEntry.rows.get(0)[1], "RowName1 should be the same");
        assertEquals("OneFinalRowName", dashboardDataEntry.rows.get(0)[2], "RowName1 should be the same");
    }


    @Test
    void positiveAddEntryTest() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");
        dashboardDataEntry.addEntry("value1", "value2", "value3");

        assertSame(2, dashboardDataEntry.rows.size(), "Rows size should be 2");
        assertSame(3, dashboardDataEntry.rows.get(1).length, "Row size should be 3");
        assertEquals("value1", dashboardDataEntry.rows.get(1)[0], "RowName1 should be the same");
        assertEquals("value2", dashboardDataEntry.rows.get(1)[1], "RowName1 should be the same");
        assertEquals("value3", dashboardDataEntry.rows.get(1)[2], "RowName1 should be the same");
    }

    @Test
    void negativeAddEntryTooFewTest() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");

        InternalServerException internalServerException = assertThrows(InternalServerException.class,
            () -> dashboardDataEntry.addEntry("value1", "value2"),
            "Exception should be throw if Number of values does not match number of columns");
        assertEquals("Number of values must match number of columns", internalServerException.getMessage(),
            "Exception message should be the same");
    }

    @Test
    void negativeAddEntryTooManyTest() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");

        InternalServerException internalServerException = assertThrows(InternalServerException.class,
            () -> dashboardDataEntry.addEntry("value1", "value2", "value3", "value4"),
            "Exception should be throw if Number of values does not match number of columns");
        assertEquals("Number of values must match number of columns", internalServerException.getMessage(),
            "Exception message should be the same");
    }


    @Test
    void negativeGetLatestDateBothNull() {
        assertNull(DashboardDataEntry.getLatestDate(null, null),
            "Should return null if both dates are null");
    }

    @Test
    void positiveGetLatestDateFirstOneNull() {
        LocalDateTime date2 = LocalDateTime.now();

        assertEquals(date2, DashboardDataEntry.getLatestDate(null, date2),
            "Should return date2 if date1 is null");
    }

    @Test
    void positiveGetLatestDateSecondOneNull() {
        LocalDateTime date1 = LocalDateTime.now();
        assertEquals(date1, DashboardDataEntry.getLatestDate(date1, null),
            "Should return date1 if date2 is null");

    }

    @Test
    void positiveGetLatestDateFirstAfterSecond() {
        LocalDateTime date2 = LocalDateTime.now();
        LocalDateTime date1 = date2.plusSeconds(1);
        assertEquals(date1, DashboardDataEntry.getLatestDate(date1, date2),
            "Should return date1 if date1 is after date2");
    }

    @Test
    void positiveGetLatestDateFirstBeforeSecond() {
        LocalDateTime date2 = LocalDateTime.now();
        LocalDateTime date1 = date2.minusSeconds(1);
        assertEquals(date2, DashboardDataEntry.getLatestDate(date1, date2),
            "Should return date2 if date1 is before date2");
    }

    @Test
    void positivePopulateTimestampLocalDateTime() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");
        Timestamps timestamps = mock(Timestamps.class);
        when(dashboardData.getTimestamps()).thenReturn(timestamps);

        LocalDateTime localDateTime = LocalDateTime.now();
        dashboardDataEntry.populateTimestamp(dashboardData, "sectionTitle", localDateTime);

        verify(timestamps, times(1))
            .addRow("sectionTitle", localDateTime);
        verifyNoMoreInteractions(timestamps);
    }

    @Test
    void positivePopulateTimestampString() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");
        Timestamps timestamps = mock(Timestamps.class);
        when(dashboardData.getTimestamps()).thenReturn(timestamps);

        dashboardDataEntry.populateTimestamp(dashboardData, "sectionTitle", "SomeStringValue");

        verify(timestamps, times(1))
            .addRow("sectionTitle", "SomeStringValue");
        verifyNoMoreInteractions(timestamps);
    }

    @Test
    void positiveToCsv() {
        DashboardDataEntry dashboardDataEntry = new DashboardDataEntry(dashboardData,
            "SomeTitle", "RowName1", "SomeOtherRowName", "OneFinalRowName");

        assertEquals("[SomeTitle]\n"
            + "RowName1,SomeOtherRowName,OneFinalRowName", dashboardDataEntry.toCsv().toString(), "tbc");

        dashboardDataEntry.addEntry("value1", "value2", "value3");
        assertEquals("[SomeTitle]\n"
            + "RowName1,SomeOtherRowName,OneFinalRowName\n"
            + "value1,value2,value3", dashboardDataEntry.toCsv().toString(), "tbc");

        dashboardDataEntry.addEntry("new value", "other new value", "yet a third new value");
        assertEquals("[SomeTitle]\n"
            + "RowName1,SomeOtherRowName,OneFinalRowName\n"
            + "value1,value2,value3\n"
            + "new value,other new value,yet a third new value", dashboardDataEntry.toCsv().toString(), "tbc");
    }
}
