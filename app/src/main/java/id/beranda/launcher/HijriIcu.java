package id.beranda.launcher;

import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.IslamicCalendar;
import android.icu.util.TimeZone;

import java.time.LocalDate;

/** Cadangan bila java.time.HijrahDate tidak tersedia di perangkat. */
final class HijriIcu implements Hijri.Engine {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    static Hijri.Engine create() {
        return new HijriIcu();
    }

    private IslamicCalendar newIslamic() {
        IslamicCalendar c = new IslamicCalendar(UTC);
        c.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        return c;
    }

    @Override
    public int[] toHijri(LocalDate date) {
        GregorianCalendar g = new GregorianCalendar(UTC);
        g.clear();
        g.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(), 12, 0, 0);
        IslamicCalendar c = newIslamic();
        c.setTimeInMillis(g.getTimeInMillis());
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)};
    }

    @Override
    public LocalDate toGregorian(int year, int month, int day) {
        IslamicCalendar c = newIslamic();
        c.clear();
        c.set(year, month - 1, day, 12, 0, 0);
        GregorianCalendar g = new GregorianCalendar(UTC);
        g.setTimeInMillis(c.getTimeInMillis());
        return LocalDate.of(g.get(Calendar.YEAR), g.get(Calendar.MONTH) + 1, g.get(Calendar.DAY_OF_MONTH));
    }
}
