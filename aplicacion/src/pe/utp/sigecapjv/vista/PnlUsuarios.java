package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlUsuarios extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    // Diseño conservado para la siguiente etapa; todavía utiliza datos de ejemplo.
    public PnlUsuarios() {
        setLayout(new BorderLayout());
        add(crearContenido(), BorderLayout.CENTER);
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

        JLabel titulo = new JLabel("Usuarios");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Administración de usuarios y roles de acceso");
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(subtitulo);

        JLabel usuario = new JLabel("Administrador");
        usuario.setForeground(TEXTO);
        usuario.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cabecera.add(titulos, BorderLayout.WEST);
        cabecera.add(usuario, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.setBackground(BLANCO);
        tabs.addTab("Listado", crearListado());
        tabs.addTab("Registrar usuario", crearRegistro());
        tabs.addTab("Roles y permisos", crearRoles());

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(22, 0, 0, 0));
        centro.add(tabs, BorderLayout.CENTER);

        contenedor.add(cabecera, BorderLayout.NORTH);
        contenedor.add(centro, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearListado() {
        JPanel panel = panelBase();

        JPanel superior = new JPanel(new BorderLayout(10, 0));
        superior.setOpaque(false);

        JPanel buscador = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buscador.setOpaque(false);
        buscador.add(new JLabel("Buscar:"));

        JTextField txtBuscar = campoTexto(28);
        buscador.add(txtBuscar);
        buscador.add(botonSecundario("Buscar"));

        JButton nuevo = botonPrimario("Nuevo usuario");

        superior.add(buscador, BorderLayout.WEST);
        superior.add(nuevo, BorderLayout.EAST);

        String[] columnas = {"Usuario", "Nombre", "Rol", "Estado"};
        Object[][] datos = {
                {"apisco", "Adrián Pisco", "Administrador", "Activo"},
                {"jcamac", "Jorge Sacha", "Responsable de capacitaciones", "Activo"},
                {"lmachaca", "Lijhoan Machaca", "Supervisor de Operaciones", "Activo"},
                {"cchirihuana", "Christopher Chirihuana", "Trabajador", "Inactivo"}
        };

        panel.add(superior, BorderLayout.NORTH);
        panel.add(scrollTabla(datos, columnas), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(botonSecundario("Editar"));
        acciones.add(botonSecundario("Desactivar"));
        panel.add(acciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearRegistro() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);
        panel.setBorder(new EmptyBorder(26, 26, 26, 26));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        agregarCampo(formulario, g, 0, 0, "Usuario", campoTexto(18));
        agregarCampo(formulario, g, 1, 0, "Nombre completo", campoTexto(18));
        agregarCampo(formulario, g, 0, 1, "Correo", campoTexto(18));
        agregarCampo(formulario, g, 1, 1, "Contraseña", new JPasswordField(18));

        JComboBox<String> rol = new JComboBox<>(new String[]{
                "Administrador",
                "Responsable de capacitaciones",
                "Supervisor de Operaciones",
                "Trabajador"
        });
        agregarCampo(formulario, g, 0, 2, "Rol", rol);

        JComboBox<String> estado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
        agregarCampo(formulario, g, 1, 2, "Estado", estado);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(botonSecundario("Cancelar"));
        botones.add(botonPrimario("Guardar"));

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        g.insets = new Insets(22, 8, 8, 8);
        formulario.add(botones, g);

        panel.add(formulario);
        return panel;
    }

    private JPanel crearRoles() {
        JPanel panel = panelBase();

        JPanel superior = new JPanel(new GridBagLayout());
        superior.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        JComboBox<String> usuario = new JComboBox<>(new String[]{
                "apisco - Adrián Pisco",
                "jcamac - Jorge Sacha",
                "lmachaca - Lijhoan Machaca",
                "cchirihuana - Christopher Chirihuana"
        });

        JComboBox<String> rol = new JComboBox<>(new String[]{
                "Administrador",
                "Responsable de capacitaciones",
                "Supervisor de Operaciones",
                "Trabajador"
        });

        agregarCampo(superior, g, 0, 0, "Usuario", usuario);
        agregarCampo(superior, g, 1, 0, "Rol asignado", rol);

        JPanel accion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accion.setOpaque(false);
        accion.add(botonPrimario("Actualizar rol"));

        g.gridx = 0;
        g.gridy = 1;
        g.gridwidth = 2;
        g.insets = new Insets(16, 8, 8, 8);
        superior.add(accion, g);

        String[] columnas = {"Funcionalidad", "Administrador", "Responsable", "Supervisor", "Trabajador"};
        Object[][] datos = {
                {"Personal", "Sí", "Sí", "Consulta", "No"},
                {"Capacitaciones", "Sí", "Sí", "Consulta", "Consulta propia"},
                {"Seguimiento", "Sí", "Sí", "Sí", "Consulta propia"},
                {"Reportes", "Sí", "No", "Sí", "No"},
                {"Usuarios", "Sí", "No", "No", "No"}
        };

        panel.add(superior, BorderLayout.NORTH);
        panel.add(scrollTabla(datos, columnas), BorderLayout.CENTER);

        return panel;
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

    private void agregarCampo(JPanel panel, GridBagConstraints g, int col, int fila, String etiqueta, JComponent campo) {
        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(new BoxLayout(bloque, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(etiqueta);
        label.setForeground(TEXTO);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setPreferredSize(new Dimension(270, 38));
        campo.setMaximumSize(new Dimension(270, 38));
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
        campo.setPreferredSize(new Dimension(270, 38));
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
