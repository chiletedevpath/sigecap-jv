package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.modelo.Trabajador;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class PruebaSelectorParticipantes {
    public static void verificar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var ana = new Trabajador(1, "10000001", "Ana", "Ruiz", "Agente", "ACTIVO");
            var luis = new Trabajador(2, "20000002", "Luis", "Díaz", "Agente", "ACTIVO");
            var panel = new SelectorParticipantes(List.of(ana, luis));
            var texto = encontrar(panel, JTextField.class);
            var tabla = encontrar(panel, JTable.class);
            texto.setText("dÍaZ");
            if (tabla.getRowCount()!=1 || !tabla.getValueAt(0,1).equals(luis.getDni()))
                throw new AssertionError("Filtro por apellido");
            tabla.setValueAt(true, 0, 0);
            texto.setText(ana.getDni());
            tabla.setValueAt(true, 0, 0);
            texto.setText("[");
            if (tabla.getRowCount()!=0 || !panel.seleccionados().equals(List.of(ana,luis)))
                throw new AssertionError("Cambiar de búsqueda debe conservar la selección");
        });
    }

    private static <T> T encontrar(Component componente, Class<T> tipo) {
        if (tipo.isInstance(componente)) return tipo.cast(componente);
        if (componente instanceof Container contenedor) {
            for (var hijo : contenedor.getComponents()) {
                T valor = encontrar(hijo, tipo);
                if (valor != null) return valor;
            }
        }
        return null;
    }
}
