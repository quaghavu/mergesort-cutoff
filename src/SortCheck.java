public class SortCheck {
    // true nếu mảng đã được sắp xếp tăng dần
    public static boolean isSorted(Comparable[] a) {
        for (int i = 1; i < a.length; i++)
            if (a[i].compareTo(a[i - 1]) < 0) return false;
        return true;
    }
}