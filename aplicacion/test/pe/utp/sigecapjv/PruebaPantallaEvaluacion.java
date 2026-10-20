package pe.utp.sigecapjv;

import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.vista.*;

public final class PruebaPantallaEvaluacion {
    private static int total;
    private static void comprobar(boolean valor) {
        if (!valor) throw new AssertionError("Falló la interacción de resultados/certificados.");
        total++;
    }
    private static <T> T campo(PnlEvaluacion panel, String nombre, Class<T> tipo) {
        try {
            var campo = PnlEvaluacion.class.getDeclaredField(nombre); campo.setAccessible(true);
            return tipo.cast(campo.get(panel));
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void seleccionar(PnlEvaluacion panel, int idCapacitacion) {
        panel.refrescar();
        JComboBox<?> combo = campo(panel,"selector",JComboBox.class);
        for (int i=0;i<combo.getItemCount();i++) {
            if (((Capacitacion) combo.getItemAt(i)).getIdCapacitacion()==idCapacitacion) combo.setSelectedIndex(i);
        }
        JTable tabla=campo(panel,"tabla",JTable.class);
        comprobar(tabla.getRowCount()==1);
        tabla.setRowSelectionInterval(0,0);
    }
    public static void verificarMySql(CapacitacionControlador gestion, SeguimientoControlador s, int id) throws Exception {
        int idCap=gestion.listarParticipaciones().stream().filter(p -> p.getIdParticipacion()==id).findFirst().orElseThrow().getCapacitacion().getIdCapacitacion();
        SwingUtilities.invokeAndWait(() -> {
            PnlEvaluacion panel=new PnlEvaluacion(gestion,s,false);
            seleccionar(panel,idCap);
            campo(panel,"resultado",JComboBox.class).setSelectedItem("APTO");
            campo(panel,"nota",JTextField.class).setText("21");
            campo(panel,"guardar",JButton.class).doClick();
            comprobar("APROBADO".equals(s.consultarResultado(id).getCondicion()));
            campo(panel,"nota",JTextField.class).setText("");
            campo(panel,"guardar",JButton.class).doClick();
            comprobar("APTO".equals(s.consultarResultado(id).getCondicion()) && s.consultarResultado(id).getCalificacion()==null);
            PnlEvaluacion documentos=new PnlEvaluacion(gestion,s,true);
            seleccionar(documentos,idCap);
            campo(documentos,"numero",JTextField.class).setText("CERT-EVAL-EDITADO");
            campo(documentos,"emision",JTextField.class).setText("incorrecta");
            campo(documentos,"guardar",JButton.class).doClick();
            comprobar("CERT-EVAL".equals(s.consultarCertificado(id).getNumero()));
            campo(documentos,"emision",JTextField.class).setText(LocalDate.now().toString());
            campo(documentos,"guardar",JButton.class).doClick();
            comprobar("CERT-EVAL-EDITADO".equals(s.consultarCertificado(id).getNumero()));
            documentos.refrescar();
            comprobar(campo(documentos,"tabla",JTable.class).getValueAt(0,1).equals("CERT-EVAL-EDITADO"));
        });
        System.out.println("OK: "+total+" comprobaciones Swing de resultados/certificados con MySQL.");
    }
}
