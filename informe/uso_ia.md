# Documentación del uso de IA (Parte 2)

Herramienta: Claude (Anthropic), mediante Claude Code y el chat. La IA se usó para generar el
código. El análisis, la interpretación de los resultados y las explicaciones del informe son
del grupo.

## Prompts principales

| # | Para qué | Prompt principal |
|---|----------|------------------|
| 1 | Selection, Bubble e Insertion | "Implementa en Java Selection Sort, Bubble Sort e Insertion Sort sin ninguna optimización (Bubble sin bandera de corte, Insertion sin búsqueda binaria), cada uno como public static void sort(int[] a). Agrega pruebas que comparen el resultado contra Arrays.sort con arreglos vacíos, de un elemento, con repetidos y con los 4 tipos de entrada de la tarea." |
| 2 | Shell, Merge y Quicksort | "Necesito Shell Sort con los saltos originales n/2, n/4, …, 1, Merge Sort top-down y Quicksort con pivote fijo en el último elemento (Lomuto), en Java y sin optimizaciones. Explica en comentarios qué se considera optimización y por qué no se usa." |
| 3 | Radix | "Escribe Radix Sort LSD en base 10 para enteros no negativos, usando counting sort estable por dígito. Tiene que soportar valores hasta 999 999 999 sin desbordar el int." |
| 4 | Generador de entradas | "Realiza un generador de arreglos de enteros entre 0 y 999 999 con cuatro tipos: aleatorio, ascendente, descendente y con muchos duplicados (0 a 10). Con semilla fija para que siempre salga la misma entrada. También una función para generar n valores de hasta d dígitos (para la prueba de Radix)." |
| 5 | Benchmark | "Escribe el programa de benchmark: medir solo el ordenamiento con System.nanoTime, la misma entrada para todos los algoritmos, una corrida de calentamiento descartada más 5 corridas medidas, reportar la mediana, duplicar n desde 1 000 hasta 16 000 (cuadráticos) o 1 024 000 (los demás), capturar StackOverflowError en Quicksort y dejar de crecer n en esa combinación, y escribir un CSV con todas las mediciones individuales." |
| 6 | Conteo de operaciones | "Haz copias instrumentadas de los algoritmos que cuenten comparaciones, intercambios/desplazamientos y la profundidad máxima de recursión de Quicksort, sin tocar las clases originales para no afectar las mediciones." |
| 7 | Gráficas | "Con el resumen del CSV, genera una gráfica log-log de todos los algoritmos sobre entrada aleatoria, y una gráfica por tipo de entrada para Insertion y para Quicksort marcando el n donde se desbordó la pila." |

## Lo que se tuvo que corregir o ajustar

- **Bubble con bandera.** La primera versión traía la bandera `swapped` que corta cuando no hubo
  intercambios. Es una optimización que cambia el mejor caso a O(n); se quitó.
- **Selection con intercambio condicional.** La IA ponía `if (min != i)` antes del intercambio.
  Se dejó incondicional para que siempre haga n−1 intercambios.
- **Quicksort con pivote "mejorado".** Una versión sugería mediana de tres y otra recurría primero
  sobre el lado más pequeño. Se quitaron: el enunciado pide pivote fijo.
- **Merge Sort creando arreglos en cada mezcla.** Hacía `new int[]` en cada llamada a `merge`,
  lo que mete al recolector de basura en las mediciones. Se cambió a un solo arreglo auxiliar.
- **Desbordamiento del int en Radix.** Multiplicar `exp *= 10` después de la última pasada se
  sale del rango de `int` cuando el máximo tiene 10 dígitos (≥ 1 000 000 000, hasta
  `Integer.MAX_VALUE`). Se ajustó para no multiplicar después de la última pasada, y se agregó una
  excepción para valores negativos.
- **Tres generadores de entrada distintos** (semillas 42 + n, 2026 + 31·tipo + n y 42). Violaba la
  regla de "exactamente la misma entrada". Se unificó en un solo `GeneradorEntradas` con semilla 42
  y se volvieron a correr todas las mediciones.
- **Dos benchmarks separados** (cuadráticos por un lado, Shell/Merge/Quicksort por otro) con CSV
  distintos. Se integraron en `Benchmark.java` con un solo formato de CSV.
- **Script de gráficas en Python.** La IA propuso matplotlib; para no depender de Python se pidió
  una versión en Java2D (`Graficas.java`).
- **Limpieza del repositorio.** Se quitaron archivos `.class`, archivos duplicados y un repositorio
  anidado que se habían subido por error, y se agregó `.gitignore`.
