/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hospital.gestionasistenciashorarioshospital.dao;

import com.hospital.gestionasistenciashorarioshospital.util.JPAUtil;
import jakarta.persistence.EntityManager;

/**
 *
 * @author Angel
 */
public class BaseDAO {
    protected EntityManager getEntityManager() {
        return JPAUtil.getEntityManager();
    }
}
