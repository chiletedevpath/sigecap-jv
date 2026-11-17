package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Una participación admite un único registro de asistencia.
 */
public class Asistencia {
    private int idAsistencia;
    private int idParticipacion;
    private String condicion;
    private LocalDateTime fechaRegistro;

    public Asistencia() {
    }

    public Asistencia(int idAsistencia, int idParticipacion, String condicion, LocalDateTime fechaRegistro) {
        this.idAsistencia = idAsistencia;
        this.idParticipacion = idParticipacion;
        this.condicion = condicion;
        this.fechaRegistro = fechaRegistro;
    }

    public void registrar() {
        if (idAsistencia != 0 || fechaRegistro != null) {
            throw new IllegalStateException("La asistencia ya fue registrada.");
        }
        validar();
        fechaRegistro = LocalDateTime.now().withNano(0);
    }

    public void actualizar() {
        if (idAsistencia <= 0 || fechaRegistro == null) {
            throw new IllegalStateException("La asistencia no está registrada.");
        }
        validar();
    }

    private void validar() {
        if (idParticipacion <= 0) throw new IllegalArgumentException("Seleccione una participación registrada.");
        String valor = Validador.texto(condicion, "Condición").toUpperCase(Locale.ROOT);
        Validador.estado(valor, "ASISTIO", "NO_ASISTIO", "JUSTIFICADA");
        condicion = valor;
    }

    public int getIdAsistencia() {
        return idAsistencia;
    }

    public void setIdAsistencia(int valor) {
        idAsistencia = valor;
    }

    public int getIdParticipacion() {
        return idParticipacion;
    }

    public void setIdParticipacion(int valor) {
        idParticipacion = valor;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String valor) {
        condicion = valor;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime valor) {
        fechaRegistro = valor;
    }
}
