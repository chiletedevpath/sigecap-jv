package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.util.Contrasenas;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class PruebaRecuperacionMySql {
    private static void comprobar(boolean valor) { if(!valor) throw new AssertionError("Recuperación de contraseña"); }
    private static void rechazar(Runnable accion) {
        try { accion.run(); } catch(IllegalArgumentException | IllegalStateException | SecurityException esperado) { return; }
        throw new AssertionError("Se aceptó una recuperación inválida");
    }
    public static void verificar() throws Exception {
        String nombre="recuperacion_prueba";
        char[] anterior="Clave-anterior-2026".toCharArray(), nueva="Clave-renovada-2026".toCharArray();
        int id=Jdbc.transaccion(c -> Jdbc.insertar(c,"INSERT INTO usuario(idRol,nombreCompleto,nombreUsuario,correo,contrasenaHash,estado) SELECT idRol,'Prueba recuperación',?,'recuperacion@example.test',?,'ACTIVO' FROM rol WHERE nombre='Administrador'",nombre,Contrasenas.crearHash(anterior)));
        AtomicReference<String> codigo=new AtomicReference<>(); AtomicInteger envios=new AtomicInteger();
        RecuperacionControlador recuperacion=new RecuperacionControlador((correo,valor)-> {
            comprobar(correo.equals("recuperacion@example.test")); codigo.set(valor); envios.incrementAndGet();
        });
        recuperacion.solicitar("cuenta_ausente"); comprobar(envios.get()==0);
        recuperacion.solicitar(nombre); comprobar(envios.get()==1 && codigo.get().matches("[0-9]{12}"));
        recuperacion.solicitar(nombre); comprobar(envios.get()==1);
        try(var sesion=new LoginControlador().autenticar(nombre,anterior)) {
            rechazar(()->recuperacion.cambiar(nombre,"1",nueva));
            recuperacion.cambiar(nombre,codigo.get(),nueva);
            rechazar(()->AccesoControlador.exigir(sesion,"usuarios","consultar"));
        }
        rechazar(()->recuperacion.cambiar(nombre,codigo.get(),nueva));
        rechazar(()->new LoginControlador().autenticar(nombre,anterior));
        try(var sesion=new LoginControlador().autenticar(nombre,nueva)) { comprobar(sesion.getIdUsuario()==id); }
        liberar(id); recuperacion.solicitar(nombre);
        String incorrecto=codigo.get().equals("000000000000") ? "111111111111" : "000000000000";
        for(int i=0;i<5;i++) rechazar(()->recuperacion.cambiar(nombre,incorrecto,anterior));
        rechazar(()->recuperacion.cambiar(nombre,codigo.get(),anterior));
        liberar(id); recuperacion.solicitar(nombre);
        Jdbc.transaccion(c->{ Jdbc.actualizar(c,"UPDATE recuperacion_contrasena SET vence=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE idUsuario=?",id); return null; });
        rechazar(()->recuperacion.cambiar(nombre,codigo.get(),anterior));
        liberar(id); recuperacion.solicitar(nombre);
        Jdbc.transaccion(c->{ Jdbc.actualizar(c,"UPDATE usuario SET correo='cambiado@example.test' WHERE idUsuario=?",id); return null; });
        rechazar(()->recuperacion.cambiar(nombre,codigo.get(),anterior));
        Jdbc.transaccion(c->{ Jdbc.actualizar(c,"UPDATE usuario SET correo='recuperacion@example.test' WHERE idUsuario=?",id); return null; });
        liberar(id); recuperacion.solicitar(nombre);
        var pool=Executors.newFixedThreadPool(2);
        try {
            CountDownLatch inicio=new CountDownLatch(1);
            Callable<Boolean> accion=()-> { inicio.await(); try { recuperacion.cambiar(nombre,codigo.get(),anterior); return true; } catch(IllegalArgumentException ex) { return false; } };
            var uno=pool.submit(accion); var dos=pool.submit(accion); inicio.countDown();
            comprobar(uno.get(30,TimeUnit.SECONDS) ^ dos.get(30,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        liberar(id);
        rechazar(()->new RecuperacionControlador((c,v)->{throw new IllegalStateException("Fallo simulado de envío");}).solicitar(nombre));
        int antes=envios.get(); recuperacion.solicitar(nombre); comprobar(envios.get()==antes+1);
        Jdbc.transaccion(c->{ Jdbc.actualizar(c,"UPDATE recuperacion_contrasena SET ultimoEnvio=DATE_SUB(NOW(),INTERVAL 2 MINUTE),envios=5 WHERE idUsuario=?",id); return null; });
        recuperacion.solicitar(nombre); comprobar(envios.get()==antes+1);
        Jdbc.transaccion(c->{ Jdbc.actualizar(c,"UPDATE usuario SET estado='INACTIVO' WHERE idUsuario=?",id); return null; });
        liberar(id); recuperacion.solicitar(nombre); comprobar(envios.get()==antes+1);
        rechazar(()->recuperacion.cambiar(nombre,codigo.get(),nueva));
        System.out.println("OK: recuperación MySQL, correo registrado, caducidad, reintentos, límite de envíos, uso único concurrente y revocación de sesión.");
    }
    private static void liberar(int id) {
        Jdbc.transaccion(c->{ Jdbc.actualizar(c,"UPDATE recuperacion_contrasena SET ultimoEnvio=DATE_SUB(NOW(),INTERVAL 2 MINUTE),inicioVentana=DATE_SUB(NOW(),INTERVAL 2 HOUR) WHERE idUsuario=?",id); return null; });
    }
}
