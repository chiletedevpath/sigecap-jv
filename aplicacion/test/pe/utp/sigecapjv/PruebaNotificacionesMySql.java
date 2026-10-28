package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.Notificacion;
import pe.utp.sigecapjv.vista.PnlAlertas;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Se ejecuta después de verificar la persistencia del flujo, siempre en la base desechable. */
public final class PruebaNotificacionesMySql {
    private static int total;
    private static void comprobar(boolean valor, String texto) {
        if (!valor) throw new AssertionError(texto);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) { total++; return; }
        throw new AssertionError("Operación no autorizada aceptada");
    }
    private static <T> T encontrar(java.awt.Component c, Class<T> tipo) {
        if (tipo.isInstance(c)) return tipo.cast(c);
        if (c instanceof java.awt.Container padre) for (var hijo : padre.getComponents()) {
            T resultado=encontrar(hijo,tipo); if (resultado!=null) return resultado;
        }
        return null;
    }
    public static void verificar() throws Exception {
        if (!"true".equals(System.getenv("SIGECAP_DB_TEST"))) throw new IllegalStateException("Solo base desechable");
        rechazar(() -> new NotificacionesControlador().obtenerAlertasActualizadas());
        int[] certificados = Jdbc.transaccion(c -> {
            // Las reglas de vigencia usan la fecha de MySQL, que puede diferir de la del equipo.
            LocalDate hoy;
            try (var sentencia = c.createStatement(); var resultado = sentencia.executeQuery("SELECT CURDATE()")) {
                resultado.next();
                hoy = resultado.getDate(1).toLocalDate();
            }
            int trabajador=Jdbc.insertar(c,"INSERT INTO trabajador(dni,nombres,apellidos,cargo) VALUES ('70001001','Alerta','Prueba','Agente')");
            int curso=Jdbc.insertar(c,"INSERT INTO curso(codigo,nombre,tipo,vigenciaMeses) VALUES ('ALERTA','Curso alertas','Interno',12)");
            int[] ids=new int[5]; Integer[] dias={-1,0,30,31,null};
            for (int i=0;i<dias.length;i++) {
                int cap=Jdbc.insertar(c,"INSERT INTO capacitacion(idCurso,fecha,horaInicio,horaFin,modalidad) VALUES (?,CURDATE(),'08:00','09:00','Presencial')",curso);
                int part=Jdbc.insertar(c,"INSERT INTO participacion(idTrabajador,idCapacitacion,fechaAsignacion) VALUES (?,?,CURDATE())",trabajador,cap);
                ids[i]=Jdbc.insertar(c,"INSERT INTO certificado(idParticipacion,idTrabajador,numero,fechaEmision,fechaVencimiento) VALUES (?,?,?,CURDATE()-INTERVAL 1 YEAR,?)",part,trabajador,"ALERTA-"+i,dias[i]==null?null:hoy.plusDays(dias[i]));
            }
            return ids;
        });
        try (SesionUsuario admin=new LoginControlador().autenticar("admin_prueba","Clave-usuarios-prueba-2026".toCharArray());
             SesionUsuario supervisor=new LoginControlador().autenticar("supervisor_prueba","Clave-renovada-prueba-2026".toCharArray());
             SesionUsuario trabajador=new LoginControlador().autenticar("trabajador_prueba","Clave-usuarios-prueba-2026".toCharArray())) {
            NotificacionesControlador control=new NotificacionesControlador(admin);
            List<Notificacion> lista=control.obtenerAlertasActualizadas();
            for (int i=0;i<3;i++) {
                int indice=i;
                var n=lista.stream().filter(x -> x.getClaveEvento().startsWith("VENCIMIENTO:"+certificados[indice]+":" )).findFirst().orElseThrow();
                comprobar(n.getTipo().equals(i==0?"VENCIDO":"POR_VENCER"),"Límites de vencimiento");
                comprobar(n.getMensaje().contains("Curso alertas"),"Aviso identifica el curso");
            }
            comprobar(lista.stream().noneMatch(x -> x.getClaveEvento().startsWith("VENCIMIENTO:"+certificados[3]+":")),"31 días no genera aviso");
            comprobar(lista.stream().noneMatch(x -> x.getClaveEvento().startsWith("VENCIMIENTO:"+certificados[4]+":")),"Sin vencimiento no genera aviso");
            int cantidad=lista.size();
            comprobar(control.obtenerAlertasActualizadas().size()==cantidad,"Consulta idempotente");
            int id=lista.get(0).getIdNotificacion(); control.marcarComoLeida(id);
            comprobar(control.obtenerAlertasActualizadas().stream().anyMatch(x -> x.getIdNotificacion()==id && x.getEstado().equals("LEIDA")),"Conserva lectura");
            var consulta=new NotificacionesControlador(supervisor);
            comprobar(consulta.obtenerAlertasActualizadas().size()==cantidad,"Supervisor consulta");
            rechazar(() -> consulta.marcarComoLeida(id));

            // Simula un aviso previo con la misma fecha, en la etapa preventiva.
            Jdbc.transaccion(c -> { Jdbc.actualizar(c,"UPDATE notificacion SET tipo='POR_VENCER',claveEvento=REPLACE(claveEvento,':VENCIDO',':POR_VENCER') WHERE claveEvento LIKE ?","VENCIMIENTO:"+certificados[0]+":%"); return null; });
            ExecutorService pool=Executors.newFixedThreadPool(2);
            CountDownLatch inicio=new CountDownLatch(1);
            try {
                Callable<Integer> tarea=() -> { inicio.await(); return control.obtenerAlertasActualizadas().size(); };
                var a=pool.submit(tarea); var b=pool.submit(tarea); inicio.countDown();
                comprobar(a.get(30,TimeUnit.SECONDS)==cantidad+1 && b.get(30,TimeUnit.SECONDS)==cantidad+1,"Concurrencia y paso a vencido sin duplicar");
            } finally { pool.shutdownNow(); }
            SwingUtilities.invokeAndWait(() -> {
                PnlAlertas panel=new PnlAlertas(control); panel.refrescar();
                JTable tabla=encontrar(panel,JTable.class);
                comprobar(tabla.getRowCount()==cantidad+1 && tabla.getColumnCount()==6,"Tabla muestra datos reales en sus columnas");
                encontrar(panel,JComboBox.class).setSelectedItem("VENCIDO"); panel.refrescar();
                comprobar(tabla.getRowCount()>0,"Filtro vencido tiene resultados");
                for(int f=0;f<tabla.getRowCount();f++) comprobar(tabla.getValueAt(f,3).equals("VENCIDO"),"Filtro por tipo");
            });
            var propias = new NotificacionesControlador(trabajador);
            int propia = Jdbc.transaccion(c -> Jdbc.insertar(c,
                    "INSERT INTO notificacion(idTrabajador,tipo,mensaje,estado,claveEvento) VALUES (?, 'POR_VENCER','Aviso propio','PENDIENTE','PRUEBA:PROPIA')",
                    trabajador.getIdTrabajador()));
            var antes = consulta.obtenerAlertasActualizadas();
            int ajena = antes.stream().filter(n -> n.getIdTrabajador()!=trabajador.getIdTrabajador()
                    && "PENDIENTE".equals(n.getEstado())).findFirst().orElseThrow().getIdNotificacion();
            var misAlertas = propias.obtenerAlertasActualizadas();
            comprobar(misAlertas.stream().allMatch(n -> n.getIdTrabajador()==trabajador.getIdTrabajador()), "Solo devuelve alertas propias");
            comprobar(misAlertas.stream().anyMatch(n -> n.getIdNotificacion()==propia), "Incluye aviso propio");
            comprobar(consulta.obtenerAlertasActualizadas().size()==antes.size(), "Trabajador no genera avisos");
            rechazar(() -> propias.marcarComoLeida(ajena));
            comprobar(consulta.obtenerAlertasActualizadas().stream().anyMatch(n -> n.getIdNotificacion()==ajena && "PENDIENTE".equals(n.getEstado())), "Aviso ajeno permanece pendiente");
            propias.marcarComoLeida(propia);
            propias.marcarComoLeida(propia);
            comprobar(propias.obtenerAlertasActualizadas().stream().anyMatch(n -> n.getIdNotificacion()==propia && "LEIDA".equals(n.getEstado())), "Lectura propia persistente e idempotente");
            rechazar(() -> propias.marcarComoLeida(Integer.MAX_VALUE));
            SwingUtilities.invokeAndWait(() -> {
                PnlAlertas panel=new PnlAlertas(propias); panel.refrescar();
                JTable tabla=encontrar(panel,JTable.class);
                comprobar(tabla.getRowCount()==misAlertas.size(), "Pantalla muestra solo alertas propias");
                for(int f=0;f<tabla.getRowCount();f++) comprobar(tabla.getValueAt(f,1).equals(trabajador.getIdTrabajador()), "Pantalla aislada por trabajador");
                trabajador.close(); panel.refrescar();
                comprobar(tabla.getRowCount()==0, "Sesion cerrada limpia alertas visibles");
            });
            rechazar(propias::obtenerAlertasActualizadas);
            rechazar(() -> propias.marcarComoLeida(propia));
            admin.close(); rechazar(control::obtenerAlertasActualizadas);
        }
        System.out.println("OK: "+total+" comprobaciones MySQL y Swing de notificaciones.");
    }
}
