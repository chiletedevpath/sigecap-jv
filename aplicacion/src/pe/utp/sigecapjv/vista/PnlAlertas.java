package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlAlertas extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    // Diseño conservado para la siguiente etapa; todavía utiliza datos de ejemplo.
    public PnlAlertas() {
        setLayout(new BorderLayout());
        add(crearContenido(), BorderLayout.CENTER);
    }

    private JPanel crearContenido() {
        JPanel contenedor = new JPanel(
                new BorderLayout()
        );

        contenedor.setBackground(FONDO);

        contenedor.setBorder(
                new EmptyBorder(24, 28, 26, 28)
        );

        contenedor.add(
                crearCabecera(),
                BorderLayout.NORTH
        );

        JPanel centro = new JPanel();

        centro.setOpaque(false);

        centro.setLayout(
                new BoxLayout(
                        centro,
                        BoxLayout.Y_AXIS
                )
        );

        centro.add(
                Box.createVerticalStrut(22)
        );

        centro.add(crearTarjetas());

        centro.add(
                Box.createVerticalStrut(22)
        );

        centro.add(crearPanelAlertas());

        JScrollPane scroll =
                new JScrollPane(centro);

        scroll.setBorder(null);
        scroll.setOpaque(false);

        scroll.getViewport().setOpaque(false);

        scroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        contenedor.add(
                scroll,
                BorderLayout.CENTER
        );

        return contenedor;
    }

    private JPanel crearCabecera() {
        JPanel cabecera =
                new JPanel(new BorderLayout());

        cabecera.setOpaque(false);

        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);

        izquierda.setLayout(
                new BoxLayout(
                        izquierda,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel titulo =
                new JLabel("Alertas");

        titulo.setForeground(TEXTO);

        titulo.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        26
                )
        );

        JLabel subtitulo =
                new JLabel(
                        "Consulta de alertas relacionadas con mis capacitaciones"
                );

        subtitulo.setForeground(TEXTO_SUAVE);

        subtitulo.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        izquierda.add(titulo);

        izquierda.add(
                Box.createVerticalStrut(4)
        );

        izquierda.add(subtitulo);

        JPanel usuario = new JPanel();

        usuario.setOpaque(false);

        usuario.setLayout(
                new BoxLayout(
                        usuario,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel nombre =
                new JLabel(
                        "Trabajador del área de Operaciones"
                );

        nombre.setForeground(TEXTO);

        nombre.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        nombre.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        JLabel estado =
                new JLabel("Usuario activo");

        estado.setForeground(TEXTO_SUAVE);

        estado.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        estado.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        usuario.add(nombre);
        usuario.add(estado);

        cabecera.add(
                izquierda,
                BorderLayout.WEST
        );

        cabecera.add(
                usuario,
                BorderLayout.EAST
        );

        return cabecera;
    }

    private JPanel crearTarjetas() {
        JPanel tarjetas =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                14,
                                0
                        )
                );

        tarjetas.setOpaque(false);

        tarjetas.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        110
                )
        );

        tarjetas.setPreferredSize(
                new Dimension(
                        800,
                        110
                )
        );

        tarjetas.add(
                crearTarjeta(
                        "Nuevas asignaciones",
                        "1"
                )
        );

        tarjetas.add(
                crearTarjeta(
                        "Recordatorios",
                        "1"
                )
        );

        tarjetas.add(
                crearTarjeta(
                        "Próximas a vencer",
                        "1"
                )
        );

        return tarjetas;
    }

    private JPanel crearTarjeta(
            String titulo,
            String valor
    ) {

        JPanel tarjeta = new JPanel();

        tarjeta.setBackground(BLANCO);

        tarjeta.setLayout(
                new BoxLayout(
                        tarjeta,
                        BoxLayout.Y_AXIS
                )
        );

        tarjeta.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory
                                .createLineBorder(BORDE),

                        new EmptyBorder(
                                18,
                                18,
                                16,
                                18
                        )
                )
        );

        JLabel lblTitulo =
                new JLabel(titulo);

        lblTitulo.setForeground(
                TEXTO_SUAVE
        );

        lblTitulo.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        lblTitulo.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JLabel lblValor =
                new JLabel(valor);

        lblValor.setForeground(TEXTO);

        lblValor.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        28
                )
        );

        lblValor.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        tarjeta.add(lblTitulo);

        tarjeta.add(
                Box.createVerticalStrut(8)
        );

        tarjeta.add(lblValor);

        return tarjeta;
    }

    private JPanel crearPanelAlertas() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(BLANCO);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory
                                .createLineBorder(BORDE),

                        new EmptyBorder(
                                16,
                                16,
                                16,
                                16
                        )
                )
        );

        panel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        360
                )
        );

        panel.setPreferredSize(
                new Dimension(
                        850,
                        360
                )
        );

        JPanel superior =
                new JPanel(
                        new BorderLayout()
                );

        superior.setOpaque(false);

        JLabel titulo =
                new JLabel(
                        "Listado de alertas"
                );

        titulo.setForeground(TEXTO);

        titulo.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        16
                )
        );

        JPanel filtros =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                8,
                                0
                        )
                );

        filtros.setOpaque(false);

        JLabel lblTipo =
                new JLabel("Tipo:");

        lblTipo.setForeground(TEXTO);

        lblTipo.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        JComboBox<String> cboTipo =
                new JComboBox<>(
                        new String[]{
                                "Todas",
                                "Nueva asignación",
                                "Recordatorio",
                                "Próximo vencimiento"
                        }
                );

        cboTipo.setPreferredSize(
                new Dimension(
                        190,
                        32
                )
        );

        cboTipo.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        JButton consultar =
                new JButton("Consultar");

        consultar.setBackground(
                AZUL_CLARO
        );

        consultar.setForeground(
                BLANCO
        );

        consultar.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        consultar.setFocusPainted(false);

        consultar.setBorder(
                new EmptyBorder(
                        8,
                        14,
                        8,
                        14
                )
        );

        filtros.add(lblTipo);
        filtros.add(cboTipo);
        filtros.add(consultar);

        superior.add(
                titulo,
                BorderLayout.WEST
        );

        superior.add(
                filtros,
                BorderLayout.EAST
        );

        String[] columnas = {
                "Fecha",
                "Tipo",
                "Capacitación",
                "Detalle",
                "Estado"
        };

        Object[][] datos = {

                {
                        "24/09/2026",
                        "Próximo vencimiento",
                        "Formación Básica",
                        "Vence el 15/10/2026",
                        "Generada"
                },

                {
                        "23/09/2026",
                        "Recordatorio",
                        "Protección Portuaria",
                        "Programada para el 02/10/2026",
                        "Generada"
                },

                {
                        "20/09/2026",
                        "Nueva asignación",
                        "Formación Básica",
                        "Asignada para el 28/09/2026",
                        "Generada"
                }

        };

        DefaultTableModel modelo =
                new DefaultTableModel(
                        datos,
                        columnas
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {
                        return false;
                    }
                };

        JTable tabla =
                new JTable(modelo);

        tabla.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        tabla.setRowHeight(31);

        tabla.setForeground(TEXTO);

        tabla.setGridColor(
                new Color(
                        232,
                        235,
                        239
                )
        );

        tabla.setSelectionBackground(
                new Color(
                        224,
                        235,
                        246
                )
        );

        tabla.getTableHeader()
                .setFont(
                        new Font(
                                "Segoe UI",
                                Font.BOLD,
                                12
                        )
                );

        tabla.getTableHeader()
                .setBackground(
                        new Color(
                                239,
                                243,
                                247
                        )
                );

        tabla.getTableHeader()
                .setForeground(TEXTO);

        tabla.setFillsViewportHeight(true);

        /*
         * Ajustamos las columnas para que
         * "Detalle" tenga mayor espacio.
         */
        tabla.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(90);

        tabla.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(150);

        tabla.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(160);

        tabla.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(280);

        tabla.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(90);

        JScrollPane scroll =
                new JScrollPane(tabla);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(
                                232,
                                235,
                                239
                        )
                )
        );

        panel.add(
                superior,
                BorderLayout.NORTH
        );

        panel.add(
                scroll,
                BorderLayout.CENTER
        );

        return panel;
    }
}