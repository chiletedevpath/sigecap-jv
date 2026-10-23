package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.util.Contrasenas;

public final class PruebaAccesoMySql {
    public static void verificar() {
        if (!"true".equals(System.getenv("SIGECAP_DB_TEST"))) throw new IllegalStateException("Solo base desechable.");
        char[] clave = "Clave-prueba-MySQL-2026".toCharArray();
        try {
            String hash = Contrasenas.crearHash(clave);
            Jdbc.transaccion(c -> {
                int rol = Jdbc.insertar(c, "INSERT INTO rol(nombre) VALUES (?)", "Rol prueba acceso");
                int permiso;
                try (var p = c.prepareStatement("SELECT idPermiso FROM permiso WHERE modulo='usuarios' AND nombre='consultar'"); var r = p.executeQuery()) {
                    permiso = r.next() ? r.getInt(1) : Jdbc.insertar(c,"INSERT INTO permiso(nombre,modulo) VALUES (?,?)","consultar","usuarios");
                }
                try (var s = c.prepareStatement("INSERT INTO rol_permiso(idRol,idPermiso) VALUES (?,?)")) {
                    Jdbc.parametros(s, rol, permiso); s.executeUpdate();
                }
                Jdbc.insertar(c, "INSERT INTO usuario(idRol,nombreCompleto,nombreUsuario,correo,contrasenaHash) VALUES (?,?,?,?,?)",
                        rol, "Prueba Acceso", "prueba_acceso", "acceso@example.test", hash);
                return null;
            });
            verificarPersistencia();
            LoginControlador login = new LoginControlador();
            try { login.autenticar("' OR 1=1 --", clave); throw new AssertionError("Inyección aceptada"); }
            catch (SecurityException esperado) { }
            Jdbc.transaccion(c -> { Jdbc.actualizar(c, "UPDATE usuario SET estado='INACTIVO' WHERE nombreUsuario=?", "prueba_acceso"); return null; });
            try { login.autenticar("prueba_acceso", clave); throw new AssertionError("Cuenta inactiva aceptada"); }
            catch (SecurityException esperado) { }
            Jdbc.transaccion(c -> { Jdbc.actualizar(c, "UPDATE usuario SET estado='ACTIVO' WHERE nombreUsuario=?", "prueba_acceso"); return null; });
            System.out.println("OK: inyección e inactividad rechazadas en MySQL.");
        } finally { java.util.Arrays.fill(clave, '\0'); }
    }
    public static void verificarPersistencia() {
        char[] clave = "Clave-prueba-MySQL-2026".toCharArray();
        try (SesionUsuario sesion = new LoginControlador().autenticar("prueba_acceso", clave)) {
            if (!sesion.puede("usuarios", "consultar") || sesion.puede("usuarios", "editar"))
                throw new AssertionError("Permisos MySQL incorrectos");
            if (sesion.getIdTrabajador() != null) throw new AssertionError("Trabajador opcional incorrecto");
            System.out.println("OK: autenticación y permisos persistidos en MySQL.");
        } finally { java.util.Arrays.fill(clave, '\0'); }
    }
}
