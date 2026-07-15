package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Empleado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class EmpleadoDAO extends BaseDAO {

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Empleado> listarPaginado(int inicio, int limite) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT e FROM Empleado e "
                    + "JOIN FETCH e.sede "
                    + "JOIN FETCH e.area "
                    + "JOIN FETCH e.cargo "
                    + "LEFT JOIN FETCH e.especialidad "
                    + "LEFT JOIN FETCH e.usuario "
                    + "ORDER BY e.apellidoPaterno, e.nombres",
                    Empleado.class)
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

    public List<Empleado> listarFiltrado(int inicio, int limite, Long cargoId, Long areaId, String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean activo = resolverActivo(estado);
            return em.createQuery(
                    "SELECT e FROM Empleado e "
                    + "JOIN FETCH e.sede "
                    + "JOIN FETCH e.area "
                    + "JOIN FETCH e.cargo "
                    + "LEFT JOIN FETCH e.especialidad "
                    + "LEFT JOIN FETCH e.usuario "
                    + "WHERE (:cargoId IS NULL OR e.cargo.id = :cargoId) "
                    + "AND (:areaId IS NULL OR e.area.id = :areaId) "
                    + "AND (:activo IS NULL OR e.activo = :activo) "
                    + "AND (:filtro = '' OR LOWER(e.codigoEmpleado) LIKE :like OR LOWER(e.nombres) LIKE :like "
                    + "OR LOWER(e.apellidoPaterno) LIKE :like OR LOWER(e.apellidoMaterno) LIKE :like OR e.dni LIKE :like) "
                    + "ORDER BY e.apellidoPaterno, e.nombres",
                    Empleado.class)
                    .setParameter("cargoId", cargoId)
                    .setParameter("areaId", areaId)
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

    public long contarFiltrado(Long cargoId, Long areaId, String estado, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            Boolean activo = resolverActivo(estado);
            return em.createQuery(
                    "SELECT COUNT(e) FROM Empleado e WHERE "
                    + "(:cargoId IS NULL OR e.cargo.id = :cargoId) "
                    + "AND (:areaId IS NULL OR e.area.id = :areaId) "
                    + "AND (:activo IS NULL OR e.activo = :activo) "
                    + "AND (:filtro = '' OR LOWER(e.codigoEmpleado) LIKE :like OR LOWER(e.nombres) LIKE :like "
                    + "OR LOWER(e.apellidoPaterno) LIKE :like OR LOWER(e.apellidoMaterno) LIKE :like OR e.dni LIKE :like)",
                    Long.class)
                    .setParameter("cargoId", cargoId)
                    .setParameter("areaId", areaId)
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
                    "SELECT e.codigoEmpleado FROM Empleado e WHERE e.codigoEmpleado LIKE :patron ORDER BY e.codigoEmpleado DESC",
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
            return em.createQuery("SELECT COUNT(e) FROM Empleado e", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarActivos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(e) FROM Empleado e WHERE e.activo = true", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public Empleado buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT e FROM Empleado e "
                    + "JOIN FETCH e.sede "
                    + "JOIN FETCH e.area "
                    + "JOIN FETCH e.cargo "
                    + "LEFT JOIN FETCH e.especialidad "
                    + "LEFT JOIN FETCH e.usuario "
                    + "WHERE e.id = :id",
                    Empleado.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Empleado buscarPorUsuarioId(Long usuarioId) {
        EntityManager em = getEntityManager();
        try {
            List<Empleado> empleados = em.createQuery(
                    "SELECT e FROM Empleado e "
                    + "JOIN FETCH e.sede "
                    + "JOIN FETCH e.area "
                    + "JOIN FETCH e.cargo "
                    + "LEFT JOIN FETCH e.especialidad "
                    + "LEFT JOIN FETCH e.usuario "
                    + "WHERE e.usuario.id = :usuarioId",
                    Empleado.class)
                    .setParameter("usuarioId", usuarioId)
                    .setMaxResults(1)
                    .getResultList();
            return empleados.isEmpty() ? null : empleados.get(0);
        } finally {
            em.close();
        }
    }

    public void guardar(Empleado empleado) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (empleado.getId() == null) {
                em.persist(empleado);
                em.flush();
                auditoriaDAO.registrar("empleados", empleado.getId(), "INSERT", "Registro de empleado " + empleado.getCodigoEmpleado());
            } else {
                em.merge(empleado);
                auditoriaDAO.registrar("empleados", empleado.getId(), "UPDATE", "Actualización de empleado " + empleado.getCodigoEmpleado());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el empleado", e);
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
