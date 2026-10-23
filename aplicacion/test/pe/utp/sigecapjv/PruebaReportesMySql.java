package pe.utp.sigecapjv;

import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import javax.swing.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.vista.PnlReportes;

/** Agrega sus propios datos en 2020 para no depender de lo registrado por las demás pruebas. */
public final class PruebaReportesMySql {
    private static int total;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException | SecurityException esperado) { total++; return; }
        throw new AssertionError("Se aceptó una operación inválida del reporte.");
    }
    private static <T> T campo(PnlReportes panel, String nombre, Class<T> tipo) {
        try {
            var campo = PnlReportes.class.getDeclaredField(nombre); campo.setAccessible(true);
            return tipo.cast(campo.get(panel));
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static JButton boton(java.awt.Component c, String texto) {
        if (c instanceof JButton b && texto.equals(b.getText())) return b;
        if (c instanceof java.awt.Container padre) for (java.awt.Component hijo : padre.getComponents()) {
            JButton encontrado = boton(hijo, texto); if (encontrado != null) return encontrado;
        }
        return null;
    }
    /** Programa en fecha futura y luego simula que la sesión ocurrió en la fecha indicada. */
    private static Capacitacion sesion(CapacitacionJdbcControlador gestion, Curso curso, LocalDate fecha) {
        Capacitacion c = new Capacitacion(0, LocalDate.now().plusDays(10), LocalTime.of(8, 0), LocalTime.of(10, 0),
                "Presencial", null, curso);
        gestion.programarCapacitacion(c);
        if (fecha != null) Jdbc.transaccion(x -> {
            Jdbc.actualizar(x, "UPDATE capacitacion SET fecha=? WHERE idCapacitacion=?", fecha, c.getIdCapacitacion());
            return null;
        });
        return c;
    }
    private static Reporte generar(ReporteControlador r, LocalDate desde, LocalDate hasta, Curso curso,
                                   Trabajador trabajador, String estado, String vigencia) {
        return r.generar(new Reporte(desde, hasta, curso, trabajador, estado, vigencia));
    }

    public static void verificar() throws Exception {
        PersonalJdbcControlador personal = new PersonalJdbcControlador();
        CapacitacionJdbcControlador gestion = new CapacitacionJdbcControlador(personal);
        SeguimientoControlador s = new SeguimientoControlador();
        Trabajador alfa = new Trabajador(0, "70000001", "Reporte", "Alfa", "Agente", "ACTIVO");
        Trabajador beta = new Trabajador(0, "70000002", "Reporte", "Beta", "Agente", "ACTIVO");
        personal.registrarTrabajador(alfa); personal.registrarTrabajador(beta);
        Curso basico = new Curso(0, "REP-A", "Reporte básico", "SUCAMEC", 12, "ACTIVO");
        Curso portuario = new Curso(0, "REP-B", "Reporte portuario", "APN", null, "ACTIVO");
        gestion.registrarCurso(basico); gestion.registrarCurso(portuario);
        LocalDate enero = LocalDate.of(2020, 1, 15), febrero = LocalDate.of(2020, 2, 20);
        Capacitacion capA = sesion(gestion, basico, null), capB = sesion(gestion, portuario, null);
        Capacitacion cancelada = sesion(gestion, portuario, null);
        int pAlfa = gestion.asignarTrabajador(alfa, capA).getIdParticipacion();
        int pBeta = gestion.asignarTrabajador(beta, capA).getIdParticipacion();
        gestion.asignarTrabajador(alfa, capB);
        gestion.asignarTrabajador(beta, cancelada);
        gestion.cancelarCapacitacion(cancelada.getIdCapacitacion());
        for (Capacitacion c : List.of(capA, capB)) Jdbc.transaccion(x -> {
            Jdbc.actualizar(x, "UPDATE capacitacion SET fecha=? WHERE idCapacitacion=?",
                    c == capA ? enero : febrero, c.getIdCapacitacion());
            return null;
        });
        s.registrarAsistencia(pAlfa, "ASISTIO"); s.registrarResultado(pAlfa, "APROBADO", null);
        s.registrarCertificado(pAlfa, "REP-VENCIDO", enero, LocalDate.of(2021, 1, 15), null);
        s.registrarAsistencia(pBeta, "ASISTIO"); s.registrarResultado(pBeta, "APTO", null);
        s.registrarCertificado(pBeta, "REP-VIGENTE", enero, LocalDate.now().plusYears(1), null);

        ReporteControlador r = new ReporteControlador();
        LocalDate inicio = LocalDate.of(2020, 1, 1), fin = LocalDate.of(2020, 12, 31);
        Reporte anual = generar(r, inicio, fin, null, null, null, null);
        comprobar(anual.getFilas().equals(List.of(
                new Reporte.Fila("Reporte Alfa", "Reporte básico", enero, "COMPLETADA", "APROBADO", "VENCIDO"),
                new Reporte.Fila("Reporte Beta", "Reporte básico", enero, "COMPLETADA", "APTO", "VIGENTE"),
                new Reporte.Fila("Reporte Alfa", "Reporte portuario", febrero, "PENDIENTE", "-", "SIN_CERTIFICADO"))),
                "Información consolidada del periodo");
        comprobar(generar(r, null, null, basico, null, null, null).getFilas().size() == 2, "Filtro por curso");
        comprobar(generar(r, null, null, null, beta, null, null).getFilas().size() == 2, "Filtro por trabajador");
        List<Reporte.Fila> canceladas = generar(r, null, null, null, beta, "CANCELADA", null).getFilas();
        comprobar(canceladas.size() == 1 && canceladas.get(0).curso().equals("Reporte portuario"),
                "La capacitación cancelada se informa como CANCELADA");
        comprobar(generar(r, inicio, fin, null, null, "COMPLETADA", null).getFilas().size() == 2, "Filtro por estado");
        comprobar(generar(r, null, null, basico, null, null, "VENCIDO").getFilas().get(0).trabajador().equals("Reporte Alfa"),
                "Filtro por vigencia vencida");
        comprobar(generar(r, null, null, null, alfa, null, "SIN_CERTIFICADO").getFilas().size() == 1, "Filtro sin certificado");
        comprobar(generar(r, enero, enero, portuario, null, null, null).getFilas().isEmpty(), "Filtros combinados");
        Reporte vacio = generar(r, LocalDate.of(1990, 1, 1), LocalDate.of(1990, 12, 31), null, null, null, null);
        comprobar(vacio.estaGenerado() && vacio.getFilas().isEmpty(), "Reporte sin resultados");
        rechazar(() -> generar(r, fin, inicio, null, null, null, null));

        Path archivo = Files.createTempFile("sigecap-reporte-mysql", ".pdf");
        try {
            rechazar(() -> r.exportarPdf(vacio, archivo));
            r.exportarPdf(anual, archivo);
            byte[] pdf = Files.readAllBytes(archivo);
            PruebaReportes.comprobarPdf(pdf, 1);
            String texto = new String(pdf, Charset.forName("windows-1252"));
            comprobar(texto.contains("Periodo: 2020-01-01 a 2020-12-31") && texto.contains("Registros: 3")
                    && texto.contains("(Reporte portuario)") && texto.contains("(SIN_CERTIFICADO)"),
                    "El PDF conserva filtros y filas generadas");
        } finally { Files.deleteIfExists(archivo); }

        try (SesionUsuario admin = new LoginControlador().autenticar("admin_prueba", "Clave-usuarios-prueba-2026".toCharArray())) {
            ReporteControlador autorizado = new ReporteControlador(admin);
            comprobar(generar(autorizado, inicio, fin, null, null, null, null).getFilas().size() == 3, "Acceso con sesión autorizada");
            admin.close();
            rechazar(() -> generar(autorizado, inicio, fin, null, null, null, null));
        }

        SwingUtilities.invokeAndWait(() -> {
            PnlReportes panel = new PnlReportes(r);
            JTable tabla = (JTable) ((JScrollPane) encontrarScroll(panel)).getViewport().getView();
            JButton exportar = campo(panel, "exportar", JButton.class);
            JLabel mensaje = campo(panel, "mensaje", JLabel.class);
            JComboBox<?> cursos = campo(panel, "curso", JComboBox.class);
            for (int i = 0; i < cursos.getItemCount(); i++)
                if (cursos.getItemAt(i) instanceof Curso c && c.getCodigo().equals("REP-A")) cursos.setSelectedIndex(i);
            campo(panel, "desde", JTextField.class).setText("01/01/2020");
            campo(panel, "hasta", JTextField.class).setText("31/12/2020");
            boton(panel, "Generar reporte").doClick();
            comprobar(tabla.getRowCount() == 2 && exportar.isEnabled(), "Pantalla muestra el reporte filtrado");
            comprobar("Reporte Alfa".equals(tabla.getValueAt(0, 0)) && "VENCIDO".equals(tabla.getValueAt(0, 5)), "Columnas de la tabla");
            campo(panel, "desde", JTextField.class).setText("15/01/2020");
            campo(panel, "hasta", JTextField.class).setText("15/01/2020");
            boton(panel, "Generar reporte").doClick();
            comprobar(tabla.getRowCount() == 2, "Desde y Hasta el mismo día incluyen esa fecha");
            campo(panel, "desde", JTextField.class).setText("01/01/1990");
            campo(panel, "hasta", JTextField.class).setText("31/12/1990");
            boton(panel, "Generar reporte").doClick();
            comprobar(tabla.getRowCount() == 0 && !exportar.isEnabled()
                    && mensaje.getText().equals("No existen registros para los filtros seleccionados."), "Pantalla sin resultados");
            for (String invalida : new String[]{"2020-01-15", "31/02/2020", "1/1/2020", "texto"}) {
                campo(panel, "desde", JTextField.class).setText(invalida);
                boton(panel, "Generar reporte").doClick();
                comprobar(mensaje.getText().contains("DD/MM/AAAA") && !exportar.isEnabled(), "Fecha inválida: " + invalida);
            }
            boton(panel, "Limpiar").doClick();
            comprobar(campo(panel, "desde", JTextField.class).getText().isEmpty() && cursos.getSelectedIndex() == 0
                    && tabla.getRowCount() == 0, "Limpiar filtros");
            boton(panel, "Generar reporte").doClick();
            comprobar(tabla.getRowCount() >= 4, "Sin filtros muestra todos los registros");
        });
        System.out.println("OK: " + total + " comprobaciones MySQL del módulo Reportes.");
    }

    private static java.awt.Component encontrarScroll(java.awt.Container c) {
        for (java.awt.Component hijo : c.getComponents()) {
            if (hijo instanceof JScrollPane) return hijo;
            if (hijo instanceof java.awt.Container padre) {
                java.awt.Component encontrado = encontrarScroll(padre);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
}
