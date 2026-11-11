package ordenamiento.algoritmos;

/**
 * Radix Sort LSD (dígito menos significativo primero) en base 10, sin optimizaciones.
 *
 * Ordena por unidades, luego por decenas, luego por centenas, etc.
 * En cada pasada usa un counting sort estable sobre un solo dígito.
 * Hace d pasadas, donde d es la cantidad de dígitos del valor máximo,
 * así que su costo es O(d · (n + b)), con b = 10 (la base).
 *
 * Solo admite enteros no negativos.
 */
public final class RadixSort {

    private static final int BASE = 10;

    private RadixSort() { }

    public static void sort(int[] a) {
        int n = a.length;
        if (n < 2) {
            return;
        }

        int max = a[0];
        for (int i = 0; i < n; i++) {
            if (a[i] < 0) {
                throw new IllegalArgumentException("RadixSort solo admite enteros no negativos");
            }
            if (a[i] > max) {
                max = a[i];
            }
        }

        int digitos = cantidadDeDigitos(max);
        int[] salida = new int[n];
        int exp = 1;                       // 1, 10, 100, ... indica qué dígito se ordena
        for (int d = 0; d < digitos; d++) {
            countingSortPorDigito(a, salida, exp);
            if (d < digitos - 1) {
                exp *= BASE;               // no se multiplica después de la última pasada (evita desbordar int)
            }
        }
    }

    /** Counting sort estable según el dígito que indica exp. */
    private static void countingSortPorDigito(int[] a, int[] salida, int exp) {
        int n = a.length;
        int[] conteo = new int[BASE];

        // 1. Contar cuántos valores tienen cada dígito (0 a 9).
        for (int i = 0; i < n; i++) {
            int digito = (a[i] / exp) % BASE;
            conteo[digito]++;
        }

        // 2. Suma acumulada: conteo[k] queda como la posición siguiente al último valor con dígito k.
        for (int k = 1; k < BASE; k++) {
            conteo[k] += conteo[k - 1];
        }

        // 3. Recorrer de derecha a izquierda para que el orden sea estable.
        for (int i = n - 1; i >= 0; i--) {
            int digito = (a[i] / exp) % BASE;
            conteo[digito]--;
            salida[conteo[digito]] = a[i];
        }

        // 4. Copiar el resultado de vuelta al arreglo original.
        for (int i = 0; i < n; i++) {
            a[i] = salida[i];
        }
    }

    private static int cantidadDeDigitos(int valor) {
        int digitos = 1;
        while (valor >= BASE) {
            valor /= BASE;
            digitos++;
        }
        return digitos;
    }
}
