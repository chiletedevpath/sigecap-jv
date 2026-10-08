package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlMisCapacitaciones extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    // Diseño conservado para la siguiente etapa; todavía utiliza datos de ejemplo.
    public PnlMisCapacitaciones() {
        setLayout(new BorderLayout());
        add(crearContenido(), BorderLayout.CENTER);
    }

    private JPanel crearContenido() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.setBorder(new EmptyBorder(24, 28, 26, 28));

        contenedor.add(crearCabecera(), BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        centro.add(Box.createVerticalStrut(22));
        centro.add(crearTarjetas());
        centro.add(Box.createVerticalStrut(22));
        centro.add(crearPanelCapacitaciones());

        JScrollPane scroll = new JScrollPane(centro);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        contenedor.add(scroll, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Mis capacitaciones");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel(
                "Consulta de mis capacitaciones y certificaciones"
        );
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        izquierda.add(titulo);
        izquierda.add(Box.createVerticalStrut(4));
        izquierda.add(subtitulo);

        JPanel usuario = new JPanel();
        usuario.setOpaque(false);
        usuario.setLayout(new BoxLayout(usuario, BoxLayout.Y_AXIS));

        JLabel nombre = new JLabel("Trabajador del área de Operaciones");
        nombre.setForeground(TEXTO);
        nombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nombre.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel rol = new JLabel("Usuario activo");
        rol.setForeground(TEXTO_SUAVE);
        rol.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rol.setAlignmentX(Component.RIGHT_ALIGNMENT);

        usuario.add(nombre);
        usuario.add(rol);

        cabecera.add(izquierda, BorderLayout.WEST);
        cabecera.add(usuario, BorderLayout.EAST);

        return cabecera;
    }

    private JPanel crearTarjetas() {
        JPanel tarjetas = new JPanel(new GridLayout(1, 3, 14, 0));
        tarjetas.setOpaque(false);
        tarjetas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        tarjetas.setPreferredSize(new Dimension(800, 110));

        tarjetas.add(crearTarjeta("Asignadas", "1"));
        tarjetas.add(crearTarjeta("Pendientes", "1"));
        tarjetas.add(crearTarjeta("Completadas", "2"));

        return tarjetas;
    }

    private JPanel crearTarjeta(String titulo, String valor) {
        JPanel tarjeta = new JPanel();
        tarjeta.setBackground(BLANCO);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));

        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(18, 18, 16, 18)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(TEXTO_SUAVE);
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValor = new JLabel(valor);
        lblValor.setForeground(TEXTO);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);

        tarjeta.add(lblTitulo);
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(lblValor);

        return tarjeta;
    }

    private JPanel crearPanelCapacitaciones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(16, 16, 16, 16)
        ));

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        panel.setPreferredSize(new Dimension(850, 360));

        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);

        JLabel titulo = new JLabel("Listado de mis capacitaciones");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel filtros = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        filtros.setOpaque(false);

        JLabel lblEstado = new JLabel("Estado:");
        lblEstado.setForeground(TEXTO);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JComboBox<String> cboEstado = new JComboBox<>(new String[]{
                "Todas",
                "Asignadas",
                "Pendientes",
                "Completadas"
        });

        cboEstado.setPreferredSize(new Dimension(150, 32));
        cboEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton consultar = new JButton("Consultar");
        consultar.setBackground(AZUL_CLARO);
        consultar.setForeground(BLANCO);
        consultar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        consultar.setFocusPainted(false);
        consultar.setBorder(new EmptyBorder(8, 14, 8, 14));

        filtros.add(lblEstado);
        filtros.add(cboEstado);
        filtros.add(consultar);

        superior.add(titulo, BorderLayout.WEST);
        superior.add(filtros, BorderLayout.EAST);

        String[] columnas = {
                "Curso",
                "Fecha",
                "Estado",
                "Resultado",
                "Certificación",
                "Vigencia"
        };

        Object[][] datos = {
                {
                        "Formación Básica",
                        "28/09/2026",
                        "Asignada",
                        "Pendiente",
                        "Pendiente",
                        "No aplica"
                },
                {
                        "Protección Portuaria",
                        "02/10/2026",
                        "Pendiente",
                        "Pendiente",
                        "Pendiente",
                        "No aplica"
                },
                {
                        "Perfeccionamiento",
                        "08/07/2025",
                        "Completada",
                        "Aprobado",
                        "Registrado",
                        "08/07/2027"
                },
                {
                        "Formación Básica",
                        "15/10/2023",
                        "Completada",
                        "Aprobado",
                        "Registrado",
                        "15/10/2026"
                }
        };

        DefaultTableModel modelo =
                new DefaultTableModel(datos, columnas) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {
                        return false;
                    }
                };

        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(31);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(new Color(232, 235, 239));
        tabla.setSelectionBackground(
                new Color(224, 235, 246)
        );

        tabla.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 12)
        );

        tabla.getTableHeader().setBackground(
                new Color(239, 243, 247)
        );

        tabla.getTableHeader().setForeground(TEXTO);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(232, 235, 239)
                )
        );

        panel.add(superior, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
}
