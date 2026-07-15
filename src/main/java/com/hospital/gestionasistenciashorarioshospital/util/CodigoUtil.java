package com.hospital.gestionasistenciashorarioshospital.util;

import java.text.Normalizer;
import java.util.Locale;

public final class CodigoUtil {

    private CodigoUtil() {
    }

    public static String generarCodigoSecuencial(String prefijo, Long ultimoNumero, int longitud) {
        long siguiente = ultimoNumero == null ? 1L : ultimoNumero + 1L;
        return prefijo + String.format("%0" + longitud + "d", siguiente);
    }

    public static String normalizarCodigo(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return limpio.length() > 40 ? limpio.substring(0, 40) : limpio;
    }

    public static String normalizarUsuario(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }
}
