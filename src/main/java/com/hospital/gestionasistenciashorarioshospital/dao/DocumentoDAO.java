package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Documento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class DocumentoDAO extends BaseDAO {

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Documento> listarFiltrado(int inicio, int limite, Long empleadoId, Long tipoDocumentoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT d FROM Documento d "
                    + "JOIN FETCH d.empleado e "
                    + "JOIN FETCH d.tipoDocumento td "
                    + "WHERE (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND (:tipoDocumentoId IS NULL OR td.id = :tipoDocumentoId) "
                    + "AND (:filtro = '' OR LOWER(d.nombreArchivo) LIKE :like "
                    + "OR LOWER(d.descripcion) LIKE :like OR LOWER(td.nombre) LIKE :like "
                    + "OR LOWER(e.nombres) LIKE :like OR LOWER(e.apellidoPaterno) LIKE :like "
                    + "OR LOWER(e.codigoEmpleado) LIKE :like) "
                    + "ORDER BY d.fechaSubida DESC, d.id DESC",
                    Documento.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("tipoDocumentoId", tipoDocumentoId)
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

    public long contarFiltrado(Long empleadoId, Long tipoDocumentoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT COUNT(d) FROM Documento d "
                    + "JOIN d.empleado e JOIN d.tipoDocumento td "
                    + "WHERE (:empleadoId IS NULL OR e.id = :empleadoId) "
                    + "AND (:tipoDocumentoId IS NULL OR td.id = :tipoDocumentoId) "
                    + "AND (:filtro = '' OR LOWER(d.nombreArchivo) LIKE :like "
                    + "OR LOWER(d.descripcion) LIKE :like OR LOWER(td.nombre) LIKE :like "
                    + "OR LOWER(e.nombres) LIKE :like OR LOWER(e.apellidoPaterno) LIKE :like "
                    + "OR LOWER(e.codigoEmpleado) LIKE :like)",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("tipoDocumentoId", tipoDocumentoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Documento> listarPorEmpleado(int inicio, int limite, Long empleadoId, Long tipoDocumentoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT d FROM Documento d "
                    + "JOIN FETCH d.empleado e "
                    + "JOIN FETCH d.tipoDocumento td "
                    + "WHERE e.id = :empleadoId "
                    + "AND (:tipoDocumentoId IS NULL OR td.id = :tipoDocumentoId) "
                    + "AND (:filtro = '' OR LOWER(d.nombreArchivo) LIKE :like "
                    + "OR LOWER(d.descripcion) LIKE :like OR LOWER(td.nombre) LIKE :like) "
                    + "ORDER BY d.fechaSubida DESC, d.id DESC",
                    Documento.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("tipoDocumentoId", tipoDocumentoId)
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

    public long contarPorEmpleado(Long empleadoId, Long tipoDocumentoId, String busqueda) {
        EntityManager em = getEntityManager();
        try {
            String filtro = busqueda == null ? "" : busqueda.trim().toLowerCase();
            return em.createQuery(
                    "SELECT COUNT(d) FROM Documento d "
                    + "JOIN d.empleado e JOIN d.tipoDocumento td "
                    + "WHERE e.id = :empleadoId "
                    + "AND (:tipoDocumentoId IS NULL OR td.id = :tipoDocumentoId) "
                    + "AND (:filtro = '' OR LOWER(d.nombreArchivo) LIKE :like "
                    + "OR LOWER(d.descripcion) LIKE :like OR LOWER(td.nombre) LIKE :like)",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("tipoDocumentoId", tipoDocumentoId)
                    .setParameter("filtro", filtro)
                    .setParameter("like", "%" + filtro + "%")
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarVigentesPorEmpleado(Long empleadoId) {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(d) FROM Documento d "
                    + "WHERE d.empleado.id = :empleadoId "
                    + "AND (d.fechaVencimiento IS NULL OR d.fechaVencimiento >= CURRENT_DATE)",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarTotal() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(d) FROM Documento d", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long contarVigentes() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(d) FROM Documento d "
                    + "WHERE d.fechaVencimiento IS NULL OR d.fechaVencimiento >= CURRENT_DATE",
                    Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public Documento buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            List<Documento> documentos = em.createQuery(
                    "SELECT d FROM Documento d "
                    + "JOIN FETCH d.empleado "
                    + "JOIN FETCH d.tipoDocumento "
                    + "WHERE d.id = :id",
                    Documento.class)
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList();
            return documentos.isEmpty() ? null : documentos.get(0);
        } finally {
            em.close();
        }
    }

    public void guardar(Documento documento) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (documento.getId() == null) {
                em.persist(documento);
                em.flush();
                auditoriaDAO.registrar("documentos", documento.getId(), "INSERT",
                        "Registro de documento " + documento.getNombreArchivo());
            } else {
                em.merge(documento);
                auditoriaDAO.registrar("documentos", documento.getId(), "UPDATE",
                        "Actualizacion de documento " + documento.getNombreArchivo());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el documento", e);
        } finally {
            em.close();
        }
    }

    public void eliminar(Long id) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Documento documento = em.find(Documento.class, id);
            if (documento != null) {
                String nombreArchivo = documento.getNombreArchivo();
                em.remove(documento);
                auditoriaDAO.registrar("documentos", id, "DELETE",
                        "Eliminacion de documento " + nombreArchivo);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al eliminar el documento", e);
        } finally {
            em.close();
        }
    }
}
