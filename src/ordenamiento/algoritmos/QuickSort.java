package ordenamiento.algoritmos;

/**
 * Quicksort sin optimizaciones, con PIVOTE FIJO = ÚLTIMO elemento.
 *
 * Sin optimizaciones significa:
 *  - El pivote siempre es a[hi]. No hay mediana de 3 ni pivote aleatorio.
 *  - NO se usa insertion sort para subarreglos pequeños.
 *  - NO se recurre primero sobre el lado más pequeño ni se elimina la recursión
 *    de cola: se hacen las dos llamadas recursivas siempre.
 *  - NO hay partición en 3 vías para duplicados (esquema de Lomuto clásico).
 *
 * Consecuencias que hay que explicar en el informe:
 *  - Caso promedio / mejor: O(n log n), profundidad de recursión ~ log n.
 *  - Entrada ORDENADA (ascendente o descendente): el pivote es siempre el
 *    máximo o el mínimo del rango, la partición queda n-1 / 0, el costo es
 *    O(n^2) y la profundidad de recursión es n  ->  puede desbordar la pila.
 *  - Muchos DUPLICADOS (pocos valores distintos): con Lomuto los iguales al
 *    pivote caen todos de un mismo lado, así que también degenera en O(n^2)
 *    y con recursión muy profunda.
 */
public class QuickSort {

    public static void sort(int[] a) {
        sort(a, 0, a.length - 1);
    }

    private static void sort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = partition(a, lo, hi);
        sort(a, lo, p - 1);
        sort(a, p + 1, hi);
    }

    /**
     * Partición de Lomuto con pivote = a[hi].
     * Al terminar, el pivote queda en su posición final y se devuelve su índice.
     * Los elementos <= pivote quedan a la izquierda; los > pivote, a la derecha.
     */
    private static int partition(int[] a, int lo, int hi) {
        int pivot = a[hi];
        int i = lo - 1;                       // frontera de los elementos <= pivote
        for (int j = lo; j < hi; j++) {
            if (a[j] <= pivot) {
                i++;
                swap(a, i, j);
            }
        }
        swap(a, i + 1, hi);                   // colocar el pivote en su lugar
        return i + 1;
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
