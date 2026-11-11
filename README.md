# Benchmark de algoritmos de ordenamiento

Tarea extraclase #2 — Algoritmos y Estructuras de Datos I (CE 1103), TEC, II Semestre 2026.
Grupo: Mariel Herrera González, Sofía Vargas González y María Ureña Carrasco.

Comprueba experimentalmente la complejidad de Selection, Bubble, Insertion, Shell, Merge,
Quicksort (pivote fijo = último elemento) y Radix (LSD, base 10), todos **sin optimizaciones**.

## Estructura

```
src/ordenamiento/
├── algoritmos/            Un archivo por algoritmo + la interfaz Algoritmo
│                          (cada uno con public static void sort(int[] a))
├── entrada/               GeneradorEntradas (semilla fija 42) y TipoEntrada
├── Validacion.java        36 casos por algoritmo contra Arrays.sort + batería ampliada
├── Benchmark.java         Mediciones de los 7 algoritmos + prueba especial de Radix
├── ConteoOperaciones.java Copias instrumentadas: comparaciones, intercambios, profundidad...
└── Graficas.java          Las 4 gráficas del informe (Java2D, sin librerías externas)
resultados/                CSV, entorno.txt y gráficas PNG usados en el informe
informe/                   Documentación del uso de IA (y el informe en PDF)
```

## Compilar y ejecutar

Requiere Java 17 o superior. Desde la carpeta del proyecto:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")

java -cp out ordenamiento.Validacion                    # 1) pruebas contra Arrays.sort
java -cp out ordenamiento.Benchmark resultados          # 2) mediciones (varios minutos)
java -cp out ordenamiento.ConteoOperaciones resultados  # 3) conteo de operaciones
java -cp out ordenamiento.Graficas resultados           # 4) gráficas PNG
```

En Windows (PowerShell), el primer paso es:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

### En macOS

1. Instalar un JDK 17 o superior (si `java -version` no muestra uno). Con [Homebrew](https://brew.sh):

   ```bash
   brew install --cask temurin
   java -version
   ```

   Si hay varios JDK instalados, se lista cuáles hay con `/usr/libexec/java_home -V` y se elige
   uno para la sesión con `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`.

2. Desde la Terminal (zsh), en la carpeta del proyecto, compilar y ejecutar igual que arriba:

   ```bash
   javac -encoding UTF-8 -d out $(find src -name "*.java")

   java -cp out ordenamiento.Validacion
   java -cp out ordenamiento.Benchmark resultados
   java -cp out ordenamiento.ConteoOperaciones resultados
   java -Djava.awt.headless=true -cp out ordenamiento.Graficas resultados
   ```

   La opción `-Djava.awt.headless=true` evita que aparezca el ícono de Java en el Dock al
   generar las gráficas; las imágenes se crean igual sin ella.

3. Abrir las gráficas generadas:

   ```bash
   open resultados/*.png
   ```

Notas:

- Si la ruta de la carpeta tiene espacios, `$(find ...)` separa mal los nombres; en ese caso
  compilar con `find src -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -d out`.
- Durante el benchmark conviene tener la Mac conectada a la corriente y cerrar otras
  aplicaciones, para reducir el ruido en los tiempos. Para evitar que se suspenda en las
  corridas largas: `caffeinate -i java -cp out ordenamiento.Benchmark resultados`.

## Metodología

- Entradas de enteros entre 0 y 999 999: aleatorio, ascendente (el aleatorio ordenado),
  descendente (el ascendente invertido) y muchos duplicados (valores entre 0 y 10).
  Generador con semilla fija 42: para cada tipo y tamaño siempre sale el mismo arreglo.
- Para cada (tipo, n) se genera **un** arreglo y los 7 algoritmos ordenan una copia de ese
  mismo arreglo. La generación y la copia no se miden.
- Tamaños duplicando n: Selection, Bubble e Insertion de 1 000 a 16 000; Shell, Merge,
  Quicksort y Radix de 1 000 a 1 024 000.
- Calentamiento global del JIT (10 ordenamientos de un aleatorio de 10 000 por algoritmo) y,
  en cada tamaño, 1 corrida de calentamiento descartada + 5 corridas medidas con
  `System.nanoTime()`. Se reporta la mediana. `System.gc()` antes de cada corrida, fuera del tiempo.
- Cada corrida se verifica contra `Arrays.sort`. Si la validación inicial falla, no se mide nada.
- Si Quicksort produce `StackOverflowError`, se registra `desbordamiento` y no se prueban
  tamaños mayores para esa combinación. Lo mismo si una corrida supera 60 s.
- Prueba especial de Radix: n = 100 000 con valores de 1 a 9 dígitos (0 a 999 999 999).

## Archivos de resultados

| Archivo | Columnas |
|---|---|
| `mediciones.csv` | `algoritmo,tipo_entrada,n,repeticion,tiempo_ns,estado` (todas las corridas, sin mediana; `repeticion = 0` es el calentamiento) |
| `resumen.csv` | `algoritmo,tipo_entrada,n,mediana_ns,mediana_ms,razon_t2n_tn,estado` |
| `radix_digitos.csv` | `digitos,n,repeticion,tiempo_ns,estado` |
| `conteo_operaciones.csv` | `algoritmo,metrica,tipo_entrada,n,valor` |
| `entorno.txt` | Versión de Java, sistema operativo, núcleos y memoria máxima de la JVM |

Los resultados de `resultados/` se obtuvieron en una MacBook con Apple M3 (16 GB, macOS 27.2.1,
OpenJDK 25.0.4 Temurin, `-Xss` por defecto de 2 048 KB). Si se vuelve a correr el benchmark,
los tiempos cambian un poco por ruido, pero las razones t(2n)/t(n) y los puntos de
desbordamiento de Quicksort (n = 64 000 con entradas ordenadas, n = 512 000 con duplicados)
deberían repetirse. El conteo de operaciones no depende de la computadora.
