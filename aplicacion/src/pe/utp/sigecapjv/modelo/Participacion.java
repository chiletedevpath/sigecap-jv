package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;
import java.time.LocalDate;

public class Participacion {
    private int idParticipacion;
    private LocalDate fechaAsignacion;
    private String estado;
    private Trabajador trabajador;
    private Capacitacion capacitacion;

    public Participacion() {}

    public Participacion(int idParticipacion, LocalDate fechaAsignacion, String estado, Trabajador trabajador, Capacitacion capacitacion) {
        this.idParticipacion = idParticipacion;
        this.fechaAsignacion = fechaAsignacion;
        this.estado = estado;
        this.trabajador = trabajador;
        this.capacitacion = capacitacion;
    }

    public void asignarTrabajador() {
        if (fechaAsignacion != null) {
            throw new IllegalStateException("La participación ya fue asignada.");
        }
        Validador.requerido(trabajador, "Trabajador");
        Validador.requerido(capacitacion, "Capacitación");
        if (!"ACTIVO".equals(trabajador.getEstado())) {
            throw new IllegalArgumentException("El trabajador debe estar ACTIVO.");
        }
        if (!"PROGRAMADA".equals(capacitacion.getEstado())) {
            throw new IllegalArgumentException("La capacitación debe estar PROGRAMADA.");
        }
        capacitacion.agregarParticipacion(this);
        fechaAsignacion = LocalDate.now();
        estado = "PENDIENTE";
    }

    public void actualizarEstado() {
        Validador.requerido(fechaAsignacion, "Fecha de asignación");
        // El cumplimiento se incorporará con Asistencia y Resultado.
        Validador.estado(estado, "PENDIENTE", "CANCELADA");
    }

    Participacion copiaPara(Capacitacion copiaCapacitacion) {
        return new Participacion(idParticipacion, fechaAsignacion, estado,
                trabajador == null ? null : trabajador.copia(), copiaCapacitacion);
    }

    public Participacion copia() {
        return copiaPara(capacitacion == null ? null : capacitacion.copia());
    }

    public int getIdParticipacion() {
        return idParticipacion;
    }

    public void setIdParticipacion(int idParticipacion) {
        this.idParticipacion = idParticipacion;
    }

    public LocalDate getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDate fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Trabajador getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(Trabajador trabajador) {
        this.trabajador = trabajador;
    }

    public Capacitacion getCapacitacion() {
        return capacitacion;
    }

    public void setCapacitacion(Capacitacion capacitacion) {
        this.capacitacion = capacitacion;
    }
}
