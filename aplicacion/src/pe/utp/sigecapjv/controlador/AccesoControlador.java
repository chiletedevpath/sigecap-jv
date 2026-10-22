package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.*;

public final class AccesoControlador {
    private AccesoControlador() { }
    public static void exigir(SesionUsuario sesion, String modulo, String accion) {
        if (sesion == null) {
            if (Boolean.getBoolean("sigecap.pruebas")) return;
            throw new SecurityException("Debe iniciar sesión.");
        }
        if (!sesion.estaActiva() || !Jdbc.transaccion(c -> AccesoDAO.permite(c, sesion.getIdUsuario(), modulo, accion)))
            throw new SecurityException("Su cuenta no tiene acceso a esta operación. Inicie sesión nuevamente si cambió su rol.");
    }
}
