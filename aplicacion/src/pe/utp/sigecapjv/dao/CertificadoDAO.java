package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Certificado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class CertificadoDAO {
    public Certificado buscarPorParticipacion(Connection c, int id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM certificado WHERE idParticipacion=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new Certificado(
                        r.getInt("idCertificado"), id, r.getInt("idTrabajador"), r.getString("numero"),
                        r.getObject("fechaEmision", LocalDate.class), r.getObject("fechaVencimiento", LocalDate.class),
                        r.getString("rutaEvidencia"));
            }
        }
    }

    public int insertar(Connection c, Certificado dato) throws SQLException {
        return Jdbc.insertar(
                c,
                "INSERT INTO certificado(idParticipacion,idTrabajador,numero,fechaEmision,fechaVencimiento," +
                        "rutaEvidencia,estadoVigencia) VALUES (?,?,?,?,?,?,NULL)",
                dato.getIdParticipacion(), dato.getIdTrabajador(), dato.getNumero(), dato.getFechaEmision(),
                dato.getFechaVencimiento(), dato.getRutaEvidencia());
    }

    public void actualizar(Connection c, Certificado dato) throws SQLException {
        Jdbc.actualizar(
                c,
                "UPDATE certificado SET numero=?,fechaEmision=?,fechaVencimiento=?,rutaEvidencia=?," +
                        "estadoVigencia=NULL WHERE idCertificado=? AND idParticipacion=?",
                dato.getNumero(), dato.getFechaEmision(), dato.getFechaVencimiento(), dato.getRutaEvidencia(),
                dato.getIdCertificado(), dato.getIdParticipacion());
    }
}
