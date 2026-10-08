package pe.utp.sigecapjv.controlador;

import java.util.ArrayList;
import java.util.List;
import pe.utp.sigecapjv.modelo.Trabajador;
import pe.utp.sigecapjv.util.Validador;

public class PersonalControlador {
    // Mientras no exista persistencia, los registros se mantienen en memoria.
    private final List<Trabajador> trabajadores = new ArrayList<>();
    private int siguienteId = 1;

    public void registrarTrabajador(Trabajador trabajador) {
        Validador.requerido(trabajador, "Trabajador");
        Trabajador nuevo = trabajador.copia(); nuevo.registrar();
        validarDniUnico(nuevo, 0);
        nuevo.setIdTrabajador(siguienteId++); trabajadores.add(nuevo);
        trabajador.setIdTrabajador(nuevo.getIdTrabajador());
    }
    public void actualizarTrabajador(Trabajador trabajador) {
        Validador.requerido(trabajador, "Trabajador");
        int indice = indice(trabajador.getIdTrabajador());
        Trabajador actualizado = trabajador.copia(); actualizado.actualizar();
        validarDniUnico(actualizado, actualizado.getIdTrabajador());
        Trabajador actual = trabajadores.get(indice);
        actual.setDni(actualizado.getDni()); actual.setNombres(actualizado.getNombres());
        actual.setApellidos(actualizado.getApellidos()); actual.setCargo(actualizado.getCargo());
        actual.setEstado(actualizado.getEstado());
    }
    private void validarDniUnico(Trabajador trabajador, int exceptuarId) {
        if (trabajadores.stream().anyMatch(t -> t.getIdTrabajador() != exceptuarId && t.getDni().equals(trabajador.getDni())))
            throw new IllegalArgumentException("Ya existe un trabajador con ese DNI.");
    }
    private int indice(int id) {
        for (int i = 0; i < trabajadores.size(); i++) if (trabajadores.get(i).getIdTrabajador() == id) return i;
        throw new IllegalArgumentException("El trabajador no está registrado.");
    }
    public Trabajador buscarTrabajador(int id) { return trabajadores.get(indice(id)).copia(); }
    Trabajador trabajadorRegistrado(int id) { return trabajadores.get(indice(id)); }
    public List<Trabajador> listarTrabajadores() { return trabajadores.stream().map(Trabajador::copia).toList(); }
}
