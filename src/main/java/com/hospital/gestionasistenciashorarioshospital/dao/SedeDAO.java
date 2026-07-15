package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Sede;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class SedeDAO extends BaseDAO {
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Sede> listarPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT s FROM Sede s ORDER BY s.nombre", Sede.class)
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

    public List<Sede> listarFiltrado(int inicio, int limite, String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean activo = resolverActivo(estado);
            String jpql = "SELECT s FROM Sede s WHERE "
                    + "(:activo IS NULL OR s.activo = :activo) AND "
                    + "(:filtro = '' OR LOWER(s.codigo) LIKE :like OR LOWER(s.nombre) LIKE :like OR LOWER(s.direccion) LIKE :like) "
                    + "ORDER BY s.nombre";
            return em.createQuery(jpql, Sede.class)
                    .setParameter("activo", activo)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .setFirstResult(inicio)
                    .setMaxResults(limite)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long contarFiltrado(String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean activo = resolverActivo(estado);
            String jpql = "SELECT COUNT(s) FROM Sede s WHERE "
                    + "(:activo IS NULL OR s.activo = :activo) AND "
                    + "(:filtro = '' OR LOWER(s.codigo) LIKE :like OR LOWER(s.nombre) LIKE :like OR LOWER(s.direccion) LIKE :like)";
            return em.createQuery(jpql, Long.class)
                    .setParameter("activo", activo)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Long obtenerUltimoNumeroCodigo(String prefijo) {
        EntityManager em = getEntityManager();
        try {
            List<String> codigos = em.createQuery(
                    "SELECT s.codigo FROM Sede s WHERE s.codigo LIKE :patron ORDER BY s.codigo DESC",
                    String.class)
                    .setParameter("patron", prefijo + "%")
                    .setMaxResults(1)
                    .getResultList();
            return extraerNumero(codigos.isEmpty() ? null : codigos.get(0), prefijo);
        } finally {
            em.close();
        }
    }

    public long contar() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(s) FROM Sede s", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarActivas() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(s) FROM Sede s WHERE s.activo = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public Sede buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Sede.class, id);
        } finally {
            em.close();
        }
    }

    public void guardar(Sede sede) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (sede.getId() == null) {
                em.persist(sede);
                em.flush();
                auditoriaDAO.registrar("sedes", sede.getId(), "INSERT", "Registro de sede " + sede.getCodigo());
            } else {
                em.merge(sede);
                auditoriaDAO.registrar("sedes", sede.getId(), "UPDATE", "Actualización de sede " + sede.getCodigo());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar la sede", e);
        } finally {
            em.close();
        }
    }

    private Boolean resolverActivo(String estado) {
        if ("activo".equalsIgnoreCase(estado)) {
            return true;
        }
        if ("inactivo".equalsIgnoreCase(estado)) {
            return false;
        }
        return null;
    }

    private Long extraerNumero(String codigo, String prefijo) {
        if (codigo == null || !codigo.startsWith(prefijo)) {
            return 0L;
        }
        try {
            return Long.valueOf(codigo.substring(prefijo.length()));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
