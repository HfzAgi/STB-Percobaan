package id.beranda.launcher;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

/** Tanggal Hijriah (Umm al-Qura) + hitung mundur Ramadan, Idul Fitri, Idul Adha. */
final class Hijri {

    static final String[] MONTHS = {
            "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir", "Jumadil Awal", "Jumadil Akhir",
            "Rajab", "Syakban", "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"
    };

    /** {tahun, bulan(1-12), hari} */
    interface Engine {
        int[] toHijri(LocalDate date);
        LocalDate toGregorian(int year, int month, int day);
    }

    static final class Event {
        final String name;
        final LocalDate date;
        final long days;

        Event(String name, LocalDate date, long days) {
            this.name = name;
            this.date = date;
            this.days = days;
        }
    }

    private static Engine engine;

    static synchronized Engine engine() {
        if (engine == null) {
            Engine e = new JavaTimeEngine();
            try {
                e.toHijri(LocalDate.of(2026, 11, 1));
            } catch (Throwable t) {
                e = HijriIcu.create();
            }
            engine = e;
        }
        return engine;
    }

    static String format(LocalDate date, int adjustDays) {
        int[] h = engine().toHijri(date.plusDays(adjustDays));
        return h[2] + " " + MONTHS[h[1] - 1] + " " + h[0] + " H";
    }

    static Event nextEvent(LocalDate today, int adjustDays) {
        Engine e = engine();
        int year = e.toHijri(today.plusDays(adjustDays))[0];
        int[][] events = {{9, 1}, {10, 1}, {12, 10}};
        String[] names = {"Ramadan", "Idul Fitri", "Idul Adha"};
        Event best = null;
        for (int y = year; y <= year + 1; y++) {
            for (int i = 0; i < events.length; i++) {
                LocalDate g = e.toGregorian(y, events[i][0], events[i][1]).minusDays(adjustDays);
                if (g.isBefore(today)) continue;
                long days = ChronoUnit.DAYS.between(today, g);
                if (best == null || days < best.days) best = new Event(names[i], g, days);
            }
        }
        return best;
    }

    private static final class JavaTimeEngine implements Engine {
        @Override
        public int[] toHijri(LocalDate date) {
            HijrahDate h = HijrahDate.from(date);
            return new int[]{h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH)};
        }

        @Override
        public LocalDate toGregorian(int year, int month, int day) {
            return LocalDate.from(HijrahDate.of(year, month, day));
        }
    }
}
