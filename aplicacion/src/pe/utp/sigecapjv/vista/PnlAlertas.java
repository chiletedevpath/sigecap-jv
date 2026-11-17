package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.NotificacionesControlador;
import pe.utp.sigecapjv.modelo.Notificacion;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Consulta autorizada por sesión; todos los valores proceden de MySQL. */
public class PnlAlertas extends JPanel {
    private final NotificacionesControlador controlador;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Trabajador (ID)", "Fecha", "Tipo", "Detalle", "Estado"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Todos", "ASIGNACION", "REPROGRAMACION", "CANCELACION", "RECORDATORIO", "POR_VENCER", "VENCIDO"});
    private final JComboBox<String> estado = new JComboBox<>(new String[]{"Todos", "PENDIENTE", "LEIDA"});
    private final JLabel mensaje = new JLabel("Pulse Consultar para cargar las alertas.");

    public PnlAlertas(NotificacionesControlador controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(0, 18));
        setBackground(new Color(245,247,249));
        setBorder(new EmptyBorder(24,28,26,28));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel(controlador.esTrabajador() ? "Mis alertas" : "Vigencias y notificaciones");
        titulo.setFont(new Font("Segoe UI",Font.BOLD,26));
        cabecera.add(titulo,BorderLayout.NORTH);
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Tipo")); filtros.add(tipo);
        filtros.add(new JLabel("Estado")); filtros.add(estado);
        JButton consultar = new JButton("Consultar");
        consultar.addActionListener(e -> refrescar()); filtros.add(consultar);
        JButton leida = new JButton("Marcar como leída");
        leida.setEnabled(controlador.puedeMarcarLeida());
        leida.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) { mensaje.setText("Seleccione una notificación."); return; }
            try {
                controlador.marcarComoLeida((Integer) modelo.getValueAt(tabla.convertRowIndexToModel(fila),0));
                refrescar();
            } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) { modelo.setRowCount(0); mensaje.setText(ex.getMessage()); }
        });
        filtros.add(leida);
        if (controlador.puedeConfigurar()) {
            JButton configurar = new JButton("Configurar plazos");
            configurar.addActionListener(e -> configurarPlazos());
            cabecera.add(configurar, BorderLayout.EAST);
        }
        cabecera.add(filtros,BorderLayout.CENTER);
        add(cabecera,BorderLayout.NORTH);
        tabla.setRowHeight(30); tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(440);
        add(new JScrollPane(tabla),BorderLayout.CENTER);
        add(mensaje,BorderLayout.SOUTH);
    }

    private void configurarPlazos() {
        try {
            var anterior = controlador.consultarPlazos();
            JSpinner vencimiento = new JSpinner(new SpinnerNumberModel(anterior.vencimiento(), 0, 365, 1));
            JSpinner recordatorio = new JSpinner(new SpinnerNumberModel(anterior.recordatorio(), 0, 365, 1));
            Object[] campos = {"Días antes del vencimiento del certificado", vencimiento,
                    "Días antes de la capacitación", recordatorio};
            if (JOptionPane.showConfirmDialog(this, campos, "Anticipación de alertas",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            vencimiento.commitEdit();
            recordatorio.commitEdit();
            var nuevo = new NotificacionesControlador.Plazos((int) vencimiento.getValue(), (int) recordatorio.getValue());
            if (!nuevo.equals(anterior)) controlador.guardarPlazos(anterior, nuevo);
            refrescar();
        } catch (java.text.ParseException ex) {
            mensaje.setText("Ingrese días enteros entre 0 y 365.");
        } catch (IllegalStateException | SecurityException | IllegalArgumentException ex) {
            mensaje.setText(ex.getMessage());
        }
    }

    public void refrescar() {
        modelo.setRowCount(0);
        try {
            for (Notificacion n : controlador.obtenerAlertasActualizadas()) {
                if (tipo.getSelectedIndex()>0 && !tipo.getSelectedItem().equals(n.getTipo())) continue;
                if (estado.getSelectedIndex()>0 && !estado.getSelectedItem().equals(n.getEstado())) continue;
                modelo.addRow(new Object[]{n.getIdNotificacion(),n.getIdTrabajador(),n.getFechaGeneracion(),n.getTipo(),n.getMensaje(),n.getEstado()});
            }
            mensaje.setText(modelo.getRowCount()==0 ? "No existen alertas para los filtros seleccionados."
                    : modelo.getRowCount()+" notificación(es).");
        } catch (SecurityException | IllegalStateException ex) { mensaje.setText(ex.getMessage()); }
    }
}
