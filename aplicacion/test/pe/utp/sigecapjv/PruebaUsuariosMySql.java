package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.vista.PnlUsuarios;
import java.util.concurrent.*;

public final class PruebaUsuariosMySql {
    private static int total;
    private static void comprobar(boolean valor) {
        if (!valor) throw new AssertionError("Usuarios: comprobación " + (total+1));
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) { total++; return; }
        throw new AssertionError("Se aceptó una operación inválida de usuarios.");
    }
    private static Usuario datos(int id,int rol,Integer trabajador,String login,String estado) {
        return new Usuario(id,rol,trabajador,"Persona de prueba",login,login+"@example.test",estado);
    }
    public static void inicializar() throws Exception {
        char[] clave="Clave-usuarios-prueba-2026".toCharArray();
        try {
            comprobar(UsuarioControlador.requiereAdministradorInicial());
            ExecutorService pool=Executors.newFixedThreadPool(2);
            CountDownLatch inicio=new CountDownLatch(1);
            Callable<Boolean> crear=() -> {
                inicio.await();
                try { UsuarioControlador.crearAdministradorInicial("Administrador prueba","admin_prueba","admin@example.test",clave); return true; }
                catch (IllegalStateException ex) { return false; }
            };
            try {
                Future<Boolean> uno=pool.submit(crear),dos=pool.submit(crear); inicio.countDown();
                comprobar((uno.get(30,TimeUnit.SECONDS)?1:0)+(dos.get(30,TimeUnit.SECONDS)?1:0)==1);
            } finally { pool.shutdownNow(); }
        } finally { java.util.Arrays.fill(clave, '\0'); }
    }
    public static void verificar() throws Exception {
        char[] clave="Clave-usuarios-prueba-2026".toCharArray();
        try {
            comprobar(!UsuarioControlador.requiereAdministradorInicial());
            LoginControlador login=new LoginControlador();
            try (SesionUsuario admin=login.autenticar("admin_prueba",clave)) {
                UsuarioControlador gestion=new UsuarioControlador(admin);
                comprobar(gestion.roles().size()==4 && gestion.permisos().size()==12);
                int rolAdmin=gestion.roles().stream().filter(r->r.nombre().equals("Administrador")).findFirst().orElseThrow().idRol();
                int supervisor=gestion.roles().stream().filter(r->r.nombre().equals("Supervisor de Operaciones")).findFirst().orElseThrow().idRol();
                int trabajador=gestion.roles().stream().filter(r->r.nombre().equals("Trabajador")).findFirst().orElseThrow().idRol();
                rechazar(()->gestion.guardar(datos(admin.getIdUsuario(),rolAdmin,null,"admin_prueba","INACTIVO"),null));
                rechazar(()->gestion.guardar(datos(admin.getIdUsuario(),supervisor,null,"admin_prueba","ACTIVO"),null));
                rechazar(()->gestion.guardar(datos(0,trabajador,null,"sin_vinculo","ACTIVO"),clave));
                rechazar(()->gestion.guardar(datos(0,supervisor,null,"sin_clave","ACTIVO"),null));
                int id=gestion.guardar(datos(0,supervisor,null,"supervisor_prueba","ACTIVO"),clave);
                rechazar(()->gestion.guardar(datos(0,supervisor,null,"supervisor_prueba","ACTIVO"),clave));
                comprobar(gestion.listar().size()==2);
                try (SesionUsuario lectura=login.autenticar("supervisor_prueba",clave)) {
                    comprobar(new PersonalJdbcControlador(lectura).listarTrabajadores()!=null);
                    rechazar(()->new PersonalJdbcControlador(lectura).registrarTrabajador(null));
                    rechazar(()->new CapacitacionJdbcControlador(new PersonalJdbcControlador(lectura),lectura).cancelarCapacitacion(1));
                    rechazar(()->new SeguimientoControlador(lectura).registrarAsistencia(1,"ASISTIO"));
                    rechazar(()->new UsuarioControlador(lectura).listar());
                    gestion.guardar(datos(id,supervisor,null,"supervisor_prueba","INACTIVO"),null);
                    rechazar(()->new PersonalJdbcControlador(lectura).listarTrabajadores());
                    rechazar(()->login.autenticar("supervisor_prueba",clave));
                }
                gestion.guardar(datos(id,supervisor,null,"supervisor_prueba","ACTIVO"),null);
                try (SesionUsuario lectura=login.autenticar("supervisor_prueba",clave)) {
                    lectura.close(); rechazar(()->new PersonalJdbcControlador(lectura).listarTrabajadores());
                }
                char[] nueva="Clave-renovada-prueba-2026".toCharArray();
                try { gestion.guardar(datos(id,supervisor,null,"supervisor_prueba","ACTIVO"),nueva);
                    rechazar(()->login.autenticar("supervisor_prueba",clave));
                    try (SesionUsuario renovada=login.autenticar("supervisor_prueba",nueva)) { comprobar(renovada.estaActiva()); }
                } finally { java.util.Arrays.fill(nueva,'\0'); }
                javax.swing.SwingUtilities.invokeAndWait(()-> {
                    PnlUsuarios panel=new PnlUsuarios(gestion);
                    comprobar(encontrar(panel,javax.swing.JTable.class).getRowCount()==2);
                });
                String propiedad=System.getProperty("sigecap.pruebas");
                try { System.clearProperty("sigecap.pruebas"); rechazar(()->new PersonalJdbcControlador().listarTrabajadores()); }
                finally { if(propiedad!=null) System.setProperty("sigecap.pruebas",propiedad); }
                int responsable=gestion.roles().stream().filter(r->r.nombre().equals("Responsable de capacitaciones")).findFirst().orElseThrow().idRol();
                gestion.guardar(datos(0,responsable,null,"responsable_prueba","ACTIVO"),clave);
                try (SesionUsuario responsableSesion=login.autenticar("responsable_prueba",clave)) {
                    comprobar(responsableSesion.puede("personal","gestionar") && responsableSesion.puede("capacitaciones","gestionar"));
                    comprobar(new PersonalJdbcControlador(responsableSesion).listarTrabajadores()!=null);
                    rechazar(()->new UsuarioControlador(responsableSesion).listar());
                    rechazar(()->new RolControlador(responsableSesion).listar());
                }
                int idTrabajador=pe.utp.sigecapjv.dao.Jdbc.transaccion(c -> {
                    try(var p=c.prepareStatement("SELECT idTrabajador FROM trabajador ORDER BY idTrabajador LIMIT 1");var r=p.executeQuery()) {
                        if(!r.next()) throw new AssertionError("Falta trabajador de prueba.");
                        return r.getInt(1);
                    }
                });
                gestion.guardar(datos(0,trabajador,idTrabajador,"trabajador_prueba","ACTIVO"),clave);
                try (SesionUsuario trabajadorSesion=login.autenticar("trabajador_prueba",clave)) {
                    comprobar(trabajadorSesion.getIdTrabajador().equals(idTrabajador));
                    comprobar(!trabajadorSesion.puede("personal","consultar") && !trabajadorSesion.puede("usuarios","gestionar"));
                    rechazar(()->new PersonalJdbcControlador(trabajadorSesion).listarTrabajadores());
                    rechazar(()->new UsuarioControlador(trabajadorSesion).listar());
                    rechazar(()->new SeguimientoControlador(trabajadorSesion).registrarAsistencia(1,"ASISTIO"));
                }
            }
            System.out.println("OK: "+total+" comprobaciones de administración y autorización MySQL.");
        } finally { java.util.Arrays.fill(clave,'\0'); }
    }
    private static <T> T encontrar(java.awt.Component c,Class<T> tipo) {
        if(tipo.isInstance(c)) return tipo.cast(c);
        if(c instanceof java.awt.Container contenedor) for(java.awt.Component hijo:contenedor.getComponents()) {
            T resultado=encontrar(hijo,tipo); if(resultado!=null)return resultado;
        }
        return null;
    }
}
