package tecnomovil.analisis;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import tecnomovil.modelo.Registro;

/**
 * Operaciones funcionales de análisis sobre los registros de transporte.
 *
 * Todos los métodos son funciones puras: no modifican la entrada, no guardan estado
 * y no producen efectos secundarios. Reciben un Stream para que quien los llame
 * decida si procesa de forma secuencial (lista.stream()) o paralela (lista.parallelStream()).
 */
public final class AnalizadorTransporte {

    /** Cantidad de pasajeros que ingresaron en una hora del día. */
    public record FlujoHora(int hora, long ingresos) { }

    /** Cantidad de pasajeros que ingresaron a una ruta. */
    public record UsoRuta(String ruta, long ingresos) { }

    /** Tiempo promedio (en minutos) entre dos estaciones consecutivas de una ruta. */
    public record TiempoTramo(String ruta, String origen, String destino,
                              double minutosPromedio, long viajes) { }

    /** Estado de una ruta: su ocupación máxima y si supera el umbral (crítica). */
    public record EstadoRuta(String ruta, long ocupacionPico, int horaPico, boolean critica) { }

    /** Un recorrido de un usuario entre dos estaciones (uso interno). */
    private record Trayecto(String ruta, String origen, String destino, long segundos) { }

    /** Clave de agrupación para un tramo (uso interno). */
    private record Tramo(String ruta, String origen, String destino) { }

    private AnalizadorTransporte() {
        // Clase de utilidades: no se instancia.
    }

    // ------------------------------------------------------------------
    // Función de orden superior reutilizada por las tareas a), b) y c)
    // ------------------------------------------------------------------

