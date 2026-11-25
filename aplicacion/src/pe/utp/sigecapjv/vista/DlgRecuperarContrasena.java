package pe.utp.sigecapjv.vista;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import pe.utp.sigecapjv.controlador.RecuperacionControlador;

final class DlgRecuperarContrasena extends JDialog {
    private final JTextField usuario = new JTextField(24), codigo = new JTextField(24);
    private final JPasswordField clave = new JPasswordField(24), confirmar = new JPasswordField(24);
    private final JButton enviar = new JButton("Enviar código"), guardar = new JButton("Cambiar contraseña");
    private final JLabel estado = new JLabel(" ");
    private final RecuperacionControlador controlador = new RecuperacionControlador();

    DlgRecuperarContrasena(JFrame padre, String nombre) {
        super(padre,"Recuperar contraseña",true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        usuario.setText(nombre);
        JPanel contenido = new JPanel(new GridLayout(0,1,0,8));
        contenido.setBorder(BorderFactory.createEmptyBorder(20,24,20,24));
        contenido.add(new JLabel("Recibirá un código en el correo registrado en su cuenta."));
        contenido.add(new JLabel("Nombre de usuario")); contenido.add(usuario); contenido.add(enviar);
        contenido.add(new JLabel("Código de 12 dígitos")); contenido.add(codigo);
        contenido.add(new JLabel("Nueva contraseña (12 a 128 caracteres)")); contenido.add(CampoContrasena.conVisibilidad(clave));
        contenido.add(new JLabel("Repita la contraseña")); contenido.add(CampoContrasena.conVisibilidad(confirmar));
        contenido.add(guardar); contenido.add(estado);
        JButton cerrar=new JButton("Volver al inicio de sesión"); cerrar.addActionListener(e -> dispose()); contenido.add(cerrar);
        setContentPane(contenido); pack(); setResizable(false); setLocationRelativeTo(padre);
        enviar.addActionListener(e -> solicitar()); guardar.addActionListener(e -> cambiar());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { clave.setText(""); confirmar.setText(""); codigo.setText(""); }
        });
    }

    private void ocupado(boolean valor) {
        enviar.setEnabled(!valor); guardar.setEnabled(!valor); usuario.setEnabled(!valor);
    }

    private void solicitar() {
        String nombre=usuario.getText(); ocupado(true); estado.setText("Enviando…");
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() { controlador.solicitar(nombre); return null; }
            @Override protected void done() {
                ocupado(false);
                try { get(); estado.setText("Si la cuenta está disponible, recibirá un código."); }
                catch(Exception ex) { error(ex); }
            }
        }.execute();
    }

    private void cambiar() {
        char[] nueva=clave.getPassword(), repetida=confirmar.getPassword();
        boolean iguales=Arrays.equals(nueva,repetida); Arrays.fill(repetida,'\0');
        if(!iguales) { Arrays.fill(nueva,'\0'); estado.setText("Las contraseñas no coinciden."); return; }
        String nombre=usuario.getText(), valor=codigo.getText().strip();
        ocupado(true); estado.setText("Guardando…");
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() {
                try { controlador.cambiar(nombre,valor,nueva); return null; }
                finally { Arrays.fill(nueva,'\0'); }
            }
            @Override protected void done() {
                ocupado(false); clave.setText(""); confirmar.setText("");
                try {
                    get();
                    if(isDisplayable()) JOptionPane.showMessageDialog(DlgRecuperarContrasena.this,"Contraseña actualizada. Ingrese con su nueva contraseña.");
                    dispose();
                } catch(Exception ex) { error(ex); }
            }
        }.execute();
    }

    private void error(Exception ex) {
        estado.setText("No se completó la operación.");
        Throwable causa=ex instanceof java.util.concurrent.ExecutionException ? ex.getCause() : ex;
        if(isDisplayable()) JOptionPane.showMessageDialog(this,
                causa instanceof IllegalArgumentException || causa instanceof IllegalStateException
                        ? causa.getMessage() : "Intente nuevamente más tarde.","Recuperar contraseña",JOptionPane.WARNING_MESSAGE);
    }
}
