package pe.utp.sigecapjv;

import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.modelo.*;

public final class PruebaResultadosMySql {
    private static int total;
    static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch (IllegalArgumentException | IllegalStateException ex) { total++; return; }
        throw new AssertionError("La operación inválida fue aceptada.");
    }
    public static void verificar(PersonalJdbcControlador personal, CapacitacionJdbcControlador gestion, SesionUsuario admin) throws Exception {
        SeguimientoControlador s = new SeguimientoControlador(admin);
        Trabajador t = new Trabajador(0,"22223333","Evaluacion","Prueba","Operaciones","ACTIVO");
        personal.registrarTrabajador(t);
        Curso curso = new Curso(0,"EVALUACION","Evaluación","Interno",12,"ACTIVO"); gestion.registrarCurso(curso);
        Capacitacion cap = new Capacitacion(0,LocalDate.now().plusDays(1),LocalTime.of(8,0),LocalTime.of(9,0),"Presencial",null,curso);
        gestion.programarCapacitacion(cap);
        Participacion p = gestion.asignarTrabajador(t,cap); int id = p.getIdParticipacion();
        rechazar(() -> s.registrarResultado(id,"APTO",null));
        Jdbc.transaccion(c -> { Jdbc.actualizar(c,"UPDATE capacitacion SET fecha=? WHERE idCapacitacion=?",LocalDate.now(),cap.getIdCapacitacion()); return null; });
        rechazar(() -> s.registrarResultado(id,"APTO",null));
        s.registrarAsistencia(id,"NO_ASISTIO");
        rechazar(() -> s.registrarResultado(id,"APTO",null));
        s.actualizarAsistencia(id,"ASISTIO");
        rechazar(() -> s.actualizarResultado(id,"APTO",null));
        rechazar(() -> s.registrarResultado(id,"APROBADO",new BigDecimal("20.01")));
        comprobar(s.consultarResultado(id) == null,"Nota inválida no persistida");
        Resultado r = s.registrarResultado(id,"DESAPROBADO",new BigDecimal("8.50"));
        comprobar(r.getIdResultado() > 0,"ID resultado");
        comprobar("COMPLETADA".equals(gestion.listarParticipaciones().stream().filter(x -> x.getIdParticipacion()==id).findFirst().orElseThrow().getEstado()),"Estado completado transaccional");
        rechazar(() -> s.registrarResultado(id,"APTO",null));
        rechazar(() -> s.registrarCertificado(id,"CERT-EVAL",LocalDate.now(),null,null));
        rechazar(() -> s.actualizarAsistencia(id,"JUSTIFICADA"));
        comprobar("ASISTIO".equals(s.consultarAsistencia(id).getCondicion()),"Asistencia preservada");
        Resultado editado = s.actualizarResultado(id,"APROBADO",new BigDecimal("18.75"));
        comprobar(editado.getIdResultado()==r.getIdResultado() && editado.getFechaRegistro().equals(r.getFechaRegistro()),"Identidad resultado preservada");
        rechazar(() -> s.actualizarCertificado(id,"CERT-EVAL",LocalDate.now(),null,null));
        rechazar(() -> s.registrarCertificado(id,"CERT-EVAL",LocalDate.now().minusDays(1),null,null));
        rechazar(() -> s.registrarCertificado(id,"CERT-EVAL",LocalDate.now(),LocalDate.now().minusDays(1),null));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Boolean> registrar = () -> {
            inicio.await();
            try { s.registrarCertificado(id,"CERT-EVAL",LocalDate.now(),LocalDate.now().plusYears(1),"evidencia/prueba.pdf"); return true; }
            catch (IllegalStateException ex) { return false; }
        };
        try {
            Future<Boolean> a=pool.submit(registrar), b=pool.submit(registrar); inicio.countDown();
            comprobar((a.get(20,TimeUnit.SECONDS)?1:0)+(b.get(20,TimeUnit.SECONDS)?1:0)==1,"Certificado concurrente único");
        } finally { pool.shutdownNow(); }
        Certificado cert = s.consultarCertificado(id);
        comprobar(cert.getIdTrabajador()==t.getIdTrabajador() && "VIGENTE".equals(cert.getEstadoVigencia()),"Trabajador y vigencia correctos");
        rechazar(() -> s.actualizarResultado(id,"NO_APTO",null));
        comprobar("APROBADO".equals(s.consultarResultado(id).getCondicion()),"Resultado favorable conservado");
        Certificado editCert = s.actualizarCertificado(id,"CERT-EVAL",LocalDate.now(),null,"");
        comprobar(editCert.getIdCertificado()==cert.getIdCertificado() && "NO_APLICA".equals(editCert.getEstadoVigencia()),"Edición certificado sin vigencia");
        rechazar(() -> gestion.cancelarCapacitacion(cap.getIdCapacitacion()));
        rechazar(() -> gestion.actualizarCapacitacion(cap));
        gestion.cerrarCapacitacion(cap.getIdCapacitacion());
        s.actualizarCertificado(id,"CERT-EVAL",LocalDate.now(),LocalDate.now().plusYears(1),null);
        comprobar(s.consultarCertificado(id).getFechaVencimiento().equals(LocalDate.now().plusYears(1)),"Certificado corregible tras cierre");
        PruebaPantallaEvaluacion.verificarMySql(gestion,s,id);
        System.out.println("OK: " + total + " comprobaciones MySQL de resultados/certificados.");
    }
}
