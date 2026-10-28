package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.HistorialDAO;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.modelo.HistorialCapacitacion;
import java.util.List;

public class HistorialControlador {
    private final SesionUsuario sesion;
    private final HistorialDAO dao = new HistorialDAO();

    public HistorialControlador(SesionUsuario sesion) { this.sesion = sesion; }

    // El trabajador no puede enviar un identificador ajeno desde la vista.
    public List<HistorialCapacitacion> consultarMisCapacitaciones() {
        return Jdbc.transaccion(c -> {
            if (sesion == null || !SesionControlador.vigente(c, sesion))
                throw new SecurityException("Debe iniciar sesión nuevamente.");
            if (!"Trabajador".equals(sesion.getRol()) || sesion.getIdTrabajador() == null)
                throw new SecurityException("Esta consulta requiere una cuenta de trabajador vinculada.");
            return dao.consultar(c, sesion.getIdTrabajador());
        });
    }

    public List<HistorialCapacitacion> consultarTrabajador(int trabajador) {
        if (sesion == null) throw new SecurityException("Debe iniciar sesión nuevamente.");
        return Jdbc.transaccion(c -> {
            AccesoControlador.exigir(c, sesion, "personal", "consultar");
            sesion.exigirPermiso("capacitaciones", "consultar");
            if ("Trabajador".equals(sesion.getRol())) throw new SecurityException("Consulte Mis capacitaciones.");
            if (trabajador <= 0) throw new IllegalArgumentException("Seleccione un trabajador.");
            return dao.consultar(c, trabajador);
        });
    }
}
