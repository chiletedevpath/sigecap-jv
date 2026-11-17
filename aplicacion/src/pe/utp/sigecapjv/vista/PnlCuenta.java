package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.CuentaControlador;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

final class PnlCuenta extends JPanel {
    PnlCuenta(CuentaControlador controlador, Runnable alGuardar) {
        super(new BorderLayout());
        setBackground(new Color(245,247,249));
        setBorder(new EmptyBorder(24,28,26,28));
        JPanel formulario = new JPanel(new GridLayout(0,1,0,8));
        formulario.setOpaque(false);
        JLabel titulo = new JLabel("Cambiar contraseña");
        titulo.setFont(new Font("Segoe UI",Font.BOLD,26));
        JPasswordField actual = new JPasswordField(24);
        JPasswordField nueva = new JPasswordField(24);
        JPasswordField confirmar = new JPasswordField(24);
        JLabel mensaje = new JLabel("Al guardar deberá ingresar con su nueva contraseña.");
        JButton guardar = new JButton("Guardar contraseña");
        formulario.add(titulo);
        formulario.add(new JLabel("Contraseña actual"));
        formulario.add(CampoContrasena.conVisibilidad(actual));
        formulario.add(new JLabel("Nueva contraseña (12 a 128 caracteres)"));
        formulario.add(CampoContrasena.conVisibilidad(nueva));
        formulario.add(new JLabel("Confirmar nueva contraseña"));
        formulario.add(CampoContrasena.conVisibilidad(confirmar));
        formulario.add(guardar);
        formulario.add(mensaje);
        JPanel alineacion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        alineacion.setOpaque(false);
        alineacion.add(formulario);
        add(alineacion,BorderLayout.NORTH);
        guardar.addActionListener(e -> {
            char[] anterior = actual.getPassword(), siguiente = nueva.getPassword(), confirmacion = confirmar.getPassword();
            actual.setText(""); nueva.setText(""); confirmar.setText("");
            guardar.setEnabled(false);
            new SwingWorker<Void,Void>() {
                protected Void doInBackground() {
                    try { controlador.cambiarClave(anterior,siguiente,confirmacion); }
                    finally {
                        Arrays.fill(anterior,'\0'); Arrays.fill(siguiente,'\0'); Arrays.fill(confirmacion,'\0');
                    }
                    return null;
                }
                protected void done() {
                    guardar.setEnabled(true);
                    try { get(); alGuardar.run(); }
                    catch (Exception ex) {
                        Throwable causa = ex.getCause() == null ? ex : ex.getCause();
                        mensaje.setText(causa.getMessage());
                    }
                }
            }.execute();
        });
    }
}
