package id.beranda.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Layar utama (launcher). */
public class MainActivity extends Activity {

    static final Locale ID = new Locale("id", "ID");

    /** Kolom jadwal yang ditampilkan. */
    private static final int[] SHOWN = {PrayerTimes.IMSAK, PrayerTimes.SUBUH, PrayerTimes.DZUHUR,
            PrayerTimes.ASHAR, PrayerTimes.MAGHRIB, PrayerTimes.ISYA};

    private TextClock clock;
    private TextView dateView;
    private TextView hijriView;
    private LinearLayout weatherBox;
    private TextView weatherMain;
    private TextView weatherDesc;
    private TextView weatherExtra;
    private LinearLayout prayerPanel;
    private TextView prayerHeader;
    private final TextView[] prayerNames = new TextView[SHOWN.length];
    private final TextView[] prayerTimes = new TextView[SHOWN.length];
    private HorizontalScrollView favScroll;
    private LinearLayout favRow;
    private FrameLayout root;
    private MediaCard media;

    private boolean resumed;
    private int builtVersion = -1;
    private int builtApps = -1;
    private int skyKey = -1;
    private int gloom;

    private LocalDate cachedDate;
    private String cachedCity;
    private int[] todayTimes;
    private int[] tomorrowTimes;
    private String hijriText;
    private int hijriAdj = Integer.MIN_VALUE;

