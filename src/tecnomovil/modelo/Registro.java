package tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro inmutable generado por validadores, sensores o aplicaciones.
 * Al ser un record, sus campos son final y no pueden modificarse.
 */
public record Registro(
        String idUsuario,
        String ruta,
        String estacion,
        Accion accion,
        LocalDateTime timestamp) {

    // Constructor compacto: valida que ningún dato llegue nulo.
    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario no puede ser nulo");
        Objects.requireNonNull(ruta, "ruta no puede ser nula");
        Objects.requireNonNull(estacion, "estacion no puede ser nula");
        Objects.requireNonNull(accion, "accion no puede ser nula");
        Objects.requireNonNull(timestamp, "timestamp no puede ser nulo");
    }

    /** Hora del día (0-23) en la que ocurrió el registro. */
    public int hora() {
        return timestamp.getHour();
    }

    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }

    public boolean esSalida() {
        return accion == Accion.SALIDA;
    }
}
