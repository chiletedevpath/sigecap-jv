package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.vista.PnlSeguimiento;
import javax.swing.*;
import java.util.List;

public final class PruebaVigenciasMySql {
    private static int total;
    private static void comprobar(boolean valor,String mensaje) {
        if(!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch(SecurityException ex) { total++; return; }
        throw new AssertionError("Consulta de vigencias no autorizada aceptada");
    }
    private static <T> T campo(PnlSeguimiento panel,String nombre,Class<T> tipo) {
        try { var f=PnlSeguimiento.class.getDeclaredField(nombre); f.setAccessible(true); return tipo.cast(f.get(panel)); }
        catch(ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void esperar(PnlSeguimiento panel) throws Exception {
        long limite=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<limite) {
            boolean[] listo={false};
            SwingUtilities.invokeAndWait(() -> listo[0]=!campo(panel,"mensaje",JLabel.class).getText().startsWith("Cargando"));
            if(listo[0]) return;
            Thread.sleep(25);
        }
        throw new AssertionError("La consulta no terminó");
    }
    public static void verificar() throws Exception {
        rechazar(() -> new VigenciasControlador(null).consultar());
        List<pe.utp.sigecapjv.modelo.Vigencia> referencia=null;
        for(String usuario:new String[]{"admin_prueba","responsable_prueba","supervisor_prueba","trabajador_prueba"}) {
            String clave=usuario.equals("supervisor_prueba")?"Clave-renovada-prueba-2026":"Clave-usuarios-prueba-2026";
            try(var sesion=new LoginControlador().autenticar(usuario,clave.toCharArray())) {
                var control=new VigenciasControlador(sesion);
                if(usuario.equals("trabajador_prueba")) { rechazar(control::consultar); continue; }
                var filas=control.consultar();
                comprobar(!filas.isEmpty(),"Consulta real para "+usuario);
                if(referencia==null) referencia=filas;
                else comprobar(referencia.equals(filas),"Perfiles autorizados consultan el mismo conjunto");
                for(var fila:filas) {
                    Integer dias=fila.diasRestantes();
                    String esperado=dias==null?"NO_APLICA":dias<0?"VENCIDO":dias<=30?"PROXIMO_A_VENCER":"VIGENTE";
                    comprobar(esperado.equals(fila.estado()),"Vigencia coherente con días restantes");
                }
                PnlSeguimiento[] vista={null};
                SwingUtilities.invokeAndWait(() -> {
                    vista[0]=new PnlSeguimiento(control,new NotificacionesControlador(sesion),sesion.getRol());
                    vista[0].refrescar();
                });
                esperar(vista[0]);
                SwingUtilities.invokeAndWait(() -> {
                    var modelo=campo(vista[0],"modelo",javax.swing.table.DefaultTableModel.class);
                    comprobar(modelo.getRowCount()==filas.size(),"Pantalla usa todos los certificados reales");
                    campo(vista[0],"estado",JComboBox.class).setSelectedItem("VENCIDO");
                    comprobar(modelo.getRowCount()==filas.stream().filter(f->f.estado().equals("VENCIDO")).count(),"Filtro de estado");
                    for(int i=0;i<modelo.getRowCount();i++) comprobar("VENCIDO".equals(modelo.getValueAt(i,5)),"Solo vencidos");
                    campo(vista[0],"estado",JComboBox.class).setSelectedItem("Todos");
                    campo(vista[0],"curso",JComboBox.class).setSelectedIndex(1);
                    int curso=filas.get(0).idCurso();
                    comprobar(modelo.getRowCount()==filas.stream().filter(f->f.idCurso()==curso).count(),"Filtro por identidad del curso");
                    sesion.close(); vista[0].refrescar();
                });
                esperar(vista[0]);
                SwingUtilities.invokeAndWait(() -> comprobar(campo(vista[0],"modelo",javax.swing.table.DefaultTableModel.class).getRowCount()==0,"Sesion cerrada limpia datos"));
                rechazar(control::consultar);
            }
        }
        System.out.println("OK: "+total+" comprobaciones MySQL y Swing de vigencias y cuatro perfiles.");
    }
}
