import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Pruebas de corrección de Shell, Merge y QuickSort.
 * Cada resultado se compara contra Arrays.sort (algoritmo de referencia de la
 * biblioteca estándar, como pide la tarea).
 *
 * No necesita JUnit: se ejecuta como programa normal y termina con código 1
 * si alguna prueba falla.
 *
 * Los tamaños de los arreglos ordenados / con duplicados se mantienen pequeños
 * para QuickSort a propósito: ahí degenera a O(n^2) con recursión profunda.
 * El límite real se mide con SondeoDesbordamiento.
 */
public class PruebasOrdenamiento {

    static int pruebas = 0;
    static int fallos = 0;

    /** Algoritmos a probar. Si la Persona 3 agrega los suyos, se pueden poner aquí. */
    static Map<String, Consumer<int[]>> algoritmos() {
        Map<String, Consumer<int[]>> m = new LinkedHashMap<>();
        m.put("Shell", ShellSort::sort);
        m.put("Merge", MergeSort::sort);
        m.put("Quicksort", QuickSort::sort);
        return m;
    }

    public static void main(String[] args) {
        for (Map.Entry<String, Consumer<int[]>> alg : algoritmos().entrySet()) {
            String nombre = alg.getKey();
            Consumer<int[]> sort = alg.getValue();
            int fallosAntes = fallos;

            casosBorde(nombre, sort);
            casosConDuplicados(nombre, sort);
            tiposDeLaTarea(nombre, sort);
            pruebaAleatoriaMasiva(nombre, sort);

            System.out.printf("%-10s %s%n", nombre, fallos == fallosAntes ? "OK" : "FALLÓ");
        }

        System.out.printf("%nPruebas ejecutadas: %d | Fallos: %d%n", pruebas, fallos);
        if (fallos > 0) System.exit(1);
    }

    // ---------------------------------------------------------------- casos

    /** Arreglos vacío, de 1 y 2 elementos, etc. */
    static void casosBorde(String nombre, Consumer<int[]> sort) {
        verificar(nombre, sort, new int[]{}, "vacío");
        verificar(nombre, sort, new int[]{7}, "un elemento");
        verificar(nombre, sort, new int[]{1, 2}, "dos ordenados");
        verificar(nombre, sort, new int[]{2, 1}, "dos invertidos");
        verificar(nombre, sort, new int[]{3, 1, 2}, "tres elementos");
        verificar(nombre, sort, new int[]{5, 4, 3, 2, 1}, "descendente corto");
        verificar(nombre, sort, new int[]{1, 2, 3, 4, 5}, "ascendente corto");
        verificar(nombre, sort, new int[]{0, 999_999, 0, 999_999}, "extremos del rango");
    }

    /** Arreglos con elementos repetidos (lo que más suele romper a Quicksort). */
    static void casosConDuplicados(String nombre, Consumer<int[]> sort) {
        verificar(nombre, sort, new int[]{4, 4}, "dos iguales");
        verificar(nombre, sort, new int[]{4, 4, 4, 4, 4, 4, 4, 4}, "todos iguales");
        verificar(nombre, sort, new int[]{2, 1, 2, 1, 2, 1, 2, 1}, "dos valores alternados");
        verificar(nombre, sort, new int[]{3, 3, 1, 1, 2, 2, 3, 1, 2}, "pocos valores repetidos");
        verificar(nombre, sort, new int[]{9, 5, 5, 9, 5, 9, 9, 5, 0, 0}, "repetidos con extremos");

        int[] iguales = new int[1000];
        Arrays.fill(iguales, 42);
        verificar(nombre, sort, iguales, "1000 elementos iguales");
    }

    /** Los 4 tipos de la tarea con varios tamaños (moderados, ver nota arriba). */
    static void tiposDeLaTarea(String nombre, Consumer<int[]> sort) {
        int[] tamanos = {10, 100, 1000, 2000};
        for (GeneradorEntradas.Tipo tipo : GeneradorEntradas.Tipo.values()) {
            for (int n : tamanos) {
                verificar(nombre, sort, GeneradorEntradas.generar(tipo, n),
                        tipo.etiqueta() + " n=" + n);
            }
        }
        // Aleatorio y con duplicados sí se pueden probar más grande
        verificar(nombre, sort, GeneradorEntradas.generar(GeneradorEntradas.Tipo.ALEATORIO, 100_000),
                "aleatorio n=100000");
    }

    /** Muchos arreglos aleatorios de tamaño y rango de valores variables. */
    static void pruebaAleatoriaMasiva(String nombre, Consumer<int[]> sort) {
        Random rnd = new Random(12345);
        for (int t = 0; t < 2000; t++) {
            int n = rnd.nextInt(200);
            int rango = 1 + rnd.nextInt(rnd.nextBoolean() ? 5 : 1_000_000); // rango chico = muchos duplicados
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(rango);
            verificar(nombre, sort, a, "aleatorio #" + t + " (n=" + n + ", rango=" + rango + ")");
        }
    }

    // ------------------------------------------------------------- utilidades

    /** Ordena una copia con el algoritmo y la compara contra Arrays.sort. */
    static void verificar(String nombre, Consumer<int[]> sort, int[] entrada, String descripcion) {
        pruebas++;
        int[] obtenido = entrada.clone();
        int[] esperado = entrada.clone();

        sort.accept(obtenido);
        Arrays.sort(esperado);

        if (!Arrays.equals(obtenido, esperado)) {
            fallos++;
            System.out.printf("  [FALLO] %s - %s%n", nombre, descripcion);
            if (entrada.length <= 20) {
                System.out.println("     entrada : " + Arrays.toString(entrada));
                System.out.println("     obtenido: " + Arrays.toString(obtenido));
                System.out.println("     esperado: " + Arrays.toString(esperado));
            }
        }
    }
}
