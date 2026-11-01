package id.beranda.launcher;

import android.content.Context;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.TimeZone;

/** Jadwal sholat untuk kota terpilih, dengan zona waktu perangkat. */
final class Schedule {

    /** Lima waktu sholat yang dihitung mundur. */
    static final int[] FARDHU = {PrayerTimes.SUBUH, PrayerTimes.DZUHUR, PrayerTimes.ASHAR,
            PrayerTimes.MAGHRIB, PrayerTimes.ISYA};

    static final class Next {
        final int index;     // indeks PrayerTimes
        final int minute;    // menit sejak tengah malam hari ini (bisa > 1440 = besok)
        final int remaining; // menit lagi
        final boolean tomorrow;

        Next(int index, int minute, int remaining, boolean tomorrow) {
            this.index = index;
            this.minute = minute;
            this.remaining = remaining;
            this.tomorrow = tomorrow;
        }

        String name() {
            return PrayerTimes.NAMES[index];
        }
    }

    private Schedule() {
    }

    static int[] times(Context c, LocalDate date) {
        Cities.City city = Cities.current(c);
        double tz = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0;
        return PrayerTimes.compute(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                city.lat, city.lon, tz);
    }

    static int nowMinute() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    static Next next(int[] today, int[] tomorrow, int now) {
        for (int idx : FARDHU) {
            if (today[idx] >= now) return new Next(idx, today[idx], today[idx] - now, false);
        }
        int m = tomorrow[PrayerTimes.SUBUH] + 1440;
        return new Next(PrayerTimes.SUBUH, m, m - now, true);
    }

    static String label(Next n) {
        if (n.remaining == 0) return "Waktunya " + n.name();
        return "Menuju " + n.name() + " " + PrayerTimes.hhmm(n.minute) + "  ·  " + Ui.duration(n.remaining) + " lagi";
    }
}
