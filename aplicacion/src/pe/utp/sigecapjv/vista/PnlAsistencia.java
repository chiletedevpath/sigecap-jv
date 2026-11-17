package pe.utp.sigecapjv.vista;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;

/** Registro individual de asistencia sobre participantes ya asignados. */
public class PnlAsistencia extends JPanel {
    private final CapacitacionControlador capacitaciones;
    private final SeguimientoControlador seguimiento;
    private final JComboBox<Capacitacion> selector = new JComboBox<>();
    private final JComboBox<String> condicion = new JComboBox<>(new String[]{"ASISTIO", "NO_ASISTIO", "JUSTIFICADA"});
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Trabajador", "Asistencia", "Fecha de registro"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JButton guardar = new JButton("Guardar asistencia");
    private final JLabel mensaje = new JLabel("Seleccione una capacitación y un trabajador.");
    private List<Participacion> participantes = List.of();
    private List<Asistencia> asistencias = List.of();
    private boolean cargando;

    public PnlAsistencia(CapacitacionControlador capacitaciones, SeguimientoControlador seguimiento) {
        this.capacitaciones = capacitaciones;
        this.seguimiento = seguimiento;
        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(22, 22, 22, 22));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Capacitación:"));
        selector.setPreferredSize(new Dimension(420, 36));
        filtros.add(selector);
        JButton actualizar = new JButton("Actualizar");
        filtros.add(actualizar);
        add(filtros, BorderLayout.NORTH);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(31);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setSelectionBackground(new Color(224, 235, 246));
        tabla.setGridColor(new Color(232, 235, 239));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(new Color(239, 243, 247));
        tabla.setFillsViewportHeight(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(0, 10));
        pie.setOpaque(false);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(new JLabel("Condición:"));
        acciones.add(condicion);
        guardar.setBackground(new Color(46, 102, 158));
        guardar.setForeground(Color.WHITE);
        guardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        guardar.setBorder(new EmptyBorder(10, 16, 10, 16));
        guardar.setEnabled(false);
        acciones.add(guardar);
        pie.add(mensaje, BorderLayout.NORTH);
        pie.add(acciones, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);
        selector.addActionListener(e -> { if (!cargando) cargarParticipantes(); });
        actualizar.addActionListener(e -> refrescar());
        tabla.getSelectionModel().addListSelectionListener(e -> seleccionar());
        guardar.addActionListener(e -> guardar());
    }

    public void refrescar() {
        Capacitacion anterior = (Capacitacion) selector.getSelectedItem();
        cargando = true;
        try {
            List<Capacitacion> lista = capacitaciones.listarCapacitaciones();
            selector.removeAllItems();
            for (Capacitacion c : lista) {
                selector.addItem(c);
                if (anterior != null && anterior.getIdCapacitacion() == c.getIdCapacitacion()) selector.setSelectedItem(c);
            }
        } catch (IllegalStateException ex) {
            selector.removeAllItems();
            limpiar();
            mensaje.setText(ex.getMessage());
            return;
        } finally { cargando = false; }
        cargarParticipantes();
    }

    private void limpiar() {
        participantes = List.of();
        asistencias = List.of();
        modelo.setRowCount(0);
        guardar.setEnabled(false);
    }

    private void cargarParticipantes() {
        limpiar();
        Capacitacion c = (Capacitacion) selector.getSelectedItem();
        if (c == null) { mensaje.setText("No hay capacitaciones registradas."); return; }
        try {
            List<Participacion> filas = capacitaciones.listarParticipaciones().stream()
                    .filter(p -> p.getCapacitacion().getIdCapacitacion() == c.getIdCapacitacion()).toList();
            java.util.ArrayList<Asistencia> registros = new java.util.ArrayList<>();
            for (Participacion p : filas) registros.add(seguimiento.consultarAsistencia(p.getIdParticipacion()));
            participantes = filas;
            asistencias = registros;
            for (int i = 0; i < filas.size(); i++) {
                Trabajador t = filas.get(i).getTrabajador();
                Asistencia a = registros.get(i);
                modelo.addRow(new Object[]{t.getNombres() + " " + t.getApellidos() + " (" + t.getDni() + ")",
                        a == null ? "Sin registrar" : a.getCondicion(), a == null ? "" : a.getFechaRegistro()});
            }
            mensaje.setText(filas.isEmpty() ? "No hay trabajadores asignados." : "Seleccione un trabajador para registrar o actualizar su asistencia.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            limpiar();
            mensaje.setText(ex.getMessage());
        }
    }

    private void seleccionar() {
        int fila = tabla.getSelectedRow();
        Capacitacion c = (Capacitacion) selector.getSelectedItem();
        boolean seleccion = fila >= 0 && fila < participantes.size();
        boolean editable = seleccion && c != null && "PROGRAMADA".equals(c.getEstado())
                && !c.getFecha().isAfter(LocalDate.now())
                && !"CANCELADA".equals(participantes.get(fila).getEstado());
        guardar.setEnabled(editable);
        condicion.setEnabled(editable);
        if (seleccion) {
            Asistencia a = asistencias.get(fila);
            condicion.setSelectedItem(a == null ? "ASISTIO" : a.getCondicion());
            guardar.setText(a == null ? "Guardar asistencia" : "Actualizar asistencia");
            if (!editable) mensaje.setText("Consulta: capacitación futura, cerrada/cancelada o participación cancelada.");
            else mensaje.setText("Seleccione la condición y guarde la asistencia.");
        }
    }

    private void guardar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= participantes.size()) return;
        try {
            int id = participantes.get(fila).getIdParticipacion();
            String valor = (String) condicion.getSelectedItem();
            if (asistencias.get(fila) == null) seguimiento.registrarAsistencia(id, valor);
            else seguimiento.actualizarAsistencia(id, valor);
            cargarParticipantes();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje.setText(ex.getMessage());
        }
    }
}
