package com.hospital.gestionasistenciashorarioshospital.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WebConfigurationTest {

    private static final Path WEB_XML = Path.of("src/main/webapp/WEB-INF/web.xml");
    private static final Path FACES_CONFIG = Path.of("src/main/webapp/WEB-INF/faces-config.xml");
    private static final Path INDEX_XHTML = Path.of("src/main/webapp/index.xhtml");

    @Test
    void webXmlTieneParametrosDeProduccionYDocumentos() throws Exception {
        String webXml = Files.readString(WEB_XML);

        assertTrue(webXml.contains("jakarta.faces.PROJECT_STAGE"));
        assertTrue(webXml.contains("<param-value>Production</param-value>"));
        assertTrue(webXml.contains("jakarta.faces.FACELETS_REFRESH_PERIOD"));
        assertTrue(webXml.contains("hospital.documentos.path"));
        assertTrue(webXml.contains("<multipart-config>"));
    }

    @Test
    void facesConfigConectaMensajesEnEspanol() throws Exception {
        String facesConfig = Files.readString(FACES_CONFIG);

        assertTrue(facesConfig.contains("<default-locale>es</default-locale>"));
        assertTrue(facesConfig.contains("com.hospital.gestionasistenciashorarioshospital.i18n.messages"));
        assertTrue(facesConfig.contains("<var>msg</var>"));
    }

    @Test
    void indexRedirigeAlLogin() throws Exception {
        String index = Files.readString(INDEX_XHTML);

        assertTrue(index.contains("inicioController.redirigirLogin"));
        assertTrue(index.contains("/views/login/login.xhtml"));
    }
}
