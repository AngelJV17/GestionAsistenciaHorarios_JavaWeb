package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Turno;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class TurnoDAO extends BaseDAO {
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Turno> listarPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT t FROM Turno t ORDER BY t.nombre", Turno.class)
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

    public List<Turno> listarFiltrado(int inicio, int limite, String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean activo = resolverActivo(estado);
            return em.createQuery(
                    "SELECT t FROM Turno t WHERE "
                    + "(:activo IS NULL OR t.activo = :activo) AND "
                    + "(:filtro = '' OR LOWER(t.codigo) LIKE :like OR LOWER(t.nombre) LIKE :like) "
                    + "ORDER BY t.nombre",
                    Turno.class)
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
            return em.createQuery(
                    "SELECT COUNT(t) FROM Turno t WHERE "
                    + "(:activo IS NULL OR t.activo = :activo) AND "
                    + "(:filtro = '' OR LOWER(t.codigo) LIKE :like OR LOWER(t.nombre) LIKE :like)",
                    Long.class)
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
                    "SELECT t.codigo FROM Turno t WHERE t.codigo LIKE :patron ORDER BY t.codigo DESC",
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
            return em.createQuery("SELECT COUNT(t) FROM Turno t", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarActivos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(t) FROM Turno t WHERE t.activo = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public Turno buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Turno.class, id);
        } finally {
            em.close();
        }
    }

    public void guardar(Turno turno) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (turno.getId() == null) {
                em.persist(turno);
                em.flush();
                auditoriaDAO.registrar("turnos", turno.getId(), "INSERT", "Registro de turno " + turno.getCodigo());
            } else {
                em.merge(turno);
                auditoriaDAO.registrar("turnos", turno.getId(), "UPDATE", "Actualización de turno " + turno.getCodigo());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el turno", e);
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
