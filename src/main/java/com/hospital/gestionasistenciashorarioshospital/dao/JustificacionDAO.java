package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Justificacion;
import static com.hospital.gestionasistenciashorarioshospital.util.JPAUtil.getEntityManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class JustificacionDAO {

    public List<Justificacion> listarFiltrado(int inicio, int limite, String estado, Long empleadoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            String estadoFiltro = estado == null ? "todos" : estado;
            return em.createQuery(
                    "SELECT j FROM Justificacion j "
                    + "JOIN FETCH j.empleado e "
                    + "LEFT JOIN FETCH j.asistencia a "
                    + "WHERE (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND (:estado = 'todos' OR (:estado = 'aprobada' AND j.aprobada = true) "
                    + "OR (:estado = 'pendiente' AND j.aprobada = false)) "
                    + "AND (:filtro = '' OR LOWER(e.nombres) LIKE :like OR LOWER(e.apellidoPaterno) LIKE :like "
                    + "OR LOWER(e.codigoEmpleado) LIKE :like OR LOWER(j.motivo) LIKE :like) "
                    + "ORDER BY j.fechaRegistro DESC", Justificacion.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("estado", estadoFiltro)
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

    public long contarFiltrado(String estado, Long empleadoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            String estadoFiltro = estado == null ? "todos" : estado;
            return em.createQuery(
                    "SELECT COUNT(j) FROM Justificacion j JOIN j.empleado e "
                    + "WHERE (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND (:estado = 'todos' OR (:estado = 'aprobada' AND j.aprobada = true) "
                    + "OR (:estado = 'pendiente' AND j.aprobada = false)) "
                    + "AND (:filtro = '' OR LOWER(e.nombres) LIKE :like OR LOWER(e.apellidoPaterno) LIKE :like "
                    + "OR LOWER(e.codigoEmpleado) LIKE :like OR LOWER(j.motivo) LIKE :like)", Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("estado", estadoFiltro)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarPorEstado(boolean aprobada) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(j) FROM Justificacion j WHERE j.aprobada = :aprobada", Long.class)
                    .setParameter("aprobada", aprobada)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public void cambiarEstado(Long justificacionId, boolean aprobada) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Justificacion justificacion = em.find(Justificacion.class, justificacionId);
            if (justificacion != null) {
                justificacion.setAprobada(aprobada);
                em.merge(justificacion);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al actualizar la corrección", e);
        } finally {
            em.close();
        }
    }
}
