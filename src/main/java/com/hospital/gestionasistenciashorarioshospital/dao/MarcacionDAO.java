package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Marcacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MarcacionDAO extends BaseDAO {

    public void guardar(Marcacion marcacion) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(marcacion);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar la marcacion", e);
        } finally {
            em.close();
        }
    }

    public boolean existeMarcacionHoy(Long empleadoId, Long tipoMarcacionId) {
        EntityManager em = getEntityManager();
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        try {
            Long total = em.createQuery(
                    "SELECT COUNT(m) FROM Marcacion m "
                    + "WHERE m.empleadoId = :empleadoId "
                    + "AND m.tipoMarcacionId = :tipoMarcacionId "
                    + "AND m.fechaHora >= :inicio AND m.fechaHora < :fin",
                    Long.class)
                    .setParameter("empleadoId", empleadoId)
                    .setParameter("tipoMarcacionId", tipoMarcacionId)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getSingleResult();
            return total > 0;
        } finally {
            em.close();
        }
    }
}
