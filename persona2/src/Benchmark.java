import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Benchmark de Shell, Merge y Quicksort (Parte 3 de la tarea).
 *
 * Metodología (ver el PDF de la tarea):
 *  - Se mide SOLO el ordenamiento: la entrada se genera y se copia fuera del
 *    cronómetro.
 *  - Todos los algoritmos usan exactamente la misma entrada para cada (tipo, n).
 *  - Para cada (algoritmo, tipo, n): 1 ejecución de calentamiento (JIT) que se
 *    DESCARTA + 5 mediciones. Se reporta la mediana.
 *  - n se duplica: 1000, 2000, 4000, ... hasta N_MAXIMO (por defecto 1 024 000).
 *  - Si Quicksort desborda la pila, o una ejecución tarda más de
 *    LIMITE_SEGUNDOS, se detiene ese (algoritmo, tipo) y se registra en
 *    incidencias.csv. Esa es la "reducción de n" que pide la tarea.
 *
 * Archivos que genera (en la carpeta de salida):
 *  - mediciones.csv : TODAS las mediciones individuales (sin mediana).
 *  - resumen.csv    : mediana por (algoritmo, tipo, n) y razón t(2n)/t(n).
 *  - incidencias.csv: desbordamientos de pila o cortes por lentitud.
 *
 * Uso:   java Benchmark [nMaximo] [carpetaSalida] [limiteSegundos]
 * Ej.:   java Benchmark 1024000 resultados 10
 *
 * Para integrarlo con los algoritmos de la Persona 3 basta agregarlos en
 * algoritmos() con el mismo formato (nombre -> función que ordena int[]).
 */
public class Benchmark {

    static final int REPETICIONES = 5;
    static final int N_INICIAL = 1_000;

    static int nMaximo = 1_024_000;
    static double limiteSegundos = 10.0;
    static Path carpeta = Paths.get("resultados");

    /** Resultado de una medición de un (algoritmo, tipo, n). */
    static class Medicion {
        long[] tiemposNs;     // las REPETICIONES mediciones
        boolean desbordo;     // StackOverflowError
        boolean muyLento;     // el calentamiento superó el límite de tiempo
    }

    static Map<String, Consumer<int[]>> algoritmos() {
        Map<String, Consumer<int[]>> m = new LinkedHashMap<>();
        m.put("Shell", ShellSort::sort);
        m.put("Merge", MergeSort::sort);
        m.put("Quicksort", QuickSort::sort);
        return m;
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) nMaximo = Integer.parseInt(args[0]);
        if (args.length > 1) carpeta = Paths.get(args[1]);
        if (args.length > 2) limiteSegundos = Double.parseDouble(args[2]);
        Files.createDirectories(carpeta);

        Map<String, Consumer<int[]>> algs = algoritmos();
        Set<String> detenidos = new HashSet<>();        // combinaciones "algoritmo|tipo" ya cortadas

        List<String> filasMediciones = new ArrayList<>();
        List<String> filasResumen = new ArrayList<>();
        List<String> filasIncidencias = new ArrayList<>();
        Map<String, Long> medianaAnterior = new LinkedHashMap<>();   // para la razón t(2n)/t(n)

        System.out.printf("Benchmark: n de %,d a %,d | %d repeticiones + 1 calentamiento | límite %.0f s%n%n",
                N_INICIAL, nMaximo, REPETICIONES, limiteSegundos);

