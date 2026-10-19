package pe.utp.sigecapjv;

import java.awt.*;
import java.time.*;
import javax.swing.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.vista.PnlAsistencia;

public final class PruebaPantallaAsistencia {
    public static void verificarMySql(CapacitacionControlador gestion, SeguimientoControlador seguimiento) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PnlAsistencia panel = new PnlAsistencia(gestion, seguimiento);
            panel.refrescar();
            JTable tabla = componente(panel, JTable.class, x -> true);
            comprobar(tabla.getRowCount() == 1);
            comprobar("JUSTIFICADA".equals(tabla.getValueAt(0, 1)));
            tabla.setRowSelectionInterval(0, 0);
            JComboBox<?> condicion = componente(panel, JComboBox.class, x -> x.getItemCount() == 3);
            JButton guardar = componente(panel, JButton.class, x -> x.getText().equals("Actualizar asistencia"));
            condicion.setSelectedItem("NO_ASISTIO");
            guardar.doClick();
            comprobar("NO_ASISTIO".equals(tabla.getValueAt(0, 1)));
            panel.refrescar();
            comprobar("NO_ASISTIO".equals(tabla.getValueAt(0, 1)));
            tabla.setRowSelectionInterval(0,0);
            condicion.setSelectedItem("JUSTIFICADA"); guardar.doClick();
            comprobar("JUSTIFICADA".equals(tabla.getValueAt(0, 1)));
        });
        System.out.println("OK: " + comprobaciones + " comprobaciones Swing con MySQL.");
    }
    private static int comprobaciones;
    private static void comprobar(boolean valor) {
        if (!valor) throw new AssertionError("Falló la pantalla de asistencia.");
        comprobaciones++;
    }
    private static <T> T componente(Container padre, Class<T> tipo, java.util.function.Predicate<T> filtro) {
        for (Component c : padre.getComponents()) {
            if (tipo.isInstance(c) && filtro.test(tipo.cast(c))) return tipo.cast(c);
            if (c instanceof Container hijo) {
                T encontrado = componente(hijo, tipo, filtro);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
    public static void verificar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Curso curso = new Curso(1, "C1", "Curso", "Interno", null, "ACTIVO");
            Capacitacion cap = new Capacitacion(1, LocalDate.now(), LocalTime.of(9,0), LocalTime.of(10,0), "Presencial", "PROGRAMADA", curso);
            Trabajador t = new Trabajador(1, "12345678", "Persona", "Prueba", "Operaciones", "ACTIVO");
            Participacion p = new Participacion(1, LocalDate.now(), "PENDIENTE", t, cap);
            CapacitacionControlador gestion = new CapacitacionControlador(new PersonalControlador()) {
                @Override public java.util.List<Capacitacion> listarCapacitaciones() { return java.util.List.of(cap); }
                @Override public java.util.List<Participacion> listarParticipaciones() { return java.util.List.of(p); }
            };
            class SeguimientoPrueba extends SeguimientoControlador {
                Asistencia dato;
                boolean falla;
                @Override public Asistencia consultarAsistencia(int id) { return dato; }
                @Override public Asistencia registrarAsistencia(int id, String valor) {
                    dato = new Asistencia(1, id, valor, LocalDateTime.now()); return dato;
                }
                @Override public Asistencia actualizarAsistencia(int id, String valor) {
                    if (falla) throw new IllegalStateException("Conexión no disponible.");
                    dato.setCondicion(valor); return dato;
                }
            }
            SeguimientoPrueba seguimiento = new SeguimientoPrueba();
            PnlAsistencia panel = new PnlAsistencia(gestion, seguimiento);
            panel.refrescar();
            JTable tabla = componente(panel, JTable.class, x -> true);
            JButton guardar = componente(panel, JButton.class, x -> x.getText().equals("Guardar asistencia"));
            JComboBox<?> condicion = componente(panel, JComboBox.class, x -> x.getItemCount() == 3);
            comprobar(tabla.getRowCount() == 1 && "Sin registrar".equals(tabla.getValueAt(0, 1)));
            comprobar(!guardar.isEnabled());
            tabla.setRowSelectionInterval(0, 0);
            comprobar(guardar.isEnabled());
            guardar.doClick();
            comprobar("ASISTIO".equals(tabla.getValueAt(0, 1)));
            tabla.setRowSelectionInterval(0, 0);
            condicion.setSelectedItem("JUSTIFICADA");
            guardar.doClick();
            comprobar("JUSTIFICADA".equals(tabla.getValueAt(0, 1)));
            seguimiento.falla = true;
            tabla.setRowSelectionInterval(0, 0);
            condicion.setSelectedItem("NO_ASISTIO");
            guardar.doClick();
            comprobar("JUSTIFICADA".equals(tabla.getValueAt(0, 1)));
            comprobar(componente(panel, JLabel.class, x -> x.getText().equals("Conexión no disponible.")) != null);
            cap.setFecha(LocalDate.now().plusDays(1)); panel.refrescar(); tabla.setRowSelectionInterval(0,0);
            comprobar(!guardar.isEnabled());
            cap.setFecha(LocalDate.now()); cap.setEstado("CERRADA"); panel.refrescar(); tabla.setRowSelectionInterval(0,0);
            comprobar(!guardar.isEnabled());
        });
        System.out.println("OK: " + comprobaciones + " comprobaciones Swing de asistencia.");
    }
}
