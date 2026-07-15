USE hospital_bd;

ALTER TABLE horarios
    ADD COLUMN IF NOT EXISTS dias_descanso VARCHAR(80) NULL AFTER observacion;
