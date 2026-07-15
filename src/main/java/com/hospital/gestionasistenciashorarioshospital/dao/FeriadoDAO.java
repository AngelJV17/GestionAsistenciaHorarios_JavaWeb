package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Feriado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class FeriadoDAO extends BaseDAO {
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Feriado> listarPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT f FROM Feriado f ORDER BY f.fecha", Feriado.class)
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

    public List<Feriado> listarFiltrado(int inicio, int limite, String tipo, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean recuperable = resolverRecuperable(tipo);
            return em.createQuery(
                    "SELECT f FROM Feriado f WHERE "
                    + "(:recuperable IS NULL OR f.esRecuperable = :recuperable) AND "
                    + "(:filtro = '' OR LOWER(f.descripcion) LIKE :like) "
                    + "ORDER BY f.fecha",
                    Feriado.class)
                    .setParameter("recuperable", recuperable)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .setFirstResult(inicio)
                    .setMaxResults(limite)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long contarFiltrado(String tipo, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean recuperable = resolverRecuperable(tipo);
            return em.createQuery(
                    "SELECT COUNT(f) FROM Feriado f WHERE "
                    + "(:recuperable IS NULL OR f.esRecuperable = :recuperable) AND "
                    + "(:filtro = '' OR LOWER(f.descripcion) LIKE :like)",
                    Long.class)
                    .setParameter("recuperable", recuperable)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contar() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(f) FROM Feriado f", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarRecuperables() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(f) FROM Feriado f WHERE f.esRecuperable = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public Feriado buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Feriado.class, id);
        } finally {
            em.close();
        }
    }

    public void guardar(Feriado feriado) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (feriado.getId() == null) {
                em.persist(feriado);
                em.flush();
                auditoriaDAO.registrar("feriados", feriado.getId(), "INSERT", "Registro de feriado " + feriado.getDescripcion());
            } else {
                em.merge(feriado);
                auditoriaDAO.registrar("feriados", feriado.getId(), "UPDATE", "Actualización de feriado " + feriado.getDescripcion());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el feriado", e);
        } finally {
            em.close();
        }
    }

    private Boolean resolverRecuperable(String tipo) {
        if ("recuperable".equalsIgnoreCase(tipo)) {
            return true;
        }
        if ("no_recuperable".equalsIgnoreCase(tipo)) {
            return false;
        }
        return null;
    }
}
