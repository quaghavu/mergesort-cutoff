import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class DataGenerator {

    // Mảng ngẫu nhiên: cùng seed -> cùng dữ liệu (để mọi CUTOFF được so sánh công bằng)
    public static Integer[] random(int n, long seed) {
        Random rnd = new Random(seed);
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = rnd.nextInt();
        return a;
    }

    // Gần như đã sắp xếp: sắp xếp xong rồi hoán đổi ngẫu nhiên khoảng 1% số cặp
    public static Integer[] nearlySorted(int n, long seed) {
        Integer[] a = random(n, seed);
        Arrays.sort(a);
        Random rnd = new Random(seed + 1);
        int swaps = Math.max(1, n / 100);
        for (int s = 0; s < swaps; s++) {
            int i = rnd.nextInt(n), j = rnd.nextInt(n);
            Integer t = a[i]; a[i] = a[j]; a[j] = t;
        }
        return a;
    }

    // Sắp xếp ngược (giảm dần)
    public static Integer[] reversed(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = n - i;
        return a;
    }

    // Lưu dữ liệu ra file text, mỗi dòng một số
    public static void save(Integer[] a, String path) throws IOException {
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            for (Integer x : a) {
                w.write(Integer.toString(x));
                w.newLine();
            }
        }
    }

    // Đọc lại dữ liệu từ file đã lưu
    public static Integer[] load(String path) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            return r.lines().map(Integer::valueOf).toArray(Integer[]::new);
        }
    }
}