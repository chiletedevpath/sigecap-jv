package pe.utp.sigecapjv.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Una participación y su seguimiento; los datos ausentes conservan su valor null. */
public record HistorialCapacitacion(int idParticipacion, String curso, LocalDate fecha,
                                    String estado, String asistencia, String resultado,
                                    BigDecimal calificacion, String certificado,
                                    LocalDate vencimiento, String vigencia, LocalTime horaInicio, LocalTime horaFin, String modalidad) { }
