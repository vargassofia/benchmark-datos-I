package ordenamiento;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;

import ordenamiento.algoritmos.Algoritmo;
import ordenamiento.entrada.GeneradorEntradas;
import ordenamiento.entrada.TipoEntrada;

/**
 * Conteo exacto de operaciones de los 7 algoritmos con las mismas entradas del benchmark.
 *
 * Son copias instrumentadas de los algoritmos de ordenamiento.algoritmos (misma lógica,
 * con contadores). Las clases originales no se tocan, así el conteo no afecta las mediciones.
 * Los números no dependen de la computadora: con las mismas entradas salen iguales siempre.
 *
 * Métricas: comparaciones; intercambios (Selection, Bubble, Quicksort), desplazamientos
 * (Insertion, Shell) o movimientos (Merge, Radix); profundidad máxima de recursión (Quicksort);
 * pasadas (Radix).
 *
 * Uso: java -cp out ordenamiento.ConteoOperaciones [carpetaSalida]  ->  conteo_operaciones.csv
 */
public class ConteoOperaciones {
    static long c, m, prof, profMax;

    static void selection(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) { c++; if (a[j] < a[min]) min = j; }
            int t = a[i]; a[i] = a[min]; a[min] = t; m++;
        }
    }

    static void bubble(int[] a) {
        int n = a.length;
        for (int i = 0; i < n - 1; i++)
            for (int j = 0; j < n - 1 - i; j++) { c++; if (a[j] > a[j + 1]) { int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t; m++; } }
    }

    static void insertion(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0) { c++; if (a[j] > key) { a[j + 1] = a[j]; j--; m++; } else break; }
            a[j + 1] = key;
        }
    }

    static void shell(int[] a) {
        int n = a.length;
        for (int gap = n / 2; gap > 0; gap /= 2)
            for (int i = gap; i < n; i++) {
                int temp = a[i], j = i;
                while (j >= gap) { c++; if (a[j - gap] > temp) { a[j] = a[j - gap]; m++; j -= gap; } else break; }
                a[j] = temp;
            }
    }

    static void merge(int[] a) { if (a.length < 2) return; ms(a, new int[a.length], 0, a.length - 1); }

    static void ms(int[] a, int[] aux, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        ms(a, aux, lo, mid); ms(a, aux, mid + 1, hi);
        for (int k = lo; k <= hi; k++) aux[k] = a[k];
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = aux[j++];
            else if (j > hi) a[k] = aux[i++];
            else { c++; if (aux[j] < aux[i]) a[k] = aux[j++]; else a[k] = aux[i++]; }
            m++;
        }
    }

    static void quick(int[] a) { q(a, 0, a.length - 1); }

    static void q(int[] a, int lo, int hi) {
        prof++; if (prof > profMax) profMax = prof;
        if (lo < hi) {
            int pivot = a[hi], i = lo - 1;
            for (int j = lo; j < hi; j++) { c++; if (a[j] <= pivot) { i++; int t = a[i]; a[i] = a[j]; a[j] = t; m++; } }
            int t = a[i + 1]; a[i + 1] = a[hi]; a[hi] = t; m++;
            q(a, lo, i); q(a, i + 2, hi);
        }
        prof--;
    }

    /** Radix: c = pasadas, m = movimientos (escrituras en salida + copia de vuelta). */
    static void radix(int[] a) {
        int n = a.length; if (n < 2) return;
        int max = 0; for (int v : a) max = Math.max(max, v);
        int d = 1; for (int v = max; v >= 10; v /= 10) d++;
        int[] out = new int[n]; int exp = 1;
        for (int p = 0; p < d; p++) {
            c++;
            int[] cnt = new int[10];
            for (int v : a) cnt[(v / exp) % 10]++;
            for (int k = 1; k < 10; k++) cnt[k] += cnt[k - 1];
            for (int i = n - 1; i >= 0; i--) { int dg = (a[i] / exp) % 10; out[--cnt[dg]] = a[i]; m++; }
            for (int i = 0; i < n; i++) { a[i] = out[i]; m++; }
            if (p < d - 1) exp *= 10;
        }
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "resultados");
        dir.mkdirs();
        String[] nombres = {"Selection", "Bubble", "Insertion", "Shell", "Merge", "Quicksort", "Radix"};
        String[] metM = {"intercambios", "intercambios", "desplazamientos", "desplazamientos", "movimientos", "intercambios", "movimientos"};
        String[] metC = {"comparaciones", "comparaciones", "comparaciones", "comparaciones", "comparaciones", "comparaciones", "pasadas"};
        Algoritmo[] fs = {ConteoOperaciones::selection, ConteoOperaciones::bubble, ConteoOperaciones::insertion,
                ConteoOperaciones::shell, ConteoOperaciones::merge, ConteoOperaciones::quick, ConteoOperaciones::radix};
        try (PrintWriter pw = new PrintWriter(new File(dir, "conteo_operaciones.csv"), "UTF-8")) {
            pw.println("algoritmo,metrica,tipo_entrada,n,valor");
            for (int k = 0; k < fs.length; k++)
                for (TipoEntrada t : TipoEntrada.values())
                    for (int n = 1000; n <= (k < 3 ? 16000 : 1_024_000); n *= 2) {
                        int[] a = GeneradorEntradas.generar(t, n), e = a.clone();
                        Arrays.sort(e);
                        c = m = prof = profMax = 0;
                        try {
                            fs[k].ordenar(a);
                        } catch (StackOverflowError so) {
                            pw.printf("%s,desbordamiento,%s,%d,%n", nombres[k], t.nombre(), n);
                            break;
                        }
                        if (!Arrays.equals(a, e)) throw new IllegalStateException(nombres[k]);
                        pw.printf("%s,%s,%s,%d,%d%n", nombres[k], metC[k], t.nombre(), n, c);
                        pw.printf("%s,%s,%s,%d,%d%n", nombres[k], metM[k], t.nombre(), n, m);
                        if (k == 5) pw.printf("%s,profundidad_max,%s,%d,%d%n", nombres[k], t.nombre(), n, profMax);
                    }
        }
        System.out.println("Conteo listo: " + new File(dir, "conteo_operaciones.csv").getAbsolutePath());
    }
}
