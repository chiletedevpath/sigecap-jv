package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.InicioControlador;
import pe.utp.sigecapjv.dao.InicioDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PnlInicio extends JPanel {

    public enum Perfil {
        ADMINISTRADOR, RESPONSABLE_CAPACITACIONES, SUPERVISOR_OPERACIONES, TRABAJADOR
    }

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    private final Perfil perfil;
    private int carga;
    private final JLabel estado = new JLabel("Pendiente de consultar MySQL");
    private final Map<String, JLabel> indicadoresAdmin = new HashMap<>();
    private final Map<String, DefaultTableModel> tablasAdmin = new HashMap<>();

    // La vista inicia vacía y recibe únicamente resultados del controlador MySQL.
    public PnlInicio() {
        this(Perfil.ADMINISTRADOR);
    }

    public PnlInicio(Perfil perfil) {
        this.perfil = java.util.Objects.requireNonNull(perfil);
        setLayout(new BorderLayout());
        add(crearContenidoAdministrador(), BorderLayout.CENTER);
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

    private JPanel crearContenidoAdministrador() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.setBorder(new EmptyBorder(24, 28, 26, 28));

        contenedor.add(crearCabecera(), BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        centro.add(Box.createVerticalStrut(22));
        centro.add(crearTarjetasAdministrador());
        centro.add(Box.createVerticalStrut(22));

        centro.add(
                crearPanelTablaAdministrador(
                        "Próximos vencimientos", new String[]{"Trabajador", "Curso", "Vencimiento", "Estado"},
                        new Object[0][4]));

        centro.add(Box.createVerticalStrut(18));

        centro.add(
                crearPanelTablaAdministrador(
                        "Capacitaciones programadas", new String[]{"Curso", "Fecha", "Horario", "Participantes"},
                        new Object[0][4]));

        JScrollPane scroll = new JScrollPane(centro);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        contenedor.add(scroll, BorderLayout.CENTER);
        contenedor.add(estado, BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel crearTarjetasAdministrador() {
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 14, 0));
        tarjetas.setOpaque(false);
        tarjetas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        tarjetas.setPreferredSize(new Dimension(800, 110));

        tarjetas.add(crearTarjetaAdministrador("Programadas", "—"));
        tarjetas.add(crearTarjetaAdministrador("Pendientes", "—"));
        tarjetas.add(crearTarjetaAdministrador("Próximas a vencer", "—"));
        tarjetas.add(crearTarjetaAdministrador("Vencidas", "—"));

        return tarjetas;
    }

    private JPanel crearTarjetaAdministrador(String titulo, String valor) {
        JPanel tarjeta = new JPanel();
        tarjeta.setBackground(BLANCO);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(18, 18, 16, 18)));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(TEXTO_SUAVE);
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValor = new JLabel(valor);
        lblValor.setForeground(TEXTO);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);
        indicadoresAdmin.put(titulo, lblValor);

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
                new EmptyBorder(16, 16, 16, 16)));
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

        tablasAdmin.put(titulo, modelo);

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

    public void cargarResumen(InicioControlador controlador) {
        java.util.Objects.requireNonNull(controlador);
        int solicitud=++carga;
        indicadoresAdmin.values().forEach(e->e.setText("…"));
        tablasAdmin.values().forEach(t->t.setRowCount(0));
        estado.setText("Consultando MySQL…");
        new SwingWorker<InicioControlador.Panel,Void>() {
            @Override protected InicioControlador.Panel doInBackground() { return controlador.obtenerPanel(); }
            @Override protected void done() {
                if(solicitud!=carga) return;
                try {
                    var panel=get(); var resumen=panel.resumen();
                    indicadoresAdmin.get("Programadas").setText(String.valueOf(resumen.programadas()));
                    indicadoresAdmin.get("Pendientes").setText(String.valueOf(resumen.pendientes()));
                    indicadoresAdmin.get("Próximas a vencer").setText(String.valueOf(resumen.proximasVencer()));
                    indicadoresAdmin.get("Vencidas").setText(String.valueOf(resumen.vencidas()));
                    var fecha=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    var hora=java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                    for(var d:panel.programadas()) tablasAdmin.get("Capacitaciones programadas").addRow(new Object[]{d.curso(),d.fecha().format(fecha),d.horaInicio().format(hora)+" - "+d.horaFin().format(hora),d.participantes()});
                    for(var d:panel.vencimientos()) tablasAdmin.get("Próximos vencimientos").addRow(new Object[]{d.trabajador(),d.curso(),d.vencimiento().format(fecha),d.estado()});
                    estado.setText("Datos de MySQL · Próximos vencimientos: de hoy a 30 días · Tablas: hasta 20 registros" + (perfil==Perfil.TRABAJADOR ? " · Solo sus capacitaciones" : ""));
                } catch(Exception ex) {
                    indicadoresAdmin.values().forEach(e->e.setText("—"));
                    tablasAdmin.values().forEach(t->t.setRowCount(0));
                    Throwable causa=ex.getCause()==null?ex:ex.getCause();
                    estado.setText(causa instanceof SecurityException ? "Sin acceso al resumen. Revise sus permisos o inicie sesión nuevamente." : "No se pudo consultar MySQL. Vuelva a Inicio para reintentar.");
                }
            }
        }.execute();
    }
}
