package pe.utp.sigecapjv.util;

import pe.utp.sigecapjv.modelo.Reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PDF 1.4 de solo texto, sin bibliotecas externas: A4 horizontal, fuentes estándar Helvetica
 * y la tabla del reporte repartida en páginas. Los caracteres fuera de Windows-1252 se muestran como "?".
 */
public final class ReportePdf {
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] COLUMNAS = {"Trabajador", "Curso", "Fecha", "Estado", "Resultado", "Vigencia"};
    private static final int[] X = {40, 220, 440, 515, 610, 700};
    private static final int[] CARACTERES = {34, 42, 12, 16, 16, 18};
    private static final int FILAS_POR_PAGINA = 27;

    private ReportePdf() {
    }

    public static void escribir(Reporte reporte, Path destino) throws IOException {
        List<Reporte.Fila> filas = reporte.getFilas();
        int paginas = Math.max(1, (filas.size() + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
        List<byte[]> objetos = new ArrayList<>();
        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < paginas; i++) hijos.append(5 + 2 * i).append(" 0 R ");
        objetos.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        objetos.add(bytes("<< /Type /Pages /Kids [" + hijos + "] /Count " + paginas + " >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objetos.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < paginas; i++) {
            objetos.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>"));
            List<Reporte.Fila> parte = filas.subList(
                    i * FILAS_POR_PAGINA, Math.min(filas.size(), (i + 1) * FILAS_POR_PAGINA));
            byte[] contenido = bytes(pagina(reporte, parte, i + 1, paginas));
            ByteArrayOutputStream flujo = new ByteArrayOutputStream();
            flujo.writeBytes(bytes("<< /Length " + contenido.length + " >>\nstream\n"));
            flujo.writeBytes(contenido);
            flujo.writeBytes(bytes("\nendstream"));
            objetos.add(flujo.toByteArray());
        }
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.writeBytes(bytes("%PDF-1.4\n"));
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            posiciones.add(pdf.size());
            pdf.writeBytes(bytes((i + 1) + " 0 obj\n"));
            pdf.writeBytes(objetos.get(i));
            pdf.writeBytes(bytes("\nendobj\n"));
        }
        int xref = pdf.size();
        StringBuilder cierre = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int posicion : posiciones) cierre.append(String.format(Locale.ROOT, "%010d 00000 n \n", posicion));
        cierre.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        pdf.writeBytes(bytes(cierre.toString()));
        Files.write(destino, pdf.toByteArray());
    }

    private static String pagina(Reporte reporte, List<Reporte.Fila> filas, int numero, int total) {
        StringBuilder s = new StringBuilder();
        texto(s, "F2", 16, 40, 550, "Reporte de capacitaciones - SIGECAP J&V");
        texto(s, "F1", 9, 40, 532, "Generado: " + reporte.getFechaGeneracion().format(FECHA_HORA)
                + "    Registros: " + reporte.getFilas().size());
        int y = 518;
        for (String filtro : reporte.describirFiltros()) {
            texto(s, "F1", 9, 40, y, recortar(filtro, 150));
            y -= 12;
        }
        y -= 10;
        for (int i = 0; i < COLUMNAS.length; i++) texto(s, "F2", 9, X[i], y, COLUMNAS[i]);
        s.append("0.6 w 40 ").append(y - 5).append(" m 802 ").append(y - 5).append(" l S\n");
        for (Reporte.Fila f : filas) {
            y -= 14;
            String[] valores = {f.trabajador(), f.curso(), String.valueOf(f.fecha()), f.estado(), f.resultado(),
                    "PROXIMO_A_VENCER".equals(f.vigencia()) ? "Próximo a vencer" : f.vigencia()};
            for (int i = 0; i < valores.length; i++) texto(s, "F1", 9, X[i], y, recortar(valores[i], CARACTERES[i]));
        }
        texto(s, "F1", 8, 40, 25, "Página " + numero + " de " + total);
        return s.toString();
    }

    private static void texto(StringBuilder s, String fuente, int tamano, int x, int y, String valor) {
        String seguro = valor.replaceAll("[\\r\\n\\t]", " ")
                .replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        s.append("BT /").append(fuente).append(' ').append(tamano).append(" Tf ").append(x).append(' ').append(y)
                .append(" Td (").append(seguro).append(") Tj ET\n");
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo - 3) + "...";
    }

    private static byte[] bytes(String valor) {
        return valor.getBytes(WIN_ANSI);
    }
}
