import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Cuenta las operaciones que realizan Shell, Merge y Quicksort (Parte 4).
 *
 * IMPORTANTE: usa COPIAS instrumentadas de los algoritmos. Las clases
 * ShellSort, MergeSort y QuickSort originales NO se modifican, así que no
 * afectan las mediciones de tiempo del Benchmark.
 *
 * Las entradas son exactamente las del Benchmark (mismo GeneradorEntradas con
 * semilla fija), por lo que los conteos son deterministas: se obtienen los
 * mismos números en cualquier computadora.
 *
 * Qué se cuenta:
 *  - comparaciones: cada vez que se compara el valor de dos elementos del arreglo.
 *      Shell:     a[j-gap] > temp
 *      Merge:     aux[j] < aux[i]   (solo cuando ambas mitades tienen elementos)
 *      Quicksort: a[j] <= pivote
 *  - intercambios (Shell): cada desplazamiento a[j] = a[j-gap] del insertion sort
 *      con saltos. Equivale al número de intercambios que haría esa misma
 *      ordenación si se implementara con swaps.
 *  - intercambios (Quicksort): cada llamada a swap(), incluidos los
 *      "auto-intercambios" (i == j) que el código hace cuando el elemento ya
 *      está en su lugar y el intercambio del pivote al final.
 *  - movimientos (Merge): cada escritura a[k] = ... durante la mezcla (un
 *      elemento colocado). No depende de los valores, solo de n.
 *  - profundidad máxima (Quicksort): mayor número de llamadas recursivas
 *      activas al mismo tiempo (la llamada inicial cuenta como 1). Es lo que
 *      consume la pila y provoca el desbordamiento.
 *
 * Uso:   java -cp out ConteoOperaciones [nMaximo] [archivoSalida]
 * Salida: CSV con columnas algoritmo,metrica,tipo_entrada,n,valor
 * Si Quicksort desborda la pila, se deja de crecer n para esa combinación.
 */
public class ConteoOperaciones {

    static long comparaciones, movimientos, profundidadMax, profundidadActual;

    // ------------------------------------------------------------ Shell
    static void shell(int[] a) {
        int n = a.length;
        for (int gap = n / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < n; i++) {
                int temp = a[i];
                int j = i;
                while (j >= gap) {
                    comparaciones++;
                    if (a[j - gap] > temp) {
                        a[j] = a[j - gap];
                        movimientos++;               // desplazamiento
                        j -= gap;
                    } else {
                        break;
                    }
                }
                a[j] = temp;
            }
        }
    }

    // ------------------------------------------------------------ Merge
    static void merge(int[] a) {
        if (a.length < 2) return;
        int[] aux = new int[a.length];
        mergeSort(a, aux, 0, a.length - 1);
    }

    private static void mergeSort(int[] a, int[] aux, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSort(a, aux, lo, mid);
        mergeSort(a, aux, mid + 1, hi);
        for (int k = lo; k <= hi; k++) aux[k] = a[k];
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = aux[j++];
            } else if (j > hi) {
                a[k] = aux[i++];
            } else {
                comparaciones++;
                if (aux[j] < aux[i]) a[k] = aux[j++]; else a[k] = aux[i++];
            }
            movimientos++;                           // un elemento colocado en a[k]
        }
    }

    // -------------------------------------------------------- Quicksort
    static void quick(int[] a) {
        quick(a, 0, a.length - 1);
    }

    private static void quick(int[] a, int lo, int hi) {
        profundidadActual++;
        if (profundidadActual > profundidadMax) profundidadMax = profundidadActual;
        if (lo < hi) {
            int p = partition(a, lo, hi);
            quick(a, lo, p - 1);
            quick(a, p + 1, hi);
        }
        profundidadActual--;
    }

    private static int partition(int[] a, int lo, int hi) {
        int pivot = a[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            comparaciones++;
            if (a[j] <= pivot) {
                i++;
                swap(a, i, j);
            }
        }
        swap(a, i + 1, hi);
        return i + 1;
    }

    private static void swap(int[] a, int i, int j) {
        movimientos++;                               // una llamada a swap
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    // ------------------------------------------------------------- main
    interface Alg { void ordenar(int[] a); }

    public static void main(String[] args) throws IOException {
        int nMaximo = args.length > 0 ? Integer.parseInt(args[0]) : 1_024_000;
        Path salida = Paths.get(args.length > 1 ? args[1] : "resultados/conteo_operaciones.csv");
        if (salida.getParent() != null) Files.createDirectories(salida.getParent());

        String[] nombres = {"Shell", "Merge", "Quicksort"};
        Alg[] algs = { ConteoOperaciones::shell, ConteoOperaciones::merge, ConteoOperaciones::quick };
        // Nombre de la métrica "intercambios/movimientos" según el algoritmo
        String[] metricaMov = {"intercambios", "movimientos", "intercambios"};

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(salida, StandardCharsets.UTF_8))) {
            pw.println("algoritmo,metrica,tipo_entrada,n,valor");

            for (int k = 0; k < nombres.length; k++) {
                for (GeneradorEntradas.Tipo tipo : GeneradorEntradas.Tipo.values()) {
                    for (int n = 1000; n <= nMaximo; n *= 2) {
                        int[] entrada = GeneradorEntradas.generar(tipo, n);
                        int[] esperado = entrada.clone();
                        Arrays.sort(esperado);

                        comparaciones = 0; movimientos = 0; profundidadMax = 0; profundidadActual = 0;
                        try {
                            algs[k].ordenar(entrada);
                        } catch (StackOverflowError e) {
                            System.out.printf("%-10s %-11s n=%,d: desbordamiento de pila, se deja de crecer n%n",
                                    nombres[k], tipo.etiqueta(), n);
                            break;
                        }
                        if (!Arrays.equals(entrada, esperado))
                            throw new IllegalStateException("Mal ordenado: " + nombres[k] + " " + tipo + " n=" + n);

                        pw.printf("%s,comparaciones,%s,%d,%d%n", nombres[k], tipo.etiqueta(), n, comparaciones);
                        pw.printf("%s,%s,%s,%d,%d%n", nombres[k], metricaMov[k], tipo.etiqueta(), n, movimientos);
                        if (k == 2)
                            pw.printf("%s,profundidad_max,%s,%d,%d%n", nombres[k], tipo.etiqueta(), n, profundidadMax);
                    }
                }
                System.out.println(nombres[k] + " listo");
            }
        }
        System.out.println("Escrito: " + salida.toAbsolutePath());
    }
}
