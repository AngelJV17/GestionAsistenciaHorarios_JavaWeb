/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hospital.gestionasistenciashorarioshospital.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Angel
 */
public class DatabaseConnection {
    private static final String URL =
            "jdbc:mysql://localhost:3306/hospital_bd?useSSL=false&serverTimezone=America/Lima";

    private static final String USER = "root";

    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el Driver de MySQL", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
