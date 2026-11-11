package ordenamiento;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/**
 * Genera las 4 gráficas del informe a partir de los CSV del benchmark.
 * No necesita librerías externas: dibuja con Java2D y guarda PNG.
 *
 *   grafica_loglog_aleatorio.png        Figura 1: todos los algoritmos, entrada aleatoria (log-log)
 *   grafica_insertion_tipos_entrada.png Figura 2: Insertion, los 4 tipos de entrada (log-log)
 *   grafica_quicksort_tipos_entrada.png Figura 3: Quicksort, los 4 tipos de entrada (log-log),
 *                                       con una línea vertical donde se desbordó la pila
 *   grafica_radix_digitos.png           Figura 4: Radix con n = 100 000, tiempo vs. dígitos
 *
 * Uso: java -cp out ordenamiento.Graficas [carpetaResultados]
 * (lee resumen.csv y radix_digitos.csv de esa carpeta y escribe ahí los PNG)
 */
public class Graficas {

    static final String[] ALGORITMOS = {"Selection", "Bubble", "Insertion", "Shell", "Merge", "Quicksort", "Radix"};
    static final Color[] COLORES_ALG = {
            new Color(0x1f77b4), new Color(0xff7f0e), new Color(0x2ca02c), new Color(0xd62728),
            new Color(0x9467bd), new Color(0x8c564b), new Color(0xe377c2)};

    static final String[] TIPOS = {"aleatorio", "ascendente", "descendente", "duplicados"};
    static final String[] ETIQUETAS_TIPO = {"Aleatorio", "Ordenado ascendente", "Ordenado descendente", "Muchos duplicados (0-10)"};
    static final Color[] COLORES_TIPO = {new Color(0x1f77b4), new Color(0xd62728), new Color(0xff7f0e), new Color(0x2ca02c)};

    static final String EJE_N = "Tamaño del arreglo n (escala log)";
    static final String EJE_T = "Tiempo, mediana de 5 ejecuciones (ms, escala log)";

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Locale.setDefault(Locale.ROOT);
        File dir = new File(args.length > 0 ? args[0] : "resultados");

        Map<String, TreeMap<Long, Double>> medianas = new LinkedHashMap<>();   // "alg|tipo" -> n -> ms
        Map<String, Long> desbordes = new LinkedHashMap<>();                   // "alg|tipo" -> n
        List<String> lineas = Files.readAllLines(new File(dir, "resumen.csv").toPath(), StandardCharsets.UTF_8);
        for (String l : lineas.subList(1, lineas.size())) {
            if (l.isBlank()) continue;
            String[] f = l.split(",", -1);   // algoritmo,tipo_entrada,n,mediana_ns,mediana_ms,razon_t2n_tn,estado
            String clave = f[0] + "|" + f[1];
            if (f[6].equals("desbordamiento")) {
                desbordes.put(clave, Long.parseLong(f[2]));
            } else {
                medianas.computeIfAbsent(clave, k -> new TreeMap<>()).put(Long.parseLong(f[2]), Double.parseDouble(f[4]));
            }
        }

        // Figura 1: todos los algoritmos sobre entrada aleatoria
        Grafica g1 = new Grafica("Todos los algoritmos sobre entrada aleatoria (log-log)", EJE_N, EJE_T, true);
        for (int i = 0; i < ALGORITMOS.length; i++) {
            g1.serie(ALGORITMOS[i], COLORES_ALG[i], i % 4, medianas.get(ALGORITMOS[i] + "|aleatorio"), false);
        }
        g1.serie("referencia ∝ n²", Color.BLACK, -1, puntos(1000, 0.05, 16000, 0.05 * 256), true);
        g1.serie("referencia ∝ n", Color.GRAY, -1, puntos(1000, 0.01, 1_024_000, 0.01 * 1024), true);
        g1.guardar(new File(dir, "grafica_loglog_aleatorio.png"));

