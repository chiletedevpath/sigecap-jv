package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.VigenciasControlador;
import pe.utp.sigecapjv.controlador.NotificacionesControlador;
import pe.utp.sigecapjv.modelo.Vigencia;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PnlSeguimiento extends JPanel {

    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    private final VigenciasControlador controlador;
    private final String rol;
    private final PnlAlertas alertas;
    private final JComboBox<String> estado = new JComboBox<>(new String[]{
            "Todos", "VIGENTE", "PROXIMO_A_VENCER", "VENCIDO", "NO_APLICA"});
    private record OpcionCurso(int id, String nombre) {
        @Override public String toString() { return nombre; }
    }
    private final JComboBox<OpcionCurso> curso = new JComboBox<>();
    private final JLabel mensaje = new JLabel("Pulse Consultar para cargar las vigencias.");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Trabajador", "Curso", "Certificado", "Vencimiento", "Días restantes", "Estado"},0) {
        @Override public boolean isCellEditable(int fila,int columna) { return false; }
    };
    private List<Vigencia> filas = List.of();
    private int consulta;

    public PnlSeguimiento(VigenciasControlador controlador,
                          NotificacionesControlador notificaciones, String rol) {
        this.controlador=controlador;
        this.rol=rol;
        this.alertas=new PnlAlertas(notificaciones);
        setLayout(new BorderLayout());
        add(crearContenido(), BorderLayout.CENTER);
        estado.addActionListener(e -> filtrar());
        curso.addActionListener(e -> filtrar());
    }

    public void refrescar() {
        int actual=++consulta;
        filas=List.of(); modelo.setRowCount(0);
        mensaje.setText("Cargando vigencias…");
        new SwingWorker<List<Vigencia>,Void>() {
            @Override protected List<Vigencia> doInBackground() {
                return controlador.consultar();
            }
            @Override protected void done() {
                if(actual!=consulta) return;
                try {
                    filas=get();
                    OpcionCurso seleccionado=(OpcionCurso)curso.getSelectedItem();
                    curso.removeAllItems(); curso.addItem(new OpcionCurso(0,"Todos"));
                    Set<Integer> ids=new HashSet<>();
                    for(var fila:filas) if(ids.add(fila.idCurso())) {
                        var opcion=new OpcionCurso(fila.idCurso(),fila.curso()); curso.addItem(opcion);
                        if(seleccionado!=null && seleccionado.id()==opcion.id()) curso.setSelectedItem(opcion);
                    }
                    filtrar();
                } catch(java.util.concurrent.ExecutionException | InterruptedException ex) {
                    if(ex instanceof InterruptedException) Thread.currentThread().interrupt();
                    filas=List.of(); modelo.setRowCount(0);
                    curso.removeAllItems();
                    mensaje.setText("No se pudieron cargar las vigencias. Verifique su sesión y vuelva a consultar.");
                }
            }
        }.execute();
    }

    private void filtrar() {
        modelo.setRowCount(0);
        OpcionCurso elegido=(OpcionCurso)curso.getSelectedItem();
        for(var fila:filas) {
            if(estado.getSelectedIndex()>0 && !estado.getSelectedItem().equals(fila.estado())) continue;
            if(elegido!=null && elegido.id()!=0 && elegido.id()!=fila.idCurso()) continue;
            modelo.addRow(new Object[]{fila.trabajador(),fila.curso(),fila.certificado(),
                    fila.vencimiento()==null?"No aplica":fila.vencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    fila.diasRestantes()==null?"No aplica":fila.diasRestantes(),fila.estado()});
        }
        mensaje.setText(modelo.getRowCount()==0?"No hay certificados para los filtros seleccionados.":modelo.getRowCount()+" certificado(s).");
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

        JLabel usuario = new JLabel(rol);
        usuario.setForeground(TEXTO);
        usuario.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cabecera.add(titulos, BorderLayout.WEST);
        cabecera.add(usuario, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.setBackground(BLANCO);
        tabs.addTab("Vigencias", crearVigencias());
        tabs.addTab("Alertas", alertas);
        tabs.addChangeListener(e -> { if(tabs.getSelectedComponent()==alertas) alertas.refrescar(); });

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
        estado.setPreferredSize(new Dimension(180, 36));
        filtros.add(estado);

        filtros.add(new JLabel("Curso:"));
        curso.setPreferredSize(new Dimension(220, 36));
        filtros.add(curso);

        JButton consultar=botonSecundario("Consultar");
        consultar.addActionListener(e -> refrescar()); filtros.add(consultar);
        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scrollTabla(), BorderLayout.CENTER);
        panel.add(mensaje, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelBase() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BLANCO);
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));
        return panel;
    }

    private JScrollPane scrollTabla() {
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
