package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.vista.PnlCapacitaciones;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.*;

public final class PruebaEliminarCursos {
    private static int total;
    static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }
    static Curso curso(String codigo) {
        return new Curso(0, codigo, "Curso " + codigo, "Interno", null, "ACTIVO");
    }
    static Capacitacion capacitacion(Curso curso) {
        return new Capacitacion(0, LocalDate.now().plusDays(1), LocalTime.of(9,0),
                LocalTime.of(10,0), "Presencial", null, curso);
    }
    static void rechazar(Runnable accion, Class<? extends RuntimeException> tipo) {
        try { accion.run(); } catch (RuntimeException ex) {
            if (!tipo.isInstance(ex)) throw ex;
            total++; return;
        }
        throw new AssertionError("Se permitió eliminar un curso protegido.");
    }
    public static void verificar() throws Exception {
        var personal = new PersonalControlador();
        var control = new CapacitacionControlador(personal);
        Curso libre = curso("BORRAR-LIBRE"); control.registrarCurso(libre);
        control.eliminarCurso(libre.getIdCurso());
        comprobar(control.listarCursos().isEmpty(), "Elimina el curso sin uso");
        rechazar(() -> control.eliminarCurso(libre.getIdCurso()), IllegalArgumentException.class);
        rechazar(() -> control.eliminarCurso(0), IllegalArgumentException.class);
        Curso usado = curso("BORRAR-USADO"); control.registrarCurso(usado);
        Capacitacion cap = capacitacion(usado); control.programarCapacitacion(cap);
        rechazar(() -> control.eliminarCurso(usado.getIdCurso()), IllegalStateException.class);
        control.cancelarCapacitacion(cap.getIdCapacitacion());
        rechazar(() -> control.eliminarCurso(usado.getIdCurso()), IllegalStateException.class);
        usado.desactivar(); control.actualizarCurso(usado);
        comprobar("INACTIVO".equals(control.listarCursos().get(0).getEstado()), "Permite desactivar el curso con historial");
        verificarVista(control, personal);
        System.out.println("OK: " + total + " comprobaciones de eliminación y confirmación Swing.");
    }
    public static void verificarVista(CapacitacionControlador control, PersonalControlador personal) throws Exception {
        Curso b = curso("BORRAR-VISTA-B"), a = curso("BORRAR-VISTA-A");
        control.registrarCurso(b); control.registrarCurso(a);
        SwingUtilities.invokeAndWait(() -> {
            Vista vista = new Vista(control, personal);
            campo(vista, "buscar", JTextField.class).setText("BORRAR-VISTA"); vista.refrescar();
            JTable tabla = campo(vista, "tablaCursos", JTable.class);
            tabla.setRowSorter(new TableRowSorter<>(tabla.getModel()));
            tabla.getRowSorter().toggleSortOrder(0);
            JButton eliminar = boton(vista, "Eliminar curso");
            eliminar.doClick();
            comprobar(vista.confirmaciones == 0 && vista.mensaje.contains("Seleccione"), "Sin selección no confirma ni elimina");
            tabla.setRowSelectionInterval(0,0);
            eliminar.doClick();
            comprobar(vista.elegido.getIdCurso() == a.getIdCurso(), "Confirma el curso correcto con tabla ordenada y filtrada");
            comprobar(control.listarCursos().stream().anyMatch(c -> c.getIdCurso()==a.getIdCurso()), "Cancelar conserva el curso");
            vista.aceptar = true; eliminar.doClick();
            comprobar(control.listarCursos().stream().noneMatch(c -> c.getIdCurso()==a.getIdCurso()), "Confirmar elimina el curso elegido");
            comprobar(control.listarCursos().stream().anyMatch(c -> c.getIdCurso()==b.getIdCurso()), "Conserva el otro curso");
            comprobar(tabla.getRowCount()==1 && vista.mensaje.contains("correctamente"), "Actualiza listado y confirma resultado");
        });
        control.eliminarCurso(b.getIdCurso());
    }
    private static final class Vista extends PnlCapacitaciones {
        boolean aceptar;
        int confirmaciones;
        Curso elegido;
        String mensaje = "";
        Vista(CapacitacionControlador c, PersonalControlador p) { super(c,p); }
        @Override protected boolean confirmarEliminacionCurso(Curso curso) {
            confirmaciones++; elegido = curso; return aceptar;
        }
        @Override protected void mostrarAvisoCurso(String texto, int tipo) { mensaje = texto; }
    }
    private static <T> T campo(PnlCapacitaciones panel, String nombre, Class<T> tipo) {
        try { var f=PnlCapacitaciones.class.getDeclaredField(nombre); f.setAccessible(true); return tipo.cast(f.get(panel)); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static JButton boton(Container panel, String texto) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JButton b && texto.equals(b.getText())) return b;
            if (c instanceof Container sub) { JButton b=boton(sub,texto); if (b!=null) return b; }
        }
        return null;
    }
}
