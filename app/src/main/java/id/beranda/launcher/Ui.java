package id.beranda.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/** Helper tampilan: ukuran dp, teks, panel kaca, efek fokus remote, warna langit. */
final class Ui {

    static final int WHITE = 0xFFFFFFFF;
    static final int ACCENT = 0xFFFFD27F;
    static final int GLASS = 0x47000000;
    static final int DIALOG_THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    static final Typeface THIN = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    static TextView text(Context c, float sp, float alpha, Typeface tf) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(WHITE);
        t.setAlpha(alpha);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        t.setShadowLayer(dp(c, 6), 0, dp(c, 1), 0x55000000);
        return t;
    }

    static void oneLine(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    /** Panel transparan gelap dengan garis tipis, kesan "kaca". */
    static GradientDrawable glass(Context c) {
        GradientDrawable g = rounded(GLASS, dp(c, 18));
        g.setStroke(Math.max(1, dp(c, 1)), 0x22FFFFFF);
        return g;
    }

    /** Efek saat dipilih remote: membesar sedikit + bingkai putih. */
    static void focusFx(View v, float radiusPx, float scale) {
        Context c = v.getContext();
        GradientDrawable ring = new GradientDrawable();
        ring.setColor(0x00000000);
        ring.setCornerRadius(radiusPx);
        ring.setStroke(dp(c, 2.5f), WHITE);
        float lift = dp(c, 6);
        v.setFocusable(true);
        v.setOnFocusChangeListener((view, has) -> {
            view.animate().scaleX(has ? scale : 1f).scaleY(has ? scale : 1f).setDuration(140).start();
            view.setForeground(has ? ring : null);
            view.setTranslationZ(has ? lift : 0f);
        });
    }

    static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** 0 subuh, 1 pagi, 2 siang, 3 sore, 4 senja, 5 malam */
    static int skyBucket(int hour) {
        if (hour >= 4 && hour < 6) return 0;
        if (hour >= 6 && hour < 10) return 1;
        if (hour >= 10 && hour < 15) return 2;
        if (hour >= 15 && hour < 18) return 3;
        if (hour == 18) return 4;
        return 5;
    }

    /** Wallpaper gradasi: berubah sesuai jam, lebih kelabu saat mendung/hujan. */
    static GradientDrawable sky(int hour, int gloom) {
        int[] c;
        switch (skyBucket(hour)) {
            case 0: c = new int[]{0xFF14213D, 0xFF2E4A7A, 0xFF7A5470}; break;
            case 1: c = new int[]{0xFF1D3557, 0xFF2F6690, 0xFF3A7CA5}; break;
            case 2: c = new int[]{0xFF15304D, 0xFF1F5F8B, 0xFF2C88B5}; break;
            case 3: c = new int[]{0xFF2B1B47, 0xFF6B2D5C, 0xFFA8524A}; break;
            case 4: c = new int[]{0xFF1A1335, 0xFF43264F, 0xFF6E344A}; break;
            default: c = new int[]{0xFF0B1020, 0xFF141A33, 0xFF1E2547}; break;
        }
        if (gloom > 0) {
            float t = gloom == 1 ? 0.35f : 0.5f;
            for (int i = 0; i < c.length; i++) c[i] = blend(c[i], 0xFF2A2F38, t);
        }
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, c);
    }

    static String duration(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        if (h > 0 && m > 0) return h + " jam " + m + " menit";
        if (h > 0) return h + " jam";
        return m + " menit";
    }
}
