package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlReportes extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    // Diseño conservado para la siguiente etapa; todavía utiliza datos de ejemplo.
    public PnlReportes() {
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

        JLabel titulo = new JLabel("Reportes");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Consulta y exportación de información de capacitaciones");
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

        JPanel centro = new JPanel(new BorderLayout(0, 18));
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(22, 0, 0, 0));

        JPanel filtros = crearFiltros();
        JPanel resultados = crearResultados();

        centro.add(filtros, BorderLayout.NORTH);
        centro.add(resultados, BorderLayout.CENTER);

        contenedor.add(cabecera, BorderLayout.NORTH);
        contenedor.add(centro, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearFiltros() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(18, 18, 18, 18)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        agregarCampo(panel, g, 0, 0, "Desde", new JTextField("01/09/2026"));
        agregarCampo(panel, g, 1, 0, "Hasta", new JTextField("30/09/2026"));
        agregarCampo(panel, g, 2, 0, "Curso", new JComboBox<>(new String[]{
                "Todos", "Formación Básica", "Perfeccionamiento", "Protección Portuaria"
        }));

        agregarCampo(panel, g, 0, 1, "Trabajador", new JComboBox<>(new String[]{
                "Todos", "Juan Carlos Pérez Soto", "María Elena Torres Ruiz", "Carlos Alberto Rojas Díaz"
        }));
        agregarCampo(panel, g, 1, 1, "Estado", new JComboBox<>(new String[]{
                "Todos", "Programada", "Pendiente", "Completada"
        }));
        agregarCampo(panel, g, 2, 1, "Vigencia", new JComboBox<>(new String[]{
                "Todas", "Vigente", "Próxima a vencer", "Vencida"
        }));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(botonSecundario("Limpiar"));
        acciones.add(botonPrimario("Generar reporte"));

        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 3;
        g.insets = new Insets(12, 6, 0, 6);
        panel.add(acciones, g);

        return panel;
    }

    private JPanel crearResultados() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BLANCO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);

        JLabel titulo = new JLabel("Resultado del reporte");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton exportar = botonPrimario("Exportar PDF");

        superior.add(titulo, BorderLayout.WEST);
        superior.add(exportar, BorderLayout.EAST);

        String[] columnas = {
                "Trabajador", "Curso", "Fecha", "Estado", "Resultado", "Vigencia"
        };

        Object[][] datos = {
                {"Juan Carlos Pérez Soto", "Formación Básica", "12/04/2026", "Completada", "Aprobado", "Vigente"},
                {"María Elena Torres Ruiz", "Protección Portuaria", "20/06/2026", "Completada", "Apto", "Vigente"},
                {"Carlos Alberto Rojas Díaz", "Perfeccionamiento", "05/09/2026", "Completada", "Aprobado", "Vencida"},
                {"Ana Lucía Vásquez León", "Formación Básica", "28/09/2026", "Programada", "-", "Pendiente"}
        };

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

        panel.add(superior, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints g, int col, int fila, String etiqueta, JComponent campo) {
        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(new BoxLayout(bloque, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(etiqueta);
        label.setForeground(TEXTO);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setPreferredSize(new Dimension(220, 36));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (campo instanceof JTextField tf) {
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDE),
                    new EmptyBorder(0, 9, 0, 9)
            ));
        }

        bloque.add(label);
        bloque.add(Box.createVerticalStrut(5));
        bloque.add(campo);

        g.gridx = col;
        g.gridy = fila;
        g.gridwidth = 1;
        panel.add(bloque, g);
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
