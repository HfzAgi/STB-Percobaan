package id.beranda.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Daftar aplikasi yang bisa dibuka + gambar banner/ikon. */
final class Apps {

    static final class App {
        final ComponentName component;
        final String label;
        final boolean tv;

        App(ComponentName component, String label, boolean tv) {
            this.component = component;
            this.label = label;
            this.tv = tv;
        }

        String key() {
            return component.flattenToString();
        }

        String pkg() {
            return component.getPackageName();
        }
    }

    /** Naik setiap ada aplikasi dipasang/dihapus/diubah. */
    static volatile int changeCount;

    private static List<App> cache;
    private static int cacheCount = -1;

    /** Aplikasi populer yang dijadikan favorit awal bila terpasang. */
    private static final String[] PREFERRED = {
            "com.google.android.youtube.tv",
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.smarttubetv",
            "com.teamsmart.videomanager.tv",
            "com.netflix.ninja",
            "com.vidio.android.tv",
            "com.spotify.tv.android",
            "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom",
            "com.google.android.youtube.tvmusic",
            "com.android.vending",
    };

    private static final String[] SKIP_DEFAULT = {
            "com.android.tv.settings", "com.android.settings", "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
    };

    private Apps() {
    }

    static synchronized List<App> all(Context c) {
        if (cache != null && cacheCount == changeCount) return cache;
        PackageManager pm = c.getPackageManager();
        Map<String, App> map = new LinkedHashMap<>();
        String self = c.getPackageName();

        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(tv, 0)) add(pm, map, ri, true, self);
        Intent phone = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(phone, 0)) add(pm, map, ri, false, self);

        List<App> list = new ArrayList<>(map.values());
        Collator col = Collator.getInstance(new Locale("id", "ID"));
        Collections.sort(list, (a, b) -> col.compare(a.label, b.label));
        cache = Collections.unmodifiableList(list);
        cacheCount = changeCount;
        return cache;
    }

    private static void add(PackageManager pm, Map<String, App> map, ResolveInfo ri, boolean tv, String self) {
        if (ri.activityInfo == null) return;
        String pkg = ri.activityInfo.packageName;
        if (pkg.equals(self) || map.containsKey(pkg)) return;
        CharSequence label = ri.loadLabel(pm);
        ComponentName cn = new ComponentName(pkg, ri.activityInfo.name);
        map.put(pkg, new App(cn, label == null ? pkg : label.toString(), tv));
    }

    /** Cari aplikasi untuk kunci favorit; cocokkan nama paket bila nama activity berubah. */
    static App find(List<App> list, String key) {
        ComponentName want = ComponentName.unflattenFromString(key);
        if (want == null) return null;
        App byPkg = null;
        for (App a : list) {
            if (a.component.equals(want)) return a;
            if (byPkg == null && a.pkg().equals(want.getPackageName())) byPkg = a;
        }
        return byPkg;
    }

    static int indexIn(List<String> keys, App app) {
        for (int i = 0; i < keys.size(); i++) {
            ComponentName cn = ComponentName.unflattenFromString(keys.get(i));
            if (cn != null && cn.getPackageName().equals(app.pkg())) return i;
        }
        return -1;
    }

    static List<String> defaultFavorites(List<App> all) {
        List<String> out = new ArrayList<>();
        for (String p : PREFERRED) {
            for (App a : all) {
                if (a.pkg().equals(p)) {
                    out.add(a.key());
                    break;
                }
            }
            if (out.size() >= 8) return out;
        }
        for (App a : all) {
            if (out.size() >= 8) break;
            if (!a.tv || isSkipped(a.pkg()) || out.contains(a.key())) continue;
            out.add(a.key());
        }
        return out;
    }

    private static boolean isSkipped(String pkg) {
        for (String s : SKIP_DEFAULT) if (s.equals(pkg)) return true;
        return false;
    }

    static Drawable banner(Context c, App a) {
        if (!a.tv) return null;
        try {
            PackageManager pm = c.getPackageManager();
            ActivityInfo ai = pm.getActivityInfo(a.component, 0);
            Drawable d = ai.loadBanner(pm);
            if (d == null) d = pm.getApplicationBanner(a.pkg());
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    static Drawable icon(Context c, App a) {
        try {
            return c.getPackageManager().getActivityIcon(a.component);
        } catch (Exception e) {
            return null;
        }
    }

    static void launch(Context c, App a) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(a.tv ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER);
        i.setComponent(a.component);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            c.startActivity(i);
        } catch (Exception e) {
            Intent fallback = c.getPackageManager().getLeanbackLaunchIntentForPackage(a.pkg());
            if (fallback == null) fallback = c.getPackageManager().getLaunchIntentForPackage(a.pkg());
            if (fallback != null) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    c.startActivity(fallback);
                    return;
                } catch (Exception ignored) {
                }
            }
            Toast.makeText(c, "Aplikasi tidak bisa dibuka", Toast.LENGTH_SHORT).show();
        }
    }
}
