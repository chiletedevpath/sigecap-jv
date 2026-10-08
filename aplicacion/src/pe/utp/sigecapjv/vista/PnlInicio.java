package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlInicio extends JPanel {

    public enum Perfil {
        ADMINISTRADOR,
        RESPONSABLE_CAPACITACIONES,
        SUPERVISOR_OPERACIONES,
        TRABAJADOR
    }

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    private final Perfil perfil;

    // Los cuatro perfiles se conservan como diseños referenciales, sin autenticar usuarios.
    public PnlInicio() { this(Perfil.ADMINISTRADOR); }
    public PnlInicio(Perfil perfil) {
        this.perfil = java.util.Objects.requireNonNull(perfil);
        setLayout(new BorderLayout());
        add(perfil == Perfil.ADMINISTRADOR ? crearContenidoAdministrador() : crearContenido(), BorderLayout.CENTER);
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

        agregarTablas(centro);

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

        JLabel titulo = new JLabel("Panel principal");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel(subtituloPerfil());
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        izquierda.add(titulo);
        izquierda.add(Box.createVerticalStrut(4));
        izquierda.add(subtitulo);

        JPanel usuario = new JPanel();
        usuario.setOpaque(false);
        usuario.setLayout(new BoxLayout(usuario, BoxLayout.Y_AXIS));

        JLabel nombre = new JLabel(nombrePerfil());
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

    private String nombrePerfil() {
        return switch (perfil) {
            case ADMINISTRADOR -> "Administrador";
            case RESPONSABLE_CAPACITACIONES -> "Responsable de Capacitaciones";
            case SUPERVISOR_OPERACIONES -> "Supervisor de Operaciones";
            case TRABAJADOR -> "Trabajador del área de Operaciones";
        };
    }

    private String subtituloPerfil() {
        return switch (perfil) {
            case ADMINISTRADOR -> "Resumen general de capacitaciones";
            case RESPONSABLE_CAPACITACIONES -> "Resumen de gestión y seguimiento de capacitaciones";
            case SUPERVISOR_OPERACIONES -> "Resumen de seguimiento operativo";
            case TRABAJADOR -> "Resumen de mis capacitaciones";
        };
    }

    private JPanel crearTarjetas() {
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 14, 0));
        tarjetas.setOpaque(false);
        tarjetas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        tarjetas.setPreferredSize(new Dimension(800, 110));

        switch (perfil) {
            case RESPONSABLE_CAPACITACIONES -> {
                tarjetas.add(crearTarjeta("Programadas", "12"));
                tarjetas.add(crearTarjeta("Pendientes", "8"));
                tarjetas.add(crearTarjeta("Próximas a vencer", "5"));
                tarjetas.add(crearTarjeta("Vencidas", "3"));
            }
            case SUPERVISOR_OPERACIONES -> {
                tarjetas.add(crearTarjeta("Personal con pendientes", "8"));
                tarjetas.add(crearTarjeta("Próximas a vencer", "5"));
                tarjetas.add(crearTarjeta("Vencidas", "3"));
                tarjetas.add(crearTarjeta("Programadas", "12"));
            }
            case TRABAJADOR -> {
                tarjetas.add(crearTarjeta("Asignadas", "4"));
                tarjetas.add(crearTarjeta("Pendientes", "2"));
                tarjetas.add(crearTarjeta("Completadas", "7"));
                tarjetas.add(crearTarjeta("Próximas a vencer", "1"));
            }
        }

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

    private void agregarTablas(JPanel centro) {
        switch (perfil) {
            case RESPONSABLE_CAPACITACIONES -> {
                centro.add(crearPanelTabla(
                        "Próximos vencimientos",
                        new String[]{"Trabajador", "Curso", "Vencimiento", "Estado"},
                        new Object[][]{
                                {"Juan Pérez", "Formación Básica", "15/10/2026", "Próximo"},
                                {"María Torres", "Protección Portuaria", "22/10/2026", "Próximo"},
                                {"Carlos Rojas", "Perfeccionamiento", "30/10/2026", "Próximo"}
                        }
                ));
                centro.add(Box.createVerticalStrut(18));
                centro.add(crearPanelTabla(
                        "Capacitaciones programadas",
                        new String[]{"Curso", "Fecha", "Horario", "Participantes"},
                        new Object[][]{
                                {"Formación Básica", "28/09/2026", "09:00 - 13:00", "18"},
                                {"Protección Portuaria", "02/10/2026", "08:30 - 12:30", "12"},
                                {"Perfeccionamiento", "06/10/2026", "14:00 - 18:00", "15"}
                        }
                ));
            }
            case SUPERVISOR_OPERACIONES -> {
                centro.add(crearPanelTabla(
                        "Seguimiento de vigencias",
                        new String[]{"Trabajador", "Curso", "Vencimiento", "Estado"},
                        new Object[][]{
                                {"Juan Pérez", "Formación Básica", "15/10/2026", "Próximo"},
                                {"María Torres", "Protección Portuaria", "22/10/2026", "Próximo"},
                                {"Carlos Rojas", "Perfeccionamiento", "05/09/2026", "Vencido"}
                        }
                ));
                centro.add(Box.createVerticalStrut(18));
                centro.add(crearPanelTabla(
                        "Estado de capacitación del personal",
                        new String[]{"Trabajador", "Asignadas", "Completadas", "Pendientes"},
                        new Object[][]{
                                {"Juan Pérez", "8", "7", "1"},
                                {"María Torres", "6", "5", "1"},
                                {"Carlos Rojas", "7", "5", "2"}
                        }
                ));
            }
            case TRABAJADOR -> {
                centro.add(crearPanelTabla(
                        "Mis próximas capacitaciones",
                        new String[]{"Curso", "Fecha", "Horario", "Estado"},
                        new Object[][]{
                                {"Formación Básica", "28/09/2026", "09:00 - 13:00", "Programada"},
                                {"Protección Portuaria", "02/10/2026", "08:30 - 12:30", "Pendiente"}
                        }
                ));
                centro.add(Box.createVerticalStrut(18));
                centro.add(crearPanelTabla(
                        "Mis vigencias",
                        new String[]{"Curso / Certificación", "Emisión", "Vencimiento", "Estado"},
                        new Object[][]{
                                {"Formación Básica", "15/10/2023", "15/10/2026", "Próximo"},
                                {"Protección Portuaria", "12/03/2026", "12/03/2028", "Vigente"},
                                {"Perfeccionamiento", "08/07/2025", "08/07/2027", "Vigente"}
                        }
                ));
            }
        }
    }

    private JPanel crearPanelTabla(String titulo, String[] columnas, Object[][] datos) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(16, 16, 16, 16)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));
        panel.setPreferredSize(new Dimension(850, 215));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(TEXTO);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setBorder(new EmptyBorder(0, 0, 12, 0));

        DefaultTableModel modelo = new DefaultTableModel(datos, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(30);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(new Color(232, 235, 239));
        tabla.setSelectionBackground(new Color(224, 235, 246));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(new Color(239, 243, 247));
        tabla.getTableHeader().setForeground(TEXTO);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(232, 235, 239)));

        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
    private JPanel crearContenidoAdministrador() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.setBorder(new EmptyBorder(24, 28, 26, 28));

        contenedor.add(crearCabeceraAdministrador(), BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        centro.add(Box.createVerticalStrut(22));
        centro.add(crearTarjetasAdministrador());
        centro.add(Box.createVerticalStrut(22));

        centro.add(crearPanelTablaAdministrador(
                "Próximos vencimientos",
                new String[]{"Trabajador", "Curso", "Vencimiento", "Estado"},
                new Object[][]{
                        {"Juan Pérez", "Formación Básica", "15/10/2026", "Próximo"},
                        {"María Torres", "Protección Portuaria", "22/10/2026", "Próximo"},
                        {"Carlos Rojas", "Perfeccionamiento", "30/10/2026", "Próximo"}
                }
        ));

        centro.add(Box.createVerticalStrut(18));

        centro.add(crearPanelTablaAdministrador(
                "Capacitaciones programadas",
                new String[]{"Curso", "Fecha", "Horario", "Participantes"},
                new Object[][]{
                        {"Formación Básica", "28/09/2026", "09:00 - 13:00", "18"},
                        {"Protección Portuaria", "02/10/2026", "08:30 - 12:30", "12"},
                        {"Perfeccionamiento", "06/10/2026", "14:00 - 18:00", "15"}
                }
        ));

        JScrollPane scroll = new JScrollPane(centro);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        contenedor.add(scroll, BorderLayout.CENTER);
        return contenedor;
    }

    private JPanel crearCabeceraAdministrador() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Panel principal");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel("Resumen de capacitaciones");
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        izquierda.add(titulo);
        izquierda.add(Box.createVerticalStrut(4));
        izquierda.add(subtitulo);

        JPanel usuario = new JPanel();
        usuario.setOpaque(false);
        usuario.setLayout(new BoxLayout(usuario, BoxLayout.Y_AXIS));

        JLabel nombre = new JLabel("Administrador");
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

    private JPanel crearTarjetasAdministrador() {
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 14, 0));
        tarjetas.setOpaque(false);
        tarjetas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        tarjetas.setPreferredSize(new Dimension(800, 110));

        tarjetas.add(crearTarjetaAdministrador("Programadas", "12"));
        tarjetas.add(crearTarjetaAdministrador("Pendientes", "8"));
        tarjetas.add(crearTarjetaAdministrador("Próximas a vencer", "5"));
        tarjetas.add(crearTarjetaAdministrador("Vencidas", "3"));

        return tarjetas;
    }

    private JPanel crearTarjetaAdministrador(String titulo, String valor) {
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

    private JPanel crearPanelTablaAdministrador(String titulo, String[] columnas, Object[][] datos) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(16, 16, 16, 16)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));
        panel.setPreferredSize(new Dimension(850, 215));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(TEXTO);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setBorder(new EmptyBorder(0, 0, 12, 0));

        DefaultTableModel modelo = new DefaultTableModel(datos, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(30);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(new Color(232, 235, 239));
        tabla.setSelectionBackground(new Color(224, 235, 246));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(new Color(239, 243, 247));
        tabla.getTableHeader().setForeground(TEXTO);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(232, 235, 239)));

        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
}
