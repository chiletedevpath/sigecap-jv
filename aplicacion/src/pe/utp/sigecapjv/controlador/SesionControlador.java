package pe.utp.sigecapjv.controlador;

import java.sql.Connection;
import java.sql.SQLException;
import pe.utp.sigecapjv.dao.*;

/** Las sesiones con cambios de rol, vínculo o permisos deben autenticarse de nuevo. */
public class SesionControlador {
    public boolean vigente(SesionUsuario sesion) {
        if (!sesion.estaActiva()) return false;
        return Jdbc.transaccion(c -> vigente(c, sesion));
    }

    static boolean vigente(Connection c, SesionUsuario sesion) throws SQLException {
        if (!sesion.estaActiva()) return false;
        AccesoDAO.EstadoCuenta cuenta = AccesoDAO.estado(c, sesion.getIdUsuario());
        if (cuenta == null || !cuenta.activa()
                || !sesion.conservaAcceso(cuenta.trabajador(), cuenta.rol(), cuenta.permisos())) {
            sesion.close();
            return false;
        }
        return sesion.estaActiva();
    }
}
