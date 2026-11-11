package ordenamiento.algoritmos;

/**
 * Selection Sort sin optimizaciones.
 *
 * Siempre hace n(n-1)/2 comparaciones y n-1 intercambios: intercambia
 * aunque el mínimo ya esté en su lugar (no revisa si min == i).
 */
public class SelectionSort {

    public static void sort(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) {
                if (a[j] < a[min]) {
                    min = j;
                }
            }
            int tmp = a[i];          // intercambio incondicional (sin optimizar)
            a[i] = a[min];
            a[min] = tmp;
        }
    }
}
