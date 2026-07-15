package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Solicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import com.hospital.gestionasistenciashorarioshospital.modelo.TipoSolicitud;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import static com.hospital.gestionasistenciashorarioshospital.util.JPAUtil.getEntityManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SolicitudDAO {

    public List<Solicitud> listarFiltrado(int inicio, int limite, Long estadoId, Long empleadoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT s FROM Solicitud s "
                    + "JOIN FETCH s.empleado e "
                    + "JOIN FETCH s.tipoSolicitud ts "
                    + "JOIN FETCH s.estado es "
                    + "WHERE (:estadoId IS NULL OR es.id = :estadoId) "
                    + "AND (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND (:filtro = '' OR LOWER(e.nombres) LIKE :like OR LOWER(e.apellidoPaterno) LIKE :like "
                    + "OR LOWER(e.codigoEmpleado) LIKE :like OR LOWER(ts.nombre) LIKE :like OR LOWER(s.motivo) LIKE :like) "
                    + "ORDER BY s.fechaRegistro DESC", Solicitud.class)
                    .setParameter("estadoId", estadoId)
                    .setParameter("empleadoId", empleadoId)
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

    public long contarFiltrado(Long estadoId, Long empleadoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s "
                    + "JOIN s.empleado e JOIN s.tipoSolicitud ts JOIN s.estado es "
                    + "WHERE (:estadoId IS NULL OR es.id = :estadoId) "
                    + "AND (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND (:filtro = '' OR LOWER(e.nombres) LIKE :like OR LOWER(e.apellidoPaterno) LIKE :like "
                    + "OR LOWER(e.codigoEmpleado) LIKE :like OR LOWER(ts.nombre) LIKE :like OR LOWER(s.motivo) LIKE :like)", Long.class)
                    .setParameter("estadoId", estadoId)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarPorEstadoCodigo(String codigoEstado) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s WHERE UPPER(s.estado.codigo) = :codigo", Long.class)
                    .setParameter("codigo", codigoEstado.toUpperCase())
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Solicitud> listarPorEmpleado(int inicio, int limite, Long empleadoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT s FROM Solicitud s "
                    + "JOIN FETCH s.empleado e "
                    + "JOIN FETCH s.tipoSolicitud ts "
                    + "JOIN FETCH s.estado es "
                    + "WHERE e.id = :empleadoId "
                    + "AND (:filtro = '' OR LOWER(ts.nombre) LIKE :like OR LOWER(s.motivo) LIKE :like "
                    + "OR LOWER(es.nombre) LIKE :like) "
                    + "ORDER BY s.fechaRegistro DESC", Solicitud.class)
                    .setParameter("empleadoId", empleadoId)
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

    public long contarPorEmpleado(Long empleadoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s "
                    + "JOIN s.empleado e JOIN s.tipoSolicitud ts JOIN s.estado es "
                    + "WHERE e.id = :empleadoId "
                    + "AND (:filtro = '' OR LOWER(ts.nombre) LIKE :like OR LOWER(s.motivo) LIKE :like "
                    + "OR LOWER(es.nombre) LIKE :like)", Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarPorEmpleadoYEstadoCodigo(Long empleadoId, String codigoEstado) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s "
                    + "WHERE s.empleado.id = :empleadoId AND UPPER(s.estado.codigo) = :codigo",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("codigo", codigoEstado.toUpperCase())
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarPorEmpleado(Long empleadoId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(s) FROM Solicitud s WHERE s.empleado.id = :empleadoId",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Solicitud buscarJustificacionPorEmpleadoYFecha(Long empleadoId, LocalDate fecha) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM Solicitud s "
                    + "JOIN FETCH s.empleado e "
                    + "JOIN FETCH s.tipoSolicitud ts "
                    + "JOIN FETCH s.estado es "
                    + "WHERE e.id = :empleadoId "
                    + "AND UPPER(ts.codigo) = 'JUSTIFICACION' "
                    + "AND s.fechaInicio = :fecha "
                    + "ORDER BY s.fechaRegistro DESC",
                    Solicitud.class)
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

    public boolean existeJustificacionPorEmpleadoYFecha(Long empleadoId, LocalDate fecha) {
        return buscarJustificacionPorEmpleadoYFecha(empleadoId, fecha) != null;
    }

    public void guardar(Solicitud solicitud) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (solicitud.getId() == null) {
                solicitud.setFechaRegistro(LocalDateTime.now());
                solicitud.setEmpleado(em.getReference(Empleado.class, solicitud.getEmpleado().getId()));
                solicitud.setTipoSolicitud(em.getReference(TipoSolicitud.class, solicitud.getTipoSolicitud().getId()));
                solicitud.setEstado(em.getReference(VariableGlobal.class, solicitud.getEstado().getId()));
                em.persist(solicitud);
            } else {
                em.merge(solicitud);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar la solicitud", e);
        } finally {
            em.close();
        }
    }

    public void cambiarEstado(Long solicitudId, Long estadoId, Usuario aprobador, String observacion) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Solicitud solicitud = em.find(Solicitud.class, solicitudId);
            if (solicitud != null) {
                solicitud.setEstado(em.getReference(VariableGlobal.class, estadoId));
                solicitud.setUsuarioAprobador(aprobador == null ? null : em.getReference(Usuario.class, aprobador.getId()));
                solicitud.setObservacionAprobacion(observacion);
                solicitud.setFechaAprobacion(LocalDateTime.now());
                em.merge(solicitud);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al cambiar el estado de la solicitud", e);
        } finally {
            em.close();
        }
    }

    public Solicitud buscarPorId(Long solicitudId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM Solicitud s "
                    + "JOIN FETCH s.empleado "
                    + "JOIN FETCH s.tipoSolicitud "
                    + "JOIN FETCH s.estado "
                    + "WHERE s.id = :solicitudId",
                    Solicitud.class)
                    .setParameter("solicitudId", solicitudId)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }
}
