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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/**
 * Gráfica log-log que compara los 4 tipos de entrada para UN algoritmo
 * (por defecto Quicksort), usando las medianas de resultados/resumen.csv.
 *
 * No necesita librerías externas: dibuja con Java2D y guarda un PNG.
 *
 * Uso (desde la carpeta que contiene "resultados"):
 *   javac -d out src/GraficaTiposEntrada.java
 *   java -cp out GraficaTiposEntrada                      // Quicksort
 *   java -cp out GraficaTiposEntrada Insertion            // otro algoritmo
 *   java -cp out GraficaTiposEntrada Quicksort resultados/resumen.csv resultados/incidencias.csv
 *
 * Si el algoritmo desbordó la pila (o se cortó por lento) en algún tipo de entrada,
 * se dibuja una línea vertical punteada en el n donde ocurrió.
 */
public class GraficaTiposEntrada {

    static final String[] ORDEN_TIPOS = {"aleatorio", "ascendente", "descendente", "duplicados"};
    static final Map<String, String> ETIQUETAS = new LinkedHashMap<>();
    static final Map<String, Color> COLORES = new LinkedHashMap<>();
    static final Map<String, Integer> MARCADORES = new LinkedHashMap<>();

    static {
        ETIQUETAS.put("aleatorio", "Aleatorio");
        ETIQUETAS.put("ascendente", "Ordenado ascendente");
        ETIQUETAS.put("descendente", "Ordenado descendente");
        ETIQUETAS.put("duplicados", "Muchos duplicados (0-10)");

        COLORES.put("aleatorio", new Color(0x1f77b4));
        COLORES.put("ascendente", new Color(0xd62728));
        COLORES.put("descendente", new Color(0xff7f0e));
        COLORES.put("duplicados", new Color(0x2ca02c));

        MARCADORES.put("aleatorio", 0);   // círculo
        MARCADORES.put("ascendente", 1);  // cuadrado
        MARCADORES.put("descendente", 2); // triángulo
        MARCADORES.put("duplicados", 3);  // rombo
    }

    // ---------------------------------------------------------------- lectura de CSV

