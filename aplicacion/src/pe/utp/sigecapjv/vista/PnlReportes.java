package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.ReporteControlador;
import pe.utp.sigecapjv.modelo.Curso;
import pe.utp.sigecapjv.modelo.Reporte;
import pe.utp.sigecapjv.modelo.Trabajador;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public class PnlReportes extends JPanel {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);
    private static final String INDICACION = "Seleccione los filtros y pulse Generar reporte.";
    // STRICT rechaza fechas inexistentes como 31/02/2026.
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final ReporteControlador controlador;
    private final JTextField desde = new JTextField(), hasta = new JTextField();
    private final JComboBox<Object> curso = new JComboBox<>(), trabajador = new JComboBox<>();
    private final JComboBox<String> estado = opciones("Todos", Reporte.ESTADOS);
    private final JComboBox<String> vigencia = opciones("Todas", Reporte.VIGENCIAS);
    private final JLabel mensaje = new JLabel(INDICACION);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Trabajador", "Curso", "Fecha", "Estado", "Resultado", "Vigencia"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JButton exportar;
    private Reporte reporte;

    public PnlReportes(ReporteControlador controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout());
        add(crearContenido(), BorderLayout.CENTER);
        refrescar();
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

        JLabel usuario = new JLabel(controlador.rolActual());
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

        agregarCampo(panel, g, 0, 0, "Desde (DD/MM/AAAA)", desde);
        agregarCampo(panel, g, 1, 0, "Hasta (DD/MM/AAAA)", hasta);
        agregarCampo(panel, g, 2, 0, "Curso", curso);
        agregarCampo(panel, g, 0, 1, "Trabajador", trabajador);
        agregarCampo(panel, g, 1, 1, "Estado", estado);
        agregarCampo(panel, g, 2, 1, "Vigencia", vigencia);

        JButton limpiar = botonSecundario("Limpiar");
        JButton generar = botonPrimario("Generar reporte");
        limpiar.addActionListener(e -> limpiar());
        generar.addActionListener(e -> generar());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(limpiar);
        acciones.add(generar);

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

        exportar = botonPrimario("Exportar PDF");
        exportar.setEnabled(false);
        exportar.addActionListener(e -> exportar());

        mensaje.setForeground(TEXTO_SUAVE);
        mensaje.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        superior.add(titulo, BorderLayout.WEST);
        superior.add(exportar, BorderLayout.EAST);
        superior.add(mensaje, BorderLayout.SOUTH);

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
        // Más espacio para nombres de trabajador y curso; las demás columnas tienen valores cortos.
        int[] anchos = {250, 200, 95, 110, 105, 135};
        for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));

        panel.add(superior, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /** Recarga cursos y trabajadores registrados conservando la selección actual. */
    public void refrescar() {
        try {
            recargar(curso, controlador.listarCursos());
            recargar(trabajador, controlador.listarTrabajadores());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje.setText(ex.getMessage());
        }
    }

    private void recargar(JComboBox<Object> combo, List<?> datos) {
        String anterior = String.valueOf(combo.getSelectedItem());
        combo.removeAllItems();
        combo.addItem("Todos");
        for (Object dato : datos) {
            combo.addItem(dato);
            if (dato.toString().equals(anterior)) combo.setSelectedItem(dato);
        }
    }

    private void generar() {
        try {
            Reporte filtros = new Reporte(
                    fecha(desde), fecha(hasta), seleccion(curso, Curso.class), seleccion(trabajador, Trabajador.class),
                    estado.getSelectedIndex() <= 0 ? null : (String) estado.getSelectedItem(),
                    vigencia.getSelectedIndex() <= 0 ? null : (String) vigencia.getSelectedItem());
            mostrar(controlador.generar(filtros));
        } catch (DateTimeParseException ex) {
            mostrar(null);
            mensaje.setText("Use fechas válidas con formato DD/MM/AAAA.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrar(null);
            mensaje.setText(ex.getMessage());
        }
    }

    private void mostrar(Reporte generado) {
        reporte = generado;
        modelo.setRowCount(0);
        exportar.setEnabled(generado != null && !generado.getFilas().isEmpty());
        if (generado == null) return;
        for (Reporte.Fila f : generado.getFilas()) {
            modelo.addRow(new Object[]{f.trabajador(), f.curso(), f.fecha(), f.estado(), f.resultado(), f.vigencia()});
        }
        mensaje.setText(generado.getFilas().isEmpty()
                ? "No existen registros para los filtros seleccionados."
                : generado.getFilas().size() + " registro(s) encontrados.");
    }

    private void limpiar() {
        desde.setText("");
        hasta.setText("");
        for (JComboBox<?> combo : List.of(curso, trabajador, estado, vigencia))
            if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
        mostrar(null);
        mensaje.setText(INDICACION);
    }

    private void exportar() {
        JFileChooser dialogo = new JFileChooser();
        dialogo.setFileFilter(new FileNameExtensionFilter("Documento PDF", "pdf"));
        // En Docker, ~/reportes está enlazada a la carpeta reportes/ del proyecto.
        File carpeta = new File(System.getProperty("user.home"), "reportes");
        dialogo.setSelectedFile(new File(carpeta.isDirectory() ? carpeta : dialogo.getCurrentDirectory(),
                "reporte-capacitaciones-" + LocalDate.now() + ".pdf"));
        if (dialogo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destino = dialogo.getSelectedFile().toPath();
        if (!destino.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) destino = Path.of(destino + ".pdf");
        if (Files.exists(destino) && JOptionPane.showConfirmDialog(
                this, "El archivo ya existe. ¿Desea reemplazarlo?", "Exportar PDF",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            controlador.exportarPdf(reporte, destino);
            mensaje.setText("Reporte exportado en " + destino);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje.setText(ex.getMessage());
        }
    }

    private static LocalDate fecha(JTextField campo) {
        return campo.getText().isBlank() ? null : LocalDate.parse(campo.getText().trim(), FORMATO_FECHA);
    }

    private static <T> T seleccion(JComboBox<Object> combo, Class<T> tipo) {
        Object elegido = combo.getSelectedItem();
        return tipo.isInstance(elegido) ? tipo.cast(elegido) : null;
    }

    private static JComboBox<String> opciones(String todos, String[] valores) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem(todos);
        for (String valor : valores) combo.addItem(valor);
        return combo;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints g, int col, int fila, String etiqueta,
                              JComponent campo) {
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
