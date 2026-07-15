# Sistema de Gestión de Asistencias y Horarios Hospitalarios

Sistema web desarrollado para la gestión de asistencia, horarios, solicitudes y documentos del personal médico y administrativo de un hospital.  
El proyecto está orientado al control organizado de turnos, marcaciones, incidencias y seguimiento de solicitudes internas.

## Descripción del proyecto

Este sistema permite administrar de manera centralizada los procesos relacionados con la asistencia del personal hospitalario, facilitando el registro de entradas y salidas, la consulta de horarios asignados, la gestión de solicitudes y el control documental.

El sistema fue desarrollado como una aplicación Java Web utilizando una arquitectura organizada por capas, separando la lógica de presentación, controladores, acceso a datos y modelos de dominio.

## Características principales

- Inicio de sesión de usuarios.
- Panel principal con indicadores generales.
- Gestión de empleados.
- Gestión de usuarios, roles y permisos.
- Gestión de horarios y turnos.
- Registro de asistencias y marcaciones.
- Consulta de historial mensual de asistencias.
- Calendario visual de asistencias.
- Registro y seguimiento de solicitudes.
- Gestión de documentos.
- Configuración general del sistema.
- Diseño responsive adaptable a escritorio y dispositivos móviles.

## Módulos del sistema

### Administración

- Dashboard administrativo.
- Gestión de empleados.
- Gestión de usuarios.
- Roles y permisos.
- Catálogos generales.
- Horarios.
- Solicitudes.
- Documentos.
- Configuración.

### Médico / Personal

- Inicio del personal.
- Registro de asistencia.
- Consulta de horario semanal.
- Solicitud de justificaciones o cambios.
- Consulta de documentos.
- Configuración de perfil.

## Tecnologías utilizadas

- Java
- Jakarta EE
- Jakarta Faces / JSF
- CDI
- JPA / Hibernate
- MySQL
- Maven
- Apache Tomcat
- NetBeans
- HTML5
- CSS3
- JavaScript
- Bootstrap 5
- Font Awesome
- SweetAlert2

## Arquitectura del proyecto

El sistema sigue una estructura organizada por capas:

```text
src/main/java
└── com.hospital.gestionasistenciashorarioshospital
    ├── controlador
    ├── dao
    ├── modelo
    ├── util
    └── config

src/main/webapp
├── resources
│   ├── css
│   ├── js
│   └── images
└── views
    ├── admin
    ├── medico
    └── auth
