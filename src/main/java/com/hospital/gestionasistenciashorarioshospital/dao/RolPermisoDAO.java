package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Permiso;
import com.hospital.gestionasistenciashorarioshospital.modelo.Rol;
import com.hospital.gestionasistenciashorarioshospital.modelo.RolPermiso;
import com.hospital.gestionasistenciashorarioshospital.modelo.Usuario;
import com.hospital.gestionasistenciashorarioshospital.modelo.UsuarioRol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RolPermisoDAO extends BaseDAO {

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Rol> listarRoles() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT r FROM Rol r ORDER BY r.nombre", Rol.class)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public void guardarRol(Rol rol) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (rol.getId() == null) {
                em.persist(rol);
                em.flush();
                auditoriaDAO.registrar("roles", rol.getId(), "INSERT", "Registro de rol " + rol.getCodigo());
            } else {
                em.merge(rol);
                auditoriaDAO.registrar("roles", rol.getId(), "UPDATE", "Actualización de rol " + rol.getCodigo());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el rol", e);
        } finally {
            em.close();
        }
    }

    public Rol buscarRolPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Rol.class, id);
        } finally {
            em.close();
        }
    }

    public List<Permiso> listarPermisos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Permiso p ORDER BY p.modulo, p.nombre", Permiso.class)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<RolPermiso> listarRolPermisos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT rp FROM RolPermiso rp "
                    + "JOIN FETCH rp.rol r "
                    + "JOIN FETCH rp.permiso p "
                    + "ORDER BY r.nombre, p.modulo, p.nombre",
                    RolPermiso.class
            ).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<UsuarioRol> listarUsuarioRoles() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT ur FROM UsuarioRol ur "
                    + "JOIN FETCH ur.usuario u "
                    + "JOIN FETCH ur.rol r "
                    + "ORDER BY u.nombreUsuario, r.nombre",
                    UsuarioRol.class)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<Rol> listarRolesPorUsuario(Long usuarioId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT ur.rol FROM UsuarioRol ur "
                    + "WHERE ur.usuario.id = :usuarioId "
                    + "ORDER BY ur.rol.nombre",
                    Rol.class
            ).setParameter("usuarioId", usuarioId)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<Long> listarPermisoIdsPorRol(Long rolId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT rp.permiso.id FROM RolPermiso rp WHERE rp.rol.id = :rolId ORDER BY rp.permiso.id",
                    Long.class)
                    .setParameter("rolId", rolId)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public void guardarPermisosRol(Long rolId, List<Long> permisoIds) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Rol rol = em.find(Rol.class, rolId);
            if (rol == null) {
                throw new IllegalStateException("No existe el rol seleccionado");
            }

            em.createQuery("DELETE FROM RolPermiso rp WHERE rp.rol.id = :rolId")
                    .setParameter("rolId", rolId)
                    .executeUpdate();

            if (permisoIds != null) {
                for (Long permisoId : permisoIds) {
                    Permiso permiso = em.find(Permiso.class, permisoId);
                    if (permiso != null) {
                        RolPermiso rolPermiso = new RolPermiso();
                        rolPermiso.setRol(rol);
                        rolPermiso.setPermiso(permiso);
                        em.persist(rolPermiso);
                    }
                }
            }
            auditoriaDAO.registrar("rol_permisos", rolId, "UPDATE", "Actualización de permisos del rol " + rol.getCodigo());

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar permisos del rol", e);
        } finally {
            em.close();
        }
    }

    public void asignarRolAUsuario(Long usuarioId, Long rolId) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Usuario usuario = em.find(Usuario.class, usuarioId);
            Rol rol = em.find(Rol.class, rolId);
            if (usuario == null || rol == null) {
                throw new IllegalStateException("Usuario o rol no encontrado");
            }

            Long total = em.createQuery(
                    "SELECT COUNT(ur) FROM UsuarioRol ur WHERE ur.usuario.id = :usuarioId AND ur.rol.id = :rolId",
                    Long.class)
                    .setParameter("usuarioId", usuarioId)
                    .setParameter("rolId", rolId)
                    .getSingleResult();

            if (total == null || total == 0) {
                em.persist(new UsuarioRol(usuario, rol));
                auditoriaDAO.registrar("usuario_roles", usuarioId, "INSERT", "Asignación de rol " + rol.getCodigo() + " al usuario " + usuario.getNombreUsuario());
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al asignar rol al usuario", e);
        } finally {
            em.close();
        }
    }

    public boolean usuarioTieneRol(Long usuarioId, String codigoRol) {
        EntityManager em = getEntityManager();
        try {
            Long total = em.createQuery(
                    "SELECT COUNT(ur) FROM UsuarioRol ur "
                    + "WHERE ur.usuario.id = :usuarioId "
                    + "AND UPPER(ur.rol.codigo) = :codigoRol",
                    Long.class
            ).setParameter("usuarioId", usuarioId)
                    .setParameter("codigoRol", codigoRol.toUpperCase())
                    .getSingleResult();

            return total != null && total > 0;
        } finally {
            em.close();
        }
    }

    public void asignarRolAUsuarioSiNoExiste(Long usuarioId, String codigoRol) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Usuario usuario = em.find(Usuario.class, usuarioId);
            Rol rol = buscarRolPorCodigo(em, codigoRol)
                    .orElseThrow(() -> new IllegalStateException("No existe el rol " + codigoRol));

            Long total = em.createQuery(
                    "SELECT COUNT(ur) FROM UsuarioRol ur "
                    + "WHERE ur.usuario.id = :usuarioId "
                    + "AND ur.rol.id = :rolId",
                    Long.class
            ).setParameter("usuarioId", usuarioId)
                    .setParameter("rolId", rol.getId())
                    .getSingleResult();

            if (usuario != null && (total == null || total == 0)) {
                em.persist(new UsuarioRol(usuario, rol));
                auditoriaDAO.registrar("usuario_roles", usuarioId, "INSERT", "Asignación de rol " + rol.getCodigo() + " al usuario " + usuario.getNombreUsuario());
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al asignar rol al usuario", e);
        } finally {
            em.close();
        }
    }

    public void asignarTodosLosPermisosARolSiNoExisten(String codigoRol) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Rol rol = buscarRolPorCodigo(em, codigoRol)
                    .orElseThrow(() -> new IllegalStateException("No existe el rol " + codigoRol));
            List<Permiso> permisos = em.createQuery("SELECT p FROM Permiso p", Permiso.class)
                    .getResultList();

            for (Permiso permiso : permisos) {
                Long total = em.createQuery(
                        "SELECT COUNT(rp) FROM RolPermiso rp "
                        + "WHERE rp.rol.id = :rolId "
                        + "AND rp.permiso.id = :permisoId",
                        Long.class
                ).setParameter("rolId", rol.getId())
                        .setParameter("permisoId", permiso.getId())
                        .getSingleResult();

                if (total == null || total == 0) {
                    RolPermiso rolPermiso = new RolPermiso();
                    rolPermiso.setRol(rol);
                    rolPermiso.setPermiso(permiso);
                    em.persist(rolPermiso);
                }
            }
            auditoriaDAO.registrar("rol_permisos", rol.getId(), "UPDATE", "Asignación masiva de permisos al rol " + rol.getCodigo());

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al asignar permisos al rol", e);
        } finally {
            em.close();
        }
    }

    public long contarRolesActivos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(r) FROM Rol r WHERE r.activo = true", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarPermisos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Permiso p", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarModulosConPermisos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(DISTINCT p.modulo) FROM Permiso p", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarAsignacionesRolPermiso() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(rp) FROM RolPermiso rp", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private Optional<Rol> buscarRolPorCodigo(EntityManager em, String codigoRol) {
        try {
            Rol rol = em.createQuery(
                    "SELECT r FROM Rol r WHERE UPPER(r.codigo) = :codigo",
                    Rol.class
            ).setParameter("codigo", codigoRol.toUpperCase())
                    .getSingleResult();
            return Optional.of(rol);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Rol> listarRolesPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();

        try {
            return em.createQuery(
                    "SELECT r FROM Rol r ORDER BY r.nombre",
                    Rol.class
            )
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

    public long contarRoles() {
        EntityManager em = getEntityManager();

        try {
            return em.createQuery(
                    "SELECT COUNT(r) FROM Rol r",
                    Long.class
            ).getSingleResult();

        } finally {
            em.close();
        }
    }
}
