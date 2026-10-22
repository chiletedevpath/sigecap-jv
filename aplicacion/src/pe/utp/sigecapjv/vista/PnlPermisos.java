package pe.utp.sigecapjv.vista;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import pe.utp.sigecapjv.controlador.RolControlador;
import pe.utp.sigecapjv.modelo.*;

/** Edición de permisos dentro de la pestaña existente de Usuarios. */
public class PnlPermisos extends JPanel {
    private final RolControlador controlador;
    private final JComboBox<Rol> roles = new JComboBox<>();
    private final JButton guardar = new JButton("Guardar permisos"), recargar = new JButton("Recargar");
    private final JLabel mensaje = new JLabel("Seleccione un rol.");
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{"Permitido", "Módulo", "Permiso"}, 0) {
        @Override public Class<?> getColumnClass(int columna) { return columna == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int fila, int columna) {
            return columna == 0 && !ocupado && configuracion != null && controlador.puedeEditar()
                    && !configuracion.rol().nombre().equals("Trabajador")
                    && !(configuracion.rol().nombre().equals("Administrador")
                         && configuracion.catalogo().get(fila).modulo().equals("usuarios"));
        }
    };
    private final JTable tabla = new JTable(modelo);
    private RolControlador.Configuracion configuracion;
    private boolean ocupado;

    public PnlPermisos(RolControlador controlador) {
        this.controlador = java.util.Objects.requireNonNull(controlador);
        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(22, 22, 22, 22));
        for (Rol rol : controlador.listar()) roles.addItem(rol);
        JPanel seleccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        seleccion.setOpaque(false);
        seleccion.add(new JLabel("Rol:"));
        roles.setPreferredSize(new Dimension(290, 36));
        seleccion.add(roles);
        seleccion.add(recargar);
        add(seleccion, BorderLayout.NORTH);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(31);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(0).setMaxWidth(95);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.setSelectionBackground(new Color(224, 235, 246));
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new BorderLayout(12, 0));
        acciones.setOpaque(false);
        mensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        acciones.add(mensaje, BorderLayout.CENTER);
        guardar.setBackground(new Color(46, 102, 158));
        guardar.setForeground(Color.WHITE);
        guardar.setFocusPainted(false);
        guardar.setBorder(new EmptyBorder(10, 16, 10, 16));
        acciones.add(guardar, BorderLayout.EAST);
        add(acciones, BorderLayout.SOUTH);
        roles.addActionListener(e -> cargar());
        recargar.addActionListener(e -> cargar());
        guardar.addActionListener(e -> guardar());
        cargar();
    }

    private void cargar() {
        try {
            Rol rol = (Rol) roles.getSelectedItem();
            configuracion = null;
            modelo.setRowCount(0);
            guardar.setEnabled(false);
            if (rol == null) return;
            configuracion = controlador.consultar(rol.idRol());
            for (Permiso permiso : configuracion.catalogo())
                modelo.addRow(new Object[]{configuracion.asignados().contains(permiso.idPermiso()), permiso.modulo(), permiso.nombre()});
            guardar.setEnabled(controlador.puedeEditar() && !rol.nombre().equals("Trabajador"));
            mensaje.setText(rol.nombre().equals("Trabajador")
                    ? "Trabajador usa consultas propias, no acceso global."
                    : "Gestionar requiere consultar. Los cambios requieren nuevo login.");
        } catch (RuntimeException ex) { mensaje.setText(ex.getMessage()); }
    }

    private void guardar() {
        if (configuracion == null || ocupado) return;
        if (tabla.isEditing() && !tabla.getCellEditor().stopCellEditing()) return;
        Set<Integer> seleccionados = new HashSet<>();
        for (int i = 0; i < modelo.getRowCount(); i++)
            if (Boolean.TRUE.equals(modelo.getValueAt(i, 0))) seleccionados.add(configuracion.catalogo().get(i).idPermiso());
        RolControlador.Configuracion original = configuracion;
        ocupado = true;
        guardar.setEnabled(false); roles.setEnabled(false); recargar.setEnabled(false);
        mensaje.setText("Guardando permisos…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                controlador.guardar(original.rol().idRol(), original.asignados(), seleccionados);
                return null;
            }
            @Override protected void done() {
                ocupado = false;
                roles.setEnabled(true); recargar.setEnabled(true);
                guardar.setEnabled(controlador.puedeEditar());
                try {
                    get();
                    configuracion = new RolControlador.Configuracion(original.rol(), original.catalogo(), seleccionados);
                    mensaje.setText("Permisos guardados. Las sesiones afectadas deben ingresar de nuevo.");
                } catch (Exception ex) {
                    Throwable causa = ex instanceof java.util.concurrent.ExecutionException ? ex.getCause() : ex;
                    mensaje.setText(causa.getMessage());
                }
            }
        }.execute();
    }
}
