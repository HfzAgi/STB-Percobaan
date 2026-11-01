package id.beranda.launcher;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Kartu "sedang diputar" (Spotify, YouTube Music, dll). Butuh izin akses notifikasi;
 * tanpa izin kartu ini disembunyikan saja.
 */
final class MediaCard {

    private final Activity act;
    private final ComponentName listener;
    private final LinearLayout view;
    private final ImageView art;
    private final TextView title;
    private final TextView artist;
    private final ImageView playBtn;

    private MediaSessionManager msm;
    private MediaController controller;
    private boolean started;
    private Bitmap lastSource;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = this::onSessions;

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            update();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            update();
        }

        @Override
        public void onSessionDestroyed() {
            setController(null);
            refreshSessions();
        }
    };

    MediaCard(Activity a) {
        act = a;
        listener = new ComponentName(a, MediaListener.class);

        view = new LinearLayout(a);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackground(Ui.glass(a));
        int p = Ui.dp(a, 14);
        view.setPadding(p, p, p + Ui.dp(a, 4), p);
        view.setClipChildren(false);
        view.setClipToPadding(false);
        view.setVisibility(View.GONE);

        art = new ImageView(a);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setBackground(Ui.rounded(0x33FFFFFF, Ui.dp(a, 10)));
        art.setClipToOutline(true);
        int as = Ui.dp(a, 64);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(as, as);
        alp.rightMargin = Ui.dp(a, 14);
        view.addView(art, alp);

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        title = Ui.text(a, 15, 1f, Ui.MEDIUM);
        Ui.oneLine(title);
        artist = Ui.text(a, 13, 0.7f, Ui.REGULAR);
        Ui.oneLine(artist);
        col.addView(title);
        LinearLayout.LayoutParams arLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arLp.topMargin = Ui.dp(a, 3);
        col.addView(artist, arLp);

        LinearLayout buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setClipChildren(false);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = Ui.dp(a, 8);
        buttons.addView(button(R.drawable.ic_prev, () -> controls().skipToPrevious()));
        playBtn = button(R.drawable.ic_play, this::togglePlay);
        buttons.addView(playBtn);
        buttons.addView(button(R.drawable.ic_next, () -> controls().skipToNext()));
        col.addView(buttons, bLp);

        view.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    View view() {
        return view;
    }

    private ImageView button(int icon, Runnable action) {
        ImageView b = new ImageView(act);
        b.setImageResource(icon);
        b.setColorFilter(Ui.WHITE);
        int pad = Ui.dp(act, 7);
        b.setPadding(pad, pad, pad, pad);
        int size = Ui.dp(act, 36);
        b.setBackground(Ui.rounded(0x26FFFFFF, size / 2f));
        b.setClickable(true);
        Ui.focusFx(b, size / 2f, 1.15f);
        b.setOnClickListener(v -> {
            if (controller != null) {
                try {
                    action.run();
                } catch (Exception ignored) {
                }
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.rightMargin = Ui.dp(act, 10);
        b.setLayoutParams(lp);
        return b;
    }

    private MediaController.TransportControls controls() {
        return controller.getTransportControls();
    }

    private void togglePlay() {
        PlaybackState s = controller.getPlaybackState();
        if (s != null && s.getState() == PlaybackState.STATE_PLAYING) controls().pause();
        else controls().play();
    }

    static boolean hasAccess(Context c) {
        ComponentName cn = new ComponentName(c, MediaListener.class);
        if (Build.VERSION.SDK_INT >= 27) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            return nm != null && nm.isNotificationListenerAccessGranted(cn);
        }
        String flat = android.provider.Settings.Secure.getString(c.getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    void start() {
        started = true;
        if (!Prefs.bool(act, Prefs.SHOW_MEDIA, true) || !hasAccess(act)) {
            hide();
            return;
        }
        try {
            if (msm == null) msm = (MediaSessionManager) act.getSystemService(Context.MEDIA_SESSION_SERVICE);
            msm.addOnActiveSessionsChangedListener(sessionsListener, listener, Bg.MAIN);
            refreshSessions();
        } catch (Exception e) {
            hide();
        }
    }

    void stop() {
        started = false;
        if (msm != null) {
            try {
                msm.removeOnActiveSessionsChangedListener(sessionsListener);
            } catch (Exception ignored) {
            }
        }
        setController(null);
    }

    private void refreshSessions() {
        if (!started || msm == null) return;
        try {
            onSessions(msm.getActiveSessions(listener));
        } catch (Exception e) {
            hide();
        }
    }

    private void onSessions(List<MediaController> list) {
        MediaController best = null;
        if (list != null) {
            for (MediaController c : list) {
                PlaybackState s = c.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) {
                    best = c;
                    break;
                }
                if (best == null && c.getMetadata() != null) best = c;
            }
        }
        if (best != null && controller != null && best.getSessionToken().equals(controller.getSessionToken())) {
            update();
            return;
        }
        setController(best);
    }

    private void setController(MediaController c) {
        if (controller != null) {
            try {
                controller.unregisterCallback(callback);
            } catch (Exception ignored) {
            }
        }
        controller = c;
        if (controller != null) controller.registerCallback(callback, Bg.MAIN);
        update();
    }

    private void hide() {
        if (view.hasFocus()) {
            View next = view.focusSearch(View.FOCUS_DOWN);
            if (next != null) next.requestFocus();
        }
        view.setVisibility(View.GONE);
        art.setImageDrawable(null);
        lastSource = null;
    }

    private void update() {
        if (!started || controller == null) {
            hide();
            return;
        }
        MediaMetadata md = controller.getMetadata();
        if (md == null) {
            hide();
            return;
        }
        String t = md.getString(MediaMetadata.METADATA_KEY_TITLE);
        if (t == null) t = md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (t == null || t.trim().isEmpty()) {
            hide();
            return;
        }
        String ar = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if (ar == null) ar = md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
        if (ar == null) ar = appName(controller.getPackageName());
        title.setText(t);
        artist.setText(ar);

        Bitmap b = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (b != lastSource) {
            lastSource = b;
            art.setImageBitmap(b == null ? null : shrink(b));
        }
        art.setVisibility(b == null ? View.GONE : View.VISIBLE);

        PlaybackState s = controller.getPlaybackState();
        boolean playing = s != null && s.getState() == PlaybackState.STATE_PLAYING;
        playBtn.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        view.setVisibility(View.VISIBLE);
    }

    private String appName(String pkg) {
        try {
            PackageManager pm = act.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Perkecil sampul album supaya hemat memori. */
    private Bitmap shrink(Bitmap b) {
        int max = Ui.dp(act, 64);
        if (b.getWidth() <= max && b.getHeight() <= max) return b;
        float s = Math.min((float) max / b.getWidth(), (float) max / b.getHeight());
        try {
            return Bitmap.createScaledBitmap(b, Math.max(1, Math.round(b.getWidth() * s)),
                    Math.max(1, Math.round(b.getHeight() * s)), true);
        } catch (Exception e) {
            return b;
        }
    }
}
