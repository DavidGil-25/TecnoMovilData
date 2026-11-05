# TecnoMovil Data

Módulo funcional en Java para procesar datos de transporte urbano de la ciudad ficticia de
TecnoValle. Fue desarrollado como caso de estudio de la Unidad 2 (programación funcional) de
la Tecnología en Desarrollo de Software, IU Digital de Antioquia.

El proyecto usa únicamente la biblioteca estándar de Java (sin frameworks ni dependencias externas)
y requiere **Java 17 o superior** (probado con Java 21).

## Integrantes

| Integrante | Tareas |
|------------|--------|
| *David Gil Del Rio* | a) Afluencia por estación, b) Horas pico, c) Rutas más utilizadas |
| *Valentina Garcia Agudelo* | d) Patrones de viaje, e) Tiempo promedio entre estaciones, f) Sobrecarga en rutas |

## Estructura del proyecto

```
src/tecnomovil/
├── modelo/
│   ├── Accion.java               enum ENTRADA / SALIDA
│   └── Registro.java             record inmutable con los datos de cada registro
├── datos/
│   └── GeneradorDatos.java       generador de datos simulados (reproducible con semilla)
├── analisis/
│   └── AnalizadorTransporte.java las seis tareas, como funciones puras
├── Main.java                     ejecuta todas las tareas y muestra los resultados
└── PruebasAnalizador.java        18 pruebas de verificación con datos calculados a mano
capturas/                         evidencias de ejecución
informe/                          informe final en PDF (APA 7.ª edición)
```

## Cómo compilar y ejecutar

Desde la carpeta raíz del proyecto.

**Windows (PowerShell):**

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin tecnomovil.Main
```

**Linux / macOS:**

```bash
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin tecnomovil.Main
```

**Pruebas de verificación** (sin JUnit, solo Java estándar):

```
java -cp bin tecnomovil.PruebasAnalizador
```

Al final debe aparecer `Pruebas correctas: 18 de 18`.

También se puede ejecutar desde VS Code abriendo `Main.java` y usando **Run** sobre el método `main`.

## Tareas implementadas

| Tarea | Método | Descripción |
|-------|--------|-------------|
| a) | `afluenciaPorEstacion` | Ingresos por estación durante el día |
| b) | `horasPico` | Horas con mayor flujo de pasajeros |
| c) | `rutasMasUtilizadas` | Rutas con mayor volumen de uso |
| d) | `patronesDeViaje` | Estaciones visitadas por cada usuario, en orden |
| e) | `tiempoPromedioEntreEstaciones` | Tiempo promedio de viaje entre estaciones |
| f) | `detectarSobrecarga` | Rutas críticas según un umbral de ocupación |

## Conceptos de programación funcional aplicados

- **Funciones puras:** los métodos de `AnalizadorTransporte` no modifican datos ni guardan estado.
- **Inmutabilidad:** `record`, `List.of`, `Map.copyOf` y `Stream.toList()`.
- **Lambdas y referencias a métodos:** por ejemplo `Registro::estacion`.
- **Funciones de orden superior:** `contarIngresosPor` recibe un `Function` como criterio de agrupación.
- **Streams:** pipelines con `filter`, `map`, `flatMap`, `sorted`, `limit` y `collect`.
- **Paralelización:** los métodos reciben un `Stream`, por lo que funcionan con `stream()` o `parallelStream()`.

## Resultado esperado

Con 1000 usuarios y semilla 42 se generan 5058 registros. Al final de la ejecución debe aparecer:

```
Resultado secuencial == paralelo: true
```
