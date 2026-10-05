package fi.varjoaika.widget;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Locale;

public final class Dates {
    private static final long DAY = 86400000L;
    public static final long ANCHOR = utc(2026, 8, 28);
    private static long utc(int year, int month, int day) {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        c.clear(); c.set(year,month,day); return c.getTimeInMillis();
    }
    public static long today() {
        Calendar c = Calendar.getInstance();
        return (utc(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH))-ANCHOR)/DAY;
    }
    public static long offset(long year,int month,int day) {
        if(year<1 || year>7000 || month<0 || month>12 || day<0 || day>29) throw new IllegalArgumentException("Päivämäärä ei kelpaa");
        return (year-1)*390+month*30L+day;
    }
    public static Calendar civil(long offset) {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"),Locale.ROOT);
        c.setTimeInMillis(ANCHOR+offset*DAY);return c;
    }
    public static long due(long offset,int hour,int minute) {
        Calendar civil=civil(offset), c=Calendar.getInstance();
        c.clear();c.setLenient(false);
        c.set(civil.get(Calendar.YEAR),civil.get(Calendar.MONTH),civil.get(Calendar.DAY_OF_MONTH),hour,minute,0);
        return c.getTimeInMillis();
    }
    public static String label(long offset) {
        if(offset<0)return "Ennen Varjoajan alkua";
        return VarjoDate.WEEKDAYS[(int)(offset%7)]+" · "+offset%30+". "+VarjoDate.MONTHS[(int)(offset%390/30)]+" · "+(offset/390+1);
    }
    private Dates() {}
}
