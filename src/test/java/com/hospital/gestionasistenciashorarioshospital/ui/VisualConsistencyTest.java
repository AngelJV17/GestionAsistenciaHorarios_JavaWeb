package com.hospital.gestionasistenciashorarioshospital.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VisualConsistencyTest {

    @Test
    void cssTieneReglasParaCargaOrdenadaDeDocumentos() throws Exception {
        String css = Files.readString(Path.of("src/main/webapp/resources/css/style.css"));

        assertTrue(css.contains(".document-upload-layout"));
        assertTrue(css.contains(".document-upload-group"));
        assertTrue(css.contains(".document-upload-actions"));
    }

    @Test
    void loginEvitaDobleEnvioSinDeshabilitarPostbackJsf() throws Exception {
        String loginJs = Files.readString(Path.of("src/main/webapp/resources/js/login.js"));

        assertTrue(loginJs.contains("loginForm.dataset.submitted"));
        assertTrue(loginJs.contains("aria-disabled"));
        assertTrue(!loginJs.contains("submitButton.disabled = true"));
    }
}
