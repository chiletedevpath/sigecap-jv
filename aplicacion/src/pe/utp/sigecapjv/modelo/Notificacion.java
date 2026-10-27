package pe.utp.sigecapjv.modelo;

/**
 * Clase base creada como parte de la estructura inicial del proyecto SIGECAP J&V.
 * Su implementación se completará progresivamente según los requerimientos del sistema.
 */
public class Notificacion {
        private int idNotificacion;
        private int idTrabajador;
        private String tipo;
        private String mensaje;
        private String fechaGeneracion;
        private String estado;
        private String claveEvento;

        public Notificacion() {}

        public int getIdNotificacion() {
            return idNotificacion;
        }

        public void setIdNotificacion(int idNotificacion) {
            this.idNotificacion = idNotificacion;
        }

        public int getIdTrabajador() {
            return idTrabajador;
        }

        public void setIdTrabajador(int idTrabajador) {
            this.idTrabajador = idTrabajador;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }

        public String getFechaGeneracion() {
            return fechaGeneracion;
        }

        public void setFechaGeneracion(String fechaGeneracion) {
            this.fechaGeneracion = fechaGeneracion;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public String getClaveEvento() {
            return claveEvento;
        }

        public void setClaveEvento(String claveEvento) {
            this.claveEvento = claveEvento;
        }
}
