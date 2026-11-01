package id.beranda.launcher;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Thread latar kecil + akses ke thread UI. */
final class Bg {

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2);
    static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Bg() {
    }

    static void run(Runnable r) {
        EXEC.execute(r);
    }

    static void ui(Runnable r) {
        MAIN.post(r);
    }
}
