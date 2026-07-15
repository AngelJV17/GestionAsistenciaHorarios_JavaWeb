ALTER TABLE solicitudes
    ADD COLUMN nombre_archivo VARCHAR(255) NULL AFTER fecha_registro,
    ADD COLUMN ruta_archivo VARCHAR(500) NULL AFTER nombre_archivo,
    ADD COLUMN tamano_archivo BIGINT NULL AFTER ruta_archivo,
    ADD COLUMN extension VARCHAR(20) NULL AFTER tamano_archivo;
