/**
 * Selection, Bubble (SIN bandera) e Insertion, sin ninguna optimizacion.
 */
public class SortingAlgorithms {

    /** Selection sort: siempre hace n(n-1)/2 comparaciones y n-1 intercambios (intercambia siempre). */
    public static void selectionSort(int[] a) {
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

    /** Bubble sort SIN bandera: siempre recorre todas las pasadas, aunque ya este ordenado. */
    public static void bubbleSort(int[] a) {
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

    /** Insertion sort clasico (desplazamientos, sin busqueda binaria ni centinela). */
    public static void insertionSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }
}
