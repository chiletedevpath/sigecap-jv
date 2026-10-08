package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.controlador.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlPersonal extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    private final PersonalControlador controlador;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField dni = new JTextField(18), nombres = new JTextField(18),
            apellidos = new JTextField(18), cargo = new JTextField(18), buscar = new JTextField();
    private final JComboBox<String> estado = new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});
    private JTable listado;
    private int idEdicion;

    public PnlPersonal(PersonalControlador controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout()); add(crearContenido(), BorderLayout.CENTER); refrescar();
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

        JLabel titulo = new JLabel("Personal");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Registro y consulta del personal de Operaciones");
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
        tabs.addTab("Listado", crearListado());
        tabs.addTab("Registrar trabajador", crearRegistro());
        tabs.addTab("Historial", crearHistorial());
        tabs.setEnabledAt(2, false);
        tabs.setToolTipTextAt(2, "Pendiente: asistencia, resultados y certificados");

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(22, 0, 0, 0));
        centro.add(tabs, BorderLayout.CENTER);

        contenedor.add(cabecera, BorderLayout.NORTH);
        contenedor.add(centro, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearListado() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BLANCO);
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel filtros = new JPanel(new BorderLayout(12, 0));
        filtros.setOpaque(false);

        buscar.setPreferredSize(new Dimension(360, 38));
        buscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(0, 10, 0, 10)
        ));

        JButton btnBuscar = crearBotonSecundario("Buscar");
        JButton btnNuevo = crearBotonPrimario("Nuevo trabajador");

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(btnBuscar);
        acciones.add(btnNuevo);
        JButton editar = crearBotonSecundario("Editar trabajador"); acciones.add(editar);
        btnBuscar.addActionListener(e -> refrescar()); buscar.addActionListener(e -> refrescar());
        btnNuevo.addActionListener(e -> { limpiar(); tabs.setSelectedIndex(1); });
        editar.addActionListener(e -> editarSeleccionado());

        JPanel buscador = new JPanel(new BorderLayout(8, 0));
        buscador.setOpaque(false);
        JLabel lbl = new JLabel("Buscar:");
        lbl.setForeground(TEXTO);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buscador.add(lbl, BorderLayout.WEST);
        buscador.add(buscar, BorderLayout.CENTER);

        filtros.add(buscador, BorderLayout.WEST);
        filtros.add(acciones, BorderLayout.EAST);

        String[] columnas = {"DNI", "Apellidos y nombres", "Cargo", "Estado"};
        listado = crearTabla(new Object[0][0], columnas);
        JTable tabla = listado;
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearRegistro() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);
        panel.setBorder(new EmptyBorder(26, 26, 26, 26));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        agregarCampo(formulario, gbc, 0, 0, "DNI", dni);
        agregarCampo(formulario, gbc, 1, 0, "Nombres", nombres);
        agregarCampo(formulario, gbc, 0, 1, "Apellidos", apellidos);
        agregarCampo(formulario, gbc, 1, 1, "Cargo", cargo);

        agregarCampo(formulario, gbc, 0, 2, "Estado", estado);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton cancelar = crearBotonSecundario("Cancelar"), guardar = crearBotonPrimario("Guardar");
        botones.add(cancelar); botones.add(guardar);
        cancelar.addActionListener(e -> { limpiar(); tabs.setSelectedIndex(0); });
        guardar.addActionListener(e -> guardar());

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(22, 8, 8, 8);
        formulario.add(botones, gbc);

        panel.add(formulario);
        return panel;
    }

    // Diseño pendiente de conectar al historial real.
    private JPanel crearHistorial() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BLANCO);
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel superior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        superior.setOpaque(false);

        JLabel lblTrabajador = new JLabel("Trabajador:");
        lblTrabajador.setForeground(TEXTO);
        lblTrabajador.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JComboBox<String> trabajador = new JComboBox<>(new String[]{
                "Juan Carlos Pérez Soto",
                "María Elena Torres Ruiz",
                "Carlos Alberto Rojas Díaz"
        });
        trabajador.setPreferredSize(new Dimension(280, 36));

        superior.add(lblTrabajador);
        superior.add(trabajador);
        superior.add(crearBotonSecundario("Consultar"));

        String[] columnas = {"Curso", "Fecha", "Resultado", "Vigencia"};
        Object[][] datos = {
                {"Formación Básica", "12/04/2026", "Aprobado", "Vigente"},
                {"Protección Portuaria", "20/06/2026", "Apto", "Vigente"},
                {"Perfeccionamiento", "10/08/2026", "Aprobado", "Próximo a vencer"}
        };

        JTable tabla = crearTabla(datos, columnas);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));

        panel.add(superior, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void guardar() {
        try {
            Trabajador trabajador = new Trabajador(idEdicion, dni.getText(), nombres.getText(), apellidos.getText(),
                    cargo.getText(), (String) estado.getSelectedItem());
            if (idEdicion == 0) controlador.registrarTrabajador(trabajador);
            else controlador.actualizarTrabajador(trabajador);
            limpiar(); refrescar(); tabs.setSelectedIndex(0);
            JOptionPane.showMessageDialog(this, "Trabajador guardado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Revisar datos", JOptionPane.WARNING_MESSAGE);
        }
    }
    private void editarSeleccionado() {
        int fila = listado.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Seleccione un trabajador."); return; }
        String seleccionado = (String) listado.getValueAt(fila, 0);
        Trabajador t = controlador.listarTrabajadores().stream().filter(x -> x.getDni().equals(seleccionado)).findFirst().orElseThrow();
        idEdicion = t.getIdTrabajador(); tabs.setTitleAt(1, "Editar trabajador"); dni.setText(t.getDni()); nombres.setText(t.getNombres());
        apellidos.setText(t.getApellidos()); cargo.setText(t.getCargo()); estado.setSelectedItem(t.getEstado()); tabs.setSelectedIndex(1);
    }
    private void limpiar() {
        idEdicion = 0; tabs.setTitleAt(1, "Registrar trabajador"); dni.setText(""); nombres.setText(""); apellidos.setText(""); cargo.setText(""); estado.setSelectedIndex(0);
    }
    public void refrescar() {
        DefaultTableModel modelo = (DefaultTableModel) listado.getModel(); modelo.setRowCount(0);
        String filtro = buscar.getText().trim().toLowerCase(java.util.Locale.ROOT);
        for (Trabajador t : controlador.listarTrabajadores()) {
            if ((t.getDni() + " " + t.getApellidos() + " " + t.getNombres() + " " + t.getCargo()).toLowerCase(java.util.Locale.ROOT).contains(filtro))
                modelo.addRow(new Object[]{t.getDni(), t.getApellidos() + ", " + t.getNombres(), t.getCargo(), t.getEstado()});
        }
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int col, int fila, String etiqueta, JComponent campo) {
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

        if (campo instanceof JTextField textField) {
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDE),
                    new EmptyBorder(0, 9, 0, 9)
            ));
        }

        campo.setAlignmentX(Component.LEFT_ALIGNMENT);

        bloque.add(label);
        bloque.add(Box.createVerticalStrut(6));
        bloque.add(campo);

        gbc.gridx = col;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        panel.add(bloque, gbc);
    }

    private JTable crearTabla(Object[][] datos, String[] columnas) {
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

        return tabla;
    }

    private JButton crearBotonPrimario(String texto) {
        JButton boton = new JButton(texto);
        boton.setBackground(AZUL_CLARO);
        boton.setForeground(BLANCO);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(10, 16, 10, 16));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton boton = new JButton(texto);
        boton.setBackground(BLANCO);
        boton.setForeground(TEXTO);
        boton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(9, 14, 9, 14)
        ));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }
}
