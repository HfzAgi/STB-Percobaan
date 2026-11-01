package id.beranda.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Supplier;

/** Pengaturan launcher. */
public class SettingsActivity extends Activity {

    private static final int REQ_HOME = 7;

    private static final class Row {
        final TextView value;
        final Supplier<String> supplier;

        Row(TextView value, Supplier<String> supplier) {
            this.value = value;
            this.supplier = supplier;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        Weather.Data w = Prefs.bool(this, Prefs.SHOW_WEATHER, true) ? Weather.cached(this) : null;
        root.setBackground(Ui.sky(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Weather.gloom(w)));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int side = Math.max(dp(48), (getResources().getDisplayMetrics().widthPixels - dp(720)) / 2);
        list.setPadding(side, dp(28), side, dp(36));
        list.setClipChildren(false);
        list.setClipToPadding(false);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.text(this, 26, 1f, Ui.LIGHT);
        title.setText("Pengaturan");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = dp(18);
        list.addView(title, tlp);

        section("Tampilan");
        row("Kota (cuaca & jadwal sholat)", () -> Cities.current(this).name, this::chooseCity);
        row("Format jam", () -> Prefs.bool(this, Prefs.CLOCK_24H, true) ? "24 jam" : "12 jam",
                () -> toggle(Prefs.CLOCK_24H));
        row("Tampilkan cuaca", () -> yesNo(Prefs.SHOW_WEATHER), () -> toggle(Prefs.SHOW_WEATHER));
        row("Tampilkan jadwal sholat & Hijriah", () -> yesNo(Prefs.SHOW_PRAYER), () -> toggle(Prefs.SHOW_PRAYER));
        row("Koreksi tanggal Hijriah", this::hijriAdjText, this::cycleHijriAdj);
        row("Tampilkan widget musik", () -> yesNo(Prefs.SHOW_MEDIA), () -> toggle(Prefs.SHOW_MEDIA));

        section("Sistem");
        row("Izin widget musik", () -> MediaCard.hasAccess(this) ? "Sudah diizinkan" : "Belum diizinkan",
                this::grantMediaAccess);
        row("Jadikan launcher utama", () -> isDefaultHome() ? "Sudah" : "Belum", this::makeDefaultHome);
        row("Screensaver jam", this::screensaverText, this::setupScreensaver);
        row("Pengaturan Android", () -> "", () -> open(new Intent(Settings.ACTION_SETTINGS)));
        row("Tentang", () -> "Beranda " + versionName(), () -> Toast.makeText(this,
                "Cuaca: Open-Meteo  ·  Jadwal sholat: perhitungan kriteria Kemenag RI",
                Toast.LENGTH_LONG).show());

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_HOME) {
            refresh();
            if (!isDefaultHome()) offerRootHome();
        }
    }

    // ---------------- Baris ----------------

    private void section(String name) {
        TextView t = Ui.text(this, 13, 0.6f, Ui.MEDIUM);
        t.setText(name.toUpperCase());
        t.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(8);
        lp.leftMargin = dp(4);
        list.addView(t, lp);
    }

    private void row(String title, Supplier<String> value, Runnable action) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        float radius = dp(14);
        r.setBackground(Ui.rounded(Ui.GLASS, radius));
        r.setPadding(dp(20), dp(15), dp(20), dp(15));
        r.setClickable(true);
        Ui.focusFx(r, radius, 1.03f);
        r.setOnClickListener(v -> {
            action.run();
            refresh();
        });

