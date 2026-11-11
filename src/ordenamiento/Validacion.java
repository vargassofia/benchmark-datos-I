package ordenamiento;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import ordenamiento.algoritmos.Algoritmo;
import ordenamiento.algoritmos.BubbleSort;
import ordenamiento.algoritmos.InsertionSort;
import ordenamiento.algoritmos.MergeSort;
import ordenamiento.algoritmos.QuickSort;
import ordenamiento.algoritmos.RadixSort;
import ordenamiento.algoritmos.SelectionSort;
import ordenamiento.algoritmos.ShellSort;
import ordenamiento.entrada.GeneradorEntradas;
import ordenamiento.entrada.TipoEntrada;

/**
 * Valida los 7 algoritmos contra Arrays.sort (algoritmo de referencia de la
 * biblioteca estándar de Java).
 *
 * 1. Validación oficial (sección 2.2 / 3.5 del informe): 36 casos por algoritmo,
 *    los 4 tipos de entrada con n = 0, 1, 2, 3, 10, 100, 1 000 y 5 000 (32 casos)
 *    más 4 casos fijos. El benchmark la ejecuta antes de medir y no mide nada si falla.
 * 2. Batería ampliada: casos borde, arreglos con muchos repetidos y 2 000 arreglos
 *    aleatorios de tamaño y rango variables.
 *
 * Uso: java -cp out ordenamiento.Validacion   (termina con código 1 si algo falla)
 */
public class Validacion {

    static final int[] TAMANOS = {0, 1, 2, 3, 10, 100, 1000, 5000};
    static final int[][] FIJOS = {
            {5, 5, 5, 5, 5, 5},                    // todos iguales
            {3, 1, 2},                             // tres elementos desordenados
            {0, 999_999, 0, 999_999},              // extremos del rango
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}         // descendente corto
    };

    /** Los 7 algoritmos, en el orden en que aparecen en el informe. */
    public static Map<String, Algoritmo> algoritmos() {
        Map<String, Algoritmo> m = new LinkedHashMap<>();
        m.put("Selection", SelectionSort::sort);
        m.put("Bubble", BubbleSort::sort);
        m.put("Insertion", InsertionSort::sort);
        m.put("Shell", ShellSort::sort);
        m.put("Merge", MergeSort::sort);
        m.put("Quicksort", QuickSort::sort);
        m.put("Radix", RadixSort::sort);
        return m;
    }

    public static void main(String[] args) {
        boolean ok = validarTodos(true);
        System.out.println();
        System.out.println("== Batería ampliada ==");
        ok &= bateriaAmpliada();
        System.out.println();
        System.out.println(ok ? "Todo correcto." : "HAY ERRORES.");
        if (!ok) System.exit(1);
    }

    /** Validación oficial de 36 casos por algoritmo. Devuelve false si algún caso falla. */
    public static boolean validarTodos(boolean imprimir) {
        boolean todoBien = true;
        for (Map.Entry<String, Algoritmo> e : algoritmos().entrySet()) {
            int casos = 0, fallos = 0;
            for (TipoEntrada t : TipoEntrada.values()) {
                for (int n : TAMANOS) {
                    casos++;
                    if (!ordenaBien(e.getValue(), GeneradorEntradas.generar(t, n))) {
                        fallos++;
                        System.out.printf("  [FALLO] %s - %s n=%d%n", e.getKey(), t.nombre(), n);
                    }
                }
            }
            for (int[] f : FIJOS) {
                casos++;
                if (!ordenaBien(e.getValue(), f)) {
                    fallos++;
                    System.out.printf("  [FALLO] %s - caso fijo %s%n", e.getKey(), Arrays.toString(f));
                }
            }
            if (imprimir) {
                System.out.printf("[%s] %s ordena igual que Arrays.sort (%d casos)%n",
                        fallos == 0 ? "OK" : "FALLO", e.getKey(), casos);
            }
            todoBien &= fallos == 0;
        }
        return todoBien;
    }

    /** Casos borde, repetidos y 2 000 arreglos aleatorios de tamaño y rango variables. */
    static boolean bateriaAmpliada() {
        int[][] borde = {
                {}, {7}, {1, 2}, {2, 1}, {3, 1, 2}, {5, 4, 3, 2, 1}, {1, 2, 3, 4, 5},
                {0, 999_999, 0, 999_999}, {4, 4}, {4, 4, 4, 4, 4, 4, 4, 4},
                {2, 1, 2, 1, 2, 1, 2, 1}, {3, 3, 1, 1, 2, 2, 3, 1, 2}, {9, 5, 5, 9, 5, 9, 9, 5, 0, 0}
        };
        int[] iguales = new int[1000];
        Arrays.fill(iguales, 42);

        boolean todoBien = true;
        for (Map.Entry<String, Algoritmo> e : algoritmos().entrySet()) {
            int pruebas = 0, fallos = 0;
            for (int[] c : borde) {
                pruebas++;
                if (!ordenaBien(e.getValue(), c)) fallos++;
            }
            pruebas++;
            if (!ordenaBien(e.getValue(), iguales)) fallos++;

            Random rnd = new Random(12345);
            for (int t = 0; t < 2000; t++) {
                int n = rnd.nextInt(200);
                int rango = 1 + rnd.nextInt(rnd.nextBoolean() ? 5 : 1_000_000); // rango chico = muchos duplicados
                int[] a = new int[n];
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt(rango);
                pruebas++;
                if (!ordenaBien(e.getValue(), a)) fallos++;
            }
            System.out.printf("%-10s %s (%d pruebas, %d fallos)%n", e.getKey(), fallos == 0 ? "OK" : "FALLÓ", pruebas, fallos);
            todoBien &= fallos == 0;
        }
        return todoBien;
    }

    /** Ordena una copia con el algoritmo y otra con Arrays.sort, y las compara. */
    static boolean ordenaBien(Algoritmo alg, int[] entrada) {
        int[] obtenido = entrada.clone();
        int[] esperado = entrada.clone();
        alg.ordenar(obtenido);
        Arrays.sort(esperado);
        return Arrays.equals(obtenido, esperado);
    }
}
