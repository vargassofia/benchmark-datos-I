package ordenamiento.algoritmos;

/**
 * Shell Sort sin optimizaciones.
 *
 * Usa la secuencia de saltos ORIGINAL de Shell: n/2, n/4, ..., 1.
 * (No se usan las secuencias mejoradas de Knuth, Sedgewick o Ciura, porque
 * serían una optimización.) Con esta secuencia el peor caso es O(n^2).
 *
 * Para cada salto "gap" se hace un insertion sort sobre los elementos
 * separados por "gap" posiciones.
 */
public class ShellSort {

    public static void sort(int[] a) {
        int n = a.length;

        for (int gap = n / 2; gap > 0; gap /= 2) {
            // Insertion sort con saltos de tamaño gap
            for (int i = gap; i < n; i++) {
                int temp = a[i];
                int j = i;
                while (j >= gap && a[j - gap] > temp) {
                    a[j] = a[j - gap];
                    j -= gap;
                }
                a[j] = temp;
            }
        }
    }
}
