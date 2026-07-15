package com.hospital.gestionasistenciashorarioshospital.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CodigoUtilTest {

    @Test
    void generaCodigoSecuencialConCeros() {
        assertEquals("EMP000006", CodigoUtil.generarCodigoSecuencial("EMP", 5L, 6));
        assertEquals("DOC001", CodigoUtil.generarCodigoSecuencial("DOC", null, 3));
    }

    @Test
    void normalizaCodigoParaCatalogos() {
        assertEquals("LICENCIA_MEDICA", CodigoUtil.normalizarCodigo("Licencia Medica"));
        assertEquals("AREA_01", CodigoUtil.normalizarCodigo("Area #01"));
    }

    @Test
    void normalizaNombreDeUsuario() {
        assertEquals("agarcia", CodigoUtil.normalizarUsuario("A. Garcia"));
        assertEquals("joseperez", CodigoUtil.normalizarUsuario("Jose Perez"));
    }
}
