package id.beranda.launcher;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Random;

/** Screensaver: jam besar di latar hitam, posisinya bergeser pelan supaya layar tidak "burn-in". */
public class ClockDream extends DreamService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private FrameLayout root;
    private LinearLayout box;
    private TextView date;
    private TextView next;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update(true);
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 60000 - (now % 60000) + 300);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        TextClock clock = new TextClock(this);
        String f = Prefs.bool(this, Prefs.CLOCK_24H, true) ? "HH:mm" : "h:mm";
        clock.setFormat24Hour(f);
        clock.setFormat12Hour(f);
        clock.setTextSize(120);
        clock.setTextColor(Color.WHITE);
        clock.setAlpha(0.85f);
        clock.setTypeface(Ui.THIN);
        clock.setIncludeFontPadding(false);
        box.addView(clock);

        date = Ui.text(this, 22, 0.55f, Ui.LIGHT);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dp(this, 10);
        box.addView(date, dlp);

        next = Ui.text(this, 17, 0.4f, Ui.LIGHT);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = Ui.dp(this, 8);
        box.addView(next, nlp);

        root.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(root);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        update(false);
        handler.removeCallbacks(tick);
        long now = System.currentTimeMillis();
        handler.postDelayed(tick, 60000 - (now % 60000) + 300);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private void update(boolean move) {
        date.setText(new SimpleDateFormat("EEEE, d MMMM", MainActivity.ID).format(new Date()));
        if (Prefs.bool(this, Prefs.SHOW_PRAYER, true)) {
            try {
                LocalDate today = LocalDate.now();
                Schedule.Next n = Schedule.next(Schedule.times(this, today),
                        Schedule.times(this, today.plusDays(1)), Schedule.nowMinute());
                next.setText(n.name() + " " + PrayerTimes.hhmm(n.minute));
                next.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                next.setVisibility(View.GONE);
            }
        } else {
            next.setVisibility(View.GONE);
        }
        if (move) moveBox();
    }

    private void moveBox() {
        int rangeX = Math.max(0, (root.getWidth() - box.getWidth()) / 2 - Ui.dp(this, 40));
        int rangeY = Math.max(0, (root.getHeight() - box.getHeight()) / 2 - Ui.dp(this, 30));
        float x = rangeX == 0 ? 0 : random.nextInt(rangeX * 2 + 1) - rangeX;
        float y = rangeY == 0 ? 0 : random.nextInt(rangeY * 2 + 1) - rangeY;
        box.animate().alpha(0f).setDuration(600).withEndAction(() -> {
            box.setTranslationX(x);
            box.setTranslationY(y);
            box.animate().alpha(1f).setDuration(600).start();
        }).start();
    }
}
