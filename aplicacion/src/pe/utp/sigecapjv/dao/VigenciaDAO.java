package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Vigencia;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VigenciaDAO {
    public List<Vigencia> consultar(Connection c) throws SQLException {
        String sql = """
                SELECT idCurso, CONCAT(nombres,' ',apellidos) AS trabajador, nombreCurso,
                       numeroCertificado, fechaVencimiento, diasRestantes,
                       CASE WHEN diasRestantes BETWEEN 0 AND 30 THEN 'PROXIMO_A_VENCER'
                            ELSE estadoActual END AS vigencia
                FROM vw_control_vigencias
                ORDER BY fechaVencimiento IS NULL, fechaVencimiento, apellidos, nombres, idCertificado
                """;
        List<Vigencia> filas = new ArrayList<>();
        try (var p=c.prepareStatement(sql); var r=p.executeQuery()) {
            while (r.next()) filas.add(new Vigencia(r.getInt("idCurso"), r.getString("trabajador"),
                    r.getString("nombreCurso"), r.getString("numeroCertificado"),
                    r.getObject("fechaVencimiento",LocalDate.class),
                    r.getObject("diasRestantes",Integer.class), r.getString("vigencia")));
        }
        return List.copyOf(filas);
    }
}
