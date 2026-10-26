package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.InicioDAO;
import pe.utp.sigecapjv.dao.Jdbc;

import java.util.List;
import java.util.Objects;

public class InicioControlador {

    private final SesionUsuario sesion;
    private final InicioDAO inicioDAO;

    public InicioControlador(SesionUsuario sesion) {
        this.sesion = Objects.requireNonNull(sesion);
        this.inicioDAO = new InicioDAO();
    }

    public InicioDAO.Resumen obtenerResumen() {
        return Jdbc.transaccion(conexion -> {

            if (!SesionControlador.vigente(conexion, sesion)) {
                throw new SecurityException("La sesión ha expirado. Inicie sesión nuevamente.");
            }

            if (!"Administrador".equals(sesion.getRol())) {
                throw new SecurityException("No tiene autorización para consultar el resumen general.");
            }

            return inicioDAO.obtenerResumen(conexion);
        });
    }


    public List<InicioDAO.CapacitacionProgramada> listarProgramadas() {
        return Jdbc.transaccion(conexion -> {

            if (!SesionControlador.vigente(conexion, sesion)) {
                throw new SecurityException("La sesión ha expirado. Inicie sesión nuevamente.");
            }

            if (!"Administrador".equals(sesion.getRol())) {
                throw new SecurityException("No tiene autorización para consultar las capacitaciones.");
            }

            return inicioDAO.listarProgramadas(conexion);
        });
    }

    public List<InicioDAO.ProximoVencimiento> listarProximosVencimientos() {
        return Jdbc.transaccion(conexion -> {

            if (!SesionControlador.vigente(conexion, sesion)) {
                throw new SecurityException("La sesión ha expirado. Inicie sesión nuevamente.");
            }

            if (!"Administrador".equals(sesion.getRol())) {
                throw new SecurityException("No tiene autorización para consultar los vencimientos.");
            }

            return inicioDAO.listarProximosVencimientos(conexion);
        });
    }

}
