package pe.utp.sigecapjv;

import java.math.BigDecimal;
import java.time.*;
import pe.utp.sigecapjv.modelo.*;

public final class PruebaResultados {
    private static int total;
    private static void comprobar(boolean valor) {
        if (!valor) throw new AssertionError("Regla de resultado/certificado incumplida.");
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch (IllegalArgumentException | IllegalStateException ex) { total++; return; }
        throw new AssertionError("Se aceptaron datos inválidos.");
    }
    public static void verificar() {
        for (String valor : new String[]{"0", "20", "10.25"}) {
            Resultado r = new Resultado(0,1," aprobado ",new BigDecimal(valor),null);
            r.registrar(); comprobar(r.esFavorable() && r.getFechaRegistro() != null);
            rechazar(r::registrar);
        }
        for (String valor : new String[]{"-0.01", "20.01", "1.001"}) {
            rechazar(() -> new Resultado(0,1,"APROBADO",new BigDecimal(valor),null).registrar());
        }
        Resultado apto = new Resultado(0,1,"APTO",null,null); apto.registrar();
        comprobar(apto.esFavorable());
        comprobar(apto.validarAsistencia(new Asistencia(1,1,"ASISTIO",LocalDateTime.now())));
        comprobar(!apto.validarAsistencia(new Asistencia(1,2,"ASISTIO",LocalDateTime.now())));
        comprobar(!apto.validarAsistencia(new Asistencia(1,1,"JUSTIFICADA",LocalDateTime.now())));
        rechazar(() -> new Resultado(0,1,"INVALIDO",null,null).registrar());
        rechazar(() -> new Resultado(0,1,"APTO",null,null).actualizar());
        LocalDate hoy = LocalDate.now();
        Certificado c = new Certificado(0,1,1," CERT-1 ",hoy,hoy," "); c.registrar();
        comprobar(c.getNumero().equals("CERT-1") && c.getRutaEvidencia() == null);
        comprobar("VIGENTE".equals(c.calcularEstadoVigencia(hoy)));
        comprobar("VENCIDO".equals(c.calcularEstadoVigencia(hoy.plusDays(1))));
        comprobar("NO_APLICA".equals(new Certificado(0,1,1,"C",hoy,null,null).calcularEstadoVigencia(hoy)));
        rechazar(() -> new Certificado(0,1,1,"",hoy,null,null).registrar());
        rechazar(() -> new Certificado(0,1,1,"C",hoy.plusDays(1),null,null).registrar());
        rechazar(() -> new Certificado(0,1,1,"C",hoy,hoy.minusDays(1),null).registrar());
        rechazar(() -> new Certificado(0,1,1,"C",null,null,null).registrar());
        rechazar(() -> new Certificado(0,1,1,"C",hoy,null,"a".repeat(501)).registrar());
        System.out.println("OK: " + total + " comprobaciones del modelo Resultado/Certificado.");
    }
}
