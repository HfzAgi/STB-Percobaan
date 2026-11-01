package id.beranda.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

/** Semua aplikasi terpasang, dalam grid. Tahan OK untuk menu. */
public class AppsActivity extends Activity {

    private static final int COLUMNS = 5;

    private LinearLayout grid;
    private int builtApps = -1;
    private int builtVersion = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        Weather.Data w = Prefs.bool(this, Prefs.SHOW_WEATHER, true) ? Weather.cached(this) : null;
        root.setBackground(Ui.sky(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Weather.gloom(w)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(48), dp(28), dp(48), dp(36));
        col.setClipChildren(false);
        col.setClipToPadding(false);
        scroll.addView(col, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.text(this, 26, 1f, Ui.LIGHT);
        title.setText("Semua aplikasi");
        col.addView(title);
        TextView hint = Ui.text(this, 13, 0.6f, Ui.REGULAR);
        hint.setText("Tahan OK (atau tombol Menu) untuk tambah ke beranda, info, atau uninstall");
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(6);
        hlp.bottomMargin = dp(18);
        col.addView(hint, hlp);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        col.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (builtApps != Apps.changeCount || builtVersion != Prefs.version(this)) build();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View f = getCurrentFocus();
            if (f != null && f.getTag() instanceof Apps.App) {
                menu((Apps.App) f.getTag());
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void build() {
        View focused = getCurrentFocus();
        Object focusedTag = focused == null ? null : focused.getTag();

        grid.removeAllViews();
        List<Apps.App> apps = Apps.all(this);
        int available = getResources().getDisplayMetrics().widthPixels - dp(96);
        int gap = dp(14);
        int w = (available - gap * (COLUMNS - 1)) / COLUMNS;
        int h = Math.round(w * 9f / 16f);

        LinearLayout row = null;
        View restore = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = gap;
                grid.addView(row, rlp);
            }
            Apps.App app = apps.get(i);
            View t = Tiles.app(this, app, h);
            t.setOnClickListener(v -> Apps.launch(this, app));
            t.setOnLongClickListener(v -> {
                menu(app);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            if (i % COLUMNS != COLUMNS - 1) lp.rightMargin = gap;
            row.addView(t, lp);
            if (focusedTag instanceof Apps.App && ((Apps.App) focusedTag).pkg().equals(app.pkg())) restore = t;
        }
        builtApps = Apps.changeCount;
        builtVersion = Prefs.version(this);

        View target = restore;
        if (target == null && grid.getChildCount() > 0) {
            target = ((LinearLayout) grid.getChildAt(0)).getChildAt(0);
        }
        if (target != null) target.post(target::requestFocus);
    }

    private void menu(Apps.App app) {
        List<String> favs = Prefs.favorites(this);
        boolean fav = Apps.indexIn(favs, app) >= 0;
        String[] items = {fav ? "Hapus dari beranda" : "Tambah ke beranda", "Info aplikasi", "Uninstall"};
        new AlertDialog.Builder(this, Ui.DIALOG_THEME)
                .setTitle(app.label)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        List<String> list = Prefs.favorites(this);
                        int i = Apps.indexIn(list, app);
                        if (i >= 0) {
                            list.remove(i);
                            Toast.makeText(this, "Dihapus dari beranda", Toast.LENGTH_SHORT).show();
                        } else {
                            list.add(app.key());
                            Toast.makeText(this, "Ditambahkan ke beranda", Toast.LENGTH_SHORT).show();
                        }
                        Prefs.setFavorites(this, list);
                        builtVersion = Prefs.version(this); // grid tidak perlu digambar ulang
                    } else if (which == 1) {
                        MainActivity.openAppInfo(this, app.pkg());
                    } else {
                        try {
                            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.pkg())));
                        } catch (Exception e) {
                            MainActivity.openAppInfo(this, app.pkg());
                        }
                    }
                })
                .show();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }
}
