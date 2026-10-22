package pe.utp.sigecapjv.vista;

import java.util.function.Consumer;
import javax.swing.*;
import pe.utp.sigecapjv.controlador.*;

/** Revisa el acceso fuera del EDT; se detiene junto con la ventana. */
public final class VigilanciaSesion implements AutoCloseable {
    private final SesionUsuario sesion;
    private final SesionControlador controlador;
    private final Consumer<String> alCerrar;
    private final Timer reloj;
    private boolean revisando, cerrada;

    public VigilanciaSesion(SesionUsuario sesion, SesionControlador controlador, Consumer<String> alCerrar) {
        this.sesion = sesion;
        this.controlador = controlador;
        this.alCerrar = alCerrar;
        reloj = new Timer(15_000, e -> revisar());
    }

    public void iniciar() { if (!cerrada) { reloj.start(); revisar(); } }

    public void revisar() {
        if (cerrada || revisando) return;
        revisando = true;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return controlador.vigente(sesion); }
            @Override protected void done() {
                revisando = false;
                if (cerrada) return;
                try {
                    if (!get()) expirar("Su acceso cambió o la cuenta fue desactivada. Inicie sesión nuevamente.");
                } catch (Exception ex) {
                    expirar("No se pudo verificar su acceso en MySQL. Inicie sesión cuando se restablezca la conexión.");
                }
            }
        }.execute();
    }

    private void expirar(String mensaje) {
        if (cerrada) return;
        close();
        sesion.close();
        alCerrar.accept(mensaje);
    }

    @Override public void close() { cerrada = true; reloj.stop(); }
}
