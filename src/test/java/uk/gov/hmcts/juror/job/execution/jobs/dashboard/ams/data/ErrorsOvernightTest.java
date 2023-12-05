package uk.gov.hmcts.juror.job.execution.jobs.dashboard.ams.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.job.execution.jobs.Job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class ErrorsOvernightTest {

    private ErrorsOvernight errorsOvernight;
    private DashboardData dashboardData;

    @BeforeEach
    void beforeEach(){
        this.dashboardData = mock(DashboardData.class);
        this.errorsOvernight = new ErrorsOvernight(dashboardData);
    }

    @Test
    void positiveConstructorTest(){
        assertSame(dashboardData, errorsOvernight.dashboardData);

        assertEquals("Errors Overnight", errorsOvernight.title,
            "Expected title to be Errors Overnight");
        assertEquals(2, errorsOvernight.columCount,
            "Expected column count to be 2");
        assertEquals(2, errorsOvernight.rows.get(0).length);
        assertEquals("Server Name", errorsOvernight.rows.get(0)[0]);
        assertEquals("Status", errorsOvernight.rows.get(0)[1]);
    }
    @Test
    void positiveAddRowTest(){
        assertEquals(1, errorsOvernight.rows.size());
        errorsOvernight.addRow("exampleServerName", "exampleStatus");
        assertEquals(2, errorsOvernight.rows.size());
        assertEquals(2, errorsOvernight.rows.get(1).length);
        assertEquals("exampleServerName", errorsOvernight.rows.get(1)[0]);
        assertEquals("exampleStatus", errorsOvernight.rows.get(1)[1]);
        errorsOvernight.addRow("exampleServerName2", "exampleStatus2");
        assertEquals(3, errorsOvernight.rows.size());
        assertEquals(2, errorsOvernight.rows.get(2).length);
        assertEquals("exampleServerName2", errorsOvernight.rows.get(2)[0]);
        assertEquals("exampleStatus2", errorsOvernight.rows.get(2)[1]);
    }
    @Test
    void positivePopulateTest(){
        assertEquals(1, errorsOvernight.rows.size());
        assertEquals(Job.Result.passed(),errorsOvernight.populate());
        assertEquals(2, errorsOvernight.rows.size());
        assertEquals(2, errorsOvernight.rows.get(1).length);
        assertEquals("SSUPVL04", errorsOvernight.rows.get(1)[0]);
        assertEquals("None", errorsOvernight.rows.get(1)[1]);
    }
}
