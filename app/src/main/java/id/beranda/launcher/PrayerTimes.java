package id.beranda.launcher;

/**
 * Hitung jadwal sholat secara lokal (tanpa internet), parameter Kemenag RI:
 * Subuh 20 derajat, Isya 18 derajat, Ashar mazhab Syafi'i, ihtiyat 2 menit,
 * Imsak = Subuh - 10 menit. Algoritma dasar dari PrayTimes.org.
 */
final class PrayerTimes {

    static final int IMSAK = 0, SUBUH = 1, TERBIT = 2, DZUHUR = 3, ASHAR = 4, MAGHRIB = 5, ISYA = 6;
    static final String[] NAMES = {"Imsak", "Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    private static final double FAJR_ANGLE = 20.0;
    private static final double ISHA_ANGLE = 18.0;
    private static final double RISE_SET_ANGLE = 1.0; // dikalibrasi dengan jadwal Kemenag
    private static final double IHTIYAT_MIN = 2.0;

    private final double lat;
    private final double lon;
    private double jDate;

    private PrayerTimes(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * @return menit sejak tengah malam (waktu lokal sesuai tzHours) untuk
     *         Imsak, Subuh, Terbit, Dzuhur, Ashar, Maghrib, Isya.
     */
    static int[] compute(int year, int month, int day, double lat, double lon, double tzHours) {
        return new PrayerTimes(lat, lon).run(year, month, day, tzHours);
    }

    private int[] run(int year, int month, int day, double tz) {
        jDate = julian(year, month, day) - lon / (15.0 * 24.0);

        // Tebakan awal (jam), lalu diperhalus dua kali.
        double fajr = 5, sunrise = 6, dhuhr = 12, asr = 13, sunset = 18, isha = 18;
        for (int i = 0; i < 2; i++) {
            fajr = sunAngleTime(FAJR_ANGLE, fajr / 24.0, true);
            sunrise = sunAngleTime(RISE_SET_ANGLE, sunrise / 24.0, true);
            dhuhr = midDay(dhuhr / 24.0);
            asr = asrTime(1.0, asr / 24.0);
            sunset = sunAngleTime(RISE_SET_ANGLE, sunset / 24.0, false);
            isha = sunAngleTime(ISHA_ANGLE, isha / 24.0, false);
        }

        double shift = tz - lon / 15.0;
        double[] h = new double[7];
        h[SUBUH] = fajr + shift;
        h[TERBIT] = sunrise + shift;
        h[DZUHUR] = dhuhr + shift;
        h[ASHAR] = asr + shift;
        h[MAGHRIB] = sunset + shift;
        h[ISYA] = isha + shift;

        int[] out = new int[7];
        for (int i = 1; i < 7; i++) {
            double minutes = h[i] * 60.0;
            if (i == TERBIT) {
                // Terbit dikurangi ihtiyat (batas akhir Subuh), dibulatkan ke bawah.
                out[i] = (int) Math.floor(minutes - IHTIYAT_MIN);
            } else if (i == DZUHUR) {
                // Kemenag menambah 1 menit lagi untuk Dzuhur (matahari benar-benar tergelincir).
                out[i] = (int) Math.ceil(minutes + IHTIYAT_MIN + 1.0);
            } else {
                out[i] = (int) Math.ceil(minutes + IHTIYAT_MIN);
            }
        }
        out[IMSAK] = out[SUBUH] - 10;
        return out;
    }

    // ---- Astronomi ----

    private double midDay(double time) {
        double eqt = sunPosition(jDate + time)[1];
        return fixHour(12 - eqt);
    }

    private double sunAngleTime(double angle, double time, boolean beforeNoon) {
        double decl = sunPosition(jDate + time)[0];
        double noon = midDay(time);
        double cos = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
        cos = Math.max(-1.0, Math.min(1.0, cos));
        double t = darccos(cos) / 15.0;
        return noon + (beforeNoon ? -t : t);
    }

    private double asrTime(double factor, double time) {
        double decl = sunPosition(jDate + time)[0];
        double angle = -darccot(factor + dtan(Math.abs(lat - decl)));
        return sunAngleTime(angle, time, false);
    }

    /** @return {deklinasi, persamaan waktu} */
    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0;
        double eqt = q / 15.0 - fixHour(ra);
        double decl = darcsin(dsin(e) * dsin(l));
        return new double[]{decl, eqt};
    }

    private static double julian(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double dtan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double darcsin(double x) { return Math.toDegrees(Math.asin(x)); }
    private static double darccos(double x) { return Math.toDegrees(Math.acos(x)); }
    private static double darccot(double x) { return Math.toDegrees(Math.atan(1.0 / x)); }
    private static double darctan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    private static double fixAngle(double a) { return a - 360.0 * Math.floor(a / 360.0); }
    private static double fixHour(double a) { return a - 24.0 * Math.floor(a / 24.0); }

    static String hhmm(int minutes) {
        minutes = ((minutes % 1440) + 1440) % 1440;
        int h = minutes / 60, m = minutes % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }
}
