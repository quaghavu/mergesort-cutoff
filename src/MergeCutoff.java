// Mergesort top-down + cải tiến CUTOFF dùng insertion sort.
// cutoff = 1  -> mergesort thuần (không dùng insertion sort)
// cutoff = k  -> mảng con có kích thước <= k thì chuyển sang insertion sort
public class MergeCutoff {
    private static int cutoff = 10;

    public static void setCutoff(int c) {
        if (c < 1) throw new IllegalArgumentException("cutoff phải >= 1");
        cutoff = c;
    }
    public static int getCutoff() {
        return cutoff;
    }

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    // Trộn a[lo..mid] và a[mid+1..hi] (đã sắp xếp)
    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
        for (int k = lo; k <= hi; k++)
            aux[k] = a[k];

        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)                a[k] = aux[j++];
            else if (j > hi)                 a[k] = aux[i++];
            else if (less(aux[j], aux[i]))   a[k] = aux[j++];
            else                             a[k] = aux[i++];
        }
    }

    // if (hi <= lo + CUTOFF - 1) dùng insertion sort
    private static void sort(Comparable[] a, Comparable[] aux, int lo, int hi) {
        if (hi <= lo + cutoff - 1) {
            Insertion.sort(a, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid);
        sort(a, aux, mid + 1, hi);
        merge(a, aux, lo, mid, hi);
    }

    public static void sort(Comparable[] a) {
        Comparable[] aux = new Comparable[a.length];
        sort(a, aux, 0, a.length - 1);
    }
}