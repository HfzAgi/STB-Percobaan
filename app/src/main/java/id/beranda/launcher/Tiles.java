package id.beranda.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Kotak aplikasi untuk beranda dan daftar semua aplikasi. */
final class Tiles {

    private Tiles() {
    }

    private static FrameLayout frame(Context c) {
        FrameLayout f = new FrameLayout(c);
        float r = Ui.dp(c, 12);
        f.setBackground(Ui.rounded(0x40000000, r));
        f.setClipToOutline(true);
        f.setClickable(true);
        Ui.focusFx(f, r, 1.1f);
        return f;
    }

    static View app(Context c, Apps.App app, int h) {
        FrameLayout f = frame(c);
        f.setTag(app);
        f.setContentDescription(app.label);

        ImageView banner = new ImageView(c);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.setVisibility(View.GONE);
        f.addView(banner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int pad = Ui.dp(c, 6);
        box.setPadding(pad, pad, pad, pad);
        ImageView icon = new ImageView(c);
        int is = Math.round(h * 0.42f);
        box.addView(icon, new LinearLayout.LayoutParams(is, is));
        TextView label = Ui.text(c, 12, 0.95f, Ui.REGULAR);
        Ui.oneLine(label);
        label.setGravity(Gravity.CENTER);
        label.setText(app.label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 5);
        box.addView(label, lp);
        f.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Gambar dimuat di latar supaya layar tidak tersendat.
        Context appCtx = c.getApplicationContext();
        Bg.run(() -> {
            Drawable b = Apps.banner(appCtx, app);
            Drawable ic = b == null ? Apps.icon(appCtx, app) : null;
            Bg.ui(() -> {
                if (b != null) {
                    banner.setImageDrawable(b);
                    banner.setVisibility(View.VISIBLE);
                    box.setVisibility(View.GONE);
                } else if (ic != null) {
                    icon.setImageDrawable(ic);
                }
            });
        });
        return f;
    }

    static View action(Context c, int iconRes, String text) {
        FrameLayout f = frame(c);
        f.setContentDescription(text);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Ui.WHITE);
        int is = Ui.dp(c, 30);
        box.addView(icon, new LinearLayout.LayoutParams(is, is));
        TextView label = Ui.text(c, 12, 0.95f, Ui.REGULAR);
        Ui.oneLine(label);
        label.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 6);
        box.addView(label, lp);
        f.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return f;
    }
}