    private BroadcastReceiver packageReceiver;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            onMinute();
            scheduleTick();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        setContentView(root);
        registerPackageReceiver();
        buildFavorites(0);
        if (!Prefs.get(this).getBoolean("hint_shown", false)) {
            Prefs.get(this).edit().putBoolean("hint_shown", true).apply();
            Toast.makeText(this, "Tip: tahan tombol OK pada aplikasi untuk mengatur beranda",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        applyClockFormat();
        if (builtVersion != Prefs.version(this) || builtApps != Apps.changeCount) {
            cachedDate = null; // paksa hitung ulang jadwal (kota/pengaturan mungkin berubah)
            buildFavorites(-1);
        }
        applyWeather();
        onMinute();
        scheduleTick();
        media.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        Bg.MAIN.removeCallbacks(tick);
        media.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bg.MAIN.removeCallbacks(tick);
        if (packageReceiver != null) {
            try {
                unregisterReceiver(packageReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Tombol Home ditekan saat sudah di beranda: kembali ke aplikasi pertama.
        favScroll.smoothScrollTo(0, 0);
        if (favRow.getChildCount() > 0) favRow.getChildAt(0).requestFocus();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Launcher: tombol Back tidak melakukan apa-apa.
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View f = getCurrentFocus();
            if (f != null && f.getTag() instanceof Apps.App) {
                favoriteMenu((Apps.App) f.getTag());
                return true;
            }
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---------------- Tampilan ----------------

    private void buildUi() {
        root = new FrameLayout(this);
        root.setClipChildren(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(48), dp(28), dp(48), dp(18));
        content.setClipChildren(false);
        content.setClipToPadding(false);
        root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Baris atas: jam (kiri) + cuaca (kanan)
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        clock = new TextClock(this);
        clock.setTextSize(88);
        clock.setTextColor(Ui.WHITE);
        clock.setTypeface(Ui.THIN);
        clock.setIncludeFontPadding(false);
        clock.setShadowLayer(dp(8), 0, dp(1), 0x55000000);
        left.addView(clock);
        dateView = Ui.text(this, 20, 0.95f, Ui.REGULAR);
        left.addView(dateView, marginTop(dp(6)));
        hijriView = Ui.text(this, 15, 0.7f, Ui.REGULAR);
        left.addView(hijriView, marginTop(dp(6)));
        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        weatherBox = new LinearLayout(this);
        weatherBox.setOrientation(LinearLayout.VERTICAL);
        weatherBox.setGravity(Gravity.END);
        weatherMain = Ui.text(this, 42, 1f, Ui.LIGHT);
        weatherDesc = Ui.text(this, 16, 0.95f, Ui.REGULAR);
        weatherExtra = Ui.text(this, 14, 0.7f, Ui.REGULAR);
        weatherMain.setGravity(Gravity.END);
        weatherDesc.setGravity(Gravity.END);
        weatherExtra.setGravity(Gravity.END);
        weatherBox.addView(weatherMain, marginTop(dp(10)));
        weatherBox.addView(weatherDesc, marginTop(dp(8)));
        weatherBox.addView(weatherExtra, marginTop(dp(6)));
        top.addView(weatherBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Ruang kosong supaya bersih
        content.addView(new View(this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Baris tengah: jadwal sholat + kartu musik
        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setGravity(Gravity.BOTTOM);
        middle.setClipChildren(false);
        prayerPanel = buildPrayerPanel();
        middle.addView(prayerPanel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        media = new MediaCard(this);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(dp(330), ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.leftMargin = dp(16);
        middle.addView(media.view(), mlp);
        content.addView(middle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Baris bawah: aplikasi favorit
        favScroll = new HorizontalScrollView(this);
        favScroll.setHorizontalScrollBarEnabled(false);
        favScroll.setClipChildren(false);
        favScroll.setClipToPadding(false);
        favScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        favScroll.setPadding(dp(10), dp(14), dp(10), dp(14));
        favRow = new LinearLayout(this);
        favRow.setOrientation(LinearLayout.HORIZONTAL);
        favRow.setClipChildren(false);
        favRow.setClipToPadding(false);
        favScroll.addView(favRow);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(6);
        flp.leftMargin = -dp(10);
        flp.rightMargin = -dp(10);
        content.addView(favScroll, flp);
    }

    private LinearLayout buildPrayerPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.glass(this));
        panel.setPadding(dp(22), dp(14), dp(22), dp(14));

        prayerHeader = Ui.text(this, 14, 0.85f, Ui.REGULAR);
        Ui.oneLine(prayerHeader);
        panel.addView(prayerHeader);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < SHOWN.length; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            prayerNames[i] = Ui.text(this, 13, 0.7f, Ui.REGULAR);
            prayerNames[i].setText(PrayerTimes.NAMES[SHOWN[i]]);
            prayerTimes[i] = Ui.text(this, 21, 1f, Ui.LIGHT);
            col.addView(prayerNames[i]);
            col.addView(prayerTimes[i], marginTop(dp(4)));
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        panel.addView(row, marginTop(dp(10)));
        return panel;
    }

    private void applyClockFormat() {
        String f = Prefs.bool(this, Prefs.CLOCK_24H, true) ? "HH:mm" : "h:mm";
        clock.setFormat24Hour(f);
        clock.setFormat12Hour(f);
    }

    // ---------------- Aplikasi favorit ----------------

    /** @param focusIndex kotak yang diberi fokus; -1 = pertahankan posisi fokus sekarang (bila ada). */
    private void buildFavorites(int focusIndex) {
        if (focusIndex < 0) {
            View focused = getCurrentFocus();
            focusIndex = focused != null && focused.getParent() == favRow ? favRow.indexOfChild(focused) : -1;
        }

        favRow.removeAllViews();
        List<Apps.App> all = Apps.all(this);
        if (!Prefs.hasFavorites(this)) Prefs.setFavorites(this, Apps.defaultFavorites(all));

        int h = dp(99), w = dp(176);
        for (String key : Prefs.favorites(this)) {
            Apps.App app = Apps.find(all, key);
            if (app == null) continue;
            View t = Tiles.app(this, app, h);
            t.setOnClickListener(v -> Apps.launch(this, app));
            t.setOnLongClickListener(v -> {
                favoriteMenu(app);
                return true;
            });
            favRow.addView(t, tileLp(w, h));
        }
        View allApps = Tiles.action(this, R.drawable.ic_apps, "Semua aplikasi");
        allApps.setOnClickListener(v -> startActivity(new Intent(this, AppsActivity.class)));
        favRow.addView(allApps, tileLp(dp(132), h));
        View settings = Tiles.action(this, R.drawable.ic_settings, "Pengaturan");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        favRow.addView(settings, tileLp(dp(132), h));

        builtVersion = Prefs.version(this);
        builtApps = Apps.changeCount;

        if (focusIndex >= 0) {
            int i = Math.min(focusIndex, favRow.getChildCount() - 1);
            View target = favRow.getChildAt(i);
            if (target != null) target.post(target::requestFocus);
        }
    }

    private LinearLayout.LayoutParams tileLp(int w, int h) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.rightMargin = dp(14);
        return lp;
    }

    private void favoriteMenu(Apps.App app) {
        String[] items = {"Geser ke kiri", "Geser ke kanan", "Hapus dari beranda", "Info aplikasi"};
        new AlertDialog.Builder(this, Ui.DIALOG_THEME)
                .setTitle(app.label)
                .setItems(items, (d, which) -> {
                    List<String> favs = Prefs.favorites(this);
                    int i = Apps.indexIn(favs, app);
                    if (which == 3) {
                        openAppInfo(this, app.pkg());
                        return;
                    }
                    if (i < 0) return;
                    int target = i;
                    if (which == 0 && i > 0) {
                        favs.add(i - 1, favs.remove(i));
                        target = i - 1;
                    } else if (which == 1 && i < favs.size() - 1) {
                        favs.add(i + 1, favs.remove(i));
                        target = i + 1;
                    } else if (which == 2) {
                        favs.remove(i);
                        target = Math.max(0, i - 1);
                    }
                    Prefs.setFavorites(this, favs);
                    buildFavorites(target);
                })
                .show();
    }

    static void openAppInfo(Context c, String pkg) {
        try {
            c.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", pkg, null)));
        } catch (Exception e) {
            Toast.makeText(c, "Info aplikasi tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerPackageReceiver() {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Apps.changeCount++;
                if (resumed) buildFavorites(-1);
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, f);
    }

    // ---------------- Pembaruan tiap menit ----------------

    private void scheduleTick() {
        Bg.MAIN.removeCallbacks(tick);
        long now = System.currentTimeMillis();
        long delay = 60000 - (now % 60000) + 300;
        Bg.MAIN.postDelayed(tick, delay);
    }

    private void onMinute() {
        LocalDate today = LocalDate.now();
        dateView.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", ID).format(new Date()));
        boolean islamic = Prefs.bool(this, Prefs.SHOW_PRAYER, true);
        updateHijri(today, islamic);
        updatePrayer(today, islamic);
        updateSky();
        if (Prefs.bool(this, Prefs.SHOW_WEATHER, true) && Weather.needsRefresh(this)) fetchWeather();
    }

    private void updateHijri(LocalDate today, boolean show) {
        if (!show) {
            hijriView.setVisibility(View.GONE);
            return;
        }
        int adj = Prefs.get(this).getInt(Prefs.HIJRI_ADJ, 0);
        if (!today.equals(cachedDate) || adj != hijriAdj || hijriText == null) {
            try {
                StringBuilder sb = new StringBuilder(Hijri.format(today, adj));
                Hijri.Event ev = Hijri.nextEvent(today, adj);
                if (ev != null) {
                    sb.append("  ·  ");
                    if (ev.days == 0) sb.append("Hari ini ").append(ev.name);
                    else sb.append(ev.name).append(" ").append(ev.days).append(" hari lagi");
                }
                hijriText = sb.toString();
            } catch (Throwable t) {
                hijriText = "";
            }
            hijriAdj = adj;
        }
        hijriView.setText(hijriText);
        hijriView.setVisibility(hijriText.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updatePrayer(LocalDate today, boolean show) {
        if (!show) {
            prayerPanel.setVisibility(View.GONE);
            cachedDate = today;
            return;
        }
        prayerPanel.setVisibility(View.VISIBLE);
        String city = Cities.current(this).name;
        if (!today.equals(cachedDate) || !city.equals(cachedCity) || todayTimes == null) {
            todayTimes = Schedule.times(this, today);
            tomorrowTimes = Schedule.times(this, today.plusDays(1));
            cachedCity = city;
        }
        cachedDate = today;

        int now = Schedule.nowMinute();
        Schedule.Next next = Schedule.next(todayTimes, tomorrowTimes, now);
        int[] shownTimes = next.tomorrow ? tomorrowTimes : todayTimes;
        prayerHeader.setText(Schedule.label(next) + "  ·  " + city);
        for (int i = 0; i < SHOWN.length; i++) {
            prayerTimes[i].setText(PrayerTimes.hhmm(shownTimes[SHOWN[i]]));
            boolean hi = SHOWN[i] == next.index;
            prayerTimes[i].setTextColor(hi ? Ui.ACCENT : Ui.WHITE);
            prayerTimes[i].setTypeface(hi ? Ui.MEDIUM : Ui.LIGHT);
            prayerNames[i].setTextColor(hi ? Ui.ACCENT : Ui.WHITE);
        }
    }

    private void updateSky() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int key = Ui.skyBucket(hour) * 10 + gloom;
        if (key == skyKey) return;
        skyKey = key;
        root.setBackground(Ui.sky(hour, gloom));
    }

    // ---------------- Cuaca ----------------

    private void fetchWeather() {
        Context app = getApplicationContext();
        Bg.run(() -> {
            Weather.fetch(app);
            Bg.ui(() -> {
                if (!isDestroyed()) applyWeather();
            });
        });
    }

    private void applyWeather() {
        if (!Prefs.bool(this, Prefs.SHOW_WEATHER, true)) {
            weatherBox.setVisibility(View.GONE);
            gloom = 0;
            updateSky();
            return;
        }
        weatherBox.setVisibility(View.VISIBLE);
        String city = Cities.current(this).name;
        Weather.Data d = Weather.cached(this);
        if (d == null) {
            weatherMain.setVisibility(View.GONE);
            weatherDesc.setText(city);
            weatherExtra.setText("Cuaca belum tersedia");
        } else {
            weatherMain.setVisibility(View.VISIBLE);
            weatherMain.setText(Weather.icon(d.code, d.isDay) + "  " + Math.round(d.temp) + "°");
            weatherDesc.setText(Weather.describe(d.code) + "  ·  " + city);
            StringBuilder sb = new StringBuilder();
            if (!Double.isNaN(d.max) && !Double.isNaN(d.min)) {
                sb.append("↑").append(Math.round(d.max)).append("°  ↓")
                        .append(Math.round(d.min)).append("°");
            }
            if (d.rainProb >= 0) {
                if (sb.length() > 0) sb.append("  ·  ");
                sb.append("Peluang hujan ").append(d.rainProb).append("%");
            }
            weatherExtra.setText(sb.toString());
        }
        gloom = Weather.gloom(d);
        updateSky();
    }

    // ---------------- Util ----------------

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private static LinearLayout.LayoutParams marginTop(int px) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px;
        return lp;
    }
}
