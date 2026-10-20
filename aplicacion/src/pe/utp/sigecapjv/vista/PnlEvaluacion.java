package pe.utp.sigecapjv.vista;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;

/** Dos pestañas del mismo flujo: evaluación y registro del certificado recibido. */
public class PnlEvaluacion extends JPanel {
    private final boolean certificados;
    private final CapacitacionControlador gestion;
    private final SeguimientoControlador seguimiento;
    private final JComboBox<Capacitacion> selector = new JComboBox<>();
    private final JComboBox<String> resultado = new JComboBox<>(new String[]{"APROBADO", "DESAPROBADO", "APTO", "NO_APTO"});
    private final JTextField nota = new JTextField(6), numero = new JTextField(15), emision = new JTextField(10),
            vencimiento = new JTextField(10), evidencia = new JTextField(25);
    private final JButton guardar = new JButton("Guardar");
    private final JLabel mensaje = new JLabel("Seleccione una capacitación y un trabajador.");
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private List<Participacion> filas = List.of();
    private List<Resultado> resultados = List.of();
    private List<Certificado> documentos = List.of();
    private boolean cargando;

    public PnlEvaluacion(CapacitacionControlador gestion, SeguimientoControlador seguimiento, boolean certificados) {
        this.gestion = gestion;
        this.seguimiento = seguimiento;
        this.certificados = certificados;
        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(22,22,22,22));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Capacitación:"));
        selector.setPreferredSize(new Dimension(420,36));
        filtros.add(selector);
        JButton actualizar = new JButton("Actualizar"); filtros.add(actualizar);
        add(filtros, BorderLayout.NORTH);
        modelo = new DefaultTableModel(certificados
                ? new String[]{"Trabajador", "N.° certificado", "Emisión", "Vencimiento", "Vigencia"}
                : new String[]{"Trabajador", "Resultado", "Calificación", "Fecha de registro"}, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(31);
        tabla.setFont(new Font("Segoe UI",Font.PLAIN,13));
        tabla.setSelectionBackground(new Color(224,235,246));
        tabla.setGridColor(new Color(232,235,239));
        tabla.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        tabla.getTableHeader().setBackground(new Color(239,243,247));
        tabla.setFillsViewportHeight(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout(0,10)); pie.setOpaque(false);
        JPanel campos = new JPanel(new GridLayout(0,2,12,8)); campos.setOpaque(false);
        if (certificados) {
            campo(campos,"N.° certificado",numero);
            campo(campos,"Emisión (AAAA-MM-DD)",emision);
            campo(campos,"Vencimiento (vacío: no aplica)",vencimiento);
            JPanel archivo = new JPanel(new BorderLayout(5,0)); archivo.setOpaque(false);
            archivo.add(evidencia,BorderLayout.CENTER);
            JButton elegir = new JButton("Elegir archivo"); archivo.add(elegir,BorderLayout.EAST);
            elegir.addActionListener(e -> {
                JFileChooser dialogo = new JFileChooser();
                if (dialogo.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) evidencia.setText(dialogo.getSelectedFile().getAbsolutePath());
            });
            campo(campos,"Ruta de evidencia (opcional)",archivo);
        } else {
            campo(campos,"Resultado recibido",resultado);
            campo(campos,"Nota de 0 a 20 (opcional)",nota);
        }
        pie.add(campos,BorderLayout.NORTH);
        pie.add(mensaje,BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); acciones.setOpaque(false);
        guardar.setBackground(new Color(46,102,158)); guardar.setForeground(Color.WHITE);
        guardar.setFont(new Font("Segoe UI",Font.BOLD,13)); guardar.setBorder(new EmptyBorder(10,16,10,16));
        guardar.setEnabled(false); acciones.add(guardar); pie.add(acciones,BorderLayout.SOUTH);
        add(pie,BorderLayout.SOUTH);
        selector.addActionListener(e -> { if (!cargando) cargar(); });
        actualizar.addActionListener(e -> refrescar());
        tabla.getSelectionModel().addListSelectionListener(e -> seleccionar());
        guardar.addActionListener(e -> guardar());
    }

    private void campo(JPanel panel, String titulo, JComponent componente) {
        JPanel bloque = new JPanel(new BorderLayout(0,4)); bloque.setOpaque(false);
        bloque.add(new JLabel(titulo),BorderLayout.NORTH); bloque.add(componente,BorderLayout.CENTER);
        panel.add(bloque);
    }

