package id.beranda.launcher;

import java.util.concurrent.TimeUnit;

/** Menjalankan perintah dengan akses root (STB sudah di-root). Panggil dari thread latar. */
final class Root {

    private Root() {
    }

    static boolean run(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            if (!p.waitFor(25, TimeUnit.SECONDS)) {
                p.destroy();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
