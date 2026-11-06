USE sigecap_jv;

CREATE TABLE IF NOT EXISTS configuracion_alertas (
    id TINYINT PRIMARY KEY CHECK (id = 1),
    diasVencimiento SMALLINT NOT NULL CHECK (diasVencimiento BETWEEN 0 AND 365),
    diasRecordatorio SMALLINT NOT NULL CHECK (diasRecordatorio BETWEEN 0 AND 365)
) ENGINE=InnoDB;

INSERT INTO configuracion_alertas (id,diasVencimiento,diasRecordatorio)
VALUES (1,30,3) ON DUPLICATE KEY UPDATE id=1;
