package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.vista.*;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Integración en la base desechable, después de comprobar la persistencia entre JVM. */
public final class PruebaHistorialMySql {
    private static int total;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch (SecurityException esperado) { total++; return; }
        throw new AssertionError("Se permitió una consulta sin autorización.");
    }
    private static <T> T encontrar(Component c, Class<T> tipo) {
        if (tipo.isInstance(c)) return tipo.cast(c);
        if (c instanceof Container padre) for (Component hijo : padre.getComponents()) {
            T encontrado = encontrar(hijo, tipo); if (encontrado != null) return encontrado;
        }
        return null;
    }
    private static boolean texto(Component c, String prefijo) {
        if (c instanceof JLabel l && l.getText()!=null && l.getText().startsWith(prefijo)) return true;
        if (c instanceof Container padre) for (Component hijo : padre.getComponents()) if (texto(hijo,prefijo)) return true;
        return false;
    }
    private static JButton boton(Component c, String texto) {
        if (c instanceof JButton b && texto.equals(b.getText())) return b;
        if (c instanceof Container padre) for (Component hijo : padre.getComponents()) {
            JButton encontrado = boton(hijo,texto); if (encontrado != null) return encontrado;
        }
        return null;
    }
    private static void esperar(Component panel, String mensaje) throws Exception {
        AtomicBoolean listo = new AtomicBoolean();
        for (int i=0; i<200 && !listo.get(); i++) {
            SwingUtilities.invokeAndWait(() -> listo.set(texto(panel,mensaje)));
            if (!listo.get()) Thread.sleep(50);
        }
        comprobar(listo.get(), "La consulta Swing no terminó: " + mensaje);
    }
    public static void verificar() throws Exception {
        if (!"true".equals(System.getenv("SIGECAP_DB_TEST"))) throw new IllegalStateException("Solo base desechable.");
        char[] clave = "Clave-usuarios-prueba-2026".toCharArray();
        try (var admin = new LoginControlador().autenticar("admin_prueba",clave);
             var trabajador = new LoginControlador().autenticar("trabajador_prueba",clave);
             var supervisor = new LoginControlador().autenticar("supervisor_prueba","Clave-renovada-prueba-2026".toCharArray())) {
            int propio = trabajador.getIdTrabajador();
            int ajeno = Jdbc.transaccion(c -> {
                int otro = Jdbc.insertar(c,"INSERT INTO trabajador(dni,nombres,apellidos,cargo) VALUES ('73000111','Otro','Historial','Agente')");
                int curso = Jdbc.insertar(c,"INSERT INTO curso(codigo,nombre,tipo) VALUES ('HISTORIAL-TEST','Curso historial','Interno')");
                for (int i=0;i<3;i++) {
                    int cap = Jdbc.insertar(c,"INSERT INTO capacitacion(idCurso,fecha,horaInicio,horaFin,modalidad,estado) VALUES (?,CURDATE(),'09:00','10:00','Presencial',?)",curso,i==2?"CANCELADA":"PROGRAMADA");
                    int p = Jdbc.insertar(c,"INSERT INTO participacion(idTrabajador,idCapacitacion,fechaAsignacion,estado) VALUES (?,?,CURDATE(),?)",propio,cap,i==1?"COMPLETADA":"PENDIENTE");
                    if(i==1) {
                        Jdbc.insertar(c,"INSERT INTO asistencia(idParticipacion,condicion) VALUES (?,'ASISTIO')",p);
                        Jdbc.insertar(c,"INSERT INTO resultado(idParticipacion,condicion,calificacion) VALUES (?,'APROBADO',18.50)",p);
                        Jdbc.insertar(c,"INSERT INTO certificado(idParticipacion,idTrabajador,numero,fechaEmision,fechaVencimiento) VALUES (?,?,'CERT-HISTORIAL',CURDATE(),CURDATE()+INTERVAL 30 DAY)",p,propio);
                    }
                    if(i==0) Jdbc.insertar(c,"INSERT INTO participacion(idTrabajador,idCapacitacion,fechaAsignacion) VALUES (?,?,CURDATE())",otro,cap);
                }
                return otro;
            });
            var control = new HistorialControlador(trabajador);
            var filas = control.consultarMisCapacitaciones();
            var nuevas = filas.stream().filter(f->f.curso().equals("Curso historial")).toList();
            comprobar(nuevas.size()==3,"Se mezcló el historial de otro trabajador.");
            comprobar(nuevas.stream().anyMatch(f->f.estado().equals("CANCELADA")),"Cancelación de capacitación no reflejada.");
            var completa = nuevas.stream().filter(f->f.estado().equals("COMPLETADA")).findFirst().orElseThrow();
            comprobar(completa.calificacion().toPlainString().equals("18.50") && completa.asistencia().equals("ASISTIO"),"Seguimiento incompleto.");
            comprobar(completa.certificado().equals("CERT-HISTORIAL") && completa.vigencia().equals("PROXIMO_A_VENCER"),"Certificado/vigencia incorrectos.");
            comprobar(nuevas.stream().anyMatch(f->f.certificado()==null && f.vigencia().equals("SIN_CERTIFICADO")),"Se fabricó un certificado.");
            comprobar(new HistorialControlador(admin).consultarTrabajador(propio).equals(filas),"Historial administrativo no coincide.");
            comprobar(new HistorialControlador(supervisor).consultarTrabajador(ajeno).size()==1,"Supervisor no puede consultar.");
            comprobar(new HistorialControlador(admin).consultarTrabajador(Integer.MAX_VALUE).isEmpty(),"Consulta vacía no respetada.");
            rechazar(()->control.consultarTrabajador(ajeno));
            rechazar(()->new HistorialControlador(null).consultarMisCapacitaciones());
            rechazar(()->new HistorialControlador(null).consultarTrabajador(propio));
            rechazar(()->new HistorialControlador(admin).consultarMisCapacitaciones());

            PnlPersonal[] personal = new PnlPersonal[1];
            Component[] historial = new Component[1];
            SwingUtilities.invokeAndWait(()-> {
                personal[0] = new PnlPersonal(new PersonalJdbcControlador(admin), admin.getRol(), new HistorialControlador(admin));
                JTabbedPane tabs = encontrar(personal[0],JTabbedPane.class);
                comprobar(tabs.isEnabledAt(2),"Historial deshabilitado para administrador.");
                tabs.setSelectedIndex(2); historial[0] = tabs.getComponentAt(2);
                JComboBox<?> combo = encontrar(historial[0],JComboBox.class);
                for(int i=0;i<combo.getItemCount();i++) {
                    if(((pe.utp.sigecapjv.modelo.Trabajador)combo.getItemAt(i)).getIdTrabajador()==propio) combo.setSelectedIndex(i);
                }
                boton(historial[0],"Consultar").doClick();
            });
            esperar(historial[0],"Historial actualizado.");
            SwingUtilities.invokeAndWait(()-> {
                JTable tabla = encontrar(historial[0],JTable.class);
                comprobar(tabla.getRowCount()==filas.size() && tabla.getColumnCount()==9,"Historial Swing incompleto.");
            });

            PnlMisCapacitaciones[] panel = new PnlMisCapacitaciones[1];
            SwingUtilities.invokeAndWait(()-> { panel[0]=new PnlMisCapacitaciones(control); panel[0].refrescar(); });
            esperar(panel[0],"Consulta actualizada.");
            SwingUtilities.invokeAndWait(()-> {
                comprobar(encontrar(panel[0],JTable.class).getRowCount()==filas.size(),"Tabla no muestra el historial real.");
                encontrar(panel[0],JComboBox.class).setSelectedItem("Canceladas"); panel[0].refrescar();
            });
            esperar(panel[0],"Consulta actualizada.");
            SwingUtilities.invokeAndWait(()-> {
                JTable tabla=encontrar(panel[0],JTable.class);
                comprobar(tabla.getRowCount()>0,"Filtro canceladas vacío.");
                for(int i=0;i<tabla.getRowCount();i++) comprobar(tabla.getValueAt(i,2).equals("CANCELADA"),"Filtro incorrecto.");
            });
            trabajador.close(); rechazar(control::consultarMisCapacitaciones);
            SwingUtilities.invokeAndWait(panel[0]::refrescar);
            esperar(panel[0],"No se pudo consultar.");
            SwingUtilities.invokeAndWait(()->comprobar(encontrar(panel[0],JTable.class).getRowCount()==0,"El error conservó filas anteriores."));
            System.out.println("OK: " + total + " comprobaciones MySQL y Swing de historial y aislamiento.");
        } finally { java.util.Arrays.fill(clave,'\0'); }
    }
}
