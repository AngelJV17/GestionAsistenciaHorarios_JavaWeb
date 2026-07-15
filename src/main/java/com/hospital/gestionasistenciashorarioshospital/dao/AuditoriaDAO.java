package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Auditoria;
import static com.hospital.gestionasistenciashorarioshospital.util.JPAUtil.getEntityManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class AuditoriaDAO {

    public void registrar(String tabla, Long registroId, String accion, String descripcion) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Auditoria auditoria = new Auditoria();
            auditoria.setTablaAfectada(tabla);
            auditoria.setRegistroId(registroId);
            auditoria.setAccion(accion);
            auditoria.setDescripcion(descripcion);
            auditoria.setUsuarioSistema("sistema");
            auditoria.setFechaRegistro(LocalDateTime.now());
            em.persist(auditoria);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
        } finally {
            em.close();
        }
    }

    public List<Auditoria> listarRecientes(int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT a FROM Auditoria a ORDER BY a.fechaRegistro DESC", Auditoria.class)
                    .setMaxResults(limite)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<Auditoria> listarFiltrado(int inicio, int limite, LocalDate desde, LocalDate hasta, String usuario, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            String usuarioFiltro = usuario == null ? "todos" : usuario;
            return em.createQuery(
                    "SELECT a FROM Auditoria a "
                    + "WHERE (:desde IS NULL OR a.fechaRegistro >= :desde) "
                    + "AND (:hasta IS NULL OR a.fechaRegistro <= :hasta) "
                    + "AND (:usuario = 'todos' OR a.usuarioSistema = :usuario) "
                    + "AND (:filtro = '' OR LOWER(a.accion) LIKE :like OR LOWER(a.tablaAfectada) LIKE :like "
                    + "OR LOWER(a.descripcion) LIKE :like OR LOWER(a.ip) LIKE :like) "
                    + "ORDER BY a.fechaRegistro DESC", Auditoria.class)
                    .setParameter("desde", desde == null ? null : LocalDateTime.of(desde, LocalTime.MIN))
                    .setParameter("hasta", hasta == null ? null : LocalDateTime.of(hasta, LocalTime.MAX))
                    .setParameter("usuario", usuarioFiltro)
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

    public long contarFiltrado(LocalDate desde, LocalDate hasta, String usuario, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            String usuarioFiltro = usuario == null ? "todos" : usuario;
            return em.createQuery(
                    "SELECT COUNT(a) FROM Auditoria a "
                    + "WHERE (:desde IS NULL OR a.fechaRegistro >= :desde) "
                    + "AND (:hasta IS NULL OR a.fechaRegistro <= :hasta) "
                    + "AND (:usuario = 'todos' OR a.usuarioSistema = :usuario) "
                    + "AND (:filtro = '' OR LOWER(a.accion) LIKE :like OR LOWER(a.tablaAfectada) LIKE :like "
                    + "OR LOWER(a.descripcion) LIKE :like OR LOWER(a.ip) LIKE :like)", Long.class)
                    .setParameter("desde", desde == null ? null : LocalDateTime.of(desde, LocalTime.MIN))
                    .setParameter("hasta", hasta == null ? null : LocalDateTime.of(hasta, LocalTime.MAX))
                    .setParameter("usuario", usuarioFiltro)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarPorAccion(String accion) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(a) FROM Auditoria a WHERE UPPER(a.accion) = :accion", Long.class)
                    .setParameter("accion", accion.toUpperCase())
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
