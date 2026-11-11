import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Pruebas de correctitud + benchmark de Selection, Bubble (sin bandera) e Insertion.
 * Uso: java Benchmark [carpeta_de_salida]
 * Genera: resultados_crudos.csv (todas las mediciones) y resultados_resumen.csv (mediana y razon t(2n)/t(n)).
 */

public class Benchmark {

    static final long SEED = 42L;
    static final int REPS = 5;            // mediciones por (algoritmo, tipo, n); la ejecucion de calentamiento se descarta
    static final int[] SIZES = {1000, 2000, 4000, 8000, 16000};
    static final String[] TYPES = {"aleatorio", "ascendente", "descendente", "duplicados"};
    static final String[] ALGS = {"selection", "bubble", "insertion"};

    /** Genera SIEMPRE el mismo arreglo para un (tipo, n): asi todos los algoritmos usan la misma entrada. */
    static int[] generate(String type, int n) {
        Random r = new Random(SEED + n);
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = type.equals("duplicados") ? r.nextInt(11) : r.nextInt(1_000_000);   // 0..10  /  0..999 999
        if (type.equals("ascendente") || type.equals("descendente")) {
            Arrays.sort(a);
            if (type.equals("descendente"))
                for (int i = 0; i < n / 2; i++) { int t = a[i]; a[i] = a[n - 1 - i]; a[n - 1 - i] = t; }
        }
        return a;
    }

    static Consumer<int[]> algorithm(String name) {
        switch (name) {
            case "selection": return SortingAlgorithms::selectionSort;
            case "bubble":    return SortingAlgorithms::bubbleSort;
            default:          return SortingAlgorithms::insertionSort;
        }
    }

    /** Valida cada algoritmo contra Arrays.sort (biblioteca estandar). */
    static void runTests() {
        Random r = new Random(7);
        for (String alg : ALGS) {
            Consumer<int[]> f = algorithm(alg);
            for (int len : new int[]{0, 1, 2, 3, 10, 100}) {                 // casos pequenos y borde
                for (int k = 0; k < 200; k++) {
                    int[] a = new int[len];
                    for (int i = 0; i < len; i++) a[i] = r.nextInt(20) - 5; // incluye repetidos y negativos
                    int[] exp = a.clone(); Arrays.sort(exp);
                    f.accept(a);
                    if (!Arrays.equals(a, exp)) throw new AssertionError(alg + " fallo con len=" + len);
                }
            }
            for (String t : TYPES) {                                          // los 4 tipos de entrada
                int[] a = generate(t, 2000), exp = a.clone();
                Arrays.sort(exp); f.accept(a);
                if (!Arrays.equals(a, exp)) throw new AssertionError(alg + " fallo con " + t);
            }
            System.out.println("[OK] " + alg + " ordena igual que Arrays.sort");
        }
    }

    /** Calentamiento global de la JVM (JIT) antes de medir; sus tiempos no se registran. */
    static void warmUp() {
        int[] base = generate("aleatorio", 2000);
        for (String alg : ALGS) {
            Consumer<int[]> f = algorithm(alg);
            for (int k = 0; k < 30; k++) f.accept(base.clone());
        }
    }

    static double median(long[] v) {
        long[] s = v.clone(); Arrays.sort(s);
        return s.length % 2 == 1 ? s[s.length / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2.0;
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        runTests();
        warmUp();

        try (PrintWriter raw = new PrintWriter(new File(dir, "resultados_crudos.csv"));
             PrintWriter sum = new PrintWriter(new File(dir, "resultados_resumen.csv"))) {
            raw.println("algoritmo,tipo_entrada,n,repeticion,tiempo_ns");
            sum.println("algoritmo,tipo_entrada,n,mediana_ms,razon_t(n)/t(n/2)");
            for (String alg : ALGS) {
                Consumer<int[]> f = algorithm(alg);
                for (String type : TYPES) {
                    double prev = Double.NaN;
                    for (int n : SIZES) {
                        int[] input = generate(type, n);                       // generacion FUERA del tiempo medido
                        long[] t = new long[REPS];
                        for (int rep = 0; rep <= REPS; rep++) {                // rep 0 = calentamiento JIT (se descarta)
                            int[] copy = input.clone();                        // la copia tambien queda fuera del tiempo
                            long t0 = System.nanoTime();
                            f.accept(copy);
                            long t1 = System.nanoTime();
                            if (rep > 0) { t[rep - 1] = t1 - t0; raw.println(alg + "," + type + "," + n + "," + rep + "," + (t1 - t0)); }
                        }
                        double med = median(t);
                        String ratio = Double.isNaN(prev) ? "" : String.format(Locale.ROOT, "%.2f", med / prev);
                        sum.println(alg + "," + type + "," + n + "," + String.format(Locale.ROOT, "%.3f", med / 1e6) + "," + ratio);
                        System.out.printf(Locale.ROOT, "%-9s %-12s n=%-6d mediana=%10.3f ms  razon=%s%n", alg, type, n, med / 1e6, ratio);
                        prev = med;
                    }
                }
            }
        }
    }
}

