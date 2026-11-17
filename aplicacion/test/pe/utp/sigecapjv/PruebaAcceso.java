package pe.utp.sigecapjv;

import java.util.Set;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.AutenticacionDAO;
import pe.utp.sigecapjv.modelo.PermisoAcceso;
import pe.utp.sigecapjv.util.Contrasenas;

public final class PruebaAcceso {
    private static int total;
    private static void comprobar(boolean valor) {
        if (!valor) throw new AssertionError("Acceso: comprobación " + (total + 1));
        total++;
    }
    private static String rechazo(Runnable operacion) {
        try { operacion.run(); } catch (SecurityException ex) { total++; return ex.getMessage(); }
        throw new AssertionError("Se permitió acceso sin autorización.");
    }
    public static void verificar() {
        char[] clave = "Clave-de-prueba-2026".toCharArray();
        String hash = Contrasenas.crearHash(clave);
        comprobar(!hash.equals(Contrasenas.crearHash(clave)));
        comprobar(Contrasenas.verificar(clave, hash));
        comprobar(!Contrasenas.verificar("incorrecta".toCharArray(), hash));
        comprobar(!Contrasenas.verificar(clave, "texto-plano"));
        comprobar(!Contrasenas.verificar(clave, hash.replace("600000", "999999999")));
        comprobar(!Contrasenas.verificar(clave, "pbkdf2-sha256$600000$!$!"));
        AutenticacionDAO dao = new AutenticacionDAO() {
            @Override public Cuenta buscar(String usuario) {
                if (!Set.of("activo", "inactivo", "sinPermisos").contains(usuario)) return null;
                return new Cuenta(7, null, "Persona Prueba", "Administrador", hash,
                        !usuario.equals("inactivo"), usuario.equals("sinPermisos") ? Set.of()
                        : Set.of(new PermisoAcceso("usuarios", "consultar")));
            }
        };
        LoginControlador login = new LoginControlador(dao);
        SesionUsuario sesion = login.autenticar(" activo ", clave);
        comprobar(sesion.getIdUsuario() == 7 && sesion.getIdTrabajador() == null);
        comprobar(sesion.puede("usuarios", "consultar"));
        comprobar(!sesion.puede("usuarios", "editar"));
        rechazo(() -> sesion.exigirPermiso("usuarios", "editar"));
        sesion.close();
        comprobar(!sesion.estaActiva() && !sesion.puede("usuarios", "consultar"));
        rechazo(() -> sesion.exigirPermiso("usuarios", "consultar"));
        comprobar(!login.autenticar("sinPermisos", clave).puede("usuarios", "consultar"));
        String mensaje = rechazo(() -> login.autenticar("activo", "incorrecta".toCharArray()));
        comprobar(rechazo(() -> login.autenticar("inactivo", clave)).contains("inactiva"));
        comprobar(mensaje.equals(rechazo(() -> login.autenticar("inactivo", "incorrecta".toCharArray()))));
        comprobar(mensaje.equals(rechazo(() -> login.autenticar("ausente", clave))));
        comprobar(mensaje.equals(rechazo(() -> login.autenticar("", clave))));
        java.util.Arrays.fill(clave, '\0');
        System.out.println("OK: " + total + " comprobaciones de acceso.");
    }
}
