import java.io.*;
import java.util.Arrays;

// Kết quả: results/results.csv, results/system_info.txt, data/*.txt

public class Experiment {
    static final int[] SIZES   = {10_000, 100_000, 1_000_000};
    static final String[] TYPES = {"random", "nearly_sorted", "reversed"};
    static final int[] CUTOFFS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 40, 50};

    static final int WARMUP_ROUNDS = 5;   // số vòng khởi động JIT (không ghi kết quả)
    static final int TRIALS = 20;         // số lần đo cho mỗi cấu hình
    static final long SEED = 12345L;

    public static void main(String[] args) throws IOException {
        new File("results").mkdirs();
        new File("data").mkdirs();
        writeSystemInfo("results/system_info.txt");

        try (PrintWriter out = new PrintWriter(new FileWriter("results/results.csv"))) {
            out.println("n,type,cutoff,trial,time_ms");

            for (int n : SIZES) {
                for (String type : TYPES) {
                    Integer[] original = prepareData(n, type);
                    System.out.printf("== n=%d, type=%s ==%n", n, type);

                    // 1) Warm-up: chạy đủ mọi cutoff vài vòng để JIT biên dịch kỹ, bỏ kết quả
                    for (int w = 0; w < WARMUP_ROUNDS; w++)
                        for (int c : CUTOFFS)
                            runOnce(original, c);

                    // 2) Đo thật: vòng ngoài là trial, vòng trong là cutoff
                    //    (xen kẽ để các cutoff chịu ảnh hưởng nhiễu hệ thống như nhau)
                    for (int t = 1; t <= TRIALS; t++) {
                        for (int c : CUTOFFS) {
                            double ms = runOnce(original, c);
                            out.printf("%d,%s,%d,%d,%.4f%n", n, type, c, t, ms);
                        }
                    }
                    out.flush();
                }
            }
        }
        System.out.println("Xong. Xem results/results.csv");
    }

    // Tạo dữ liệu, lưu ra data/, rồi đọc lại từ file (đảm bảo bộ dữ liệu nộp = bộ dữ liệu đã đo)
    private static Integer[] prepareData(int n, String type) throws IOException {
        String path = "data/" + type + "_" + n + ".txt";
        if (!new File(path).exists()) {
            Integer[] a;
            switch (type) {
                case "random":        a = DataGenerator.random(n, SEED); break;
                case "nearly_sorted": a = DataGenerator.nearlySorted(n, SEED); break;
                default:              a = DataGenerator.reversed(n);
            }
            DataGenerator.save(a, path);
        }
        return DataGenerator.load(path);
    }

    // Sắp xếp một bản copy mới của dữ liệu; chỉ tính thời gian của lệnh sort. Trả về ms.
    private static double runOnce(Integer[] original, int cutoff) {
        Integer[] a = Arrays.copyOf(original, original.length);
        MergeCutoff.setCutoff(cutoff);

        long start = System.nanoTime();
        MergeCutoff.sort(a);
        long end = System.nanoTime();

        if (!SortCheck.isSorted(a))
            throw new IllegalStateException("Sắp xếp sai! cutoff=" + cutoff);
        return (end - start) / 1e6;
    }

    // Ghi cấu hình máy để đưa vào báo cáo
    private static void writeSystemInfo(String path) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(path))) {
            w.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                    + " (" + System.getProperty("os.arch") + ")");
            w.println("Java: " + System.getProperty("java.version") + " - " + System.getProperty("java.vm.name"));
            w.println("CPU cores (logical): " + Runtime.getRuntime().availableProcessors());
            w.println("Max heap (MB): " + Runtime.getRuntime().maxMemory() / (1024 * 1024));
            w.println("Warm-up rounds: " + WARMUP_ROUNDS + ", trials: " + TRIALS + ", seed: " + SEED);
            w.println("(Hãy bổ sung tay: tên CPU, RAM, chế độ nguồn điện)");
        }
    }
}