package com.sgb.dto;

/**
 * Expresiones regulares compartidas por los DTO para validar el CONTENIDO
 * de los textos (no solo su longitud). Se centralizan aqui para que el
 * mismo criterio se aplique en todos los formularios y en las pruebas.
 *
 * Agregado en la Evidencia GA7-220501096-AA3-EV02, donde la guia pide
 * documentar pruebas de "caracteres especiales".
 */
public final class ValidacionesTexto {

    /**
     * Nombres (por ejemplo, el nombre de un servicio): letras de cualquier
     * idioma (incluye tildes y la n con virgulilla), numeros, espacios y
     * los signos . , ( ) -
     */
    public static final String NOMBRE_PERMITIDO = "^[\\p{L}\\p{N} .,()\\-]*$";

    public static final String MENSAJE_NOMBRE =
            "El nombre solo admite letras, numeros, espacios y los signos . , ( ) -";

    /**
     * Texto libre (descripcion, notas): se permite casi todo, menos los
     * caracteres que sirven para escribir codigo HTML/JavaScript (< > { }).
     */
    public static final String TEXTO_LIBRE_SEGURO = "^[^<>{}]*$";

    public static final String MENSAJE_TEXTO_LIBRE = "El texto no puede contener los caracteres < > { }";

    /** Correo con usuario, @, dominio y extension (nombre@dominio.com). */
    public static final String CORREO = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$";

    private ValidacionesTexto() {
        // Clase de constantes: no se instancia.
    }
}
