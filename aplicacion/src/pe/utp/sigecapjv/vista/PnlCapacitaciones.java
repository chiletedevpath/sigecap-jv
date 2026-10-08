package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.controlador.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlCapacitaciones extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    private final CapacitacionControlador controlador;
    private final PersonalControlador personal;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField buscar = new JTextField(20), fecha = new JTextField(16), horario = new JTextField(16);
    private final JComboBox<Curso> curso = new JComboBox<>();
    private final JComboBox<String> modalidad = new JComboBox<>(new String[]{"Presencial", "Virtual"});
    private final JComboBox<Capacitacion> cap = new JComboBox<>();
    private JTable tablaCursos, tablaProgramacion, tablaParticipantes;
    private java.util.List<Curso> cursosVisibles = java.util.List.of();
    private java.util.List<Capacitacion> programacionesVisibles = java.util.List.of();
    private int idEdicion;

    public PnlCapacitaciones(CapacitacionControlador controlador, PersonalControlador personal) {
        this.controlador = controlador; this.personal = personal;
        setLayout(new BorderLayout()); add(crearContenido(), BorderLayout.CENTER); refrescar();
        tabs.addChangeListener(e -> refrescar());
    }

    private JPanel crearContenido() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.setBorder(new EmptyBorder(24, 28, 26, 28));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Capacitaciones");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Gestión de cursos y capacitaciones");
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(subtitulo);

        JLabel usuario = new JLabel("Responsable de capacitaciones");
        usuario.setForeground(TEXTO);
        usuario.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cabecera.add(titulos, BorderLayout.WEST);
        cabecera.add(usuario, BorderLayout.EAST);

        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.setBackground(BLANCO);
        tabs.addTab("Cursos", crearCursos());
        tabs.addTab("Programación", crearProgramacion());
        tabs.addTab("Participantes", crearParticipantes());
        tabs.addTab("Asistencia", crearAsistencia());
        tabs.addTab("Resultados", crearResultados());
        tabs.addTab("Certificados", crearCertificados());
        for (int i = 3; i < 6; i++) { tabs.setEnabledAt(i, false); tabs.setToolTipTextAt(i, "Funcionalidad pendiente de implementación"); }

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(22, 0, 0, 0));
        centro.add(tabs, BorderLayout.CENTER);

        contenedor.add(cabecera, BorderLayout.NORTH);
        contenedor.add(centro, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearCursos() {
        JPanel panel = panelBase();

        JPanel superior = new JPanel(new BorderLayout(10, 0));
        superior.setOpaque(false);

        JButton btnBuscar = botonSecundario("Buscar");
        JButton btnNuevo = botonPrimario("Nuevo curso");

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnBuscar);
        acciones.add(btnNuevo);
        JButton editar = botonSecundario("Editar curso"); acciones.add(editar);
        btnBuscar.addActionListener(e -> refrescar()); buscar.addActionListener(e -> refrescar());
        btnNuevo.addActionListener(e -> editarCurso(null));
        editar.addActionListener(e -> {
            int fila = tablaCursos.getSelectedRow();
            if (fila < 0) JOptionPane.showMessageDialog(this, "Seleccione un curso.");
            else editarCurso(cursosVisibles.get(fila));
        });

        JPanel buscador = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buscar.setPreferredSize(new Dimension(240, 38));
        buscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buscador.setOpaque(false);
        buscador.add(new JLabel("Buscar:"));
        buscador.add(buscar);

        superior.add(buscador, BorderLayout.CENTER);
        superior.add(acciones, BorderLayout.EAST);

        String[] columnas = {"Código", "Curso", "Tipo", "Vigencia", "Estado"};
        tablaCursos = tablaVacia(columnas);
        panel.add(superior, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaCursos), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearProgramacion() {
        JPanel panel = panelBase();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = baseGbc();

        agregarCampo(form, g, 0, 0, "Curso", curso);
        fecha.setToolTipText("Fecha en formato AAAA-MM-DD, por ejemplo 2026-12-15");
        horario.setToolTipText("Horario en formato HH:mm - HH:mm, por ejemplo 09:00 - 13:00");
        agregarCampo(form, g, 1, 0, "Fecha (AAAA-MM-DD)", fecha);
        agregarCampo(form, g, 0, 1, "Horario (HH:mm - HH:mm)", horario);
        agregarCampo(form, g, 1, 1, "Modalidad", modalidad);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton limpiar = botonSecundario("Limpiar"), guardar = botonPrimario("Guardar");
        JButton editar = botonSecundario("Editar"), cancelar = botonSecundario("Cancelar sesión"), cerrar = botonSecundario("Cerrar sesión");
        botones.add(limpiar); botones.add(editar); botones.add(cancelar); botones.add(cerrar); botones.add(guardar);
        limpiar.addActionListener(e -> limpiarProgramacion()); guardar.addActionListener(e -> guardarProgramacion());
        editar.addActionListener(e -> editarProgramacion());
        cancelar.addActionListener(e -> cambiarEstado(false)); cerrar.addActionListener(e -> cambiarEstado(true));

        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        g.insets = new Insets(22, 8, 8, 8);
        form.add(botones, g);

        panel.add(form, BorderLayout.NORTH);

        String[] columnas = {"Curso", "Fecha", "Horario", "Modalidad", "Estado"};
        tablaProgramacion = tablaVacia(columnas);
        panel.add(new JScrollPane(tablaProgramacion), BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearParticipantes() {
        JPanel panel = panelBase();

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Capacitación:"));
        cap.setPreferredSize(new Dimension(420, 36));
        filtros.add(cap);
        JButton asignar = botonPrimario("Asignar trabajador"); filtros.add(asignar);
        asignar.addActionListener(e -> asignar()); cap.addActionListener(e -> refrescarParticipantes());

        String[] columnas = {"DNI", "Trabajador", "Cargo", "Estado"};
        tablaParticipantes = tablaVacia(columnas);
        panel.add(filtros, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaParticipantes), BorderLayout.CENTER);
        return panel;
    }

    // Diseños conservados para las próximas etapas, todavía sin lógica de negocio.
    private JPanel crearAsistencia() {
        JPanel panel = panelBase();

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Capacitación:"));
        JComboBox<String> cap = new JComboBox<>();
        cap.setPreferredSize(new Dimension(300, 36));
        filtros.add(cap);

        String[] columnas = {"Trabajador", "Asistencia", "Fecha de registro"};
        Object[][] datos = new Object[0][columnas.length];

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scrollTabla(datos, columnas), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearResultados() {
        JPanel panel = panelBase();

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Capacitación:"));
        JComboBox<String> cap = new JComboBox<>();
        cap.setPreferredSize(new Dimension(300, 36));
        filtros.add(cap);

        String[] columnas = {"Trabajador", "Resultado", "Calificación"};
        Object[][] datos = new Object[0][columnas.length];

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scrollTabla(datos, columnas), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearCertificados() {
        JPanel panel = panelBase();

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Trabajador:"));
        JComboBox<String> trabajador = new JComboBox<>();
        trabajador.setPreferredSize(new Dimension(280, 36));
        filtros.add(trabajador);
        filtros.add(botonPrimario("Registrar certificado"));

        String[] columnas = {"Curso", "N.° certificado", "Emisión", "Vencimiento", "Estado"};
        Object[][] datos = new Object[0][columnas.length];

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scrollTabla(datos, columnas), BorderLayout.CENTER);
        return panel;
    }

    private JTable tablaVacia(String[] columnas) {
        return (JTable) scrollTabla(new Object[0][0], columnas).getViewport().getView();
    }
    private boolean ejecutar(Runnable accion) {
        try { accion.run(); refrescar(); return true; }
        catch (IllegalArgumentException | IllegalStateException | java.time.DateTimeException ex) {
            String mensaje = ex instanceof java.time.format.DateTimeParseException
                    ? "Revise la fecha (AAAA-MM-DD) y el horario (HH:mm - HH:mm)." : ex.getMessage();
            JOptionPane.showMessageDialog(this, mensaje, "Revisar datos", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }
    private void editarCurso(Curso actual) {
        JTextField codigo = campoTexto(18), nombre = campoTexto(18), tipo = campoTexto(18), vigencia = campoTexto(18);
        JComboBox<String> estado = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});
        if (actual != null) {
            codigo.setText(actual.getCodigo()); nombre.setText(actual.getNombre()); tipo.setText(actual.getTipo());
            vigencia.setText(actual.getVigenciaMeses() == null ? "" : actual.getVigenciaMeses().toString()); estado.setSelectedItem(actual.getEstado());
        }
        JPanel form = new JPanel(new GridBagLayout()); GridBagConstraints g = baseGbc();
        agregarCampo(form, g, 0, 0, "Código", codigo); agregarCampo(form, g, 1, 0, "Nombre", nombre);
        agregarCampo(form, g, 0, 1, "Tipo", tipo); agregarCampo(form, g, 1, 1, "Vigencia en meses (vacío: no aplica)", vigencia);
        agregarCampo(form, g, 0, 2, "Estado", estado);
        while (JOptionPane.showConfirmDialog(this, form, actual == null ? "Nuevo curso" : "Editar curso",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                Integer meses = vigencia.getText().isBlank() ? null : Integer.valueOf(vigencia.getText().trim());
                Curso c = new Curso(actual == null ? 0 : actual.getIdCurso(), codigo.getText(), nombre.getText(), tipo.getText(), meses, "ACTIVO");
                if ("ACTIVO".equals(estado.getSelectedItem())) c.activar(); else c.desactivar();
                if (actual == null) controlador.registrarCurso(c); else controlador.actualizarCurso(c);
                refrescar(); return;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex instanceof NumberFormatException ? "La vigencia debe ser un número entero de meses." : ex.getMessage());
            }
        }
    }
    private void guardarProgramacion() {
        if (ejecutar(() -> {
            String[] horas = horario.getText().split("-", -1);
            if (horas.length != 2) throw new IllegalArgumentException("Use el horario HH:mm - HH:mm.");
            Capacitacion c = new Capacitacion(idEdicion, java.time.LocalDate.parse(fecha.getText().trim()),
                    java.time.LocalTime.parse(horas[0].trim()), java.time.LocalTime.parse(horas[1].trim()),
                    (String) modalidad.getSelectedItem(), null, (Curso) curso.getSelectedItem());
            if (idEdicion == 0) controlador.programarCapacitacion(c); else controlador.actualizarCapacitacion(c);
            limpiarProgramacion();
        })) JOptionPane.showMessageDialog(this, "Programación guardada correctamente.");
    }
    private void limpiarProgramacion() { idEdicion = 0; fecha.setText(""); horario.setText(""); }
    private void editarProgramacion() {
        int fila = tablaProgramacion.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Seleccione una capacitación."); return; }
        Capacitacion c = programacionesVisibles.get(fila); idEdicion = c.getIdCapacitacion();
        fecha.setText(c.getFecha().toString()); horario.setText(c.getHoraInicio() + " - " + c.getHoraFin()); modalidad.setSelectedItem(c.getModalidad());
        for (int i = 0; i < curso.getItemCount(); i++) if (curso.getItemAt(i).getIdCurso() == c.getCurso().getIdCurso()) curso.setSelectedIndex(i);
    }
    private void cambiarEstado(boolean cerrar) {
        int fila = tablaProgramacion.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Seleccione una capacitación."); return; }
        Capacitacion c = programacionesVisibles.get(fila);
        if (JOptionPane.showConfirmDialog(this, (cerrar ? "¿Cerrar" : "¿Cancelar") + " la capacitación seleccionada?",
                "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            ejecutar(() -> { if (cerrar) controlador.cerrarCapacitacion(c.getIdCapacitacion()); else controlador.cancelarCapacitacion(c.getIdCapacitacion()); limpiarProgramacion(); });
    }
    private void asignar() {
        Capacitacion seleccionada = (Capacitacion) cap.getSelectedItem();
        if (seleccionada == null) { JOptionPane.showMessageDialog(this, "Primero programe una capacitación."); return; }
        Object[] trabajadores = personal.listarTrabajadores().toArray();
        if (trabajadores.length == 0) { JOptionPane.showMessageDialog(this, "Primero registre un trabajador en Personal."); return; }
        Trabajador t = (Trabajador) JOptionPane.showInputDialog(this, "Trabajador", "Asignar trabajador",
                JOptionPane.PLAIN_MESSAGE, null, trabajadores, trabajadores[0]);
        if (t != null && ejecutar(() -> controlador.asignarTrabajador(t, seleccionada)))
            JOptionPane.showMessageDialog(this, "Trabajador asignado correctamente.");
    }
    public void refrescar() {
        int cursoId = curso.getSelectedItem() == null ? 0 : ((Curso) curso.getSelectedItem()).getIdCurso();
        int capId = cap.getSelectedItem() == null ? 0 : ((Capacitacion) cap.getSelectedItem()).getIdCapacitacion();
        curso.removeAllItems(); cap.removeAllItems();
        java.util.List<Curso> todos = controlador.listarCursos();
        String filtro = buscar.getText().trim().toLowerCase(java.util.Locale.ROOT);
        cursosVisibles = todos.stream().filter(c -> (c.getCodigo()+" "+c.getNombre()+" "+c.getTipo()).toLowerCase(java.util.Locale.ROOT).contains(filtro)).toList();
        DefaultTableModel m = (DefaultTableModel) tablaCursos.getModel(); m.setRowCount(0);
        for (Curso c : cursosVisibles) m.addRow(new Object[]{c.getCodigo(), c.getNombre(), c.getTipo(), c.getVigenciaMeses() == null ? "No aplica" : c.getVigenciaMeses() + " meses", c.getEstado()});
        for (Curso c : todos) { curso.addItem(c); if (c.getIdCurso() == cursoId) curso.setSelectedItem(c); }
        programacionesVisibles = controlador.listarCapacitaciones(); m = (DefaultTableModel) tablaProgramacion.getModel(); m.setRowCount(0);
        for (Capacitacion c : programacionesVisibles) {
            m.addRow(new Object[]{c.getCurso().getNombre(), c.getFecha(), c.getHoraInicio()+" - "+c.getHoraFin(), c.getModalidad(), c.getEstado()});
            cap.addItem(c); if (c.getIdCapacitacion() == capId) cap.setSelectedItem(c);
        }
        refrescarParticipantes();
    }
    private void refrescarParticipantes() {
        if (tablaParticipantes == null) return;
        DefaultTableModel m = (DefaultTableModel) tablaParticipantes.getModel(); m.setRowCount(0);
        Capacitacion c = (Capacitacion) cap.getSelectedItem(); if (c == null) return;
        for (Participacion p : controlador.listarParticipaciones()) if (p.getCapacitacion().getIdCapacitacion() == c.getIdCapacitacion()) {
            Trabajador t = personal.buscarTrabajador(p.getTrabajador().getIdTrabajador());
            m.addRow(new Object[]{t.getDni(), t.getNombres()+" "+t.getApellidos(), t.getCargo(), p.getEstado()});
        }
    }

    private JPanel panelBase() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BLANCO);
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));
        return panel;
    }

    private JScrollPane scrollTabla(Object[][] datos, String[] columnas) {
        DefaultTableModel modelo = new DefaultTableModel(datos, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(31);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(new Color(232, 235, 239));
        tabla.setSelectionBackground(new Color(224, 235, 246));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(new Color(239, 243, 247));
        tabla.getTableHeader().setForeground(TEXTO);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        return scroll;
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        return g;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints g, int col, int fila, String etiqueta, JComponent campo) {
        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(new BoxLayout(bloque, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(etiqueta);
        label.setForeground(TEXTO);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setPreferredSize(new Dimension(260, 38));
        campo.setMaximumSize(new Dimension(260, 38));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (campo instanceof JTextField tf) {
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDE),
                    new EmptyBorder(0, 9, 0, 9)
            ));
        }

        bloque.add(label);
        bloque.add(Box.createVerticalStrut(6));
        bloque.add(campo);

        g.gridx = col;
        g.gridy = fila;
        g.gridwidth = 1;
        panel.add(bloque, g);
    }

    private JTextField campoTexto(int columnas) {
        JTextField campo = new JTextField(columnas);
        campo.setPreferredSize(new Dimension(260, 38));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(0, 9, 0, 9)
        ));
        return campo;
    }

    private JButton botonPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(AZUL_CLARO);
        b.setForeground(BLANCO);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        return b;
    }

    private JButton botonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(BLANCO);
        b.setForeground(TEXTO);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(9, 14, 9, 14)
        ));
        return b;
    }
}
