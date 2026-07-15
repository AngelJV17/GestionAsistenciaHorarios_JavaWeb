USE hospital_bd;

INSERT INTO usuarios (id, nombre_usuario, password_hash, correo_recuperacion, estado_id, ultimo_acceso)
VALUES
    (2, 'cmedina', '$2a$10$L6iYKU9ngSCwuqZ4GhKXVuE1kMsgRqAoJibyhNvtzqH21qFtJa6Rm', 'cmedina@hospital.com', 1, NULL),
    (3, 'rquispe', '$2a$10$L6iYKU9ngSCwuqZ4GhKXVuE1kMsgRqAoJibyhNvtzqH21qFtJa6Rm', 'rquispe@hospital.com', 1, NULL),
    (4, 'lramirez', '$2a$10$L6iYKU9ngSCwuqZ4GhKXVuE1kMsgRqAoJibyhNvtzqH21qFtJa6Rm', 'lramirez@hospital.com', 1, NULL)
ON DUPLICATE KEY UPDATE
    correo_recuperacion = VALUES(correo_recuperacion),
    estado_id = VALUES(estado_id);

INSERT INTO empleados (
    id, usuario_id, nombres, apellido_paterno, apellido_materno, fecha_nacimiento,
    sexo_id, correo, telefono, dni, direccion, foto_perfil, sede_id, area_id,
    cargo_id, especialidad_id, biografia, codigo_empleado, fecha_ingreso,
    fecha_cese, numero_colegiatura, activo
)
VALUES
    (1, 2, 'Carlos Alberto', 'Medina', 'Torres', '1985-04-12',
     1, 'cmedina@hospital.com', '987654321', '45678912', 'Jr. Salud 120',
     NULL, 1, 2, 1, 2, 'Medico pediatra con experiencia en atencion hospitalaria.',
     'MED000001', '2022-03-01', NULL, 'CMP-12345', 1),
    (2, 3, 'Rosa Elena', 'Quispe', 'Huaman', '1990-08-20',
     2, 'rquispe@hospital.com', '987111222', '46789123', 'Av. Central 450',
     NULL, 1, 1, 1, 1, 'Medica cardiologa enfocada en control preventivo.',
     'MED000002', '2023-01-15', NULL, 'CMP-23456', 1),
    (3, 4, 'Luis Fernando', 'Ramirez', 'Diaz', '1988-11-03',
     1, 'lramirez@hospital.com', '987333444', '47891234', 'Calle Los Pinos 789',
     NULL, 1, 1, 2, NULL, 'Enfermero asistencial del area de emergencia.',
     'ENF000001', '2021-07-10', NULL, NULL, 1)
ON DUPLICATE KEY UPDATE
    usuario_id = VALUES(usuario_id),
    correo = VALUES(correo),
    telefono = VALUES(telefono),
    direccion = VALUES(direccion),
    activo = VALUES(activo);

INSERT IGNORE INTO usuario_roles (usuario_id, rol_id)
SELECT 2, id FROM roles WHERE codigo = 'MEDICO';

INSERT IGNORE INTO usuario_roles (usuario_id, rol_id)
SELECT 3, id FROM roles WHERE codigo = 'MEDICO';

INSERT IGNORE INTO usuario_roles (usuario_id, rol_id)
SELECT 4, id FROM roles WHERE codigo = 'ENFERMERO';

DELETE FROM documentos
WHERE nombre_archivo IN (
    'contrato-carlos-medina.pdf',
    'constancia-carlos-medina.pdf',
    'contrato-rosa-quispe.pdf',
    'licencia-rosa-quispe.pdf',
    'constancia-luis-ramirez.pdf'
);

INSERT INTO documentos (
    empleado_id, tipo_documento_id, descripcion, nombre_archivo, ruta_archivo,
    tamano_archivo, extension, fecha_vencimiento
)
VALUES
    (1, 1, 'Contrato laboral del medico Carlos Medina', 'contrato-carlos-medina.pdf',
     'C:/hospital-documentos/MED000001/contrato-carlos-medina.pdf', 245760, 'pdf', '2027-12-31'),
    (1, 3, 'Constancia laboral vigente', 'constancia-carlos-medina.pdf',
     'C:/hospital-documentos/MED000001/constancia-carlos-medina.pdf', 128000, 'pdf', NULL),
    (2, 1, 'Contrato laboral de la medica Rosa Quispe', 'contrato-rosa-quispe.pdf',
     'C:/hospital-documentos/MED000002/contrato-rosa-quispe.pdf', 251904, 'pdf', '2027-12-31'),
    (2, 4, 'Licencia medica registrada como ejemplo', 'licencia-rosa-quispe.pdf',
     'C:/hospital-documentos/MED000002/licencia-rosa-quispe.pdf', 96000, 'pdf', '2026-12-31'),
    (3, 3, 'Constancia laboral del enfermero Luis Ramirez', 'constancia-luis-ramirez.pdf',
     'C:/hospital-documentos/ENF000001/constancia-luis-ramirez.pdf', 112640, 'pdf', NULL);

DELETE FROM solicitudes
WHERE motivo IN (
    'Licencia medica por tratamiento ambulatorio.',
    'Permiso administrativo para tramite documentario.',
    'Justificacion de tardanza fuera de plazo.'
);

INSERT INTO solicitudes (empleado_id, tipo_solicitud_id, estado_id, fecha_inicio, fecha_fin, motivo, observacion_aprobacion, usuario_aprobador_id, fecha_aprobacion)
VALUES
    (1, 4, 6, '2026-07-15', '2026-07-17', 'Licencia medica por tratamiento ambulatorio.', 'Aprobado para pruebas del sistema.', 1, NOW()),
    (2, 2, 5, '2026-07-22', '2026-07-22', 'Permiso administrativo para tramite documentario.', NULL, NULL, NULL),
    (3, 5, 7, '2026-07-05', '2026-07-05', 'Justificacion de tardanza fuera de plazo.', 'Rechazado para pruebas del sistema.', 1, NOW());
