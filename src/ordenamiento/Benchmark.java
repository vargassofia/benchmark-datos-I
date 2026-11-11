package ordenamiento;

import java.io.File;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ordenamiento.algoritmos.Algoritmo;
import ordenamiento.algoritmos.RadixSort;
import ordenamiento.entrada.GeneradorEntradas;
import ordenamiento.entrada.TipoEntrada;

/**
 * Benchmark de los 7 algoritmos: misma entrada, misma máquina, misma metodología.
 *
 * - Antes de medir ejecuta la validación de Validacion (36 casos por algoritmo);
 *   si falla, no mide nada.
 * - Para cada (tipo, n) genera UN arreglo (fuera del tiempo) y cada algoritmo
 *   recibe una copia de ese mismo arreglo en cada corrida.
 * - Por cada (algoritmo, tipo, n): 1 corrida de calentamiento (repeticion 0) + 5 medidas,
 *   System.gc() y clone() fuera del tiempo, System.nanoTime() solo alrededor de sort,
 *   verificación contra Arrays.sort después de cada corrida. Se reporta la mediana.
 * - StackOverflowError (Quicksort con pivote fijo): se registra "desbordamiento" y no se
 *   prueban n mayores en esa combinación. Igual si una corrida supera 60 s.
 * - Al final, prueba especial de Radix: n = 100 000 con valores de 1 a 9 dígitos.
 *
 * Salida (en la carpeta indicada, por defecto "resultados"):
 *   entorno.txt, mediciones.csv, resumen.csv, radix_digitos.csv
 *
 * Uso: java -cp out ordenamiento.Benchmark [carpetaSalida]
 */
public class Benchmark {

    static final int REPS = 5;
    static final double LIMITE_S = 60.0;
    static final int N_MAX_CUADRATICOS = 16_000;
    static final int[] TAMANOS = {1000, 2000, 4000, 8000, 16000, 32000, 64000, 128000, 256000, 512000, 1024000};
    static final int N_RADIX = 100_000;

    static boolean cuadratico(String a) {
        return a.equals("Selection") || a.equals("Bubble") || a.equals("Insertion");
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "resultados");
        dir.mkdirs();
        Locale.setDefault(Locale.ROOT);
        Map<String, Algoritmo> algs = Validacion.algoritmos();