    /** Devuelve {tipo: [(n, mediana_ms), ...]} ordenado por n. */
    static Map<String, TreeMap<Long, Double>> leerResumen(String ruta, String algoritmo) throws IOException {
        Map<String, TreeMap<Long, Double>> datos = new LinkedHashMap<>();
        List<String> lineas = Files.readAllLines(Paths.get(ruta), StandardCharsets.UTF_8);
        if (lineas.isEmpty()) return datos;
        List<String> enc = separar(lineas.get(0));
        int iAlg = enc.indexOf("algoritmo");
        int iTipo = enc.indexOf("tipo_entrada");
        int iN = enc.indexOf("n");
        int iMs = enc.indexOf("mediana_ms");
        if (iAlg < 0 || iTipo < 0 || iN < 0 || iMs < 0) {
            throw new IOException("Encabezado inesperado en " + ruta + ": " + lineas.get(0));
        }
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) continue;
            List<String> f = separar(lineas.get(i));
            if (f.get(iAlg).equalsIgnoreCase(algoritmo)) {
                long n = Long.parseLong(f.get(iN));
                double ms = Double.parseDouble(f.get(iMs).replace(',', '.'));
                datos.computeIfAbsent(f.get(iTipo), k -> new TreeMap<>()).put(n, ms);
            }
        }
        return datos;
    }

    /** Devuelve {tipo: (n, motivo)} con el n donde se cortó cada tipo de entrada. */
    static Map<String, Object[]> leerIncidencias(String ruta, String algoritmo) throws IOException {
        Map<String, Object[]> inc = new LinkedHashMap<>();
        List<String> lineas;
        try {
            lineas = Files.readAllLines(Paths.get(ruta), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return inc; // sin incidencias: no se dibuja ninguna línea vertical
        }
        if (lineas.isEmpty()) return inc;
        List<String> enc = separar(lineas.get(0));
        int iAlg = enc.indexOf("algoritmo");
        int iTipo = enc.indexOf("tipo_entrada");
        int iN = enc.indexOf("n");
        int iMotivo = enc.indexOf("motivo");
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) continue;
            List<String> f = separar(lineas.get(i));
            if (f.get(iAlg).equalsIgnoreCase(algoritmo)) {
                inc.put(f.get(iTipo), new Object[]{Long.parseLong(f.get(iN)), f.get(iMotivo)});
            }
        }
        return inc;
    }

    static List<String> separar(String linea) {
        List<String> r = new ArrayList<>();
        for (String s : linea.split(",", -1)) r.add(s.trim());
        return r;
    }

    // ---------------------------------------------------------------- dibujo

    // Área del gráfico dentro de la imagen
    static final int ANCHO = 1275, ALTO = 825;
    static final int IZQ = 120, DER = 40, ARR = 70, ABA = 90;

    static double xMinLog, xMaxLog, yMinLog, yMaxLog;

    static double px(double n) {
        return IZQ + (Math.log10(n) - xMinLog) / (xMaxLog - xMinLog) * (ANCHO - IZQ - DER);
    }

    static double py(double ms) {
        return ALTO - ABA - (Math.log10(ms) - yMinLog) / (yMaxLog - yMinLog) * (ALTO - ABA - ARR);
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Locale.setDefault(Locale.ROOT);

        String algoritmo = args.length > 0 ? args[0] : "Quicksort";
        String rutaResumen = args.length > 1 ? args[1] : "resultados/resumen.csv";
        String rutaIncidencias = args.length > 2 ? args[2] : "resultados/incidencias.csv";

        Map<String, TreeMap<Long, Double>> datos = leerResumen(rutaResumen, algoritmo);
        if (datos.isEmpty()) {
            System.err.println("No se encontraron datos de '" + algoritmo + "' en " + rutaResumen);
            System.exit(1);
        }
        Map<String, Object[]> incidencias = leerIncidencias(rutaIncidencias, algoritmo);

        // Rango de los ejes
        double nMin = Double.MAX_VALUE, nMax = 0, tMin = Double.MAX_VALUE, tMax = 0;
        for (TreeMap<Long, Double> serie : datos.values()) {
            for (Map.Entry<Long, Double> p : serie.entrySet()) {
                nMin = Math.min(nMin, p.getKey());
                nMax = Math.max(nMax, p.getKey());
                if (p.getValue() > 0) {
                    tMin = Math.min(tMin, p.getValue());
                    tMax = Math.max(tMax, p.getValue());
                }
            }
        }
        xMinLog = Math.log10(nMin) - 0.08;
        xMaxLog = Math.log10(nMax) + 0.08;
        yMinLog = Math.floor(Math.log10(tMin));
        yMaxLog = Math.ceil(Math.log10(tMax));
        if (yMaxLog - yMinLog < 1) yMaxLog = yMinLog + 1;

        BufferedImage img = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ANCHO, ALTO);

        dibujarCuadricula(g);

        // Series
        for (String tipo : ORDEN_TIPOS) {
            TreeMap<Long, Double> serie = datos.get(tipo);
            if (serie == null) continue;
            Color color = COLORES.get(tipo);
            Path2D.Double linea = new Path2D.Double();
            boolean primero = true;
            for (Map.Entry<Long, Double> p : serie.entrySet()) {
                if (p.getValue() <= 0) continue;
                double x = px(p.getKey()), y = py(p.getValue());
                if (primero) { linea.moveTo(x, y); primero = false; } else linea.lineTo(x, y);
            }
            g.setColor(color);
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linea);
            for (Map.Entry<Long, Double> p : serie.entrySet()) {
                if (p.getValue() <= 0) continue;
                marcador(g, MARCADORES.get(tipo), px(p.getKey()), py(p.getValue()), 6.5);
            }
        }

        dibujarCortes(g, incidencias);
        dibujarMarcoYEtiquetas(g, algoritmo);
        dibujarLeyenda(g, datos);
        g.dispose();

        String salida = "grafica_" + algoritmo.toLowerCase() + "_tipos_entrada.png";
        ImageIO.write(img, "png", new File(salida));
        System.out.println("Grafica guardada en " + salida);
    }

    static void dibujarCuadricula(Graphics2D g) {
        int x0 = IZQ, x1 = ANCHO - DER, y0 = ARR, y1 = ALTO - ABA;
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics fm = g.getFontMetrics();

        // Eje X: líneas en 1..9 × 10^k; etiqueta solo en las potencias de 10
        for (int k = (int) Math.floor(xMinLog) - 1; k <= (int) Math.ceil(xMaxLog); k++) {
            for (int m = 1; m <= 9; m++) {
                double v = m * Math.pow(10, k);
                double lv = Math.log10(v);
                if (lv < xMinLog || lv > xMaxLog) continue;
                double x = px(v);
                boolean mayor = (m == 1);
                g.setColor(mayor ? new Color(0xcfcfcf) : new Color(0xececec));
                g.setStroke(new BasicStroke(mayor ? 1.1f : 0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                        new float[]{4f, 4f}, 0f));
                g.draw(new Line2D.Double(x, y0, x, y1));
                if (mayor) {
                    String t = formatoMiles((long) v);
                    g.setColor(Color.BLACK);
                    g.drawString(t, (float) (x - fm.stringWidth(t) / 2.0), y1 + 22);
                }
            }
        }

        // Eje Y
        for (int k = (int) yMinLog; k <= (int) yMaxLog; k++) {
            for (int m = 1; m <= 9; m++) {
                double v = m * Math.pow(10, k);
                double lv = Math.log10(v);
                if (lv < yMinLog - 1e-9 || lv > yMaxLog + 1e-9) continue;
                double y = py(v);
                boolean mayor = (m == 1);
                g.setColor(mayor ? new Color(0xcfcfcf) : new Color(0xececec));
                g.setStroke(new BasicStroke(mayor ? 1.1f : 0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                        new float[]{4f, 4f}, 0f));
                g.draw(new Line2D.Double(x0, y, x1, y));
                if (mayor) {
                    String t = formatoY(k);
                    g.setColor(Color.BLACK);
                    g.drawString(t, (float) (x0 - 10 - fm.stringWidth(t)), (float) (y + fm.getAscent() / 2.0 - 2));
                }
            }
        }
    }

    static String formatoMiles(long v) {
        return String.format("%,d", v).replace(',', ' ');
    }

    static String formatoY(int k) {
        if (k >= 0) return formatoMiles((long) Math.pow(10, k));
        StringBuilder sb = new StringBuilder("0.");
        for (int i = 1; i < -k; i++) sb.append('0');
        sb.append('1');
        return sb.toString();
    }

    static void dibujarCortes(Graphics2D g, Map<String, Object[]> incidencias) {
        // Si varios tipos se cortan en el mismo n (p. ej. ascendente y descendente), una sola línea.
        Map<String, List<String>> cortes = new TreeMap<>();
        Map<String, Object[]> claves = new LinkedHashMap<>();
        for (Map.Entry<String, Object[]> e : incidencias.entrySet()) {
            long n = (Long) e.getValue()[0];
            String motivo = (String) e.getValue()[1];
            String clave = String.format("%020d|%s", n, motivo);
            cortes.computeIfAbsent(clave, k -> new ArrayList<>()).add(e.getKey());
            claves.put(clave, new Object[]{n, motivo});
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();
        for (Map.Entry<String, List<String>> e : cortes.entrySet()) {
            long n = (Long) claves.get(e.getKey())[0];
            String motivo = (String) claves.get(e.getKey())[1];
            String causa = motivo.contains("DESBORDAMIENTO") ? "desbordamiento de pila" : "demasiado lento";
            StringBuilder nombres = new StringBuilder();
            for (String t : e.getValue()) {
                if (nombres.length() > 0) nombres.append(", ");
                nombres.append(ETIQUETAS.get(t).split(" \\(")[0].toLowerCase());
            }
            double x = px(n);
            g.setColor(new Color(0x696969));
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{2f, 5f}, 0f));
            g.draw(new Line2D.Double(x, ARR, x, ALTO - ABA));

            String l1 = causa + " en n=" + formatoMiles(n);
            String l2 = "(" + nombres + ")";
            AffineTransform old = g.getTransform();
            // Texto vertical a la izquierda de la línea, empezando cerca del borde inferior
            g.translate(x - 3, ALTO - ABA - 10);
            g.rotate(-Math.PI / 2);
            g.drawString(l1, 0, -fm.getHeight() - 4);
            g.drawString(l2, 0, -4);
            g.setTransform(old);
        }
    }

    static void dibujarMarcoYEtiquetas(Graphics2D g, String algoritmo) {
        int x0 = IZQ, x1 = ANCHO - DER, y0 = ARR, y1 = ALTO - ABA;
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        String titulo = algoritmo + ": comparación por tipo de entrada";
        g.drawString(titulo, (ANCHO - fm.stringWidth(titulo)) / 2, 42);

        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        fm = g.getFontMetrics();
        String ex = "Tamaño del arreglo n (escala log)";
        g.drawString(ex, (x0 + x1) / 2 - fm.stringWidth(ex) / 2, ALTO - 28);

        String ey = "Tiempo, mediana de 5 ejecuciones (ms, escala log)";
        AffineTransform old = g.getTransform();
        g.translate(34, (y0 + y1) / 2 + fm.stringWidth(ey) / 2);
        g.rotate(-Math.PI / 2);
        g.drawString(ey, 0, 0);
        g.setTransform(old);
    }

    static void dibujarLeyenda(Graphics2D g, Map<String, TreeMap<Long, Double>> datos) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics fm = g.getFontMetrics();
        int filas = 0, ancho = 0;
        for (String tipo : ORDEN_TIPOS) {
            if (!datos.containsKey(tipo)) continue;
            filas++;
            ancho = Math.max(ancho, fm.stringWidth(ETIQUETAS.get(tipo)));
        }
        int pad = 12, alto = filas * 26 + pad;
        int bx = IZQ + 14, by = ARR + 14, bw = ancho + 70 + pad;
        g.setColor(new Color(255, 255, 255, 235));
        g.fillRoundRect(bx, by, bw, alto, 8, 8);
        g.setColor(new Color(0xcccccc));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx, by, bw, alto, 8, 8);
        int fila = 0;
        for (String tipo : ORDEN_TIPOS) {
            if (!datos.containsKey(tipo)) continue;
            double cy = by + pad / 2.0 + 13 + fila * 26;
            g.setColor(COLORES.get(tipo));
            g.setStroke(new BasicStroke(2.6f));
            g.draw(new Line2D.Double(bx + 10, cy, bx + 46, cy));
            marcador(g, MARCADORES.get(tipo), bx + 28, cy, 6.5);
            g.setColor(Color.BLACK);
            g.drawString(ETIQUETAS.get(tipo), bx + 56, (float) (cy + fm.getAscent() / 2.0 - 2));
            fila++;
        }
    }

    static void marcador(Graphics2D g, int tipo, double x, double y, double d) {
        Shape s;
        switch (tipo) {
            case 0:
                s = new Ellipse2D.Double(x - d / 2, y - d / 2, d, d);
                break;
            case 1:
                s = new Rectangle2D.Double(x - d / 2, y - d / 2, d, d);
                break;
            case 2: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x, y - d * 0.65);
                p.lineTo(x + d * 0.6, y + d * 0.45);
                p.lineTo(x - d * 0.6, y + d * 0.45);
                p.closePath();
                s = p;
                break;
            }
            default: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x, y - d * 0.7);
                p.lineTo(x + d * 0.7, y);
                p.lineTo(x, y + d * 0.7);
                p.lineTo(x - d * 0.7, y);
                p.closePath();
                s = p;
            }
        }
        g.fill(s);
    }
}
