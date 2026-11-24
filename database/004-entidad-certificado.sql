USE sigecap_jv;

-- Conserva los certificados existentes y permite repetir la actualización.
SET @agregar_entidad = IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'certificado'
              AND COLUMN_NAME = 'entidadEmisora'),
    'SELECT ''Institución emisora disponible'' AS resultado',
    'ALTER TABLE certificado ADD COLUMN entidadEmisora VARCHAR(160) NULL AFTER numero'
);
PREPARE actualizar_certificado FROM @agregar_entidad;
EXECUTE actualizar_certificado;
DEALLOCATE PREPARE actualizar_certificado;
