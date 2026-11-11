package ordenamiento.entrada;

import java.util.Arrays;
import java.util.Random;

/**
 * Genera los arreglos de entrada del benchmark.
 *
 * Usa una semilla fija: con el mismo tipo, n y semilla siempre se obtiene
 * exactamente el mismo arreglo, así el experimento se puede repetir.
 * La generación NO se mide: el benchmark genera cada arreglo una sola vez
 * y le pasa una copia a cada algoritmo en cada repetición.
 */
public final class GeneradorEntradas {

    /** Semilla por defecto del experimento. */
    public static final long SEMILLA = 42L;

    /** Valores entre 0 y 999 999 (inclusive). */
    public static final int MAXIMO_VALOR = 999_999;

    /** Para la entrada con muchos duplicados: valores entre 0 y 10 (inclusive). */
    public static final int MAXIMO_DUPLICADOS = 10;

    private GeneradorEntradas() { }

    public static int[] generar(TipoEntrada tipo, int n) {
        return generar(tipo, n, SEMILLA);
    }

    public static int[] generar(TipoEntrada tipo, int n, long semilla) {
        if (n < 0) {
            throw new IllegalArgumentException("n no puede ser negativo");
        }
        Random random = new Random(semilla);
        switch (tipo) {
            case ALEATORIO:
                return aleatorio(n, MAXIMO_VALOR, random);
            case ASCENDENTE:
                return ascendente(n, random);
            case DESCENDENTE:
                return descendente(n, random);
            case DUPLICADOS:
                return aleatorio(n, MAXIMO_DUPLICADOS, random);
            default:
                throw new IllegalArgumentException("Tipo de entrada desconocido: " + tipo);
        }
    }

    /**
     * Para la prueba especial de Radix: n valores aleatorios entre 0 y 10^digitos - 1.
     * Ejemplo: digitos = 3 -> valores entre 0 y 999; digitos = 9 -> entre 0 y 999 999 999.
     */
    public static int[] generarConDigitos(int n, int digitos, long semilla) {
        if (digitos < 1 || digitos > 9) {
            throw new IllegalArgumentException("digitos debe estar entre 1 y 9");
        }
        int maximo = 1;
        for (int i = 0; i < digitos; i++) {
            maximo *= 10;
        }
        return aleatorio(n, maximo - 1, new Random(semilla));
    }

    public static int[] generarConDigitos(int n, int digitos) {
        return generarConDigitos(n, digitos, SEMILLA);
    }

    // ---------------------------------------------------------------

    /** n valores aleatorios entre 0 y maximo (inclusive). */
    private static int[] aleatorio(int n, int maximo, Random random) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt(maximo + 1);
        }
        return a;
    }

    private static int[] ascendente(int n, Random random) {
        int[] a = aleatorio(n, MAXIMO_VALOR, random);
        Arrays.sort(a);
        return a;
    }

    private static int[] descendente(int n, Random random) {
        int[] a = ascendente(n, random);
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }
}
