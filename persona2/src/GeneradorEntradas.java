import java.util.Random;

/**
 * Genera los 4 tipos de entrada de la tarea (enteros entre 0 y 999 999).
 * Usa una semilla fija: la misma (tipo, n) produce SIEMPRE el mismo arreglo,
 * así todos los algoritmos se miden con exactamente la misma entrada.
 *
 * Si la Persona 3 ya tiene su propio generador, se puede usar el de ella; lo
 * importante es que ambos usen los mismos tipos y rangos.
 */
public class GeneradorEntradas {

    public enum Tipo {
        ALEATORIO, ASCENDENTE, DESCENDENTE, DUPLICADOS;

        /** Nombre tal como aparece en los CSV y en las gráficas. */
        public String etiqueta() {
            return name().toLowerCase();
        }
    }

    public static final int MAX_VALOR = 999_999;
    public static final int MAX_VALOR_DUPLICADOS = 10;   // valores entre 0 y 10
    private static final long SEMILLA = 2026L;

    public static int[] generar(Tipo tipo, int n) {
        // Semilla distinta por tipo y tamaño, pero siempre la misma para igual (tipo, n)
        Random rnd = new Random(SEMILLA + 31L * tipo.ordinal() + n);
        int[] a = new int[n];

        switch (tipo) {
            case ALEATORIO:
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt(MAX_VALOR + 1);
                break;
            case ASCENDENTE:
                a = ordenadoAscendente(n, rnd);
                break;
            case DESCENDENTE:
                a = ordenadoAscendente(n, rnd);
                for (int i = 0; i < n / 2; i++) {
                    int t = a[i];
                    a[i] = a[n - 1 - i];
                    a[n - 1 - i] = t;
                }
                break;
            case DUPLICADOS:
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt(MAX_VALOR_DUPLICADOS + 1);
                break;
        }
        return a;
    }

    /**
     * Arreglo no decreciente con valores en [0, 999 999]: se generan valores
     * aleatorios y se ordenan con la biblioteca estándar (esto NO se mide).
     */
    private static int[] ordenadoAscendente(int n, Random rnd) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = rnd.nextInt(MAX_VALOR + 1);
        java.util.Arrays.sort(a);
        return a;
    }
}
