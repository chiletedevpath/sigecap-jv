package pe.utp.sigecapjv.vista;

import javax.swing.*;
import java.awt.*;

final class CampoContrasena {
    private CampoContrasena() { }

    static JPanel conVisibilidad(JPasswordField campo) {
        char oculto = campo.getEchoChar();
        JToggleButton mostrar = new JToggleButton();
        mostrar.setIcon(new Icon() {
            public int getIconWidth() { return 22; }
            public int getIconHeight() { return 20; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D dibujo = (Graphics2D) g.create();
                dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                dibujo.setColor(c.isEnabled() ? new Color(46,102,158) : Color.GRAY);
                dibujo.setStroke(new BasicStroke(1.6f));
                dibujo.drawOval(x+2,y+5,18,10);
                dibujo.fillOval(x+8,y+7,6,6);
                if (mostrar.isSelected()) dibujo.drawLine(x+2,y+2,x+20,y+18);
                dibujo.dispose();
            }
        });
        mostrar.setToolTipText("Mostrar contraseña");
        mostrar.getAccessibleContext().setAccessibleName("Mostrar contraseña");
        mostrar.addActionListener(e -> {
            campo.setEchoChar(mostrar.isSelected() ? (char) 0 : oculto);
            String accion = mostrar.isSelected() ? "Ocultar contraseña" : "Mostrar contraseña";
            mostrar.setToolTipText(accion);
            mostrar.getAccessibleContext().setAccessibleName(accion);
        });
        JPanel panel = new JPanel(new BorderLayout(4,0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, campo.getMaximumSize().height));
        panel.add(campo,BorderLayout.CENTER);
        panel.add(mostrar,BorderLayout.EAST);
        return panel;
    }
}
