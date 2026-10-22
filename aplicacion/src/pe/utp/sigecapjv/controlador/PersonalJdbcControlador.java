package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.dao.TrabajadorDAO;
import pe.utp.sigecapjv.modelo.Trabajador;
import pe.utp.sigecapjv.util.Validador;

import java.util.List;

/**
 * Variante persistente; la variante en memoria se conserva para pruebas unitarias.
 */
public class PersonalJdbcControlador extends PersonalControlador {
    private final SesionUsuario sesion;
    public PersonalJdbcControlador() { this(null); }
    public PersonalJdbcControlador(SesionUsuario sesion) { this.sesion = sesion; }

    private final TrabajadorDAO dao = new TrabajadorDAO();

    @Override
    public void registrarTrabajador(Trabajador trabajador) {
        AccesoControlador.exigir(sesion, "personal", "gestionar");
        Validador.requerido(trabajador, "Trabajador");
        Trabajador nuevo = trabajador.copia();
        nuevo.registrar();
        int id = Jdbc.transaccion(c -> dao.insertar(c, nuevo));
        trabajador.setIdTrabajador(id);
    }

    @Override
    public void actualizarTrabajador(Trabajador trabajador) {
        AccesoControlador.exigir(sesion, "personal", "gestionar");
        Validador.requerido(trabajador, "Trabajador");
        Trabajador nuevo = trabajador.copia();
        nuevo.actualizar();
        Jdbc.transaccion(c -> {
            dao.buscar(c, nuevo.getIdTrabajador(), true);
            dao.actualizar(c, nuevo);
            return null;
        });
    }

    @Override
    public Trabajador buscarTrabajador(int id) {
        AccesoControlador.exigir(sesion, "personal", "consultar");
        return Jdbc.transaccion(c -> dao.buscar(c, id, false));
    }

    @Override
    Trabajador trabajadorRegistrado(int id) {
        return buscarTrabajador(id);
    }

    @Override
    public List<Trabajador> listarTrabajadores() {
        AccesoControlador.exigir(sesion, "personal", "consultar");
        return Jdbc.transaccion(dao::listar);
    }
}
