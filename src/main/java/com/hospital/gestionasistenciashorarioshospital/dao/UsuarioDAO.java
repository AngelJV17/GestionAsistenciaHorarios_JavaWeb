package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import static com.hospital.gestionasistenciashorarioshospital.util.JPAUtil.getEntityManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class UsuarioDAO {

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        EntityManager em = getEntityManager();

        try {
            TypedQuery<Usuario> query = em.createQuery(
                    "SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario",
                    Usuario.class
            );

            query.setParameter("nombreUsuario", nombreUsuario);

            return query.getSingleResult();

        } catch (NoResultException e) {
            return null;

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar usuario por nombre", e);

        } finally {
            em.close();
        }
    }

    public void actualizarUltimoAcceso(Long usuarioId) {
        EntityManager em = getEntityManager();

        try {
            em.getTransaction().begin();

            Usuario usuario = em.find(Usuario.class, usuarioId);

            if (usuario != null) {
                usuario.setUltimoAcceso(LocalDateTime.now());
                em.merge(usuario);
            }

            em.getTransaction().commit();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new RuntimeException("Error al actualizar último acceso", e);

        } finally {
            em.close();
        }
    }

    public List<Usuario> listarPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u ORDER BY u.nombreUsuario", Usuario.class)
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

    public List<Usuario> listarFiltrado(int inicio, int limite, String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Long estadoId = resolverEstadoId(estado);
            return em.createQuery(
                    "SELECT u FROM Usuario u WHERE (:estadoId IS NULL OR u.estadoId = :estadoId) "
                    + "AND (:filtro = '' OR LOWER(u.nombreUsuario) LIKE :like OR LOWER(u.correoRecuperacion) LIKE :like) "
                    + "ORDER BY u.nombreUsuario", Usuario.class)
                    .setParameter("estadoId", estadoId)
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

    public long contar() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(u) FROM Usuario u", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarFiltrado(String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Long estadoId = resolverEstadoId(estado);
            return em.createQuery(
                    "SELECT COUNT(u) FROM Usuario u WHERE (:estadoId IS NULL OR u.estadoId = :estadoId) "
                    + "AND (:filtro = '' OR LOWER(u.nombreUsuario) LIKE :like OR LOWER(u.correoRecuperacion) LIKE :like)",
                    Long.class)
                    .setParameter("estadoId", estadoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Usuario buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Usuario.class, id);
        } finally {
            em.close();
        }
    }

    public boolean existeNombreUsuario(String nombreUsuario) {
        EntityManager em = getEntityManager();
        try {
            Long total = em.createQuery(
                    "SELECT COUNT(u) FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario",
                    Long.class)
                    .setParameter("nombreUsuario", nombreUsuario)
                    .getSingleResult();
            return total != null && total > 0;
        } finally {
            em.close();
        }
    }

    public void guardar(Usuario usuario) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (usuario.getId() == null) {
                em.persist(usuario);
                em.flush();
                auditoriaDAO.registrar("usuarios", usuario.getId(), "INSERT", "Registro de usuario " + usuario.getNombreUsuario());
            } else {
                em.merge(usuario);
                auditoriaDAO.registrar("usuarios", usuario.getId(), "UPDATE", "Actualización de usuario " + usuario.getNombreUsuario());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el usuario", e);
        } finally {
            em.close();
        }
    }

    private Long resolverEstadoId(String estado) {
        if ("activo".equalsIgnoreCase(estado)) {
            return 1L;
        }
        if ("inactivo".equalsIgnoreCase(estado)) {
            return 2L;
        }
        return null;
    }
}
