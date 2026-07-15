/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hospital.gestionasistenciashorarioshospital.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author Angel
 */
public class PasswordUtil {

    /**
     * Genera el hash BCrypt
     * @param password
     * @return 
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Valida contraseña contra hash
     * @param password
     * @param hash
     * @return 
     */
    public static boolean verifyPassword(String password,
            String hash) {

        return BCrypt.checkpw(password, hash);
    }

}
