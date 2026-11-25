package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.util.Contrasenas;
import pe.utp.sigecapjv.util.CorreoRecuperacion;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

public final class RecuperacionControlador {
    @FunctionalInterface public interface Envio { void enviar(String correo, String codigo); }
    private final Envio envio;
    private static final SecureRandom AZAR = new SecureRandom();
    public RecuperacionControlador() { this(CorreoRecuperacion::enviar); }
    public RecuperacionControlador(Envio envio) { this.envio = Objects.requireNonNull(envio); }

    private static String usuario(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 60)
            throw new IllegalArgumentException("Ingrese su nombre de usuario.");
        return nombre.strip();
    }

    public void solicitar(String nombre) {
        String usuario = usuario(nombre);
        Jdbc.transaccion(c -> {
            int id; String correo; String anterior;
            try (var p = c.prepareStatement("SELECT idUsuario,correo,contrasenaHash FROM usuario WHERE nombreUsuario=? AND estado='ACTIVO' FOR UPDATE")) {
                p.setString(1,usuario);
                try (var r=p.executeQuery()) { if (!r.next()) return null; id=r.getInt(1); correo=r.getString(2); anterior=r.getString(3); }
            }
            boolean reiniciar = true;
            try (var p=c.prepareStatement("SELECT ultimoEnvio>DATE_SUB(NOW(),INTERVAL 60 SECOND),inicioVentana<=DATE_SUB(NOW(),INTERVAL 1 HOUR),envios FROM recuperacion_contrasena WHERE idUsuario=? FOR UPDATE")) {
                p.setInt(1,id);
                try(var r=p.executeQuery()) {
                    if(r.next()) {
                        reiniciar=r.getBoolean(2);
                        if(r.getBoolean(1) || (!reiniciar && r.getInt(3)>=5)) return null;
                    }
                }
            }
            StringBuilder codigo=new StringBuilder();
            for(int i=0;i<12;i++) codigo.append(AZAR.nextInt(10));
            char[] caracteres=codigo.toString().toCharArray();
            String hash;
            try { hash=Contrasenas.crearHash(caracteres); } finally { Arrays.fill(caracteres,'\0'); }
            try(var p=c.prepareStatement("INSERT INTO recuperacion_contrasena(idUsuario,codigoHash,claveAnterior,correo,vence,ultimoEnvio,inicioVentana) VALUES(?,?,?,?,DATE_ADD(NOW(),INTERVAL 10 MINUTE),NOW(),NOW()) ON DUPLICATE KEY UPDATE codigoHash=?,claveAnterior=?,correo=?,vence=DATE_ADD(NOW(),INTERVAL 10 MINUTE),ultimoEnvio=NOW(),inicioVentana=IF(?,NOW(),inicioVentana),envios=IF(?,1,envios+1),intentos=0,usado=FALSE")) {
                p.setInt(1,id); p.setString(2,hash); p.setString(3,anterior); p.setString(4,correo);
                p.setString(5,hash); p.setString(6,anterior); p.setString(7,correo); p.setBoolean(8,reiniciar); p.setBoolean(9,reiniciar); p.executeUpdate();
            }
            envio.enviar(correo,codigo.toString());
            return null;
        });
    }

    public void cambiar(String nombre, String codigo, char[] clave) {
        String usuario=usuario(nombre);
        if(codigo==null || !codigo.matches("[0-9]{12}")) throw new IllegalArgumentException("Ingrese el código de 12 dígitos que recibió.");
        String nueva=Contrasenas.crearHash(clave);
        boolean cambiado=Jdbc.transaccion(c -> {
            int id; String actual; String correo;
            try(var p=c.prepareStatement("SELECT idUsuario,contrasenaHash,correo FROM usuario WHERE nombreUsuario=? AND estado='ACTIVO' FOR UPDATE")) {
                p.setString(1,usuario);
                try(var r=p.executeQuery()) { if(!r.next()) return false; id=r.getInt(1); actual=r.getString(2); correo=r.getString(3); }
            }
            String hash;
            try(var p=c.prepareStatement("SELECT codigoHash,claveAnterior,correo FROM recuperacion_contrasena WHERE idUsuario=? AND usado=FALSE AND intentos<5 AND vence>NOW() FOR UPDATE")) {
                p.setInt(1,id);
                try(var r=p.executeQuery()) {
                    if(!r.next() || !actual.equals(r.getString(2)) || !correo.equals(r.getString(3))) return false;
                    hash=r.getString(1);
                }
            }
            char[] caracteres=codigo.toCharArray();
            boolean coincide;
            try { coincide=Contrasenas.verificar(caracteres,hash); } finally { Arrays.fill(caracteres,'\0'); }
            // El intento fallido se confirma para que cerrar la ventana no reinicie el límite.
            Jdbc.actualizar(c,"UPDATE recuperacion_contrasena SET intentos=intentos+1,usado=? WHERE idUsuario=?",coincide,id);
            if(!coincide) return false;
            Jdbc.actualizar(c,"UPDATE usuario SET contrasenaHash=? WHERE idUsuario=?",nueva,id);
            return true;
        });
        if(!cambiado) throw new IllegalArgumentException("El código no es válido o venció. Solicite uno nuevo.");
    }
}
