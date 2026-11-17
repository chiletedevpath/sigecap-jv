package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.InicioDAO;
import pe.utp.sigecapjv.dao.Jdbc;
import java.util.List;
import java.util.Objects;
import java.sql.Connection;
import java.sql.SQLException;

public class InicioControlador {
    private final SesionUsuario sesion;
    private final InicioDAO dao = new InicioDAO();
    public InicioControlador(SesionUsuario sesion) { this.sesion=Objects.requireNonNull(sesion); }
    public record Panel(InicioDAO.Resumen resumen, List<InicioDAO.CapacitacionProgramada> programadas,
                        List<InicioDAO.ProximoVencimiento> vencimientos) {
        public Panel { programadas=List.copyOf(programadas); vencimientos=List.copyOf(vencimientos); }
    }
    private Integer alcance(Connection c) throws SQLException {
        if(!SesionControlador.vigente(c,sesion)) throw new SecurityException("La sesión ha expirado. Inicie sesión nuevamente.");
        if("Trabajador".equals(sesion.getRol())) {
            if(sesion.getIdTrabajador()==null) throw new SecurityException("Su cuenta no está vinculada a un trabajador.");
            return sesion.getIdTrabajador();
        }
        sesion.exigirPermiso("personal","consultar");
        sesion.exigirPermiso("capacitaciones","consultar");
        return null;
    }
    public Panel obtenerPanel() {
        return Jdbc.transaccion(c -> {
            Integer trabajador=alcance(c);
            return new Panel(dao.obtenerResumen(c,trabajador),dao.listarProgramadas(c,trabajador),dao.listarProximosVencimientos(c,trabajador));
        });
    }
    public InicioDAO.Resumen obtenerResumen() { return Jdbc.transaccion(c->dao.obtenerResumen(c,alcance(c))); }
    public List<InicioDAO.CapacitacionProgramada> listarProgramadas() { return Jdbc.transaccion(c->dao.listarProgramadas(c,alcance(c))); }
    public List<InicioDAO.ProximoVencimiento> listarProximosVencimientos() { return Jdbc.transaccion(c->dao.listarProximosVencimientos(c,alcance(c))); }
}
