-- Aplicar una vez en la misma instancia utilizada por Java. No modifica cuentas.
USE sigecap_jv;
CREATE TABLE IF NOT EXISTS recuperacion_contrasena (
    idUsuario INT NOT NULL PRIMARY KEY,
    codigoHash VARCHAR(255) NOT NULL,
    claveAnterior VARCHAR(255) NOT NULL,
    correo VARCHAR(160) NOT NULL,
    vence DATETIME NOT NULL,
    ultimoEnvio DATETIME NOT NULL,
    inicioVentana DATETIME NOT NULL,
    envios INT NOT NULL DEFAULT 1,
    intentos INT NOT NULL DEFAULT 0,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_recuperacion_usuario FOREIGN KEY (idUsuario) REFERENCES usuario(idUsuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
-- Para sigecap_app, conceder SELECT, INSERT, UPDATE en esta tabla con una cuenta administrativa.
