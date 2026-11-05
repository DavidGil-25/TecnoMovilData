package tecnomovil.modelo;

import java.util.Arrays;

/**
 * Tipos de acción posibles en un registro de transporte.
 * Se usa un enum en lugar de String para evitar errores de escritura.
 */
public enum Accion {
    ENTRADA("entrada"),
    SALIDA("salida");

    private final String etiqueta;

    Accion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Convierte "entrada" o "salida" (como en el enunciado) al enum correspondiente. */
    public static Accion desdeTexto(String texto) {
        return Arrays.stream(values())
                .filter(a -> a.etiqueta.equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Acción no válida: " + texto));
    }
}
