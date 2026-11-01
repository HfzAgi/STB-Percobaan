package id.beranda.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class Prefs {

    static final String CITY = "city";
    static final String FAVORITES = "favorites";
    static final String SHOW_WEATHER = "show_weather";
    static final String SHOW_PRAYER = "show_prayer";
    static final String SHOW_MEDIA = "show_media";
    static final String HIJRI_ADJ = "hijri_adj";
    static final String CLOCK_24H = "clock_24h";
    static final String WEATHER_JSON = "weather_json";
    static final String WEATHER_TIME = "weather_time";
    static final String WEATHER_CITY = "weather_city";

    /** Naik setiap ada perubahan pengaturan, supaya layar utama tahu harus menggambar ulang. */
    static final String VERSION = "version";

    private Prefs() {
    }

    static SharedPreferences get(Context c) {
        return c.getApplicationContext().getSharedPreferences("beranda", Context.MODE_PRIVATE);
    }

    static boolean bool(Context c, String key, boolean def) {
        return get(c).getBoolean(key, def);
    }

    static void setBool(Context c, String key, boolean value) {
        get(c).edit().putBoolean(key, value).apply();
        bump(c);
    }

    static void bump(Context c) {
        SharedPreferences p = get(c);
        p.edit().putInt(VERSION, p.getInt(VERSION, 0) + 1).apply();
    }

    static int version(Context c) {
        return get(c).getInt(VERSION, 0);
    }

    static boolean hasFavorites(Context c) {
        return get(c).contains(FAVORITES);
    }

    /** Daftar komponen favorit, format "paket/kelas". */
    static List<String> favorites(Context c) {
        List<String> out = new ArrayList<>();
        String raw = get(c).getString(FAVORITES, "");
        for (String s : raw.split("\n")) {
            if (!s.trim().isEmpty()) out.add(s.trim());
        }
        return out;
    }

    static void setFavorites(Context c, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append(s).append('\n');
        get(c).edit().putString(FAVORITES, sb.toString()).apply();
        bump(c);
    }
}
