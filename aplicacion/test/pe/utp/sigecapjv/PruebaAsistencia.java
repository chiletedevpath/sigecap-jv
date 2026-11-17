package pe.utp.sigecapjv;

import pe.utp.sigecapjv.modelo.Asistencia;

/** Reglas del modelo sin depender de MySQL. */
public final class PruebaAsistencia {
    private static int comprobaciones;

    private static void comprobar(boolean valor) {
        if (!valor) throw new AssertionError("Regla de asistencia incumplida.");
        comprobaciones++;
    }

    private static void rechazar(Runnable accion) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException esperada) { comprobaciones++; return; }
        throw new AssertionError("La asistencia inválida fue aceptada.");
    }

    public static void verificar() {
        for (String condicion : new String[]{"ASISTIO", "NO_ASISTIO", "JUSTIFICADA"}) {
            Asistencia asistencia = new Asistencia(0, 1, " " + condicion.toLowerCase() + " ", null);
            asistencia.registrar();
            comprobar(condicion.equals(asistencia.getCondicion()) && asistencia.getFechaRegistro() != null);
            rechazar(asistencia::registrar);
            asistencia.setIdAsistencia(1);
            var fecha = asistencia.getFechaRegistro();
            asistencia.setCondicion("JUSTIFICADA");
            asistencia.actualizar();
            comprobar(fecha.equals(asistencia.getFechaRegistro()));
        }
        for (String condicion : new String[]{null, "", "   ", "INVALIDA"}) {
            Asistencia asistencia = new Asistencia(0, 1, condicion, null);
            rechazar(asistencia::registrar);
            comprobar(asistencia.getFechaRegistro() == null);
        }
        rechazar(() -> new Asistencia(0, 0, "ASISTIO", null).registrar());
        rechazar(() -> new Asistencia(0, 1, "ASISTIO", null).actualizar());
        System.out.println("OK: " + comprobaciones + " comprobaciones del modelo Asistencia.");
    }
}
