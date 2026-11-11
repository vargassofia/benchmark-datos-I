package ordenamiento.algoritmos;

/**
 * Merge Sort recursivo "de arriba hacia abajo" (top-down), sin optimizaciones.
 *
 * Sin optimizaciones significa:
 *  - NO cambia a insertion sort para subarreglos pequeños.
 *  - NO revisa si las dos mitades ya están ordenadas para saltarse la mezcla.
 *  - NO intercambia los roles del arreglo auxiliar y el original.
 *
 * El único detalle de implementación: el arreglo auxiliar se crea UNA sola vez
 * y se reutiliza en todas las mezclas. Así la medición no se contamina con el
 * costo del recolector de basura creando arreglos temporales en cada merge.
 * Es el Merge Sort estándar de los libros de texto. Complejidad O(n log n) en
 * los tres casos y memoria extra O(n).
 *
 * Es estable: con elementos iguales conserva su orden original (usa <=).
 * La profundidad de recursión es log2(n), así que nunca desborda la pila.
 */
public class MergeSort {

    public static void sort(int[] a) {
        if (a.length < 2) return;
        int[] aux = new int[a.length];
        sort(a, aux, 0, a.length - 1);
    }

    private static void sort(int[] a, int[] aux, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid);
        sort(a, aux, mid + 1, hi);
        merge(a, aux, lo, mid, hi);
    }

    /** Mezcla a[lo..mid] y a[mid+1..hi], que ya están ordenadas. */
    private static void merge(int[] a, int[] aux, int lo, int mid, int hi) {
        // Copiar el rango completo al auxiliar
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }

        int i = lo;        // recorre la mitad izquierda
        int j = mid + 1;   // recorre la mitad derecha
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = aux[j++];          // izquierda agotada
            } else if (j > hi) {
                a[k] = aux[i++];          // derecha agotada
            } else if (aux[j] < aux[i]) {
                a[k] = aux[j++];          // gana la derecha (estrictamente menor)
            } else {
                a[k] = aux[i++];          // gana la izquierda (menor o igual -> estable)
            }
        }
    }
}
