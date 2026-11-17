package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public class Resultado {
    private int idResultado;
    private final int idParticipacion;
    private String condicion;
    private final BigDecimal calificacion;
    private LocalDateTime fechaRegistro;

    public Resultado(int idResultado, int idParticipacion, String condicion, BigDecimal calificacion,
                     LocalDateTime fechaRegistro) {
        this.idResultado = idResultado;
        this.idParticipacion = idParticipacion;
        this.condicion = condicion;
        this.calificacion = calificacion;
        this.fechaRegistro = fechaRegistro;
    }

    public void registrar() {
        if (idResultado != 0 || fechaRegistro != null)
            throw new IllegalStateException("El resultado ya está registrado.");
        validar();
        fechaRegistro = LocalDateTime.now().withNano(0);
    }

    public void actualizar() {
        if (idResultado <= 0 || fechaRegistro == null)
            throw new IllegalStateException("El resultado no está registrado.");
        validar();
    }

    private void validar() {
        if (idParticipacion <= 0) throw new IllegalArgumentException("Participación inválida.");
        condicion = Validador.texto(condicion, "Resultado").toUpperCase(Locale.ROOT);
        Validador.estado(condicion, "APROBADO", "DESAPROBADO", "APTO", "NO_APTO");
        if (!validarCalificacion())
            throw new IllegalArgumentException("La nota debe estar entre 0 y 20, con hasta dos decimales.");
    }

    public boolean validarCalificacion() {
        return calificacion == null || (calificacion.signum() >= 0
                && calificacion.compareTo(BigDecimal.valueOf(20)) <= 0
                && calificacion.stripTrailingZeros().scale() <= 2);
    }

    public boolean validarAsistencia(Asistencia asistencia) {
        return asistencia != null && asistencia.getIdParticipacion() == idParticipacion
                && "ASISTIO".equals(asistencia.getCondicion());
    }

    public boolean esFavorable() {
        return "APROBADO".equals(condicion) || "APTO".equals(condicion);
    }

    public int getIdResultado() {
        return idResultado;
    }

    public void setIdResultado(int id) {
        idResultado = id;
    }

    public int getIdParticipacion() {
        return idParticipacion;
    }

    public String getCondicion() {
        return condicion;
    }

    public BigDecimal getCalificacion() {
        return calificacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
