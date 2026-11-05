package tecnomovil;

import tecnomovil.analisis.AnalizadorTransporte;
import tecnomovil.analisis.AnalizadorTransporte.EstadoRuta;
import tecnomovil.analisis.AnalizadorTransporte.FlujoHora;
import tecnomovil.analisis.AnalizadorTransporte.TiempoTramo;
import tecnomovil.analisis.AnalizadorTransporte.UsoRuta;
import tecnomovil.datos.GeneradorDatos;
import tecnomovil.modelo.Accion;
import tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static tecnomovil.analisis.AnalizadorTransporte.afluenciaPorEstacion;
import static tecnomovil.analisis.AnalizadorTransporte.detectarSobrecarga;
import static tecnomovil.analisis.AnalizadorTransporte.flujoPorHora;
import static tecnomovil.analisis.AnalizadorTransporte.horasPico;
import static tecnomovil.analisis.AnalizadorTransporte.patronesDeViaje;
import static tecnomovil.analisis.AnalizadorTransporte.rutasMasUtilizadas;
import static tecnomovil.analisis.AnalizadorTransporte.tiempoPromedioEntreEstaciones;

/**
 * Pruebas de verificación sin frameworks externos (no se usa JUnit).
 *
 * Se trabaja con un conjunto pequeño de 12 registros cuyos resultados se calcularon
 * a mano. Cada prueba compara el resultado del analizador con el valor esperado.
 *
 * Datos de prueba (día 2026-11-04):
 *   U1: R1 Central->Norte 07:00-07:05  y  R2 Plaza->Mercado 17:00-17:10
 *   U2: R1 Central->Universidad 07:30-07:40
 *   U3: R1 Norte->Hospital 08:00-08:15
 *   U4: R2 Plaza->Estadio 07:10-07:20
 *   U5: R1 Central->Norte 09:00-09:15
 */
public class PruebasAnalizador {

    private record Prueba(String nombre, BooleanSupplier verificacion) { }

    private record Resultado(String nombre, boolean correcta) { }

    private static final LocalDate DIA = LocalDate.of(2026, 11, 4);

    private static Registro reg(String usuario, String ruta, String estacion, Accion accion, String hora) {
        return new Registro(usuario, ruta, estacion, accion, LocalDateTime.of(DIA, LocalTime.parse(hora)));
    }

    /** Datos de prueba, en orden cronológico. */
    private static final List<Registro> DATOS = List.of(
            reg("U1", "R1", "Central",     Accion.ENTRADA, "07:00"),
            reg("U1", "R1", "Norte",       Accion.SALIDA,  "07:05"),
            reg("U4", "R2", "Plaza",       Accion.ENTRADA, "07:10"),
            reg("U4", "R2", "Estadio",     Accion.SALIDA,  "07:20"),
            reg("U2", "R1", "Central",     Accion.ENTRADA, "07:30"),
            reg("U2", "R1", "Universidad", Accion.SALIDA,  "07:40"),
            reg("U3", "R1", "Norte",       Accion.ENTRADA, "08:00"),
            reg("U3", "R1", "Hospital",    Accion.SALIDA,  "08:15"),
            reg("U5", "R1", "Central",     Accion.ENTRADA, "09:00"),
            reg("U5", "R1", "Norte",       Accion.SALIDA,  "09:15"),
            reg("U1", "R2", "Plaza",       Accion.ENTRADA, "17:00"),
            reg("U1", "R2", "Mercado",     Accion.SALIDA,  "17:10"));

    private static final List<TiempoTramo> TIEMPOS_ESPERADOS = List.of(
            new TiempoTramo("R1", "Central", "Norte", 10.0, 2),
            new TiempoTramo("R1", "Central", "Universidad", 10.0, 1),
            new TiempoTramo("R1", "Norte", "Hospital", 15.0, 1),
            new TiempoTramo("R2", "Plaza", "Estadio", 10.0, 1),
            new TiempoTramo("R2", "Plaza", "Mercado", 10.0, 1));

    // ------------------------------------------------------------------
    // Utilidades de prueba
    // ------------------------------------------------------------------

    /** Devuelve true si la acción lanza la excepción esperada. */
    private static boolean lanza(Class<? extends Throwable> tipo, Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (Throwable e) {
            return tipo.isInstance(e);
        }
    }

    private static boolean ejecutar(Prueba prueba) {
        try {
            return prueba.verificacion().getAsBoolean();
        } catch (RuntimeException e) {
            return false; // una excepción inesperada cuenta como prueba fallida
        }
    }

    // ------------------------------------------------------------------
    // Catálogo de pruebas
    // ------------------------------------------------------------------

