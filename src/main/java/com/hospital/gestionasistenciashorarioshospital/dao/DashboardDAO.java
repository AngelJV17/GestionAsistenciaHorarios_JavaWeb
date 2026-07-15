package com.hospital.gestionasistenciashorarioshospital.dao;

import static com.hospital.gestionasistenciashorarioshospital.util.JPAUtil.getEntityManager;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DashboardDAO {

    public long contarEmpleados() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(e) FROM Empleado e", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarEmpleadosActivos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(e) FROM Empleado e WHERE e.activo = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarAsistenciasHoy() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(a) FROM Asistencia a WHERE a.fechaAsistencia = :hoy", Long.class)
                    .setParameter("hoy", LocalDate.now())
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarCorreccionesPendientes() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(j) FROM Justificacion j WHERE j.aprobada = false", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarSolicitudesHoy() {
        EntityManager em = getEntityManager();
        try {
            LocalDateTime inicio = LocalDate.now().atStartOfDay();
            LocalDateTime fin = inicio.plusDays(1);
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s WHERE s.fechaRegistro >= :inicio AND s.fechaRegistro < :fin",
                    Long.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarSolicitudesPendientes() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s WHERE UPPER(s.estado.codigo) = 'PENDIENTE'",
                    Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
