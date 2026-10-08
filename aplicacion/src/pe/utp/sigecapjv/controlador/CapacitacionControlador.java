package pe.utp.sigecapjv.controlador;

import java.util.ArrayList;
import java.util.List;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.util.Validador;

public class CapacitacionControlador {
    private final PersonalControlador personal;
    private final List<Curso> cursos = new ArrayList<>();
    private final List<Capacitacion> capacitaciones = new ArrayList<>();
    private final List<Participacion> participaciones = new ArrayList<>();
    private int siguienteCurso = 1, siguienteCapacitacion = 1, siguienteParticipacion = 1;

    public CapacitacionControlador(PersonalControlador personal) {
        Validador.requerido(personal, "Controlador de personal"); this.personal = personal;
    }
    public void registrarCurso(Curso curso) {
        Validador.requerido(curso, "Curso"); Curso nuevo = curso.copia(); nuevo.setIdCurso(0); nuevo.registrar(); validarCodigo(nuevo);
        nuevo.setIdCurso(siguienteCurso++); cursos.add(nuevo); curso.setIdCurso(nuevo.getIdCurso());
    }
    public void actualizarCurso(Curso curso) {
        Validador.requerido(curso, "Curso"); Curso actual = buscarCurso(curso.getIdCurso());
        Curso nuevo = curso.copia(); nuevo.actualizar(); validarCodigo(nuevo);
        // Se conserva la referencia utilizada por las capacitaciones ya registradas.
        actual.setCodigo(nuevo.getCodigo()); actual.setNombre(nuevo.getNombre()); actual.setTipo(nuevo.getTipo());
        actual.setVigenciaMeses(nuevo.getVigenciaMeses()); actual.setEstado(nuevo.getEstado());
    }
    private void validarCodigo(Curso curso) {
        if (cursos.stream().anyMatch(c -> c.getIdCurso() != curso.getIdCurso() && c.getCodigo().equalsIgnoreCase(curso.getCodigo())))
            throw new IllegalArgumentException("Ya existe un curso con ese código.");
    }
    private Curso buscarCurso(int id) {
        return cursos.stream().filter(c -> c.getIdCurso() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El curso no está registrado."));
    }
    private Capacitacion buscarCapacitacion(int id) {
        return capacitaciones.stream().filter(c -> c.getIdCapacitacion() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La capacitación no está registrada."));
    }
    public void programarCapacitacion(Capacitacion capacitacion) {
        Validador.requerido(capacitacion, "Capacitación"); Validador.requerido(capacitacion.getCurso(), "Curso");
        Capacitacion nueva = new Capacitacion(0, capacitacion.getFecha(), capacitacion.getHoraInicio(),
                capacitacion.getHoraFin(), capacitacion.getModalidad(), null, buscarCurso(capacitacion.getCurso().getIdCurso()));
        if (capacitacion.getEstado() != null && !capacitacion.getEstado().isBlank())
            throw new IllegalArgumentException("Para programar, use una capacitación nueva sin estado.");
        nueva.programar(); nueva.setIdCapacitacion(siguienteCapacitacion++); capacitaciones.add(nueva);
        capacitacion.setIdCapacitacion(nueva.getIdCapacitacion()); capacitacion.setEstado(nueva.getEstado());
    }
    public void actualizarCapacitacion(Capacitacion datos) {
        Validador.requerido(datos, "Capacitación"); Validador.requerido(datos.getCurso(), "Curso");
        Capacitacion actual = buscarCapacitacion(datos.getIdCapacitacion());
        Capacitacion candidata = new Capacitacion(actual.getIdCapacitacion(), datos.getFecha(), datos.getHoraInicio(),
                datos.getHoraFin(), datos.getModalidad(), actual.getEstado(), buscarCurso(datos.getCurso().getIdCurso()));
        candidata.actualizar();
        actual.setCurso(candidata.getCurso()); actual.setFecha(candidata.getFecha());
        actual.setHoraInicio(candidata.getHoraInicio()); actual.setHoraFin(candidata.getHoraFin()); actual.setModalidad(candidata.getModalidad());
    }
    public void cancelarCapacitacion(int id) { buscarCapacitacion(id).cancelar(); }
    public void cerrarCapacitacion(int id) { buscarCapacitacion(id).cerrar(); }
    public Participacion asignarTrabajador(Trabajador trabajador, Capacitacion capacitacion) {
        Validador.requerido(trabajador, "Trabajador"); Validador.requerido(capacitacion, "Capacitación");
        Trabajador registrado = personal.trabajadorRegistrado(trabajador.getIdTrabajador());
        Capacitacion programada = buscarCapacitacion(capacitacion.getIdCapacitacion());
        Participacion nueva = new Participacion(0, null, null, registrado, programada);
        nueva.asignarTrabajador(); nueva.setIdParticipacion(siguienteParticipacion++); participaciones.add(nueva);
        return nueva.copia();
    }
    public List<Curso> listarCursos() { return cursos.stream().map(Curso::copia).toList(); }
    public List<Capacitacion> listarCapacitaciones() { return capacitaciones.stream().map(Capacitacion::copia).toList(); }
    public List<Participacion> listarParticipaciones() { return participaciones.stream().map(Participacion::copia).toList(); }
}
