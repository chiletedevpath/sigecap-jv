package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class Capacitacion {
    private int idCapacitacion;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String modalidad;
    private String estado;
    private Curso curso;

    public Capacitacion() {}

    public Capacitacion(int idCapacitacion, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String modalidad, String estado, Curso curso) {
        this.idCapacitacion = idCapacitacion;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.modalidad = modalidad;
        this.estado = estado;
        this.curso = curso;
    }

    private final List<Participacion> participaciones = new ArrayList<>();

    public void programar() {
        if (estado != null && !estado.isBlank()) {
            throw new IllegalStateException("La capacitación ya tiene un estado.");
        }
        validarProgramacion();
        estado = "PROGRAMADA";
    }

    public void actualizar() {
        exigirProgramada();
        validarProgramacion();
    }

    public void cancelar() {
        exigirProgramada();
        estado = "CANCELADA";
    }

    public void cerrar() {
        exigirProgramada();
        estado = "CERRADA";
    }

    private void exigirProgramada() {
        if (!"PROGRAMADA".equals(estado)) {
            throw new IllegalStateException("La capacitación debe estar PROGRAMADA.");
        }
    }

    private void validarProgramacion() {
        Validador.requerido(curso, "Curso");
        // Una capacitación solo puede programarse con un curso activo.
        if (!"ACTIVO".equals(curso.getEstado())) {
            throw new IllegalArgumentException("El curso debe estar ACTIVO.");
        }
        Validador.requerido(fecha, "Fecha");
        Validador.requerido(horaInicio, "Hora de inicio");
        Validador.requerido(horaFin, "Hora de fin");
        Validador.texto(modalidad, "Modalidad");
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior al inicio.");
        }
        if (LocalDateTime.of(fecha, horaInicio).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora de inicio no pueden estar en el pasado.");
        }
        modalidad = modalidad.trim();
    }

    // La composición del UML se mantiene: la capacitación contiene sus participaciones.
    void agregarParticipacion(Participacion participacion) {
        exigirProgramada();
        if (participaciones.stream().anyMatch(p ->
                (p.getTrabajador().getIdTrabajador() > 0
                    && p.getTrabajador().getIdTrabajador() == participacion.getTrabajador().getIdTrabajador())
                || p.getTrabajador().getDni().equals(participacion.getTrabajador().getDni()))) {
            throw new IllegalArgumentException("El trabajador ya está asignado a esta capacitación.");
        }
        participaciones.add(participacion);
    }

    public List<Participacion> getParticipaciones() {
        return List.copyOf(participaciones);
    }

    public Capacitacion copia() {
        Capacitacion copia = new Capacitacion(idCapacitacion, fecha, horaInicio, horaFin, modalidad, estado,
                curso == null ? null : curso.copia());
        for (Participacion p : participaciones) {
            copia.participaciones.add(p.copiaPara(copia));
        }
        return copia;
    }

    @Override
    public String toString() {
        return (curso == null ? "Sin curso" : curso.getNombre()) + " - " + fecha + " " + horaInicio;
    }

    public int getIdCapacitacion() {
        return idCapacitacion;
    }

    public void setIdCapacitacion(int idCapacitacion) {
        this.idCapacitacion = idCapacitacion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }
}
