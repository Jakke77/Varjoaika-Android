package fi.varjoaika.widget;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;
import java.util.TimeZone;

public class DatesTest {
    @Test public void fictionalAndCivilRoundTrip() {
        for(long offset:new long[]{0,6,29,30,389,390,10000,Dates.offset(7000,12,29)}) {
            Calendar civil=Dates.civil(offset);VarjoDate d=VarjoDate.from(civil);assertNotNull(d);
            assertEquals(offset,Dates.offset(d.year,d.month,d.day));
        }
    }
    @Test public void localHourAndDstGap() {
        TimeZone old=TimeZone.getDefault();try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Helsinki"));
            Calendar c=Calendar.getInstance();c.setTimeInMillis(Dates.due(7,13,45));
            assertEquals(13,c.get(Calendar.HOUR_OF_DAY));assertEquals(45,c.get(Calendar.MINUTE));
            Calendar gap=Calendar.getInstance(TimeZone.getTimeZone("UTC"));gap.clear();gap.set(2027,2,28);
            long offset=(gap.getTimeInMillis()-Dates.ANCHOR)/86400000;
            try { Dates.due(offset,3,30);fail("Nonexistent DST time accepted"); }catch(IllegalArgumentException expected){}
        }finally{TimeZone.setDefault(old);}
    }
}