        // Figuras 2 y 3: Insertion y Quicksort por tipo de entrada
        for (String alg : new String[] {"Insertion", "Quicksort"}) {
            Grafica g = new Grafica(alg + ": comparación por tipo de entrada", EJE_N, EJE_T, true);
            for (int i = 0; i < TIPOS.length; i++) {
                g.serie(ETIQUETAS_TIPO[i], COLORES_TIPO[i], i, medianas.get(alg + "|" + TIPOS[i]), false);
            }
            // Una sola línea vertical si varios tipos se desbordan en el mismo n
            Map<Long, List<String>> cortes = new TreeMap<>();
            for (String t : TIPOS) {
                Long n = desbordes.get(alg + "|" + t);
                if (n != null) cortes.computeIfAbsent(n, k -> new ArrayList<>()).add(t);
            }
            for (Map.Entry<Long, List<String>> c : cortes.entrySet()) {
                g.corte(c.getKey(), "desbordamiento de pila en n=" + miles(c.getKey()), "(" + String.join(", ", c.getValue()) + ")");
            }
            g.guardar(new File(dir, "grafica_" + alg.toLowerCase() + "_tipos_entrada.png"));
        }

        // Figura 4: prueba especial de Radix (mediana de las corridas "ok" por número de dígitos)
        Map<Integer, List<Long>> porDigitos = new TreeMap<>();
        List<String> lr = Files.readAllLines(new File(dir, "radix_digitos.csv").toPath(), StandardCharsets.UTF_8);
        for (String l : lr.subList(1, lr.size())) {
            String[] f = l.split(",", -1);   // digitos,n,repeticion,tiempo_ns,estado
            if (f.length == 5 && f[4].equals("ok")) {
                porDigitos.computeIfAbsent(Integer.parseInt(f[0]), k -> new ArrayList<>()).add(Long.parseLong(f[3]));
            }
        }
        TreeMap<Long, Double> radix = new TreeMap<>();
        for (Map.Entry<Integer, List<Long>> e : porDigitos.entrySet()) {
            long[] v = e.getValue().stream().mapToLong(Long::longValue).toArray();
            Arrays.sort(v);
            radix.put((long) e.getKey(), v[v.length / 2] / 1e6);
        }
        Grafica g4 = new Grafica("Radix (base 10, LSD) con n = 100 000: tiempo vs. dígitos",
                "Número de dígitos d de los valores", "Tiempo, mediana (ms)", false);
        g4.serie("Radix", COLORES_ALG[0], 0, radix, false);
        g4.guardar(new File(dir, "grafica_radix_digitos.png"));
    }

    static TreeMap<Long, Double> puntos(long x0, double y0, long x1, double y1) {
        TreeMap<Long, Double> p = new TreeMap<>();
        p.put(x0, y0);
        p.put(x1, y1);
        return p;
    }

    static String miles(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    /** Una gráfica de líneas con ejes log-log o lineales. */
    static class Grafica {
        static final int ANCHO = 1500, ALTO = 975;
        static final int IZQ = 130, DER = 40, ARR = 70, ABA = 95;

        final String titulo, ejeX, ejeY;
        final boolean log;
        final List<Object[]> series = new ArrayList<>();   // {etiqueta, color, marcador, puntos, punteada}
        final List<Object[]> cortes = new ArrayList<>();   // {n, línea 1, línea 2}
        double xMin, xMax, yMin, yMax;                     // en log10 si log == true

        Grafica(String titulo, String ejeX, String ejeY, boolean log) {
            this.titulo = titulo;
            this.ejeX = ejeX;
            this.ejeY = ejeY;
            this.log = log;
        }

        void serie(String etiqueta, Color color, int marcador, TreeMap<Long, Double> puntos, boolean punteada) {
            if (puntos != null && !puntos.isEmpty()) series.add(new Object[] {etiqueta, color, marcador, puntos, punteada});
        }

        void corte(long n, String l1, String l2) {
            cortes.add(new Object[] {n, l1, l2});
        }

        double tx(double v) { return log ? Math.log10(v) : v; }

        double px(double x) { return IZQ + (tx(x) - xMin) / (xMax - xMin) * (ANCHO - IZQ - DER); }

        double py(double y) { return ALTO - ABA - (tx(y) - yMin) / (yMax - yMin) * (ALTO - ABA - ARR); }

        @SuppressWarnings("unchecked")
        void rangos() {
            double nMin = Double.MAX_VALUE, nMax = -Double.MAX_VALUE, tMin = Double.MAX_VALUE, tMax = -Double.MAX_VALUE;
            for (Object[] s : series) {
                for (Map.Entry<Long, Double> p : ((TreeMap<Long, Double>) s[3]).entrySet()) {
                    nMin = Math.min(nMin, p.getKey());
                    nMax = Math.max(nMax, p.getKey());
                    tMin = Math.min(tMin, p.getValue());
                    tMax = Math.max(tMax, p.getValue());
                }
            }
            if (log) {
                xMin = Math.log10(nMin) - 0.08;
                xMax = Math.log10(nMax) + 0.08;
                yMin = Math.floor(Math.log10(tMin));
                yMax = Math.ceil(Math.log10(tMax));
                if (yMax - yMin < 1) yMax = yMin + 1;
            } else {
                xMin = nMin - 0.5;
                xMax = nMax + 0.5;
                yMin = 0;
                yMax = Math.ceil(tMax * 1.1 / 0.5) * 0.5;
            }
        }

        @SuppressWarnings("unchecked")
        void guardar(File archivo) throws IOException {
            rangos();
            BufferedImage img = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ANCHO, ALTO);

            cuadricula(g);
            for (Object[] s : series) {
                TreeMap<Long, Double> pts = (TreeMap<Long, Double>) s[3];
                Path2D.Double linea = new Path2D.Double();
                boolean primero = true;
                for (Map.Entry<Long, Double> p : pts.entrySet()) {
                    double x = px(p.getKey()), y = py(p.getValue());
                    if (primero) { linea.moveTo(x, y); primero = false; } else linea.lineTo(x, y);
                }
                g.setColor((Color) s[1]);
                g.setStroke(trazo((Boolean) s[4]));
                g.draw(linea);
                for (Map.Entry<Long, Double> p : pts.entrySet()) {
                    marcador(g, (Integer) s[2], px(p.getKey()), py(p.getValue()), 8);
                }
            }
            dibujarCortes(g);
            marcoYEtiquetas(g);
            leyenda(g);
            g.dispose();
            ImageIO.write(img, "png", archivo);
            System.out.println("Gráfica guardada en " + archivo.getPath());
        }

        static BasicStroke trazo(boolean punteada) {
            return punteada
                    ? new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {7f, 5f}, 0f)
                    : new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }

        static BasicStroke guia(boolean mayor) {
            return new BasicStroke(mayor ? 1.1f : 0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f);
        }

        void cuadricula(Graphics2D g) {
            int x0 = IZQ, x1 = ANCHO - DER, y0 = ARR, y1 = ALTO - ABA;
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            FontMetrics fm = g.getFontMetrics();
            List<double[]> xs = new ArrayList<>(), ys = new ArrayList<>();   // {valor, mayor?}
            if (log) {
                for (int k = (int) Math.floor(xMin) - 1; k <= (int) Math.ceil(xMax); k++)
                    for (int m = 1; m <= 9; m++) xs.add(new double[] {m * Math.pow(10, k), m == 1 ? 1 : 0});
                for (int k = (int) yMin; k <= (int) yMax; k++)
                    for (int m = 1; m <= 9; m++) ys.add(new double[] {m * Math.pow(10, k), m == 1 ? 1 : 0});
            } else {
                for (int k = (int) Math.ceil(xMin); k <= xMax; k++) xs.add(new double[] {k, 1});
                for (double v = 0; v <= yMax + 1e-9; v += 0.5) ys.add(new double[] {v, 1});
            }
            for (double[] v : xs) {
                double t = tx(v[0]);
                if (t < xMin - 1e-9 || t > xMax + 1e-9) continue;
                double x = px(v[0]);
                g.setColor(v[1] == 1 ? new Color(0xcfcfcf) : new Color(0xececec));
                g.setStroke(guia(v[1] == 1));
                g.draw(new Line2D.Double(x, y0, x, y1));
                if (v[1] == 1) {
                    String s = log ? miles((long) v[0]) : String.valueOf((long) v[0]);
                    g.setColor(Color.BLACK);
                    g.drawString(s, (float) (x - fm.stringWidth(s) / 2.0), y1 + 25);
                }
            }
            for (double[] v : ys) {
                double t = tx(v[0]);
                if (t < yMin - 1e-9 || t > yMax + 1e-9) continue;
                double y = py(v[0]);
                g.setColor(v[1] == 1 ? new Color(0xcfcfcf) : new Color(0xececec));
                g.setStroke(guia(v[1] == 1));
                g.draw(new Line2D.Double(x0, y, x1, y));
                if (v[1] == 1) {
                    String s = etiquetaY(v[0]);
                    g.setColor(Color.BLACK);
                    g.drawString(s, (float) (x0 - 10 - fm.stringWidth(s)), (float) (y + fm.getAscent() / 2.0 - 2));
                }
            }
        }

        String etiquetaY(double v) {
            if (!log) return String.format("%.1f", v);
            if (v >= 1) return miles(Math.round(v));
            return new java.math.BigDecimal(v).round(new java.math.MathContext(1)).stripTrailingZeros().toPlainString();
        }

        void dibujarCortes(Graphics2D g) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            FontMetrics fm = g.getFontMetrics();
            for (Object[] c : cortes) {
                double x = px((Long) c[0]);
                g.setColor(new Color(0x696969));
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {2f, 5f}, 0f));
                g.draw(new Line2D.Double(x, ARR, x, ALTO - ABA));
                AffineTransform old = g.getTransform();
                g.translate(x - 4, ALTO - ABA - 10);
                g.rotate(-Math.PI / 2);
                g.drawString((String) c[1], 0, -fm.getHeight() - 4);
                g.drawString((String) c[2], 0, -4);
                g.setTransform(old);
            }
        }

        void marcoYEtiquetas(Graphics2D g) {
            int x0 = IZQ, x1 = ANCHO - DER, y0 = ARR, y1 = ALTO - ABA;
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));

            g.setFont(new Font("SansSerif", Font.BOLD, 23));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(titulo, (ANCHO - fm.stringWidth(titulo)) / 2, 45);

            g.setFont(new Font("SansSerif", Font.PLAIN, 19));
            fm = g.getFontMetrics();
            g.drawString(ejeX, (x0 + x1) / 2 - fm.stringWidth(ejeX) / 2, ALTO - 30);
            AffineTransform old = g.getTransform();
            g.translate(36, (y0 + y1) / 2 + fm.stringWidth(ejeY) / 2);
            g.rotate(-Math.PI / 2);
            g.drawString(ejeY, 0, 0);
            g.setTransform(old);
        }

        void leyenda(Graphics2D g) {
            if (series.size() < 2) return;
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            FontMetrics fm = g.getFontMetrics();
            int ancho = 0;
            for (Object[] s : series) ancho = Math.max(ancho, fm.stringWidth((String) s[0]));
            int pad = 12, fila = 28, alto = series.size() * fila + pad;
            int bx = IZQ + 14, by = ARR + 14, bw = ancho + 74 + pad;
            g.setColor(new Color(255, 255, 255, 235));
            g.fillRoundRect(bx, by, bw, alto, 8, 8);
            g.setColor(new Color(0xcccccc));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(bx, by, bw, alto, 8, 8);
            for (int i = 0; i < series.size(); i++) {
                Object[] s = series.get(i);
                double cy = by + pad / 2.0 + 14 + i * fila;
                g.setColor((Color) s[1]);
                g.setStroke(trazo((Boolean) s[4]));
                g.draw(new Line2D.Double(bx + 10, cy, bx + 50, cy));
                marcador(g, (Integer) s[2], bx + 30, cy, 8);
                g.setColor(Color.BLACK);
                g.drawString((String) s[0], bx + 60, (float) (cy + fm.getAscent() / 2.0 - 2));
            }
        }

        /** 0 círculo, 1 cuadrado, 2 triángulo, 3 rombo; negativo = sin marcador. */
        static void marcador(Graphics2D g, int tipo, double x, double y, double d) {
            if (tipo < 0) return;
            Shape s;
            Path2D.Double p = new Path2D.Double();
            switch (tipo) {
                case 0:
                    s = new Ellipse2D.Double(x - d / 2, y - d / 2, d, d);
                    break;
                case 1:
                    s = new Rectangle2D.Double(x - d / 2, y - d / 2, d, d);
                    break;
                case 2:
                    p.moveTo(x, y - d * 0.65);
                    p.lineTo(x + d * 0.6, y + d * 0.45);
                    p.lineTo(x - d * 0.6, y + d * 0.45);
                    p.closePath();
                    s = p;
                    break;
                default:
                    p.moveTo(x, y - d * 0.7);
                    p.lineTo(x + d * 0.7, y);
                    p.lineTo(x, y + d * 0.7);
                    p.lineTo(x - d * 0.7, y);
                    p.closePath();
                    s = p;
            }
            g.fill(s);
        }
    }
}
