package pe.utp.sigecapjv;

import java.sql.Connection;
import java.util.Arrays;
import pe.utp.sigecapjv.conexion.ConexionBD;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.*;

public final class PruebaInicioMySql {
    private static int total;
    private static void comprobar(boolean valor) { if(!valor) throw new AssertionError("Inicio MySQL: " + (total+1)); total++; }
    public static void main(String[] args) throws Exception {
        if(!"true".equals(System.getenv("SIGECAP_DB_TEST"))) throw new IllegalStateException("Solo base de prueba.");
        char[] clave="Clave-usuarios-prueba-2026".toCharArray();
        try {
            if(UsuarioControlador.requiereAdministradorInicial()) {
                UsuarioControlador.crearAdministradorInicial("Admin prueba","admin_prueba","admin@example.test",clave);
                try(var admin=new LoginControlador().autenticar("admin_prueba",clave)) {
                    var gestion=new UsuarioControlador(admin);
                    int trabajador=Jdbc.transaccion(c->Jdbc.insertar(c,"INSERT INTO trabajador(dni,nombres,apellidos,cargo) VALUES ('99111111','Cuenta','Trabajador','Prueba')"));
                    for(var rol:gestion.roles()) {
                        String usuario=switch(rol.nombre()) {
                            case "Responsable de capacitaciones" -> "responsable_prueba";
                            case "Supervisor de Operaciones" -> "supervisor_prueba";
                            case "Trabajador" -> "trabajador_prueba";
                            default -> null;
                        };
                        if(usuario!=null) {
                            char[] pass=usuario.equals("supervisor_prueba")?"Clave-renovada-prueba-2026".toCharArray():clave;
                            gestion.guardar(new pe.utp.sigecapjv.modelo.Usuario(0,rol.idRol(),usuario.equals("trabajador_prueba")?trabajador:null,"Prueba",usuario,usuario+"@example.test","ACTIVO"),pass);
                            if(pass!=clave) Arrays.fill(pass,'\0');
                        }
                    }
                }
            }
        } finally {Arrays.fill(clave,'\0');}
        verificar();
        verificarVista();
    }
    private static void verificarVista() throws Exception {
        char[] clave="Clave-usuarios-prueba-2026".toCharArray();
        try(var sesion=new LoginControlador().autenticar("admin_prueba",clave)) {
            pe.utp.sigecapjv.vista.PnlInicio[] panel=new pe.utp.sigecapjv.vista.PnlInicio[1];
            javax.swing.SwingUtilities.invokeAndWait(()-> {
                panel[0]=new pe.utp.sigecapjv.vista.PnlInicio();
                panel[0].cargarResumen(new InicioControlador(sesion));
            });
            java.util.concurrent.atomic.AtomicBoolean listo=new java.util.concurrent.atomic.AtomicBoolean();
            for(int i=0;i<100 && !listo.get();i++) {
                javax.swing.SwingUtilities.invokeAndWait(()->listo.set(tieneTexto(panel[0],"Datos de MySQL")));
                if(!listo.get()) Thread.sleep(50);
            }
            comprobar(listo.get());
            javax.swing.SwingUtilities.invokeAndWait(()-> {
                panel[0].setSize(995,680); distribuir(panel[0]);
                var imagen=new java.awt.image.BufferedImage(995,680,java.awt.image.BufferedImage.TYPE_INT_RGB);
                var g=imagen.createGraphics();panel[0].printAll(g);g.dispose();
                try {javax.imageio.ImageIO.write(imagen,"png",new java.io.File("aplicacion/out/inicio-mysql.png"));}
                catch(java.io.IOException ex){throw new IllegalStateException(ex);}
            });
            System.out.println("OK: inicio Swing cargado desde MySQL real.");
        } finally {Arrays.fill(clave,'\0');}
    }
    private static boolean tieneTexto(java.awt.Container c,String texto) {
        for(var hijo:c.getComponents()) {
            if(hijo instanceof javax.swing.JLabel l && l.getText()!=null && l.getText().startsWith(texto))return true;
            if(hijo instanceof java.awt.Container sub && tieneTexto(sub,texto))return true;
        }
        return false;
    }
    private static void distribuir(java.awt.Container c) {
        c.doLayout();for(var hijo:c.getComponents())if(hijo instanceof java.awt.Container sub)distribuir(sub);
    }
    public static void verificar() throws Exception {
        if(!"true".equals(System.getenv("SIGECAP_DB_TEST"))) throw new IllegalStateException("Solo base de prueba.");
        InicioDAO dao=new InicioDAO();
        try(Connection c=ConexionBD.obtenerConexion()) {
            c.setAutoCommit(false);
            try {
                int uno=Jdbc.insertar(c,"INSERT INTO trabajador(dni,nombres,apellidos,cargo) VALUES ('99000001','Inicio','Uno','Prueba')");
                int dos=Jdbc.insertar(c,"INSERT INTO trabajador(dni,nombres,apellidos,cargo) VALUES ('99000002','Inicio','Dos','Prueba')");
                int curso=Jdbc.insertar(c,"INSERT INTO curso(codigo,nombre,tipo) VALUES ('INICIO-REAL','Curso inicio real','Interno')");
                int cap=Jdbc.insertar(c,"INSERT INTO capacitacion(idCurso,fecha,horaInicio,horaFin,modalidad) VALUES (?,CURDATE()+INTERVAL 1 DAY,'09:00','10:00','Presencial')",curso);
                int p1=Jdbc.insertar(c,"INSERT INTO participacion(idTrabajador,idCapacitacion,fechaAsignacion) VALUES (?,?,CURDATE())",uno,cap);
                int p2=Jdbc.insertar(c,"INSERT INTO participacion(idTrabajador,idCapacitacion,fechaAsignacion) VALUES (?,?,CURDATE())",dos,cap);
                comprobar(dao.obtenerResumen(c,uno).programadas()==1 && dao.obtenerResumen(c,uno).pendientes()==1);
                comprobar(dao.obtenerResumen(c,-1).programadas()==0 && dao.listarProgramadas(c,-1).isEmpty());
                comprobar(dao.listarProgramadas(c,uno).size()==1 && dao.listarProgramadas(c,uno).get(0).participantes()==1);
                Jdbc.insertar(c,"INSERT INTO certificado(idParticipacion,idTrabajador,numero,fechaEmision,fechaVencimiento) VALUES (?,?,'INI-UNO',CURDATE()-INTERVAL 10 DAY,CURDATE()+INTERVAL 30 DAY)",p1,uno);
                Jdbc.insertar(c,"INSERT INTO certificado(idParticipacion,idTrabajador,numero,fechaEmision,fechaVencimiento) VALUES (?,?,'INI-DOS',CURDATE()-INTERVAL 10 DAY,CURDATE()-INTERVAL 1 DAY)",p2,dos);
                comprobar(dao.obtenerResumen(c,uno).proximasVencer()==1 && dao.obtenerResumen(c,uno).vencidas()==0);
                comprobar(dao.obtenerResumen(c,dos).proximasVencer()==0 && dao.obtenerResumen(c,dos).vencidas()==1);
                comprobar(dao.listarProximosVencimientos(c,uno).size()==1 && dao.listarProximosVencimientos(c,dos).isEmpty());
                Jdbc.actualizar(c,"UPDATE certificado SET fechaVencimiento=CURDATE()+INTERVAL 31 DAY WHERE idParticipacion=?",p1);
                comprobar(dao.obtenerResumen(c,uno).proximasVencer()==0);
                Jdbc.actualizar(c,"UPDATE certificado SET fechaVencimiento=CURDATE() WHERE idParticipacion=?",p1);
                comprobar(dao.obtenerResumen(c,uno).proximasVencer()==1);
                Jdbc.actualizar(c,"UPDATE participacion SET estado='CANCELADA' WHERE idParticipacion=?",p1);
                comprobar(dao.obtenerResumen(c,uno).pendientes()==0 && dao.listarProgramadas(c,uno).isEmpty());
            } finally { c.rollback(); }
        }
        char[] clave="Clave-usuarios-prueba-2026".toCharArray();
        try {
            for(String usuario:new String[]{"admin_prueba","responsable_prueba","trabajador_prueba"}) {
                try(var sesion=new LoginControlador().autenticar(usuario,clave)) {
                    var control=new InicioControlador(sesion);
                    var panel=control.obtenerPanel();
                    Integer alcance="Trabajador".equals(sesion.getRol())?sesion.getIdTrabajador():null;
                    comprobar(panel.resumen().equals(Jdbc.transaccion(c->dao.obtenerResumen(c,alcance))));
                    sesion.close();
                    try {control.obtenerPanel();throw new AssertionError("Sesion cerrada aceptada");}catch(SecurityException esperado){total++;}
                }
            }
        } finally {Arrays.fill(clave,'\0');}
        char[] supervisor="Clave-renovada-prueba-2026".toCharArray();
        try(var sesion=new LoginControlador().autenticar("supervisor_prueba",supervisor)) {
            comprobar(new InicioControlador(sesion).obtenerPanel()!=null);
        } finally {Arrays.fill(supervisor,'\0');}
        System.out.println("OK: "+total+" comprobaciones de inicio MySQL, aislamiento y fechas.");
    }
}
