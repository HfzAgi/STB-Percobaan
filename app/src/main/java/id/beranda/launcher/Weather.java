package id.beranda.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** Cuaca dari Open-Meteo (gratis, tanpa API key). Diambil paling sering tiap 30 menit. */
final class Weather {

    static final long REFRESH_MS = 30 * 60 * 1000L;
    static final long RETRY_MS = 5 * 60 * 1000L;

    static final class Data {
        double temp;
        int code;
        boolean isDay;
        double max = Double.NaN;
        double min = Double.NaN;
        int rainProb = -1;
    }

    private static volatile boolean fetching;
    private static volatile long lastAttempt;

    private Weather() {
    }

    static boolean needsRefresh(Context c) {
        SharedPreferences p = Prefs.get(c);
        String city = Cities.current(c).name;
        long now = System.currentTimeMillis();
        boolean stale = now - p.getLong(Prefs.WEATHER_TIME, 0) > REFRESH_MS
                || !city.equals(p.getString(Prefs.WEATHER_CITY, ""));
        return stale && !fetching && now - lastAttempt > RETRY_MS;
    }

    static void forceNextRefresh() {
        lastAttempt = 0;
    }

    /** Jalankan di thread latar. @return true bila berhasil. */
    static boolean fetch(Context c) {
        if (fetching) return false;
        fetching = true;
        lastAttempt = System.currentTimeMillis();
        HttpURLConnection con = null;
        try {
            Cities.City city = Cities.current(c);
            String u = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code,is_day"
                            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                            + "&timezone=auto&forecast_days=1",
                    city.lat, city.lon);
            con = (HttpURLConnection) new URL(u).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent", "Beranda-Launcher");
            if (con.getResponseCode() != 200) return false;
            String body = read(con.getInputStream());
            parse(body); // validasi dulu sebelum disimpan
            Prefs.get(c).edit()
                    .putString(Prefs.WEATHER_JSON, body)
                    .putLong(Prefs.WEATHER_TIME, System.currentTimeMillis())
                    .putString(Prefs.WEATHER_CITY, city.name)
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) con.disconnect();
            fetching = false;
        }
    }

    /** Data tersimpan untuk kota yang sedang dipilih, atau null. */
    static Data cached(Context c) {
        SharedPreferences p = Prefs.get(c);
        if (!Cities.current(c).name.equals(p.getString(Prefs.WEATHER_CITY, ""))) return null;
        // Data lebih dari 6 jam dianggap basi.
        if (System.currentTimeMillis() - p.getLong(Prefs.WEATHER_TIME, 0) > 6 * 60 * 60 * 1000L) return null;
        String json = p.getString(Prefs.WEATHER_JSON, null);
        if (json == null) return null;
        try {
            return parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    static Data parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        JSONObject cur = o.getJSONObject("current");
        Data d = new Data();
        d.temp = cur.getDouble("temperature_2m");
        d.code = cur.getInt("weather_code");
        d.isDay = cur.optInt("is_day", 1) == 1;
        JSONObject daily = o.optJSONObject("daily");
        if (daily != null) {
            d.max = first(daily.optJSONArray("temperature_2m_max"));
            d.min = first(daily.optJSONArray("temperature_2m_min"));
            double rp = first(daily.optJSONArray("precipitation_probability_max"));
            d.rainProb = Double.isNaN(rp) ? -1 : (int) Math.round(rp);
        }
        return d;
    }

    private static double first(JSONArray a) {
        if (a == null || a.length() == 0 || a.isNull(0)) return Double.NaN;
        return a.optDouble(0, Double.NaN);
    }

    private static String read(InputStream in) throws Exception {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    static String describe(int code) {
        if (code == 0) return "Cerah";
        if (code == 1) return "Cerah berawan";
        if (code == 2) return "Berawan";
        if (code == 3) return "Mendung";
        if (code == 45 || code == 48) return "Berkabut";
        if (code >= 51 && code <= 57) return "Gerimis";
        if (code == 61 || code == 80) return "Hujan ringan";
        if (code == 63 || code == 81) return "Hujan sedang";
        if (code == 65 || code == 82 || code == 66 || code == 67) return "Hujan lebat";
        if (code >= 95) return "Hujan petir";
        return "Berawan";
    }

    static String icon(int code, boolean day) {
        if (code == 0) return day ? "☀️" : "🌙";        // ☀️ / 🌙
        if (code == 1 || code == 2) return day ? "⛅" : "☁️"; // ⛅ / ☁️
        if (code == 3) return "☁️";                               // ☁️
        if (code == 45 || code == 48) return "🌫️";          // 🌫️
        if (code >= 95) return "⛈️";                              // ⛈️
        if (code >= 51) return "🌧️";                        // 🌧️
        return "☁️";
    }

    /** 0 = cerah, 1 = mendung/berkabut, 2 = hujan */
    static int gloom(Data d) {
        if (d == null) return 0;
        if (d.code >= 51) return 2;
        if (d.code >= 3) return 1;
        return 0;
    }
}