    private static List<Prueba> pruebas() {
        return List.of(
                // --- Tareas a), b) y c) ---
                new Prueba("a) afluencia por estación",
                        () -> afluenciaPorEstacion(DATOS.stream())
                                .equals(Map.of("Central", 3L, "Norte", 1L, "Plaza", 2L))),

                new Prueba("b) flujo por hora",
                        () -> flujoPorHora(DATOS.stream())
                                .equals(Map.of(7, 3L, 8, 1L, 9, 1L, 17, 1L))),

                new Prueba("b) horas pico con desempate por hora más temprana",
                        () -> horasPico(DATOS.stream(), 3)
                                .equals(List.of(new FlujoHora(7, 3), new FlujoHora(8, 1), new FlujoHora(9, 1)))),

                new Prueba("c) rutas más utilizadas",
                        () -> rutasMasUtilizadas(DATOS.stream(), 5)
                                .equals(List.of(new UsoRuta("R1", 4), new UsoRuta("R2", 2)))),

                new Prueba("c) el límite recorta la lista",
                        () -> rutasMasUtilizadas(DATOS.stream(), 1)
                                .equals(List.of(new UsoRuta("R1", 4)))),

                // --- Tareas d), e) y f) ---
                new Prueba("d) patrones de viaje en orden de aparición",
                        () -> patronesDeViaje(DATOS.stream()).equals(Map.of(
                                "U1", List.of("Central", "Norte", "Plaza", "Mercado"),
                                "U2", List.of("Central", "Universidad"),
                                "U3", List.of("Norte", "Hospital"),
                                "U4", List.of("Plaza", "Estadio"),
                                "U5", List.of("Central", "Norte")))),

                new Prueba("e) tiempo promedio entre estaciones",
                        () -> tiempoPromedioEntreEstaciones(DATOS.stream()).equals(TIEMPOS_ESPERADOS)),

                new Prueba("e) funciona aunque los registros lleguen desordenados",
                        () -> tiempoPromedioEntreEstaciones(
                                DATOS.stream()
                                        .sorted(Comparator.comparing(Registro::timestamp).reversed()))
                                .equals(TIEMPOS_ESPERADOS)),

                new Prueba("f) rutas críticas con umbral 1",
                        () -> detectarSobrecarga(DATOS.stream(), 1).equals(List.of(
                                new EstadoRuta("R1", 2, 7, true),
                                new EstadoRuta("R2", 1, 7, false)))),

                new Prueba("f) el umbral es estricto: igualarlo no es crítico",
                        () -> detectarSobrecarga(DATOS.stream(), 2).stream()
                                .noneMatch(EstadoRuta::critica)),

                // --- Propiedades de la programación funcional ---
                new Prueba("pureza: los registros originales no se modifican",
                        () -> {
                            List<Registro> original = new ArrayList<>(DATOS);
                            List<Registro> copia = List.copyOf(original);
                            afluenciaPorEstacion(original.stream());
                            horasPico(original.stream(), 3);
                            rutasMasUtilizadas(original.stream(), 3);
                            patronesDeViaje(original.stream());
                            tiempoPromedioEntreEstaciones(original.stream());
                            detectarSobrecarga(original.stream(), 1);
                            return original.equals(copia);
                        }),

                new Prueba("pureza: misma entrada produce siempre el mismo resultado",
                        () -> tiempoPromedioEntreEstaciones(DATOS.stream())
                                .equals(tiempoPromedioEntreEstaciones(DATOS.stream()))
                                && detectarSobrecarga(DATOS.stream(), 1)
                                .equals(detectarSobrecarga(DATOS.stream(), 1))),

                new Prueba("inmutabilidad: los resultados no se pueden modificar",
                        () -> lanza(UnsupportedOperationException.class,
                                () -> afluenciaPorEstacion(DATOS.stream()).put("X", 1L))
                                && lanza(UnsupportedOperationException.class,
                                () -> horasPico(DATOS.stream(), 2).add(new FlujoHora(0, 0)))
                                && lanza(UnsupportedOperationException.class,
                                () -> patronesDeViaje(DATOS.stream()).get("U1").add("X"))
                                && lanza(UnsupportedOperationException.class,
                                () -> tiempoPromedioEntreEstaciones(DATOS.stream()).clear())),

                new Prueba("paralelización: resultado paralelo igual al secuencial",
                        () -> afluenciaPorEstacion(DATOS.stream())
                                .equals(afluenciaPorEstacion(DATOS.parallelStream()))
                                && horasPico(DATOS.stream(), 3)
                                .equals(horasPico(DATOS.parallelStream(), 3))
                                && rutasMasUtilizadas(DATOS.stream(), 3)
                                .equals(rutasMasUtilizadas(DATOS.parallelStream(), 3))
                                && patronesDeViaje(DATOS.stream())
                                .equals(patronesDeViaje(DATOS.parallelStream()))
                                && tiempoPromedioEntreEstaciones(DATOS.stream())
                                .equals(tiempoPromedioEntreEstaciones(DATOS.parallelStream()))
                                && detectarSobrecarga(DATOS.stream(), 1)
                                .equals(detectarSobrecarga(DATOS.parallelStream(), 1))),

                // --- Modelo y generador ---
                new Prueba("Registro rechaza valores nulos",
                        () -> lanza(NullPointerException.class,
                                () -> new Registro(null, "R1", "Central", Accion.ENTRADA, LocalDateTime.now()))),

                new Prueba("Accion.desdeTexto convierte \"entrada\" y \"salida\"",
                        () -> Accion.desdeTexto("entrada") == Accion.ENTRADA
                                && Accion.desdeTexto("SALIDA") == Accion.SALIDA
                                && lanza(IllegalArgumentException.class, () -> Accion.desdeTexto("otra"))),

                new Prueba("generador: misma semilla produce los mismos datos",
                        () -> GeneradorDatos.generar(50, 1L, DIA).equals(GeneradorDatos.generar(50, 1L, DIA))),

                new Prueba("generador: hay tantas entradas como salidas",
                        () -> {
                            List<Registro> datos = GeneradorDatos.generar(200, 3L, DIA);
                            return datos.stream().filter(Registro::esEntrada).count()
                                    == datos.stream().filter(Registro::esSalida).count();
                        }));
    }

    // ------------------------------------------------------------------

    public static void main(String[] args) {
        List<Resultado> resultados = pruebas().stream()
                .map(p -> new Resultado(p.nombre(), ejecutar(p)))
                .toList();

        resultados.forEach(r ->
                System.out.printf("[%s] %s%n", r.correcta() ? " OK  " : "FALLA", r.nombre()));

        long correctas = resultados.stream().filter(Resultado::correcta).count();
        System.out.printf("%nPruebas correctas: %d de %d%n", correctas, resultados.size());

        if (correctas != resultados.size()) {
            System.exit(1); // código de salida distinto de cero si alguna prueba falla
        }
    }
}
