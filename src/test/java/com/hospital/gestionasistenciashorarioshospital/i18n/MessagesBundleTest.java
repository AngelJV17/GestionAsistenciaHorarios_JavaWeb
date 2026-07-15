package com.hospital.gestionasistenciashorarioshospital.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class MessagesBundleTest {

    @Test
    void cargaMensajesEnEspanol() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "com.hospital.gestionasistenciashorarioshospital.i18n.messages",
                Locale.forLanguageTag("es"));

        assertEquals("Iniciar sesión", bundle.getString("login.titulo"));
        assertEquals("El campo es obligatorio.", bundle.getString("jakarta.faces.component.UIInput.REQUIRED"));
        assertTrue(bundle.containsKey("documentos.admin.titulo"));
        assertTrue(bundle.containsKey("configuracion.medico.titulo"));
    }
}
