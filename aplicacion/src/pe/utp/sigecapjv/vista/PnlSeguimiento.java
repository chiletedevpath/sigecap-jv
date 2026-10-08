package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlSeguimiento extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    // Diseño conservado para la siguiente etapa; todavía utiliza datos de ejemplo.
    public PnlSeguimiento() {
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

        JLabel titulo = new JLabel("Seguimiento");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Control de vigencias y alertas de capacitación");
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
        tabs.addTab("Vigencias", crearVigencias());
        tabs.addTab("Alertas", crearAlertas());

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(22, 0, 0, 0));
        centro.add(tabs, BorderLayout.CENTER);

        contenedor.add(cabecera, BorderLayout.NORTH);
        contenedor.add(centro, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearVigencias() {
        JPanel panel = panelBase();

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);

        filtros.add(new JLabel("Estado:"));
        JComboBox<String> estado = new JComboBox<>(new String[]{
                "Todos", "Vigente", "Próximo a vencer", "Vencido"
        });
        estado.setPreferredSize(new Dimension(180, 36));
        filtros.add(estado);

        filtros.add(new JLabel("Curso:"));
        JComboBox<String> curso = new JComboBox<>(new String[]{
                "Todos", "Formación Básica", "Perfeccionamiento", "Protección Portuaria"
        });
        curso.setPreferredSize(new Dimension(220, 36));
        filtros.add(curso);

        filtros.add(botonSecundario("Consultar"));

        String[] columnas = {"Trabajador", "Curso", "Vencimiento", "Días restantes", "Estado"};
        Object[][] datos = {
                {"Juan Carlos Pérez Soto", "Formación Básica", "12/10/2026", "18", "Próximo a vencer"},
                {"María Elena Torres Ruiz", "Protección Portuaria", "25/10/2026", "31", "Próximo a vencer"},
                {"Carlos Alberto Rojas Díaz", "Perfeccionamiento", "05/09/2026", "0", "Vencido"},
                {"Ana Lucía Vásquez León", "Formación Básica", "18/03/2027", "175", "Vigente"}
        };

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scrollTabla(datos, columnas), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearAlertas() {
        JPanel panel = panelBase();

        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);

        JLabel descripcion = new JLabel("Alertas generadas según las fechas y capacitaciones registradas");
        descripcion.setForeground(TEXTO_SUAVE);
        descripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JComboBox<String> tipo = new JComboBox<>(new String[]{
                "Todas", "Nueva asignación", "Recordatorio", "Próximo vencimiento"
        });
        tipo.setPreferredSize(new Dimension(210, 36));

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtro.setOpaque(false);
        filtro.add(new JLabel("Tipo:"));
        filtro.add(tipo);

        superior.add(descripcion, BorderLayout.WEST);
        superior.add(filtro, BorderLayout.EAST);

        String[] columnas = {"Fecha", "Trabajador", "Tipo de alerta", "Capacitación", "Estado"};
        Object[][] datos = {
                {"24/09/2026", "Juan Carlos Pérez Soto", "Próximo vencimiento", "Formación Básica", "Generada"},
                {"23/09/2026", "María Elena Torres Ruiz", "Recordatorio", "Protección Portuaria", "Generada"},
                {"22/09/2026", "Ana Lucía Vásquez León", "Nueva asignación", "Perfeccionamiento", "Generada"},
                {"20/09/2026", "Carlos Alberto Rojas Díaz", "Próximo vencimiento", "Perfeccionamiento", "Generada"}
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
