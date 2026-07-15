package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import static com.hospital.gestionasistenciashorarioshospital.util.JPAUtil.getEntityManager;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class ReporteDAO {

    public long contarEmpleadosActivos(Long areaId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(e) FROM Empleado e WHERE e.activo = true AND (:areaId IS NULL OR e.area.id = :areaId)",
                    Long.class)
                    .setParameter("areaId", areaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public BigDecimal sumarHorasExtras(int mes, Long areaId) {
        EntityManager em = getEntityManager();
        try {
            BigDecimal total = em.createQuery(
                    "SELECT COALESCE(SUM(a.horasExtras), 0) FROM Asistencia a "
                    + "WHERE MONTH(a.fechaAsistencia) = :mes AND (:areaId IS NULL OR a.empleado.area.id = :areaId)",
                    BigDecimal.class)
                    .setParameter("mes", mes)
                    .setParameter("areaId", areaId)
                    .getSingleResult();
            return total == null ? BigDecimal.ZERO : total;
        } finally {
            em.close();
        }
    }

    public long contarTardanzas(int mes, Long areaId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(a) FROM Asistencia a "
                    + "WHERE MONTH(a.fechaAsistencia) = :mes AND COALESCE(a.minutosTardanza, 0) > 0 "
                    + "AND (:areaId IS NULL OR a.empleado.area.id = :areaId)", Long.class)
                    .setParameter("mes", mes)
                    .setParameter("areaId", areaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarAsistencias(int mes, Long areaId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(a) FROM Asistencia a "
                    + "WHERE MONTH(a.fechaAsistencia) = :mes AND (:areaId IS NULL OR a.empleado.area.id = :areaId)",
                    Long.class)
                    .setParameter("mes", mes)
                    .setParameter("areaId", areaId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Empleado> listarEmpleadosReporte(Long areaId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT e FROM Empleado e LEFT JOIN FETCH e.area LEFT JOIN FETCH e.cargo WHERE e.activo = true "
                    + "AND (:areaId IS NULL OR e.area.id = :areaId) ORDER BY e.apellidoPaterno, e.nombres",
                    Empleado.class)
                    .setParameter("areaId", areaId)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }
}