        // ---- Entorno
        try (PrintWriter pw = new PrintWriter(new File(dir, "entorno.txt"), "UTF-8")) {
            Runtime rt = Runtime.getRuntime();
            pw.println("java.version=" + System.getProperty("java.version"));
            pw.println("java.vm=" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
            pw.println("os=" + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
            pw.println("cpus=" + rt.availableProcessors());
            pw.println("maxMemoryMB=" + rt.maxMemory() / (1024 * 1024));
            pw.println("jvmArgs=" + ManagementFactory.getRuntimeMXBean().getInputArguments());
        }

        // ---- Validación: si falla, no se mide nada
        if (!Validacion.validarTodos(true)) {
            System.err.println("La validación falló: no se ejecuta el benchmark.");
            System.exit(1);
        }

        // ---- Calentamiento global del JIT (tiempos no registrados)
        int[] base = GeneradorEntradas.generar(TipoEntrada.ALEATORIO, 10_000);
        for (Algoritmo f : algs.values()) for (int k = 0; k < 10; k++) f.ordenar(base.clone());

        // ---- Mediciones
        Set<String> detenidos = new HashSet<>();
        try (PrintWriter raw = new PrintWriter(new File(dir, "mediciones.csv"), "UTF-8");
             PrintWriter sum = new PrintWriter(new File(dir, "resumen.csv"), "UTF-8")) {
            raw.println("algoritmo,tipo_entrada,n,repeticion,tiempo_ns,estado");
            sum.println("algoritmo,tipo_entrada,n,mediana_ns,mediana_ms,razon_t2n_tn,estado");
            Map<String, Long> previa = new HashMap<>();
            for (TipoEntrada tipo : TipoEntrada.values()) {
                for (int n : TAMANOS) {
                    int[] entrada = GeneradorEntradas.generar(tipo, n);     // fuera del tiempo
                    int[] esperado = entrada.clone();
                    Arrays.sort(esperado);
                    for (Map.Entry<String, Algoritmo> e : algs.entrySet()) {
                        String alg = e.getKey(), clave = alg + "|" + tipo.nombre();
                        if (cuadratico(alg) && n > N_MAX_CUADRATICOS) continue;
                        if (detenidos.contains(clave)) continue;
                        long[] t = new long[REPS];
                        String estado = "ok";
                        for (int rep = 0; rep <= REPS; rep++) {
                            int[] copia = entrada.clone();
                            System.gc();
                            long t0 = System.nanoTime(), t1;
                            try {
                                e.getValue().ordenar(copia);
                                t1 = System.nanoTime();
                            } catch (StackOverflowError so) {
                                estado = "desbordamiento";
                                raw.println(alg + "," + tipo.nombre() + "," + n + "," + rep + ",," + estado);
                                break;
                            }
                            if (!Arrays.equals(copia, esperado)) throw new IllegalStateException("Mal ordenado " + clave + " n=" + n);
                            long dt = t1 - t0;
                            raw.println(alg + "," + tipo.nombre() + "," + n + "," + rep + "," + dt + "," + (rep == 0 ? "calentamiento" : "ok"));
                            if (rep > 0) t[rep - 1] = dt;
                            if (dt / 1e9 > LIMITE_S) estado = "limite_tiempo";
                        }
                        if (estado.equals("desbordamiento")) {
                            detenidos.add(clave);
                            sum.println(alg + "," + tipo.nombre() + "," + n + ",,,," + estado);
                            System.out.printf("%-9s %-11s n=%,9d  ** DESBORDAMIENTO DE PILA%n", alg, tipo.nombre(), n);
                            continue;
                        }
                        long med = mediana(t);
                        Long ant = previa.get(clave);
                        String razon = ant == null ? "" : String.format("%.2f", (double) med / ant);
                        previa.put(clave, med);
                        sum.println(alg + "," + tipo.nombre() + "," + n + "," + med + "," + String.format("%.3f", med / 1e6) + "," + razon + "," + estado);
                        System.out.printf("%-9s %-11s n=%,9d  mediana=%10.3f ms  razon=%s%n", alg, tipo.nombre(), n, med / 1e6, razon);
                        if (estado.equals("limite_tiempo")) detenidos.add(clave);
                    }
                }
            }
        }

        // ---- Prueba especial Radix: n = 100 000, valores de 1 a 9 dígitos
        try (PrintWriter raw = new PrintWriter(new File(dir, "radix_digitos.csv"), "UTF-8")) {
            raw.println("digitos,n,repeticion,tiempo_ns,estado");
            for (int d = 1; d <= 9; d++) {
                int[] entrada = GeneradorEntradas.generarConDigitos(N_RADIX, d);
                int[] esperado = entrada.clone();
                Arrays.sort(esperado);
                long[] t = new long[REPS];
                for (int rep = 0; rep <= REPS; rep++) {
                    int[] c = entrada.clone();
                    System.gc();
                    long t0 = System.nanoTime();
                    RadixSort.sort(c);
                    long dt = System.nanoTime() - t0;
                    if (!Arrays.equals(c, esperado)) throw new IllegalStateException("Radix mal d=" + d);
                    raw.println(d + "," + N_RADIX + "," + rep + "," + dt + "," + (rep == 0 ? "calentamiento" : "ok"));
                    if (rep > 0) t[rep - 1] = dt;
                }
                System.out.printf("Radix d=%d n=%d mediana=%.3f ms%n", d, N_RADIX, mediana(t) / 1e6);
            }
        }
        System.out.println("Listo: " + dir.getAbsolutePath());
    }

    static long mediana(long[] v) {
        long[] s = v.clone();
        Arrays.sort(s);
        return s[s.length / 2];
    }
}
