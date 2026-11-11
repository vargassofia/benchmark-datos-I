package ordenamiento.algoritmos;

/**
 * Bubble Sort SIN bandera de corte: siempre recorre las n-1 pasadas,
 * aunque el arreglo ya esté ordenado. La pasada i recorre hasta n-1-i.
 */
public class BubbleSort {

    public static void sort(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    int tmp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = tmp;
                }
            }
        }
    }
}
