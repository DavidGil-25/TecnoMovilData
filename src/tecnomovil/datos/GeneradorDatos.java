package tecnomovil.datos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import tecnomovil.modelo.Accion;
import tecnomovil.modelo.Registro;

/**
 * Genera datos simulados de viajes. Con la misma semilla produce siempre
 * los mismos registros, lo que hace los resultados reproducibles.
 */
public final class GeneradorDatos {

    // Rutas y sus estaciones, en orden de recorrido (colecciones inmutables).
    private static final Map<String, List<String>> RUTAS = Map.of(
            "R1", List.of("Central", "Norte", "Universidad", "Hospital", "Terminal"),
            "R2", List.of("Central", "Plaza", "Mercado", "Estadio", "Aeropuerto"),
            "R3", List.of("Terminal", "Parque", "Biblioteca", "Museo", "Central"),
            "R4", List.of("Norte", "Industrial", "Feria", "Plaza", "Sur"),
            "R5", List.of("Sur", "Mercado", "Universidad", "Parque", "Aeropuerto"));

    // Lista aparte para tener un orden fijo (el orden de Map.of puede variar entre ejecuciones).
    private static final List<String> NOMBRES_RUTAS = List.of("R1", "R2", "R3", "R4", "R5");

    private static final List<Integer> HORAS_PICO = List.of(6, 7, 8, 17, 18, 19);

    private GeneradorDatos() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Genera los registros de un día para la cantidad de usuarios indicada.
     * Cada usuario hace entre 1 y 4 viajes; cada viaje produce una entrada y una salida.
     *
     * @return lista inmutable de registros ordenada por fecha y hora
     */
    public static List<Registro> generar(int numUsuarios, long semilla, LocalDate dia) {
        Random rnd = new Random(semilla);

        return IntStream.rangeClosed(1, numUsuarios)
                .mapToObj(n -> "U" + String.format("%05d", n))
                .flatMap(idUsuario -> viajesDeUsuario(idUsuario, rnd, dia))
                .sorted(Comparator.comparing(Registro::timestamp)
                        .thenComparing(Registro::idUsuario))
                .toList(); // Stream.toList() devuelve una lista inmutable
    }

    private static Stream<Registro> viajesDeUsuario(String idUsuario, Random rnd, LocalDate dia) {
        int numViajes = 1 + rnd.nextInt(4);
        // Horas distintas por viaje. Como inicia antes del minuto 35 y dura máximo 24 min,
        // termina dentro de la misma hora y los viajes de un usuario nunca se solapan.
        return Stream.generate(() -> elegirHora(rnd))
                .distinct()
                .limit(numViajes)
                .flatMap(hora -> crearViaje(idUsuario, rnd, dia, hora));
    }

    private static Stream<Registro> crearViaje(String idUsuario, Random rnd, LocalDate dia, int hora) {
        String ruta = NOMBRES_RUTAS.get(rnd.nextInt(NOMBRES_RUTAS.size()));
        List<String> estaciones = RUTAS.get(ruta);

        int origen = rnd.nextInt(estaciones.size() - 1);
        int destino = origen + 1 + rnd.nextInt(estaciones.size() - origen - 1);

        LocalDateTime inicio = dia.atTime(hora, rnd.nextInt(35), rnd.nextInt(60));
        int minutosPorTramo = 3 + rnd.nextInt(4); // entre 3 y 6 minutos por tramo
        LocalDateTime fin = inicio.plusMinutes((long) (destino - origen) * minutosPorTramo);

        return Stream.of(
                new Registro(idUsuario, ruta, estaciones.get(origen), Accion.ENTRADA, inicio),
                new Registro(idUsuario, ruta, estaciones.get(destino), Accion.SALIDA, fin));
    }

    /** El 60 % de los viajes ocurre en horas pico y el resto se reparte entre las 5 y las 22 h. */
    private static int elegirHora(Random rnd) {
        return rnd.nextDouble() < 0.6
                ? HORAS_PICO.get(rnd.nextInt(HORAS_PICO.size()))
                : 5 + rnd.nextInt(18);
    }
}
