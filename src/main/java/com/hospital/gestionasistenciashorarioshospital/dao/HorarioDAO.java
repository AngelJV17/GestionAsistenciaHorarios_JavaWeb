package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Horario;
import com.hospital.gestionasistenciashorarioshospital.modelo.Turno;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class HorarioDAO extends BaseDAO {

    public List<Horario> listarPorEmpleado(Long empleadoId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT h FROM Horario h JOIN FETCH h.turno "
                    + "WHERE h.empleadoId = :empleadoId "
                    + "ORDER BY h.fechaInicio DESC",
                    Horario.class)
                    .setParameter("empleadoId", empleadoId)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<Horario> listarFiltrado(int inicio, int limite, Long empleadoId, Long turnoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT h FROM Horario h JOIN FETCH h.turno t "
                    + "WHERE (:empleadoId IS NULL OR h.empleadoId = :empleadoId) "
                    + "AND (:turnoId IS NULL OR t.id = :turnoId) "
                    + "AND (:filtro = '' OR LOWER(t.nombre) LIKE :like OR LOWER(h.observacion) LIKE :like) "
                    + "ORDER BY h.fechaInicio DESC",
                    Horario.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("turnoId", turnoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .setFirstResult(inicio)
                    .setMaxResults(limite)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public long contarFiltrado(Long empleadoId, Long turnoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT COUNT(h) FROM Horario h JOIN h.turno t "
                    + "WHERE (:empleadoId IS NULL OR h.empleadoId = :empleadoId) "
                    + "AND (:turnoId IS NULL OR t.id = :turnoId) "
                    + "AND (:filtro = '' OR LOWER(t.nombre) LIKE :like OR LOWER(h.observacion) LIKE :like)",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("turnoId", turnoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Horario buscarHorarioActivo(Long empleadoId) {
        EntityManager em = getEntityManager();
        LocalDate hoy = LocalDate.now();
        try {
            return em.createQuery(
                    "SELECT h FROM Horario h JOIN FETCH h.turno "
                    + "WHERE h.empleadoId = :empleadoId "
                    + "AND h.fechaInicio <= :hoy AND h.fechaFin >= :hoy "
                    + "ORDER BY h.fechaInicio DESC",
                    Horario.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("hoy", hoy)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public long contarPorEmpleado(Long empleadoId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(h) FROM Horario h WHERE h.empleadoId = :empleadoId",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Horario buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT h FROM Horario h JOIN FETCH h.turno WHERE h.id = :id",
                    Horario.class)
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public boolean existeTurnoSolapado(Long empleadoId, Long turnoId, LocalDate fechaInicio,
            LocalDate fechaFin, Long excluirHorarioId) {
        if (empleadoId == null || turnoId == null || fechaInicio == null || fechaFin == null) {
            return false;
        }
        EntityManager em = getEntityManager();
        try {
            String jpql = "SELECT COUNT(h) FROM Horario h JOIN h.turno t "
                    + "WHERE h.empleadoId = :empleadoId "
                    + "AND t.id = :turnoId "
                    + "AND h.fechaInicio <= :fechaFin "
                    + "AND h.fechaFin >= :fechaInicio "
                    + "AND (:excluirId IS NULL OR h.id <> :excluirId)";
            Long total = em.createQuery(jpql, Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("turnoId", turnoId)
                    .setParameter("fechaInicio", fechaInicio)
                    .setParameter("fechaFin", fechaFin)
                    .setParameter("excluirId", excluirHorarioId)
                    .getSingleResult();
            return total != null && total > 0;
        } finally {
            em.close();
        }
    }

    public void guardar(Horario horario) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            horario.setTurno(em.getReference(Turno.class, horario.getTurno().getId()));
            if (horario.getId() == null) {
                em.persist(horario);
            } else {
                em.merge(horario);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el horario", e);
        } finally {
            em.close();
        }
    }

    public void eliminar(Long id) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Horario horario = em.find(Horario.class, id);
            if (horario != null) {
                em.remove(horario);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al eliminar el horario", e);
        } finally {
            em.close();
        }
    }
}
