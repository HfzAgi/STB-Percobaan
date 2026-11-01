package id.beranda.launcher;

import android.content.Context;

/** Daftar kota untuk cuaca & jadwal sholat. */
final class Cities {

    static final class City {
        final String name;
        final double lat;
        final double lon;

        City(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static final String DEFAULT = "Cileungsi";

    static final City[] ALL = {
            new City("Cileungsi", -6.3967, 106.9594),
            new City("Cibubur", -6.3683, 106.8930),
            new City("Bekasi", -6.2383, 106.9756),
            new City("Bogor", -6.5971, 106.8060),
            new City("Cibinong", -6.4817, 106.8540),
            new City("Depok", -6.4025, 106.7942),
            new City("Jakarta Pusat", -6.1754, 106.8272),
            new City("Jakarta Selatan", -6.2615, 106.8106),
            new City("Jakarta Timur", -6.2250, 106.9004),
            new City("Jakarta Barat", -6.1683, 106.7588),
            new City("Jakarta Utara", -6.1384, 106.8640),
            new City("Tangerang", -6.1783, 106.6319),
            new City("Tangerang Selatan", -6.2886, 106.7179),
            new City("Karawang", -6.3227, 107.3376),
            new City("Purwakarta", -6.5569, 107.4433),
            new City("Sukabumi", -6.9277, 106.9300),
            new City("Cianjur", -6.8168, 107.1425),
            new City("Bandung", -6.9175, 107.6191),
            new City("Garut", -7.2279, 107.9087),
            new City("Tasikmalaya", -7.3274, 108.2207),
            new City("Cirebon", -6.7063, 108.5570),
            new City("Serang", -6.1200, 106.1503),
            new City("Cilegon", -6.0025, 106.0111),
            new City("Tegal", -6.8694, 109.1402),
            new City("Purwokerto", -7.4245, 109.2396),
            new City("Semarang", -6.9667, 110.4167),
            new City("Solo", -7.5755, 110.8243),
            new City("Yogyakarta", -7.7956, 110.3695),
            new City("Magelang", -7.4797, 110.2177),
            new City("Madiun", -7.6298, 111.5239),
            new City("Kediri", -7.8480, 112.0178),
            new City("Malang", -7.9666, 112.6326),
            new City("Surabaya", -7.2575, 112.7521),
            new City("Jember", -8.1724, 113.7005),
            new City("Banyuwangi", -8.2192, 114.3691),
            new City("Denpasar", -8.6705, 115.2126),
            new City("Mataram", -8.5833, 116.1167),
            new City("Kupang", -10.1772, 123.6070),
            new City("Banda Aceh", 5.5483, 95.3238),
            new City("Medan", 3.5952, 98.6722),
            new City("Padang", -0.9471, 100.4172),
            new City("Pekanbaru", 0.5071, 101.4478),
            new City("Batam", 1.0456, 104.0305),
            new City("Jambi", -1.6101, 103.6131),
            new City("Palembang", -2.9761, 104.7754),
            new City("Bengkulu", -3.8004, 102.2655),
            new City("Bandar Lampung", -5.3971, 105.2668),
            new City("Pangkalpinang", -2.1291, 106.1090),
            new City("Pontianak", -0.0263, 109.3425),
            new City("Palangka Raya", -2.2161, 113.9135),
            new City("Banjarmasin", -3.3186, 114.5944),
            new City("Balikpapan", -1.2379, 116.8529),
            new City("Samarinda", -0.5022, 117.1536),
            new City("Makassar", -5.1477, 119.4327),
            new City("Palu", -0.8917, 119.8707),
            new City("Kendari", -3.9985, 122.5129),
            new City("Manado", 1.4748, 124.8421),
            new City("Gorontalo", 0.5435, 123.0568),
            new City("Ambon", -3.6954, 128.1814),
            new City("Ternate", 0.7893, 127.3770),
            new City("Sorong", -0.8762, 131.2558),
            new City("Jayapura", -2.5337, 140.7181),
    };

    static City byName(String name) {
        for (City c : ALL) if (c.name.equals(name)) return c;
        return ALL[0];
    }

    static int indexOf(String name) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].name.equals(name)) return i;
        return 0;
    }

    static City current(Context ctx) {
        return byName(Prefs.get(ctx).getString(Prefs.CITY, DEFAULT));
    }

    static String[] names() {
        String[] n = new String[ALL.length];
        for (int i = 0; i < ALL.length; i++) n[i] = ALL[i].name;
        return n;
    }
}
