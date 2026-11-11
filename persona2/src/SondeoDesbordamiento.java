import java.util.Locale;

/**
 * Busca a partir de qué tamaño n Quicksort (pivote fijo) se cae por
 * desbordamiento de pila (StackOverflowError) con cada tipo de entrada.
 * Sirve para el punto 6 de la metodología de la Parte 3: reportar el
 * desbordamiento y reducir n en esos casos.
 *
 * Procedimiento por tipo de entrada:
 *   1) n = 1000, 2000, 4000, ... (se duplica, igual que en el benchmark)
 *      hasta que desborda o se llega a LIMITE_N.
 *   2) Si desbordó, se afina con búsqueda binaria entre el último n que
 *      funcionó y el primero que falló, para dar un umbral aproximado.
 *
 * IMPORTANTE:
 *  - Hay que correrlo en la MISMA computadora y con la MISMA configuración de
 *    JVM que el benchmark. El tamaño de pila por defecto depende del sistema.
 *    Para usar otro tamaño: java -Xss2m SondeoDesbordamiento
 *    (y ese mismo -Xss debe usarse al correr el Benchmark).
 *  - El umbral exacto varía un poco entre ejecuciones (el JIT cambia el tamaño
 *    de los marcos de pila). Reportarlo como "aproximadamente".
 *  - Cada intento se corta si tarda más de LIMITE_SEGUNDOS: con entradas
 *    ordenadas Quicksort es O(n^2) y un n grande tardaría una eternidad aunque
 *    no desborde la pila. Eso también se reporta.
 */
public class SondeoDesbordamiento {

    static final int N_INICIAL = 1_000;
    static final int LIMITE_N = 1_024_000;       // 1000 * 2^10, >= 1 000 000
    static final double LIMITE_SEGUNDOS = 30.0;  // tiempo máximo aceptable por intento

    enum Resultado { OK, DESBORDA, MUY_LENTO }

    public static void main(String[] args) {
        System.out.println("Sondeo de desbordamiento de pila de Quicksort (pivote = último)");
        String xss = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
                .stream().filter(s -> s.startsWith("-Xss")).findFirst().orElse("(por defecto del sistema)");
        System.out.println("Tamaño de pila (-Xss): " + xss);
        System.out.println("Java: " + System.getProperty("java.version") + " | SO: " + System.getProperty("os.name"));
        System.out.println();

        // Calentamiento del JIT para que los marcos de pila sean los "reales"
        for (int i = 0; i < 20; i++) {
            QuickSort.sort(GeneradorEntradas.generar(GeneradorEntradas.Tipo.ALEATORIO, 5_000));
        }

        System.out.printf("%-12s | %-34s | %s%n", "Tipo", "Resultado", "Detalle");
        System.out.println("-".repeat(100));

        for (GeneradorEntradas.Tipo tipo : GeneradorEntradas.Tipo.values()) {
            sondear(tipo);
        }
    }

    static void sondear(GeneradorEntradas.Tipo tipo) {
        int ultimoOk = 0;
        int primerFallo = -1;
        Resultado causa = null;

        // 1) Duplicar n hasta que falle o se llegue al límite
        for (int n = N_INICIAL; n <= LIMITE_N; n *= 2) {
            Resultado r = intentar(tipo, n);
            if (r == Resultado.OK) {
                ultimoOk = n;
            } else {
                primerFallo = n;
                causa = r;
                break;
            }
        }

        if (primerFallo == -1) {
            System.out.printf("%-12s | %-34s | n máximo probado=%,d%n",
                    tipo.etiqueta(), "sin problemas", ultimoOk);
            return;
        }

        // 2) Afinar solo si el motivo fue el desbordamiento
        int umbral = -1;
        if (causa == Resultado.DESBORDA) {
            int lo = ultimoOk, hi = primerFallo;       // lo funciona, hi desborda
            while (hi - lo > Math.max(100, lo / 50)) { // tolerancia ~2%
                int mid = lo + (hi - lo) / 2;
                if (intentar(tipo, mid) == Resultado.OK) lo = mid; else hi = mid;
            }
            umbral = lo;
        }

        String etiqueta = causa == Resultado.DESBORDA ? "DESBORDAMIENTO DE PILA" : "demasiado lento (>" + (int) LIMITE_SEGUNDOS + " s)";
        String detalle = String.format("último n que funcionó=%,d | primer n que falló=%,d", ultimoOk, primerFallo);
        if (umbral > 0) detalle += String.format(" | umbral aprox.=%,d", umbral);
        System.out.printf("%-12s | %-34s | %s%n", tipo.etiqueta(), etiqueta, detalle);
    }

    /** Intenta ordenar una entrada de tamaño n; captura el desbordamiento de pila. */
    static Resultado intentar(GeneradorEntradas.Tipo tipo, int n) {
        int[] a = GeneradorEntradas.generar(tipo, n);
        long inicio = System.nanoTime();
        try {
            QuickSort.sort(a);
        } catch (StackOverflowError e) {
            return Resultado.DESBORDA;
        }
        double seg = (System.nanoTime() - inicio) / 1e9;
        return seg > LIMITE_SEGUNDOS ? Resultado.MUY_LENTO : Resultado.OK;
    }
}
