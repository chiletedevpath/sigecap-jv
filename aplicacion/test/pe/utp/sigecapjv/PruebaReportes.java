package pe.utp.sigecapjv;

import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.utp.sigecapjv.controlador.ReporteControlador;
import pe.utp.sigecapjv.modelo.*;

public final class PruebaReportes {
    private static int total;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch (IllegalArgumentException | IllegalStateException ex) { total++; return; }
        throw new AssertionError("Se aceptó un reporte inválido.");
    }
    private static Reporte.Fila fila(String trabajador, String vigencia) {
        return new Reporte.Fila(trabajador, "Curso", LocalDate.of(2026, 9, 1), "COMPLETADA", "APROBADO", vigencia);
    }

    /** Comprueba que cada entrada de la tabla xref apunte al inicio de su objeto. */
    static void comprobarPdf(byte[] pdf, int paginas) {
        String texto = new String(pdf, Charset.forName("windows-1252"));
        comprobar(texto.startsWith("%PDF-1.4\n") && texto.endsWith("%%EOF\n"), "Cabecera y cierre PDF");
        comprobar(texto.contains("/Count " + paginas + " "), "Cantidad de páginas");
        int xref = Integer.parseInt(texto.substring(texto.lastIndexOf("startxref\n") + 10, texto.lastIndexOf("\n%%EOF")));
        String[] lineas = texto.substring(xref).split("\n");
        int objetos = Integer.parseInt(lineas[1].split(" ")[1]);
        for (int i = 1; i < objetos; i++) {
            int posicion = Integer.parseInt(lineas[2 + i].substring(0, 10));
            comprobar(texto.startsWith(i + " 0 obj\n", posicion), "Posición xref del objeto " + i);
        }
    }

    public static void verificar() throws Exception {
        LocalDate hoy = LocalDate.now();
        rechazar(() -> new Reporte(hoy, hoy.minusDays(1), null, null, null, null).validar());
        rechazar(() -> new Reporte(null, null, null, null, "PROGRAMADA", null).validar());
        rechazar(() -> new Reporte(null, null, null, null, null, "PROXIMO").validar());
        new Reporte(hoy, hoy, null, null, "PENDIENTE", "VIGENTE").validar();

        Reporte vencidos = new Reporte(null, null, null, null, null, "VENCIDO");
        comprobar(!vencidos.estaGenerado(), "Sin generar");
        vencidos.generar(List.of(fila("Uno", "VIGENTE"), fila("Dos", "VENCIDO"), fila("Tres", "SIN_CERTIFICADO")));
        comprobar(vencidos.estaGenerado() && vencidos.getFilas().size() == 1
                && vencidos.getFilas().get(0).trabajador().equals("Dos"), "Filtro de vigencia");
        Curso curso = new Curso(1, "C-1", "Formación Básica", "SUCAMEC", 12, "ACTIVO");
        List<String> filtros = new Reporte(LocalDate.of(2026, 9, 1), null, curso, null, "PENDIENTE", null).describirFiltros();
        comprobar(filtros.equals(List.of("Periodo: desde 2026-09-01", "Curso: C-1 - Formación Básica",
                "Trabajador: Todos", "Estado: PENDIENTE", "Vigencia: Todas")), "Descripción de filtros");

        ReporteControlador controlador = new ReporteControlador();
        Path archivo = Files.createTempFile("sigecap-reporte", ".pdf");
        try {
            rechazar(() -> controlador.exportarPdf(new Reporte(null, null, null, null, null, null), archivo));
            Reporte vacio = new Reporte(null, null, null, null, null, null);
            vacio.generar(List.of());
            rechazar(() -> controlador.exportarPdf(vacio, archivo));

            List<Reporte.Fila> filas = new ArrayList<>();
            filas.add(fila("José Pérez (Soto) \\ Ñuñoa", "VIGENTE"));
            for (int i = 1; i < 60; i++) filas.add(fila("Trabajador " + i, "VIGENTE"));
            Reporte completo = new Reporte(null, null, curso, null, null, "VIGENTE");
            completo.generar(filas);
            controlador.exportarPdf(completo, archivo);
            byte[] pdf = Files.readAllBytes(archivo);
            comprobarPdf(pdf, 3);
            String texto = new String(pdf, Charset.forName("windows-1252"));
            comprobar(texto.contains("(José Pérez \\(Soto\\) \\\\ Ñuñoa)"), "Texto escapado y con tildes");
            comprobar(texto.contains("Curso: C-1 - Formación Básica") && texto.contains("Registros: 60"),
                    "PDF conserva filtros y totales");
            comprobar(texto.contains("(Trabajador 59)") && texto.contains("Página 3 de 3"), "Todas las filas paginadas");
        } finally {
            Files.deleteIfExists(archivo);
        }
        System.out.println("OK: " + total + " comprobaciones del reporte y su exportación PDF.");
    }
}
