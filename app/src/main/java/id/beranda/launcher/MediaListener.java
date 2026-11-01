package id.beranda.launcher;

import android.service.notification.NotificationListenerService;

/**
 * Hanya dipakai sebagai "kunci" izin agar launcher boleh membaca & mengontrol musik
 * yang sedang diputar. Tidak membaca isi notifikasi apa pun.
 */
public class MediaListener extends NotificationListenerService {
}
