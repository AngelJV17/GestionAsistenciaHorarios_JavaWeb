package com.hospital.gestionasistenciashorarioshospital.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Collections;
import java.util.List;

public class SimpleEntityDAO<T> extends BaseDAO {

    private final Class<T> entityClass;
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();
    private final String entityName;

    public SimpleEntityDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.entityName = entityClass.getSimpleName();
    }

    public List<T> listarTodos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT e FROM " + entityName + " e ORDER BY e.nombre", entityClass)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public T buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(entityClass, id);
        } finally {
            em.close();
        }
    }

    public void guardar(T entidad) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T guardada = em.merge(entidad);
            em.flush();
            tx.commit();
            auditoriaDAO.registrar(entityClass.getSimpleName().toLowerCase(), obtenerId(guardada), "UPSERT",
                    "Registro o actualización de " + entityClass.getSimpleName());
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar " + entityName, e);
        } finally {
            em.close();
        }
    }

    private Long obtenerId(T entidad) {
        try {
            Object id = entidad.getClass().getMethod("getId").invoke(entidad);
            return id instanceof Long ? (Long) id : null;
        } catch (Exception e) {
            return null;
        }
    }
}
