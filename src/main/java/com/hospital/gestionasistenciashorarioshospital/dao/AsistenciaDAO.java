package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Asistencia;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class AsistenciaDAO extends BaseDAO {

    public List<Asistencia> listarPorMes(Long empleadoId, int mes, int anio) {
        EntityManager em = getEntityManager();
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        try {
            return em.createQuery(
                    "SELECT a FROM Asistencia a "
                    + "LEFT JOIN FETCH a.estadoAsistencia "
                    + "WHERE a.empleado.id = :empleadoId "
                    + "AND a.fechaAsistencia BETWEEN :inicio AND :fin "
                    + "ORDER BY a.fechaAsistencia DESC",
                    Asistencia.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<Asistencia> listarFiltradoPorMes(Long empleadoId, int mes, int anio) {
        EntityManager em = getEntityManager();
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        try {
            return em.createQuery(
                    "SELECT a FROM Asistencia a "
                    + "JOIN FETCH a.empleado e "
                    + "LEFT JOIN FETCH a.estadoAsistencia "
                    + "WHERE (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND a.fechaAsistencia BETWEEN :inicio AND :fin "
                    + "ORDER BY a.fechaAsistencia DESC, e.apellidoPaterno, e.nombres",
                    Asistencia.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public Asistencia buscarHoy(Long empleadoId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Asistencia a LEFT JOIN FETCH a.estadoAsistencia "
                    + "WHERE a.empleado.id = :empleadoId "
                    + "AND a.fechaAsistencia = :hoy",
                    Asistencia.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("hoy", LocalDate.now())
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public Asistencia buscarPorEmpleadoYFecha(Long empleadoId, LocalDate fecha) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Asistencia a "
                    + "JOIN FETCH a.empleado "
                    + "LEFT JOIN FETCH a.estadoAsistencia "
                    + "WHERE a.empleado.id = :empleadoId "
                    + "AND a.fechaAsistencia = :fecha",
                    Asistencia.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("fecha", fecha)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public void marcarComoJustificada(Long asistenciaId, Long estadoId, String observacion) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Asistencia asistencia = em.find(Asistencia.class, asistenciaId);
            if (asistencia != null) {
                asistencia.setEstadoAsistencia(em.getReference(VariableGlobal.class, estadoId));
                asistencia.setObservacion(observacion);
                em.merge(asistencia);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al marcar la asistencia como justificada", e);
        } finally {
            em.close();
        }
    }

    public void guardar(Asistencia asistencia) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (asistencia.getId() == null) {
                em.persist(asistencia);
            } else {
                em.merge(asistencia);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar la asistencia", e);
        } finally {
            em.close();
        }
    }

    public long contarPorEstado(Long empleadoId, String codigoEstado, int mes, int anio) {
        EntityManager em = getEntityManager();
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        try {
            return em.createQuery(
                    "SELECT COUNT(a) FROM Asistencia a "
                    + "WHERE a.empleado.id = :empleadoId "
                    + "AND UPPER(a.estadoAsistencia.codigo) = :codigoEstado "
                    + "AND a.fechaAsistencia BETWEEN :inicio AND :fin",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("codigoEstado", codigoEstado == null ? "" : codigoEstado.toUpperCase())
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
