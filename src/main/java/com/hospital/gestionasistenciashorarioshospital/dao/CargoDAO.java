/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.modelo.Cargo;
import jakarta.persistence.*;
import java.util.Collections;
import java.util.List;

public class CargoDAO extends BaseDAO {
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<Cargo> listarTodos() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery(
                    "SELECT c FROM Cargo c ORDER BY c.nombre",
                    Cargo.class
            ).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public Cargo buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Cargo.class, id);
        } finally {
            em.close();
        }
    }

    public void guardar(Cargo cargo) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (cargo.getId() == null) {
                em.persist(cargo);
                em.flush();
                auditoriaDAO.registrar("cargos", cargo.getId(), "INSERT", "Registro de cargo " + cargo.getCodigo());
            } else {
                em.merge(cargo);
                auditoriaDAO.registrar("cargos", cargo.getId(), "UPDATE", "Actualización de cargo " + cargo.getCodigo());
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al guardar el cargo", e);
        } finally {
            em.close();
        }
    }

    public void eliminar(Long id) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Cargo cargo = em.find(Cargo.class, id);
            if (cargo != null) {
                em.remove(cargo);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Error al eliminar el cargo", e);
        } finally {
            em.close();
        }
    }
}
