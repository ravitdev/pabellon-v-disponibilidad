# Registro de validación

Comprobaciones realizadas el 27 de octubre de 2026 sobre esta implementación.

## Compilación y pruebas

- JDK OpenJDK 21.0.12.1 y Maven 3.9.9 mediante Maven Wrapper 3.3.4.
- `./mvnw clean verify`: **BUILD SUCCESS**.
- **30 pruebas, 0 fallos, 0 errores, 0 omitidas**: 29 casos de aplicación y 1 de persistencia.
- El entorno Cloud requirió instalar temporalmente el JDK completo y configurar Maven con el proxy de sesión. Se utilizó `JAVA_HOME=/tmp/pabellon-jdk21 ./mvnw -s /tmp/maven-settings.xml -B clean verify`. Estas rutas son exclusivas del entorno de desarrollo y no forman parte del proyecto.
- Después de dar formato a las vistas y CSS se volvió a empaquetar con `./mvnw -o -B -DskipTests package` y se comprobaron las vistas mediante HTTP real.
- Los archivos H2 de las pruebas se crean en directorios temporales, separados de la base de la aplicación.

La prueba de persistencia crea los 19 laboratorios iniciales, importa otro conjunto, cierra el contexto Spring y su conexión, reabre el mismo archivo H2 y comprueba ambientes, actividades, fecha de actualización y ausencia de reinicialización automática.

## Arranque y HTTP real

Se ejecutó `java -jar target/pabellon-v-1.0.0.jar` con la configuración predeterminada y se verificó:

| Solicitud | Resultado comprobado |
| --- | --- |
| `GET /` | HTTP 200, 19 ambientes y aviso de datos ficticios |
| Consulta lunes 26/10/2026, 09:00–11:00 | HTTP 200, 3 ambientes sin actividades |
| Consulta lunes 26/10/2026, 10:00–11:00 | HTTP 200, 19 ambientes sin actividades |
| `GET /ambientes/V-101` | HTTP 200 y horario semanal |
| `GET /importar` | HTTP 200 |
| `GET /css/app.css` | HTTP 200 |
| `GET /ejemplos/horarios-ficticios.csv` | HTTP 200 |
| `GET /ambientes/V-999` | HTTP 404 |
| `POST /importar` con CSV de intervalo invertido | Redirección y mensaje de rechazo visible al seguirla con la cookie de sesión |

El sandbox inicial bloqueaba sockets locales; el arranque y las solicitudes HTTP se ejecutaron con el permiso de red correspondiente. No fue necesario alterar la aplicación.

## Requisitos cubiertos

| Documento académico | Implementación |
| --- | --- |
| RF-01 | Listado ordenado con código, tipo y piso |
| RF-02 / RF-03 | Consulta por fecha e intervalo, estados y filtro de disponibles |
| RF-04 | Programación semanal ordenada por día e inicio |
| RF-05 | Importación CSV transaccional, reemplazo completo y ambientes sin actividades |
| RF-06 | Fecha persistida de última importación correcta, presentada en hora de Lima |
| RNF-02 | Avisos de disponibilidad según horarios, datos ficticios y origen no verificado |
| RNF-01 | Diseño adaptable con HTML/CSS estándar; pendiente verificación manual en los navegadores objetivo |

## Límites de esta verificación

No se ejecutó la aplicación en Windows ni se hicieron pruebas visuales o automatizadas con Chrome, Firefox, Edge o un dispositivo móvil real. Las instrucciones de Windows están documentadas, pero su ejecución en ese sistema queda pendiente de comprobación. Tampoco se contrastaron los datos ficticios con horarios reales de la universidad.
