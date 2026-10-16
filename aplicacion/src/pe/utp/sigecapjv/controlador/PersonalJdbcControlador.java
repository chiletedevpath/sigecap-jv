package pe.utp.sigecapjv.controlador;

import java.util.List;
import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.Trabajador;
import pe.utp.sigecapjv.util.Validador;

/** Variante persistente; la variante en memoria se conserva para pruebas unitarias. */
public class PersonalJdbcControlador extends PersonalControlador {
    private final TrabajadorDAO dao = new TrabajadorDAO();

    @Override
    public void registrarTrabajador(Trabajador trabajador) {
        Validador.requerido(trabajador, "Trabajador");
        Trabajador nuevo = trabajador.copia();
        nuevo.registrar();
        int id = Jdbc.transaccion(c -> dao.insertar(c, nuevo));
        trabajador.setIdTrabajador(id);
    }

    @Override
    public void actualizarTrabajador(Trabajador trabajador) {
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
        return Jdbc.transaccion(c -> dao.buscar(c, id, false));
    }

    @Override
    Trabajador trabajadorRegistrado(int id) { return buscarTrabajador(id); }

    @Override
    public List<Trabajador> listarTrabajadores() { return Jdbc.transaccion(dao::listar); }
}
