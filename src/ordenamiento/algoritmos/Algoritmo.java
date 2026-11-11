package ordenamiento.algoritmos;

/**
 * Forma común de todos los algoritmos del benchmark.
 *
 * Cada algoritmo se escribe como una clase con un método
 *     public static void sort(int[] a)
 * que ordena el arreglo de forma ascendente, sobre el mismo arreglo.
 * El benchmark los usa con referencias a método, por ejemplo:
 *     Algoritmo radix = RadixSort::sort;
 */
@FunctionalInterface
public interface Algoritmo {
    void ordenar(int[] a);
}
