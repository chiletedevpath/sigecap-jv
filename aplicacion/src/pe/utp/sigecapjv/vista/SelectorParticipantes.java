package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.modelo.Trabajador;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.regex.Pattern;

final class SelectorParticipantes extends JPanel {
    private final List<Trabajador> disponibles;
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{"Asignar", "DNI", "Apellidos", "Nombres"}, 0) {
        @Override public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int fila, int col) { return col == 0; }
    };

    SelectorParticipantes(List<Trabajador> disponibles) {
        super(new BorderLayout(8, 8));
        this.disponibles = List.copyOf(disponibles);
        for (Trabajador t : disponibles) modelo.addRow(new Object[]{false, t.getDni(), t.getApellidos(), t.getNombres()});
        JTable tabla = new JTable(modelo);
        var orden = new TableRowSorter<>(modelo);
        tabla.setRowSorter(orden);
        JTextField texto = new JTextField();
        JPanel busqueda = new JPanel(new BorderLayout(8, 0));
        busqueda.add(new JLabel("DNI o nombre"), BorderLayout.WEST);
        busqueda.add(texto, BorderLayout.CENTER);
        texto.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() {
                orden.setRowFilter(RowFilter.regexFilter("(?iu)" + Pattern.quote(texto.getText().trim()), 1, 2, 3));
            }
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        JLabel total = new JLabel("Seleccionados: 0");
        modelo.addTableModelListener(e -> total.setText("Seleccionados: " + seleccionados().size()));
        add(busqueda, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(total, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(560, 310));
    }

    List<Trabajador> seleccionados() {
        return IntStream.range(0, modelo.getRowCount()).filter(i -> Boolean.TRUE.equals(modelo.getValueAt(i, 0)))
                .mapToObj(disponibles::get).toList();
    }
}
