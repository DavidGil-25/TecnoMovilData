package tecnomovil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import tecnomovil.analisis.AnalizadorTransporte;
import tecnomovil.datos.GeneradorDatos;
import tecnomovil.modelo.Registro;

public class Main {

    /** Umbral simulado: más de 65 ingresos en una hora hace que una ruta sea crítica. */
    private static final long UMBRAL_CRITICO = 65;

    public static void main(String[] args) {
        List<Registro> registros = GeneradorDatos.generar(1000, 42L, LocalDate.of(2026, 11, 4));
        System.out.println("Total de registros: " + registros.size());

        // a) Afluencia por estación
        System.out.println("\n=== a) Afluencia por estación ===");
        Map<String, Long> afluencia = AnalizadorTransporte.afluenciaPorEstacion(registros.stream());
        afluencia.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, Long> e) -> e.getValue()).reversed()
                        .thenComparing(Map.Entry::getKey))
                .forEach(e -> System.out.printf("%-12s %5d ingresos%n", e.getKey(), e.getValue()));

        // b) Horas pico
        System.out.println("\n=== b) Horas pico (top 5) ===");
        AnalizadorTransporte.horasPico(registros.stream(), 5)
                .forEach(f -> System.out.printf("%02d:00 - %02d:59  %5d ingresos%n",
                        f.hora(), f.hora(), f.ingresos()));

        // c) Rutas más utilizadas
        System.out.println("\n=== c) Rutas más utilizadas ===");
        AnalizadorTransporte.rutasMasUtilizadas(registros.stream(), 5)
                .forEach(r -> System.out.printf("%-4s %5d ingresos%n", r.ruta(), r.ingresos()));

        // d) Patrones de viaje por usuario
        System.out.println("\n=== d) Patrones de viaje (primeros 5 usuarios) ===");
        AnalizadorTransporte.patronesDeViaje(registros.stream()).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(5)
                .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));

        // e) Tiempo promedio entre estaciones
        System.out.println("\n=== e) Tiempo promedio entre estaciones (ruta R1) ===");
        AnalizadorTransporte.tiempoPromedioEntreEstaciones(registros.stream()).stream()
                .filter(t -> t.ruta().equals("R1"))
                .forEach(t -> System.out.printf("%-12s -> %-12s %6.2f min (%d viajes)%n",
                        t.origen(), t.destino(), t.minutosPromedio(), t.viajes()));

        // f) Detección de sobrecarga en rutas
        System.out.println("\n=== f) Sobrecarga en rutas (umbral: " + UMBRAL_CRITICO + " ingresos/hora) ===");
        AnalizadorTransporte.detectarSobrecarga(registros.stream(), UMBRAL_CRITICO)
                .forEach(r -> System.out.printf("%-4s pico: %3d ingresos a las %02d:00  -> %s%n",
                        r.ruta(), r.ocupacionPico(), r.horaPico(),
                        r.critica() ? "CRÍTICA" : "normal"));

        // Verificación: el resultado paralelo debe ser idéntico al secuencial
        boolean igual = afluencia.equals(
                AnalizadorTransporte.afluenciaPorEstacion(registros.parallelStream()))
                && AnalizadorTransporte.horasPico(registros.stream(), 5)
                        .equals(AnalizadorTransporte.horasPico(registros.parallelStream(), 5))
                && AnalizadorTransporte.rutasMasUtilizadas(registros.stream(), 5)
                        .equals(AnalizadorTransporte.rutasMasUtilizadas(registros.parallelStream(), 5))
                && AnalizadorTransporte.patronesDeViaje(registros.stream())
                        .equals(AnalizadorTransporte.patronesDeViaje(registros.parallelStream()))
                && AnalizadorTransporte.tiempoPromedioEntreEstaciones(registros.stream())
                        .equals(AnalizadorTransporte.tiempoPromedioEntreEstaciones(registros.parallelStream()))
                && AnalizadorTransporte.detectarSobrecarga(registros.stream(), UMBRAL_CRITICO)
                        .equals(AnalizadorTransporte.detectarSobrecarga(registros.parallelStream(), UMBRAL_CRITICO));
        System.out.println("\nResultado secuencial == paralelo: " + igual);
    }
}
