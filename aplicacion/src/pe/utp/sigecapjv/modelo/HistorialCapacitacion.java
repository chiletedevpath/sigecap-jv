package pe.utp.sigecapjv.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistorialCapacitacion(
        String curso,
        LocalDate fecha,
        String estadoCapacitacion,
        String estadoParticipacion,
        String asistencia,
        String resultado,
        BigDecimal calificacion,
        String certificado,
        String vigencia) {
}
