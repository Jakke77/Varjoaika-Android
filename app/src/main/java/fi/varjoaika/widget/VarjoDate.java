package fi.varjoaika.widget;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class VarjoDate {
    public static final String[] MONTHS = {"Varjojenkuu", "Hallankuu", "Routakuu", "Korppikuu", "Hylättyjen kuu", "Kalmistonkuu", "Ikiyön kuu", "Verikuu", "Pimentojen kuu", "Kuihtumisen kuu", "Aaveiden kuu", "Marraskuu", "Sielunkuu"};
    public static final String[] WEEKDAYS = {"Varjomaanantai", "Kallotiistai", "Ruumiskeskiviikko", "Kalmistotorstai", "Kryptaperjantai", "Noitalauantai", "Hornasunnuntai"};
    public static final String[] SHORT_VARJO_WEEKDAYS = {"Va", "Ka", "Ru", "Ka", "Kr", "No", "Ho"};
    private static final String[] OFFICIAL = {"tammikuuta", "helmikuuta", "maaliskuuta", "huhtikuuta", "toukokuuta", "kesäkuuta", "heinäkuuta", "elokuuta", "syyskuuta", "lokakuuta", "marraskuuta", "joulukuuta"};
    private static final String[] SHORT_WEEKDAYS = {"ma", "ti", "ke", "to", "pe", "la", "su"};
    private static final String[] OFFICIAL_MONTHS = {"Tammikuu", "Helmikuu", "Maaliskuu", "Huhtikuu", "Toukokuu", "Kesäkuu", "Heinäkuu", "Elokuu", "Syyskuu", "Lokakuu", "Marraskuu", "Joulukuu"};
    public final long year;
    public final int month, day, weekday;
    private VarjoDate(long offset) {
        year = offset / 390 + 1; month = (int)(offset % 390 / 30);
        day = (int)(offset % 30); weekday = (int)(offset % 7);
    }
    private static long midnight(int year, int month, int day) {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        c.clear(); c.set(year, month, day); return c.getTimeInMillis();
    }
    public static VarjoDate from(Calendar local) {
        long days = (midnight(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH)) - midnight(2026, 8, 28)) / 86400000L;
        return days < 0 ? null : new VarjoDate(days);
    }
    public static String official(Calendar now) {
        return now.get(Calendar.DAY_OF_MONTH) + ". " + OFFICIAL[now.get(Calendar.MONTH)] + " " + now.get(Calendar.YEAR);
    }
    /** Keep the Varjo weekday with the day; place the month and year below it. */
    public String widgetLabel() {
        return String.format(Locale.ROOT, "%02d. %s\n%s %04d", day,
            SHORT_VARJO_WEEKDAYS[weekday], MONTHS[month], year);
    }
    public static String officialWidgetLabel(Calendar now) {
        int weekday = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        return String.format(Locale.ROOT, "%02d. %s %s %04d", now.get(Calendar.DAY_OF_MONTH),
            SHORT_WEEKDAYS[weekday], OFFICIAL_MONTHS[now.get(Calendar.MONTH)], now.get(Calendar.YEAR));
    }
    public String dateLabel() { return WEEKDAYS[weekday] + " · Päivä " + day + "\n" + month + ". " + MONTHS[month]; }
    public String yearLabel() { return String.format(Locale.ROOT, "Varjoaika · Vuosi %04d", year); }
}

