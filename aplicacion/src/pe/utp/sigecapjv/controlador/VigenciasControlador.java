package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.dao.VigenciaDAO;
import pe.utp.sigecapjv.modelo.Vigencia;
import java.util.List;

public class VigenciasControlador {
    private final SesionUsuario sesion;
    public VigenciasControlador(SesionUsuario sesion) { this.sesion=sesion; }

    public List<Vigencia> consultar() {
        if (sesion==null) throw new SecurityException("Debe iniciar sesión.");
        return Jdbc.transaccion(c -> {
            AccesoControlador.exigir(c,sesion,"capacitaciones","consultar");
            if ("Trabajador".equals(sesion.getRol()))
                throw new SecurityException("Consulte sus certificados en Mis capacitaciones.");
            return new VigenciaDAO().consultar(c);
        });
    }
}
