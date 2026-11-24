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
    private final JTextField trabajador = new JTextField(18);
    private final JTextField entidad = new JTextField(24);
    private final JTextField desde = new JTextField(10);
    private final JTextField hasta = new JTextField(10);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private final JLabel mensaje = new JLabel("Pulse Consultar para cargar las vigencias.");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Trabajador", "Curso", "Certificado", "Vencimiento", "Días restantes", "Estado", "Institución emisora", "DNI"},0) {
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
        trabajador.addActionListener(e -> filtrar());
        entidad.addActionListener(e -> filtrar());
        desde.addActionListener(e -> filtrar());
        hasta.addActionListener(e -> filtrar());
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
                    mensaje.setText(pe.utp.sigecapjv.dao.Jdbc.mensajeError(ex));
                }
            }
        }.execute();
    }

    private void filtrar() {
        modelo.setRowCount(0);
        java.time.LocalDate inicio;
        java.time.LocalDate fin;
        try {
            inicio = desde.getText().isBlank() ? null : java.time.LocalDate.parse(desde.getText().trim(), FECHA);
            fin = hasta.getText().isBlank() ? null : java.time.LocalDate.parse(hasta.getText().trim(), FECHA);
        } catch (java.time.format.DateTimeParseException ex) {
            mensaje.setText("Ingrese fechas válidas con formato dd/MM/aaaa.");
            return;
        }
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            mensaje.setText("La fecha desde no puede ser posterior a la fecha hasta.");
            return;
        }
        String busqueda = normalizar(trabajador.getText().trim());
        String institucion = normalizar(entidad.getText().trim());
        OpcionCurso elegido=(OpcionCurso)curso.getSelectedItem();
        for(var fila:filas) {
            if(estado.getSelectedIndex()>0 && !estado.getSelectedItem().equals(fila.estado())) continue;
            if(elegido!=null && elegido.id()!=0 && elegido.id()!=fila.idCurso()) continue;
            if (!normalizar(fila.trabajador() + " " + fila.dni()).contains(busqueda)) continue;
            if (!normalizar(fila.entidadEmisora() == null ? "" : fila.entidadEmisora()).contains(institucion)) continue;
            if ((inicio != null || fin != null) && fila.vencimiento() == null) continue;
            if (inicio != null && fila.vencimiento().isBefore(inicio)) continue;
            if (fin != null && fila.vencimiento().isAfter(fin)) continue;
            modelo.addRow(new Object[]{fila.trabajador(),fila.curso(),fila.certificado(),
                    fila.vencimiento()==null?"No aplica":fila.vencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    fila.diasRestantes()==null?"No aplica":fila.diasRestantes(),fila.estado(),
                    fila.entidadEmisora()==null?"Sin registrar":fila.entidadEmisora(),fila.dni()});
        }
        mensaje.setText(modelo.getRowCount()==0?"No hay certificados para los filtros seleccionados.":modelo.getRowCount()+" certificado(s).");
    }

    private static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
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
        subtitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(subtitulo);

        JLabel usuario = new JLabel(rol);
        usuario.setForeground(TEXTO);
        usuario.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cabecera.add(titulos, BorderLayout.CENTER);
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
        JPanel fechas = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        fechas.setOpaque(false);
        trabajador.setToolTipText("Nombre, apellido o DNI. Pulse Enter o Consultar.");
        desde.setToolTipText("Vencimiento desde (dd/MM/aaaa), inclusive. Vacío: sin límite.");
        hasta.setToolTipText("Vencimiento hasta (dd/MM/aaaa), inclusive. Vacío: sin límite.");
        fechas.add(new JLabel("Trabajador / DNI:")); fechas.add(trabajador);
        fechas.add(new JLabel("Desde:")); fechas.add(desde);
        fechas.add(new JLabel("Hasta:")); fechas.add(hasta);
        JButton limpiar = botonSecundario("Limpiar");
        limpiar.addActionListener(e -> {
            trabajador.setText(""); entidad.setText(""); desde.setText(""); hasta.setText("");
            estado.setSelectedIndex(0);
            if (curso.getItemCount() > 0) curso.setSelectedIndex(0);
            filtrar();
        });
        fechas.add(limpiar);
        JPanel instituciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        instituciones.setOpaque(false);
        instituciones.add(new JLabel("Institución emisora:")); instituciones.add(entidad);
        entidad.setToolTipText("Nombre completo o parcial de la institución. Pulse Enter o Consultar.");
        JPanel cabeceraFiltros = new JPanel(new GridLayout(3, 1));
        cabeceraFiltros.setOpaque(false);
        cabeceraFiltros.add(filtros); cabeceraFiltros.add(fechas);
        cabeceraFiltros.add(instituciones);
        panel.add(cabeceraFiltros, BorderLayout.NORTH);
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
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchos = {250, 200, 180, 120, 110, 170, 190, 100};
        for (int i=0; i<anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
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
