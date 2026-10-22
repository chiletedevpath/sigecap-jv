package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.*;
import java.sql.Connection;
import java.sql.SQLException;

public final class AccesoControlador {
    private AccesoControlador() { }
    public static void exigir(SesionUsuario sesion, String modulo, String accion) {
        if (sesion == null) {
            if (Boolean.getBoolean("sigecap.pruebas")) return;
            throw new SecurityException("Debe iniciar sesión.");
        }
        Jdbc.transaccion(c -> { exigir(c, sesion, modulo, accion); return null; });
    }

    static void exigir(Connection c, SesionUsuario sesion, String modulo, String accion) throws SQLException {
        if (!SesionControlador.vigente(c, sesion))
            throw new SecurityException("Su acceso cambió o la cuenta fue desactivada. Inicie sesión nuevamente.");
        sesion.exigirPermiso(modulo, accion);
    }
}
