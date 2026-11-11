# Tarea extraclase #2 – Parte de la Persona 2 (Shell, Merge, Quicksort)

Código en Java (JDK 17 o superior; probado con JDK 21).

## Archivos

| Archivo | Qué hace |
|---|---|
| `src/ShellSort.java` | Shell Sort, saltos originales n/2, n/4, ..., 1 |
| `src/MergeSort.java` | Merge Sort top-down, un solo arreglo auxiliar |
| `src/QuickSort.java` | Quicksort, pivote fijo = último, partición de Lomuto |
| `src/GeneradorEntradas.java` | Los 4 tipos de entrada, con semilla fija (misma entrada siempre) |
| `src/PruebasOrdenamiento.java` | Pruebas de corrección contra `Arrays.sort`, incluye duplicados |
| `src/SondeoDesbordamiento.java` | Busca el n donde Quicksort desborda la pila (para coordinar con Persona 3) |
| `src/Benchmark.java` | Mide (1 calentamiento descartado + 5 repeticiones, mediana) y escribe los CSV |
| `grafica_tipos_entrada.py` | Gráfica log-log de un algoritmo con los 4 tipos de entrada |

## Cómo ejecutarlo

Desde esta carpeta:

```
javac -encoding UTF-8 -d out src/*.java

java -cp out PruebasOrdenamiento          # 1) pruebas de corrección
java -cp out SondeoDesbordamiento         # 2) límite de pila de Quicksort
java -cp out Benchmark                    # 3) mediciones -> carpeta resultados/
pip install matplotlib
python grafica_tipos_entrada.py           # 4) gráfica de Quicksort (PNG)
```

`Benchmark` acepta argumentos opcionales: `java -cp out Benchmark [nMaximo] [carpetaSalida] [limiteSegundos]`
(por defecto: 1 024 000, `resultados`, 10 s).

Para la gráfica de Insertion (la otra que pide la tarea): `python grafica_tipos_entrada.py Insertion`.

## Archivos de salida (carpeta `resultados/`)

- `mediciones.csv`: todas las mediciones individuales, sin mediana: `algoritmo,tipo_entrada,n,repeticion,tiempo_ns`
- `resumen.csv`: mediana y razón t(2n)/t(n): `algoritmo,tipo_entrada,n,mediana_ns,mediana_ms,razon_t2n_tn`
- `incidencias.csv`: dónde Quicksort desbordó la pila o se cortó por lento (va al informe)

## Cosas que hay que acordar con la Persona 3

1. **Pivote de Quicksort**: aquí es el **último** elemento. Los dos deben usar el mismo.
2. **Mismo n y mismos tipos de entrada**: n = 1000, 2000, 4000, ... y los 4 tipos de la tarea.
3. **Mismo `-Xss`** (tamaño de pila) al correr el sondeo y el benchmark, y la misma computadora.
4. **Formato del CSV**: si ella tiene su propio benchmark, unificar columnas. Para sumar sus
   algoritmos a este benchmark, agregarlos en `Benchmark.algoritmos()`.

## Notas

- Los resultados del benchmark dependen de la computadora: correrlo en la computadora que se use
  para el informe, con pocos programas abiertos.
- Con n=1000 los tiempos son de décimas de milisegundo y están afectados por el JIT, así que la razón
  t(2n)/t(n) entre 1000 y 2000 puede salir rara. Es normal; conviene mencionarlo en el análisis.
