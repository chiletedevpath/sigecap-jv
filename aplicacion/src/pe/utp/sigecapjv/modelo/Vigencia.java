package pe.utp.sigecapjv.modelo;

import java.time.LocalDate;

public record Vigencia(int idCurso, String trabajador, String curso, String certificado,
                       LocalDate vencimiento, Integer diasRestantes, String estado) {
}