    /**
     * Cuenta los ingresos (acciones "entrada") agrupándolos según el criterio recibido.
     * El criterio es una función (Function), por eso este método es de orden superior.
     */
    private static <K> Map<K, Long> contarIngresosPor(
            Stream<Registro> registros,
            Function<Registro, K> clasificador) {

        return registros
                .filter(Registro::esEntrada)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(clasificador, Collectors.counting()),
                        Map::copyOf)); // copia inmutable del resultado
    }

    // ------------------------------------------------------------------
    // a) Afluencia por estación
    // ------------------------------------------------------------------

    /** Cuántos usuarios ingresan a cada estación durante el día. */
    public static Map<String, Long> afluenciaPorEstacion(Stream<Registro> registros) {
        return contarIngresosPor(registros, Registro::estacion);
    }

    // ------------------------------------------------------------------
    // b) Horas pico
    // ------------------------------------------------------------------

    /** Ingresos agrupados por hora del día (0-23). */
    public static Map<Integer, Long> flujoPorHora(Stream<Registro> registros) {
        return contarIngresosPor(registros, Registro::hora);
    }

    /** Las "cantidad" horas con mayor flujo, de la más concurrida a la menos concurrida. */
    public static List<FlujoHora> horasPico(Stream<Registro> registros, int cantidad) {
        return flujoPorHora(registros).entrySet().stream()
                .map(e -> new FlujoHora(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(FlujoHora::ingresos).reversed()
                        .thenComparingInt(FlujoHora::hora))
                .limit(cantidad)
                .toList();
    }

    // ------------------------------------------------------------------
    // c) Rutas más utilizadas
    // ------------------------------------------------------------------

    /** Las "cantidad" rutas con mayor volumen de uso, de mayor a menor. */
    public static List<UsoRuta> rutasMasUtilizadas(Stream<Registro> registros, int cantidad) {
        return contarIngresosPor(registros, Registro::ruta).entrySet().stream()
                .map(e -> new UsoRuta(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(UsoRuta::ingresos).reversed()
                        .thenComparing(UsoRuta::ruta))
                .limit(cantidad)
                .toList();
    }

    // ------------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // ------------------------------------------------------------------

    /**
     * Para cada usuario, la lista de estaciones en el orden en que aparecen sus registros.
     * Las listas internas y el mapa externo son inmutables.
     */
    public static Map<String, List<String>> patronesDeViaje(Stream<Registro> registros) {
        return registros.collect(Collectors.collectingAndThen(
                Collectors.groupingBy(
                        Registro::idUsuario,
                        Collectors.mapping(Registro::estacion, Collectors.toUnmodifiableList())),
                Map::copyOf));
    }

    // ------------------------------------------------------------------
    // e) Tiempo promedio entre estaciones
    // ------------------------------------------------------------------

    /**
     * Estima cuánto tardan los usuarios en moverse entre estaciones. Para cada usuario toma
     * sus registros en orden cronológico, une cada "entrada" con la "salida" que le sigue
     * (misma ruta) y promedia los tiempos por tramo.
     */
    public static List<TiempoTramo> tiempoPromedioEntreEstaciones(Stream<Registro> registros) {
        return registros
                .collect(Collectors.groupingBy(Registro::idUsuario))
                .values().stream()
                .flatMap(AnalizadorTransporte::trayectosDelUsuario)
                .collect(Collectors.groupingBy(
                        t -> new Tramo(t.ruta(), t.origen(), t.destino()),
                        Collectors.summarizingLong(Trayecto::segundos)))
                .entrySet().stream()
                .map(e -> new TiempoTramo(
                        e.getKey().ruta(), e.getKey().origen(), e.getKey().destino(),
                        e.getValue().getAverage() / 60.0,
                        e.getValue().getCount()))
                .sorted(Comparator.comparing(TiempoTramo::ruta)
                        .thenComparing(TiempoTramo::origen)
                        .thenComparing(TiempoTramo::destino))
                .toList();
    }

    /** Convierte los registros de un usuario en sus trayectos (entrada seguida de salida). */
    private static Stream<Trayecto> trayectosDelUsuario(List<Registro> delUsuario) {
        List<Registro> ordenados = delUsuario.stream()
                .sorted(Comparator.comparing(Registro::timestamp))
                .toList(); // copia ordenada: la lista original no se toca

        return IntStream.range(0, ordenados.size() - 1)
                .filter(i -> esTrayecto(ordenados.get(i), ordenados.get(i + 1)))
                .mapToObj(i -> crearTrayecto(ordenados.get(i), ordenados.get(i + 1)));
    }

    private static boolean esTrayecto(Registro inicio, Registro fin) {
        return inicio.esEntrada() && fin.esSalida() && inicio.ruta().equals(fin.ruta());
    }

    private static Trayecto crearTrayecto(Registro inicio, Registro fin) {
        return new Trayecto(inicio.ruta(), inicio.estacion(), fin.estacion(),
                Duration.between(inicio.timestamp(), fin.timestamp()).getSeconds());
    }

    // ------------------------------------------------------------------
    // f) Detección de sobrecarga en rutas
    // ------------------------------------------------------------------

    /**
     * Calcula la ocupación pico de cada ruta (la mayor cantidad de ingresos en una misma hora)
     * y la marca como crítica si supera el umbral. Se ordena de la más a la menos ocupada.
     */
    public static List<EstadoRuta> detectarSobrecarga(Stream<Registro> registros, long umbral) {
        return registros
                .filter(Registro::esEntrada)
                .collect(Collectors.groupingBy(
                        Registro::ruta,
                        Collectors.groupingBy(Registro::hora, Collectors.counting())))
                .entrySet().stream()
                .map(e -> estadoDeRuta(e.getKey(), e.getValue(), umbral))
                .sorted(Comparator.comparingLong(EstadoRuta::ocupacionPico).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    private static EstadoRuta estadoDeRuta(String ruta, Map<Integer, Long> ingresosPorHora, long umbral) {
        FlujoHora pico = ingresosPorHora.entrySet().stream()
                .map(e -> new FlujoHora(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(FlujoHora::ingresos).reversed()
                        .thenComparingInt(FlujoHora::hora))
                .findFirst()
                .orElseThrow();
        return new EstadoRuta(ruta, pico.ingresos(), pico.hora(), pico.ingresos() > umbral);
    }
}