        for (GeneradorEntradas.Tipo tipo : GeneradorEntradas.Tipo.values()) {
            for (int n = N_INICIAL; n <= nMaximo; n *= 2) {
                int[] entrada = GeneradorEntradas.generar(tipo, n);     // fuera del cronómetro

                for (Map.Entry<String, Consumer<int[]>> alg : algs.entrySet()) {
                    String nombre = alg.getKey();
                    String clave = nombre + "|" + tipo.etiqueta();
                    if (detenidos.contains(clave)) continue;

                    Medicion med = medir(alg.getValue(), entrada);

                    if (med.desbordo || med.muyLento) {
                        String motivo = med.desbordo ? "DESBORDAMIENTO_PILA" : "MUY_LENTO(>" + limiteSegundos + "s)";
                        detenidos.add(clave);
                        filasIncidencias.add(String.join(",", nombre, tipo.etiqueta(), String.valueOf(n), motivo));
                        System.out.printf("%-10s %-11s n=%,10d  ** %s: se deja de crecer n para esta combinación%n",
                                nombre, tipo.etiqueta(), n, motivo);
                        continue;
                    }

                    for (int r = 0; r < med.tiemposNs.length; r++) {
                        filasMediciones.add(String.join(",", nombre, tipo.etiqueta(),
                                String.valueOf(n), String.valueOf(r + 1), String.valueOf(med.tiemposNs[r])));
                    }

                    long mediana = mediana(med.tiemposNs);
                    Long anterior = medianaAnterior.get(clave);
                    // La razón solo aplica si el tamaño anterior (n/2) también se midió
                    String razon = (anterior != null && n > N_INICIAL) ? String.format(Locale.US, "%.4f", (double) mediana / anterior) : "";
                    medianaAnterior.put(clave, mediana);

                    filasResumen.add(String.join(",", nombre, tipo.etiqueta(), String.valueOf(n),
                            String.valueOf(mediana), String.format(Locale.US, "%.4f", mediana / 1e6), razon));

                    System.out.printf(Locale.US, "%-10s %-11s n=%,10d  mediana=%12.3f ms  razon=%s%n",
                            nombre, tipo.etiqueta(), n, mediana / 1e6, razon.isEmpty() ? "-" : razon);
                }
            }
            System.out.println();
        }

        escribir("mediciones.csv", "algoritmo,tipo_entrada,n,repeticion,tiempo_ns", filasMediciones);
        escribir("resumen.csv", "algoritmo,tipo_entrada,n,mediana_ns,mediana_ms,razon_t2n_tn", filasResumen);
        escribir("incidencias.csv", "algoritmo,tipo_entrada,n,motivo", filasIncidencias);

        System.out.println("Archivos escritos en: " + carpeta.toAbsolutePath());
        if (!filasIncidencias.isEmpty()) {
            System.out.println("Incidencias (reportar en el informe):");
            for (String f : filasIncidencias) System.out.println("  " + f);
        }
    }

    /**
     * Mide un algoritmo sobre una entrada: 1 calentamiento descartado + REPETICIONES mediciones.
     * Cada ejecución trabaja sobre una copia fresca; la copia NO se cuenta en el tiempo.
     */
    static Medicion medir(Consumer<int[]> sort, int[] entrada) {
        Medicion med = new Medicion();
        try {
            // Calentamiento (JIT): se descarta, pero sirve para detectar lentitud extrema
            int[] copia = entrada.clone();
            long t0 = System.nanoTime();
            sort.accept(copia);
            double seg = (System.nanoTime() - t0) / 1e9;
            verificarOrdenado(copia);
            if (seg > limiteSegundos) {
                med.muyLento = true;
                return med;
            }

            med.tiemposNs = new long[REPETICIONES];
            for (int r = 0; r < REPETICIONES; r++) {
                copia = entrada.clone();                    // fuera del cronómetro
                long inicio = System.nanoTime();
                sort.accept(copia);
                med.tiemposNs[r] = System.nanoTime() - inicio;
                verificarOrdenado(copia);                   // fuera del cronómetro
            }
        } catch (StackOverflowError e) {
            med.desbordo = true;
        }
        return med;
    }

    /** Seguro extra: si algún algoritmo dejara el arreglo mal ordenado, se detiene todo. */
    static void verificarOrdenado(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) throw new IllegalStateException("El algoritmo dejó el arreglo desordenado (índice " + i + ")");
        }
    }

    static long mediana(long[] v) {
        long[] s = v.clone();
        Arrays.sort(s);
        int m = s.length / 2;
        return s.length % 2 == 1 ? s[m] : (s[m - 1] + s[m]) / 2;
    }

    static void escribir(String nombre, String encabezado, List<String> filas) throws IOException {
        Path destino = carpeta.resolve(nombre);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(destino, StandardCharsets.UTF_8))) {
            pw.println(encabezado);
            for (String f : filas) pw.println(f);
        }
    }
}
