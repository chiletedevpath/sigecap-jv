package pe.utp.sigecapjv.vista;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import pe.utp.sigecapjv.controlador.*;

public class FrmPrincipal extends JFrame {
    public FrmPrincipal() {
        super("SIGECAP J&V - Primera versión funcional");
        PersonalControlador personal = new PersonalControlador();
        CapacitacionControlador capacitaciones = new CapacitacionControlador(personal);
        PnlPersonal pnlPersonal = new PnlPersonal(personal);
        PnlCapacitaciones pnlCapacitaciones = new PnlCapacitaciones(capacitaciones, personal);
        CardLayout tarjetas = new CardLayout(); JPanel contenido = new JPanel(tarjetas);
        contenido.add(pnlPersonal, "Personal"); contenido.add(pnlCapacitaciones, "Capacitaciones");
        JPanel menu = new JPanel(); menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        Color azul = new Color(28, 48, 71), seleccionado = new Color(46, 102, 158);
        menu.setBackground(azul); menu.setPreferredSize(new Dimension(225, 720));
        menu.setBorder(new EmptyBorder(28, 18, 22, 18));
        JLabel marca = new JLabel("SIGECAP J&V"); marca.setForeground(Color.WHITE);
        marca.setFont(new Font("Segoe UI", Font.BOLD, 22)); marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(marca); menu.add(Box.createVerticalStrut(5));
        JLabel area = new JLabel("Área de Operaciones"); area.setForeground(new Color(182, 198, 214));
        area.setAlignmentX(Component.LEFT_ALIGNMENT); menu.add(area); menu.add(Box.createVerticalStrut(30));
        for (String nombre : new String[]{"Inicio", "Personal", "Capacitaciones", "Seguimiento", "Reportes", "Usuarios"}) {
            JButton boton = new JButton(nombre); boton.setAlignmentX(Component.LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); boton.setHorizontalAlignment(SwingConstants.LEFT);
            boton.setBackground(nombre.equals("Personal") ? seleccionado : azul); boton.setForeground(Color.WHITE);
            boton.setFont(new Font("Segoe UI", Font.PLAIN, 14)); boton.setFocusPainted(false);
            boton.setBorder(new EmptyBorder(0, 12, 0, 10));
            boolean disponible = nombre.equals("Personal") || nombre.equals("Capacitaciones");
            boton.setEnabled(disponible);
            if (!disponible) boton.setToolTipText("Funcionalidad pendiente de implementación");
            boton.addActionListener(e -> {
                pnlPersonal.refrescar(); pnlCapacitaciones.refrescar(); tarjetas.show(contenido, nombre);
                for (Component c : menu.getComponents()) if (c instanceof JButton b)
                    b.setBackground(b == boton ? seleccionado : azul);
            });
            menu.add(boton); menu.add(Box.createVerticalStrut(6));
        }
        menu.add(Box.createVerticalGlue()); JButton salir = new JButton("Salir");
        salir.setForeground(Color.WHITE); salir.setBackground(azul); salir.setFocusPainted(false);
        salir.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        salir.setBorder(new EmptyBorder(10, 12, 10, 12));
        salir.addActionListener(e -> dispose()); salir.setAlignmentX(Component.LEFT_ALIGNMENT); menu.add(salir);
        JPanel principal = new JPanel(new BorderLayout());
        principal.add(menu, BorderLayout.WEST); principal.add(contenido, BorderLayout.CENTER);
        JLabel aviso = new JLabel("  Versión académica en memoria: los datos se pierden al cerrar. Acceso de demostración sin autenticación.");
        aviso.setBorder(new EmptyBorder(8, 8, 8, 8)); principal.add(aviso, BorderLayout.SOUTH);
        setContentPane(principal); setSize(1220, 750); setMinimumSize(new Dimension(1100, 680));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setLocationRelativeTo(null);
    }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new FrmPrincipal().setVisible(true)); }
}
