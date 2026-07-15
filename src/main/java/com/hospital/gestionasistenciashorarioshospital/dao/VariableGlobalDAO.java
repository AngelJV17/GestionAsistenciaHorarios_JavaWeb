package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.VariableGlobal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class VariableGlobalDAO extends BaseDAO {
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<VariableGlobal> listarPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT v FROM VariableGlobal v ORDER BY v.categoria, v.ordenVisualizacion, v.nombre",
                    VariableGlobal.class
            ).setFirstResult(inicio)
                    .setMaxResults(limite)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<VariableGlobal> listarFiltrado(int inicio, int limite, String categoria, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            String cat = categoria == null || "todas".equalsIgnoreCase(categoria) ? "" : categoria;
            return em.createQuery(
                    "SELECT v FROM VariableGlobal v WHERE "
                    + "(:categoria = '' OR v.categoria = :categoria) AND "
                    + "(:filtro = '' OR LOWER(v.codigo) LIKE :like OR LOWER(v.nombre) LIKE :like OR LOWER(v.descripcion) LIKE :like) "
                    + "ORDER BY v.categoria, v.ordenVisualizacion, v.nombre",
                    VariableGlobal.class)
                    .setParameter("categoria", cat)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .setFirstResult(inicio)
                    .setMaxResults(limite)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long contarFiltrado(String categoria, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            String cat = categoria == null || "todas".equalsIgnoreCase(categoria) ? "" : categoria;
            return em.createQuery(
                    "SELECT COUNT(v) FROM VariableGlobal v WHERE "
                    + "(:categoria = '' OR v.categoria = :categoria) AND "
                    + "(:filtro = '' OR LOWER(v.codigo) LIKE :like OR LOWER(v.nombre) LIKE :like OR LOWER(v.descripcion) LIKE :like)",
                    Long.class)
                    .setParameter("categoria", cat)
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
            return em.createQuery("SELECT COUNT(v) FROM VariableGlobal v", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarActivas() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(v) FROM VariableGlobal v WHERE v.activo = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarCategorias() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(DISTINCT v.categoria) FROM VariableGlobal v", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public VariableGlobal buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(VariableGlobal.class, id);
        } finally {
            em.close();
        }
    }

    public VariableGlobal buscarPorCodigo(String codigo) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT v FROM VariableGlobal v WHERE UPPER(v.codigo) = :codigo",
                    VariableGlobal.class)
                    .setParameter("codigo", codigo == null ? "" : codigo.toUpperCase())
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public List<VariableGlobal> listarPorCategoria(String categoria) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT v FROM VariableGlobal v WHERE v.categoria = :categoria ORDER BY v.ordenVisualizacion, v.nombre",
                    VariableGlobal.class)
                    .setParameter("categoria", categoria)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void guardar(VariableGlobal variable) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (variable.getId() == null) {
                em.persist(variable);
                em.flush();
                auditoriaDAO.registrar("variables_globales", variable.getId(), "INSERT", "Registro de variable " + variable.getCodigo());
            } else {
                em.merge(variable);
                auditoriaDAO.registrar("variables_globales", variable.getId(), "UPDATE", "Actualización de variable " + variable.getCodigo());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar la variable global", e);
        } finally {
            em.close();
        }
    }
}
