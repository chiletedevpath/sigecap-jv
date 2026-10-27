package pe.utp.sigecapjv;

import java.util.*;
import java.util.concurrent.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.modelo.*;

public final class PruebaPermisosMySql {
    private static int total;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException | SecurityException esperado) { total++; return; }
        throw new AssertionError("Se aceptó una modificación inválida de permisos.");
    }
    private static int permiso(RolControlador.Configuracion c, String modulo, String nombre) {
        return c.catalogo().stream().filter(p -> p.modulo().equals(modulo) && p.nombre().equals(nombre))
                .findFirst().orElseThrow().idPermiso();
    }

    public static void verificar() throws Exception {
        char[] claveAdmin = "Clave-usuarios-prueba-2026".toCharArray();
        char[] claveSupervisor = "Clave-renovada-prueba-2026".toCharArray();
        LoginControlador login = new LoginControlador();
        try (SesionUsuario admin = login.autenticar("admin_prueba", claveAdmin)) {
            RolControlador control = new RolControlador(admin);
            int supervisor = control.listar().stream().filter(r -> r.nombre().equals("Supervisor de Operaciones")).findFirst().orElseThrow().idRol();
            int administrador = control.listar().stream().filter(r -> r.nombre().equals("Administrador")).findFirst().orElseThrow().idRol();
            int trabajador = control.listar().stream().filter(r -> r.nombre().equals("Trabajador")).findFirst().orElseThrow().idRol();
            RolControlador.Configuracion original = control.consultar(supervisor);
            int personal = permiso(original,"personal","consultar"), gestionar = permiso(original,"personal","gestionar");
            int cap = permiso(original,"capacitaciones","consultar"), usuarios = permiso(original,"usuarios","consultar");
            Set<Integer> iniciales = original.asignados();

            rechazar(() -> control.guardar(supervisor, iniciales, Set.of(Integer.MAX_VALUE)));
            rechazar(() -> control.guardar(supervisor, iniciales, Set.of(gestionar)));
            rechazar(() -> control.guardar(supervisor, iniciales, Set.of(cap)));
            rechazar(() -> control.guardar(trabajador, control.consultar(trabajador).asignados(), Set.of(personal)));
            rechazar(() -> control.guardar(administrador, control.consultar(administrador).asignados(), Set.of(personal)));
            comprobar(control.consultar(supervisor).asignados().equals(iniciales), "Un rechazo modificó las asignaciones.");

            try (SesionUsuario abierta = login.autenticar("supervisor_prueba", claveSupervisor)) {
                rechazar(() -> new RolControlador(abierta).guardar(supervisor, iniciales, Set.of()));
                control.guardar(supervisor, iniciales, iniciales);
                comprobar(new SesionControlador().vigente(abierta), "Guardar sin cambios invalidó la sesión.");
                control.guardar(supervisor, iniciales, Set.of(personal));
                rechazar(() -> new CapacitacionJdbcControlador(new PersonalJdbcControlador(abierta), abierta).listarCursos());
                comprobar(!abierta.estaActiva(), "La sesión conservó permisos anteriores.");
            }
            try (SesionUsuario nueva = login.autenticar("supervisor_prueba", claveSupervisor)) {
                comprobar(nueva.puede("personal","consultar") && !nueva.puede("capacitaciones","consultar"), "El nuevo login no tomó los permisos guardados.");
                comprobar(!new PersonalJdbcControlador(nueva).listarTrabajadores().isEmpty(), "Se perdió el acceso permitido.");
                rechazar(() -> new RolControlador(nueva).listar());
                control.guardar(supervisor, Set.of(personal), Set.of(personal, cap));
                comprobar(!new SesionControlador().vigente(nueva), "Agregar permisos no exigió reautenticación.");
            }
            try (SesionUsuario consulta = login.autenticar("supervisor_prueba", claveSupervisor)) {
                rechazar(() -> new RolControlador(consulta).listar());
                rechazar(() -> new RolControlador(consulta).guardar(supervisor, Set.of(personal, cap), iniciales));
            }
            control.guardar(supervisor, Set.of(personal, cap), iniciales);
            rechazar(() -> control.guardar(supervisor, Set.of(personal), Set.of()));
            comprobar(control.consultar(supervisor).asignados().equals(iniciales), "Una edición obsoleta sobreescribió los permisos.");

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch inicio = new CountDownLatch(1);
            try {
                Future<Boolean> uno = pool.submit(() -> cambiar(control, supervisor, iniciales, Set.of(personal), inicio));
                Future<Boolean> dos = pool.submit(() -> cambiar(control, supervisor, iniciales, Set.of(), inicio));
                inicio.countDown();
                comprobar((uno.get(30,TimeUnit.SECONDS) ? 1 : 0) + (dos.get(30,TimeUnit.SECONDS) ? 1 : 0) == 1,
                        "Dos ediciones simultáneas se sobrescribieron.");
            } finally { pool.shutdownNow(); }
            Set<Integer> finales = control.consultar(supervisor).asignados();
            comprobar(finales.isEmpty() || finales.equals(Set.of(personal)), "La asignación quedó mezclada.");
            control.guardar(supervisor, finales, iniciales);
            comprobar(control.consultar(supervisor).asignados().equals(iniciales), "No se restauraron los permisos de prueba.");
            comprobar(new SesionControlador().vigente(admin), "Se afectó la sesión de otro rol.");
            System.out.println("OK: " + total + " comprobaciones MySQL de permisos, sesiones y concurrencia.");
        } finally { Arrays.fill(claveAdmin, '\0'); Arrays.fill(claveSupervisor, '\0'); }
    }

    private static boolean cambiar(RolControlador control, int rol, Set<Integer> original, Set<Integer> seleccion,
                                   CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try { control.guardar(rol, original, seleccion); return true; }
        catch (IllegalStateException esperado) {
            if (!esperado.getMessage().contains("Otro administrador")) throw esperado;
            return false;
        }
    }
}