    public void refrescar() {
        Capacitacion anterior = (Capacitacion) selector.getSelectedItem();
        cargando = true;
        try {
            List<Capacitacion> lista = gestion.listarCapacitaciones();
            selector.removeAllItems();
            for (Capacitacion c : lista) {
                selector.addItem(c);
                if (anterior != null && anterior.getIdCapacitacion() == c.getIdCapacitacion()) selector.setSelectedItem(c);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            selector.removeAllItems(); limpiar(); mensaje.setText(ex.getMessage()); return;
        } finally { cargando = false; }
        cargar();
    }

    private void limpiar() {
        filas = List.of(); resultados = List.of(); documentos = List.of(); modelo.setRowCount(0);
        guardar.setEnabled(false);
        numero.setText(""); emision.setText(""); vencimiento.setText(""); evidencia.setText(""); nota.setText("");
    }

    private void cargar() {
        limpiar();
        Capacitacion cap = (Capacitacion) selector.getSelectedItem();
        if (cap == null) { mensaje.setText("No hay capacitaciones registradas."); return; }
        try {
            List<Participacion> participantes = gestion.listarParticipaciones().stream()
                    .filter(p -> p.getCapacitacion().getIdCapacitacion() == cap.getIdCapacitacion()).toList();
            List<Resultado> evaluaciones = new ArrayList<>();
            List<Certificado> registros = new ArrayList<>();
            for (Participacion p : participantes) {
                evaluaciones.add(seguimiento.consultarResultado(p.getIdParticipacion()));
                registros.add(certificados ? seguimiento.consultarCertificado(p.getIdParticipacion()) : null);
            }
            filas = participantes; resultados = evaluaciones; documentos = registros;
            for (int i = 0; i < filas.size(); i++) {
                Trabajador t = filas.get(i).getTrabajador();
                String nombre = t.getNombres() + " " + t.getApellidos() + " (" + t.getDni() + ")";
                Resultado r = resultados.get(i); Certificado c = documentos.get(i);
                if (certificados) modelo.addRow(new Object[]{nombre, c == null ? "Sin registrar" : c.getNumero(),
                        c == null ? "" : c.getFechaEmision(), c == null ? "" : c.getFechaVencimiento() == null ? "No aplica" : c.getFechaVencimiento(),
                        c == null ? "" : c.getEstadoVigencia()});
                else modelo.addRow(new Object[]{nombre, r == null ? "Sin registrar" : r.getCondicion(),
                        r == null || r.getCalificacion() == null ? "" : r.getCalificacion(), r == null ? "" : r.getFechaRegistro()});
            }
            mensaje.setText(filas.isEmpty() ? "No hay trabajadores asignados." : "Seleccione un trabajador.");
        } catch (IllegalArgumentException | IllegalStateException ex) { limpiar(); mensaje.setText(ex.getMessage()); }
    }

    private void seleccionar() {
        int i = tabla.getSelectedRow();
        if (i < 0 || i >= filas.size()) { guardar.setEnabled(false); return; }
        Participacion p = filas.get(i); Capacitacion cap = p.getCapacitacion();
        boolean editable = !"CANCELADA".equals(p.getEstado()) && !"CANCELADA".equals(cap.getEstado()) && !cap.getFecha().isAfter(LocalDate.now());
        Resultado r = resultados.get(i); Certificado c = documentos.get(i);
        guardar.setEnabled(editable);
        if (certificados) {
            numero.setText(c == null ? "" : c.getNumero());
            emision.setText(c == null ? LocalDate.now().toString() : c.getFechaEmision().toString());
            vencimiento.setText(c == null || c.getFechaVencimiento() == null ? "" : c.getFechaVencimiento().toString());
            evidencia.setText(c == null || c.getRutaEvidencia() == null ? "" : c.getRutaEvidencia());
        } else {
            resultado.setSelectedItem(r == null ? "APROBADO" : r.getCondicion());
            nota.setText(r == null || r.getCalificacion() == null ? "" : r.getCalificacion().toPlainString());
        }
        guardar.setText((certificados ? c != null : r != null) ? "Actualizar registro" : "Guardar registro");
        mensaje.setText(editable ? (certificados ? "Registre las fechas del certificado recibido. La ruta no copia el archivo." : "El resultado requiere asistencia registrada como ASISTIO.") : "Solo consulta: capacitación futura o cancelada.");
    }

    private void guardar() {
        int i = tabla.getSelectedRow(); if (i < 0 || i >= filas.size()) return;
        try {
            int id = filas.get(i).getIdParticipacion();
            if (certificados) {
                LocalDate desde = LocalDate.parse(emision.getText().trim());
                LocalDate hasta = vencimiento.getText().isBlank() ? null : LocalDate.parse(vencimiento.getText().trim());
                if (documentos.get(i) == null) seguimiento.registrarCertificado(id,numero.getText(),desde,hasta,evidencia.getText());
                else seguimiento.actualizarCertificado(id,numero.getText(),desde,hasta,evidencia.getText());
            } else {
                BigDecimal valor = nota.getText().isBlank() ? null : new BigDecimal(nota.getText().trim().replace(',', '.'));
                if (resultados.get(i) == null) seguimiento.registrarResultado(id,(String) resultado.getSelectedItem(),valor);
                else seguimiento.actualizarResultado(id,(String) resultado.getSelectedItem(),valor);
            }
            cargar();
        } catch (java.time.format.DateTimeParseException ex) { mensaje.setText("Use fechas válidas con formato AAAA-MM-DD."); }
        catch (NumberFormatException ex) { mensaje.setText("Ingrese una nota numérica de 0 a 20."); }
        catch (IllegalArgumentException | IllegalStateException ex) { mensaje.setText(ex.getMessage()); }
    }
}