        TextView t = Ui.text(this, 17, 1f, Ui.REGULAR);
        t.setText(title);
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.text(this, 15, 0.7f, Ui.REGULAR);
        v.setGravity(Gravity.END);
        r.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        list.addView(r, lp);
        rows.add(new Row(v, value));
        if (rows.size() == 1) r.post(r::requestFocus);
    }

    private void refresh() {
        for (Row r : rows) {
            String s;
            try {
                s = r.supplier.get();
            } catch (Exception e) {
                s = "";
            }
            r.value.setText(s);
        }
    }

    private String yesNo(String key) {
        return Prefs.bool(this, key, true) ? "Ya" : "Tidak";
    }

    private void toggle(String key) {
        Prefs.setBool(this, key, !Prefs.bool(this, key, true));
    }

    // ---------------- Aksi ----------------

    private void chooseCity() {
        String[] names = Cities.names();
        int current = Cities.indexOf(Cities.current(this).name);
        new AlertDialog.Builder(this, Ui.DIALOG_THEME)
                .setTitle("Pilih kota")
                .setSingleChoiceItems(names, current, (d, which) -> {
                    Prefs.get(this).edit().putString(Prefs.CITY, names[which]).apply();
                    Prefs.bump(this);
                    Weather.forceNextRefresh();
                    d.dismiss();
                    refresh();
                })
                .show();
    }

    private String hijriAdjText() {
        int a = Prefs.get(this).getInt(Prefs.HIJRI_ADJ, 0);
        return a == 0 ? "Tidak ada" : (a > 0 ? "+" + a : String.valueOf(a)) + " hari";
    }

    private void cycleHijriAdj() {
        int a = Prefs.get(this).getInt(Prefs.HIJRI_ADJ, 0);
        int next = a == 0 ? 1 : a == 1 ? 2 : a == 2 ? -2 : a == -2 ? -1 : 0;
        Prefs.get(this).edit().putInt(Prefs.HIJRI_ADJ, next).apply();
        Prefs.bump(this);
    }

    private void grantMediaAccess() {
        if (MediaCard.hasAccess(this)) {
            Toast.makeText(this, "Izin sudah aktif", Toast.LENGTH_SHORT).show();
            return;
        }
        String comp = getPackageName() + "/" + MediaListener.class.getName();
        new AlertDialog.Builder(this, Ui.DIALOG_THEME)
                .setTitle("Izin widget musik")
                .setMessage("Supaya launcher bisa menampilkan dan mengontrol lagu yang sedang diputar.")
                .setPositiveButton("Otomatis (root)", (d, w) ->
                        runRoot("cmd notification allow_listener " + comp,
                                "Izin widget musik aktif", "Gagal. Coba lewat Pengaturan Android."))
                .setNegativeButton("Pengaturan Android", (d, w) ->
                        open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                .show();
    }

    private boolean isDefaultHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return rm.isRoleHeld(RoleManager.ROLE_HOME);
            }
        }
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
    }

    private void makeDefaultHome() {
        if (isDefaultHome()) {
            Toast.makeText(this, "Beranda sudah jadi launcher utama", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_HOME);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
        offerRootHome();
    }

    private void offerRootHome() {
        String pkg = getPackageName();
        String act = pkg + "/" + MainActivity.class.getName();
        new AlertDialog.Builder(this, Ui.DIALOG_THEME)
                .setTitle("Jadikan launcher utama")
                .setMessage("Pakai akses root untuk menjadikan Beranda launcher utama? "
                        + "Setujui permintaan root yang muncul di layar.")
                .setPositiveButton("Ya, pakai root", (d, w) ->
                        runRoot("cmd package set-home-activity " + act
                                        + " || cmd role add-role-holder android.app.role.HOME " + pkg,
                                "Berhasil. Tekan tombol Home di remote untuk mencoba.",
                                "Gagal. Pastikan izin root diberikan."))
                .setNegativeButton("Batal", null)
                .show();
    }

    private String screensaverText() {
        try {
            String s = Settings.Secure.getString(getContentResolver(), "screensaver_components");
            if (s != null && s.contains("ClockDream")) return "Aktif";
            return "Belum dipasang";
        } catch (Exception e) {
            return "Atur";
        }
    }

    private void setupScreensaver() {
        String comp = getPackageName() + "/" + ClockDream.class.getName();
        new AlertDialog.Builder(this, Ui.DIALOG_THEME)
                .setTitle("Screensaver jam")
                .setMessage("Jam besar yang tampil saat STB tidak dipakai beberapa menit.")
                .setPositiveButton("Pasang otomatis (root)", (d, w) ->
                        runRoot("settings put secure screensaver_components " + comp
                                        + " && settings put secure screensaver_enabled 1",
                                "Screensaver jam aktif", "Gagal. Coba lewat Pengaturan Android."))
                .setNegativeButton("Pengaturan Android", (d, w) ->
                        open(new Intent(Settings.ACTION_DREAM_SETTINGS)))
                .show();
    }

    private void runRoot(String cmd, String ok, String fail) {
        Toast.makeText(this, "Meminta akses root…", Toast.LENGTH_SHORT).show();
        Bg.run(() -> {
            boolean success = Root.run(cmd);
            Bg.ui(() -> {
                if (isDestroyed()) return;
                Toast.makeText(this, success ? ok : fail, Toast.LENGTH_LONG).show();
                refresh();
            });
        });
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Menu ini tidak tersedia di STB ini", Toast.LENGTH_SHORT).show();
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }
}
