/**
 * Cuenta operaciones EXACTAS (comparaciones y movimientos) de cada algoritmo.
 * Estos valores NO dependen de la computadora: deben coincidir exactamente en cualquier maquina.
 * Uso: java OperationCounter
 */
public class OperationCounter {

    /** @return {comparaciones, movimientos} */
    static long[] selection(int[] a) {
        long c = 0, m = 0;
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) { c++; if (a[j] < a[min]) min = j; }
            int t = a[i]; a[i] = a[min]; a[min] = t; m++;      // 1 intercambio por pasada
        }
        return new long[]{c, m};
    }

    static long[] bubble(int[] a) {
        long c = 0, m = 0;
        int n = a.length;
        for (int i = 0; i < n - 1; i++)
            for (int j = 0; j < n - 1 - i; j++) {
                c++;
                if (a[j] > a[j + 1]) { int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t; m++; }
            }
        return new long[]{c, m};
    }

    static long[] insertion(int[] a) {
        long c = 0, m = 0;
        for (int i = 1; i < a.length; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0) {
                c++;
                if (a[j] > key) { a[j + 1] = a[j]; j--; m++; } else break;
            }
            a[j + 1] = key;
        }
        return new long[]{c, m};
    }

    public static void main(String[] args) {
        int[] sizes = {1000, 2000, 4000, 8000, 16000};
        String[] types = {"aleatorio", "ascendente", "descendente", "duplicados"};
        System.out.println("algoritmo,tipo,n,comparaciones,movimientos");
        for (String type : types)
            for (int n : sizes) {
                int[] base = Benchmark.generate(type, n);
                System.out.printf("selection,%s,%d,%s%n", type, n, fmt(selection(base.clone())));
                System.out.printf("bubble,%s,%d,%s%n", type, n, fmt(bubble(base.clone())));
                System.out.printf("insertion,%s,%d,%s%n", type, n, fmt(insertion(base.clone())));
            }
    }

    static String fmt(long[] r) { return r[0] + "," + r[1]; }
}
