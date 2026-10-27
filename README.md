# Disponibilidad del pabellón V

Aplicación académica para consultar aulas y laboratorios del pabellón V de la PUCP a partir de una programación semanal recurrente. Desarrollada para Ética y Responsabilidad Social Universitaria (1INF36).

**La disponibilidad corresponde exclusivamente a horarios registrados: no confirma ocupación física ni autoriza el uso de un ambiente.** Los datos iniciales son ficticios; no son horarios oficiales de la PUCP. No se conecta con sistemas internos de la universidad.

## Requisitos y tecnologías

- **JDK 21 completo** (incluye `java` y `javac`), por ejemplo Microsoft OpenJDK o Eclipse Temurin. No basta un JRE.
- Acceso a Internet en la primera compilación para descargar Maven y dependencias.
- Maven 3.9.9 mediante Maven Wrapper incluido; no requiere instalar Maven globalmente.
- Spring Boot 3.5.7, Spring MVC, Thymeleaf, Spring Data JPA, H2 en archivo y Apache Commons CSV.
- HTML5, CSS3 y JavaScript sin frameworks frontend ni recursos externos.
- Objetivos de compatibilidad: Chrome 130+, Firefox 131+ y Edge 130+.

## Ejecutar en Windows

1. Instala un JDK 21. Configura `JAVA_HOME` a la carpeta del JDK y agrega `%JAVA_HOME%\bin` al `PATH` si el instalador no lo hizo.
2. Descarga/clona este repositorio y abre PowerShell en su carpeta raíz (donde está `pom.xml`).
3. Verifica las herramientas:

   ```powershell
   java -version
   javac -version
   .\mvnw.cmd -version
   ```

   Los tres deben indicar Java 21. El Wrapper descarga Maven automáticamente.
4. Inicia la aplicación:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. Abre [http://localhost:8080](http://localhost:8080). Detén la aplicación con `Ctrl+C`.

Para compilar, ejecutar todas las pruebas y generar un ejecutable:

```powershell
.\mvnw.cmd clean verify
java -jar .\target\pabellon-v-1.0.0.jar
```

En Linux/macOS, usa `./mvnw` en lugar de `.\mvnw.cmd` y `java -jar target/pabellon-v-1.0.0.jar`. Siempre ejecuta desde la raíz del proyecto para reutilizar la misma base de datos. Si el puerto está ocupado, añade `--server.port=8081` al comando `java -jar`.

## Uso

- **Ambientes:** al abrir la aplicación se listan identificador, tipo y piso.
- **Disponibilidad:** selecciona fecha, inicio y fin; pulsa «Consultar ambientes». Se muestran estados para todos los ambientes, o solo los disponibles si activas el filtro. El resultado indica el intervalo consultado.
- **Horario semanal:** el enlace de cada ambiente muestra los días y horas registrados, ordenados.
- **Actualizar horarios:** carga un CSV UTF-8, identifica si contiene datos ficticios y confirma que comprendes el reemplazo. La aplicación valida e informa el resultado.
- El pie de cada página muestra la última actualización correcta en hora de Lima. Una importación rechazada no cambia esa fecha.

### Ejemplo reproducible

Con los datos iniciales, consulta el lunes **26/10/2026** de **09:00 a 11:00**: V-101 tiene una actividad de 08:00 a 10:00 y aparece con actividad programada. V-108, V-208 y V-303 no tienen actividades. De 10:00 a 11:00, V-101 está disponible según la programación. Puedes utilizar cualquier otro lunes: la programación es semanal.

## Arquitectura MVC y estructura

Una sola aplicación monolítica sirve las vistas, procesa las solicitudes y accede a H2:

```text
pom.xml                          Dependencias y compilación Java 21
mvnw / mvnw.cmd / .mvn/          Maven Wrapper
src/main/java/pe/edu/pucp/pabellon/
  PabellonApplication.java        Punto de entrada
  model/                         Ambiente, Actividad y Actualizacion (JPA)
  repository/                    Consultas y persistencia con Spring Data JPA
  service/                       Disponibilidad, validación/importación e inicialización
  controller/                    Solicitudes HTTP y coordinación de vistas
src/main/resources/
  application.properties         Configuración de H2, servidor y cargas
  templates/                     Vistas Thymeleaf y fragmentos compartidos
  static/css/                    Estilos adaptables
  static/js/                     Estado de carga del formulario
  static/ejemplos/                CSV ficticio descargable
src/test/java/                   Pruebas JUnit 5 y Spring Boot
```

`WebController` recibe formularios y coordina los servicios. `DisponibilidadService` valida el intervalo y consulta las actividades. `ImportacionService` analiza y valida el CSV y realiza el reemplazo transaccional. Los repositorios encapsulan el acceso a datos y las vistas solo presentan información.

### Cálculo de disponibilidad

La fecha determina el día semanal (lunes = 1, domingo = 7). Existe superposición cuando:

```text
inicioActividad < finConsulta Y finActividad > inicioConsulta
```

Los intervalos se interpretan como `[inicio, fin)`. Una actividad 08:00–10:00 se superpone con 09:00–11:00, pero no con 10:00–11:00. Se exige inicio anterior a fin. No se aceptan intervalos que crucen medianoche. Un ambiente sin actividades para ese día e intervalo figura disponible según los datos.

## H2 persistente e inicialización

La URL predeterminada es `jdbc:h2:file:./data/pabellon-v`; H2 genera `data/pabellon-v.mv.db`. No requiere servidor externo. `data/`, `*.mv.db` y `*.trace.db` están excluidos de Git. La consola web de H2 está desactivada.

En la primera ejecución, si no hay ambientes ni metadatos, se importa el CSV ficticio: **19 laboratorios** (8 en el primer piso, 8 en el segundo y 3 en el tercero), con varios días y actividades, además de ambientes sin actividades. Esta inicialización registra su fecha y marca los datos como ficticios. En arranques posteriores no se sobrescriben datos existentes.

Una importación válida reemplaza también los ambientes: los códigos ausentes del archivo desaparecen. No se conserva historial. Para respaldar, detén la aplicación y copia `data/`; restáurala en la misma ubicación con la aplicación detenida. Para volver deliberadamente a los ejemplos, respalda y elimina `data/` con la aplicación detenida. Esta acción elimina los datos locales.

Opcionalmente, configura `DB_URL` para cambiar la ruta, manteniendo el prefijo `jdbc:h2:file:`. Una misma base no debe abrirse desde dos instancias simultáneas.

## Contrato CSV

Archivo `.csv`, UTF-8 (con o sin BOM), separado por **comas**, hasta 2 MB y 10 000 filas de datos. Se admiten comillas CSV estándar. Excel debe exportar como CSV UTF-8 con comas; algunas configuraciones regionales exportan punto y coma y requieren cambiar el separador.

Cabecera obligatoria, exacta y en este orden:

```csv
ambiente,dia_semana,hora_inicio,hora_fin
V-201,Lunes,08:00,10:00
V-201,Miércoles,14:00,16:00
V-202,,,
```

Columna opcional `tipo`, siempre al final:

```csv
ambiente,dia_semana,hora_inicio,hora_fin,tipo
V-201,Lunes,08:00,10:00,Laboratorio
V-202,,,,Aula
```

Reglas:

- Código `V-` más tres dígitos, comenzando por piso 1, 2 o 3; se normaliza a mayúsculas. Es una convención del proyecto, no un inventario oficial.
- Sin columna `tipo`, se asigna **Laboratorio**; con ella, cada fila debe indicar **Aula** o **Laboratorio**. Un mismo código no puede tener tipos distintos.
- Días: lunes a domingo, con o sin tildes, sin distinguir mayúsculas.
- Horas estrictas `HH:mm` (00:00 a 23:59); inicio anterior a fin.
- Día, inicio y fin pueden estar **los tres vacíos** para declarar un ambiente sin actividades; una fila parcialmente vacía se rechaza.
- No se admiten registros idénticos, incluso tras normalizar día y código. Las actividades distintas que se superponen se admiten: ambas bloquean el intervalo correspondiente.
- El archivo debe tener al menos un ambiente; no se permite borrar todo cargando solo la cabecera.
- Las filas en blanco se ignoran. Se valida la estructura, codificación y cantidad de columnas.

**Política atómica:** primero se valida todo el CSV. Solo entonces una transacción reemplaza actividades, ambientes y fecha de actualización. Ante errores no se guarda ninguna fila y se conserva la fecha previa. La interfaz informa la primera fila errónea. El archivo cargado no se almacena.

Un CSV ficticio completo está en `src/main/resources/static/ejemplos/horarios-ficticios.csv` y se descarga desde la vista de carga. Para demostrar, conserva marcada la casilla de datos ficticios. Desmarcarla identifica los datos como cargados por el usuario, sin certificarlos como oficiales.

## Pruebas

```powershell
.\mvnw.cmd test
```

`verify` incluye las mismas pruebas y el empaquetado. Los informes quedan en `target/surefire-reports/`.

Las pruebas utilizan archivos H2 temporales, aislados de `data/`. Cubren listado, tipos, programación, selección del día, superposición parcial/total, límites contiguos, intervalos inválidos, CSV válido e inválido, UTF-8/BOM, tildes, duplicados, reemplazo completo y conservación de datos/fecha ante rechazos, vistas MVC y carga multipart. La prueba de persistencia cierra y reabre dos contextos Spring sobre el mismo archivo H2 para comprobar que no se repite la inicialización y se conserva la importación.

## Alcance y limitaciones

- Sin reservas, autenticación, notificaciones, sensores, IA, Docker ni integración institucional.
- Programación recurrente sin semestres, feriados ni excepciones por fecha. No hay verificación física ni autorización de acceso.
- No se garantiza integridad o veracidad académica de un CSV cargado; valida su contenido antes de usarlo.
- Sin autenticación por requisito: se escucha solo en `127.0.0.1` por defecto. Cualquier persona con acceso a esa aplicación local puede reemplazar los datos. Cada evaluador puede ejecutarla en su equipo.
- No se han ejecutado pruebas visuales en Chrome, Firefox o Edge ni en un dispositivo móvil real; se usan HTML/CSS estándar y diseño responsive, pero esa compatibilidad requiere validación manual.
- Los cambios futuros en entidades requieren revisar la evolución de esquema; `ddl-auto=update` simplifica este producto académico.

El registro detallado de resultados y límites de comprobación está en [docs/VALIDACION.md](docs/VALIDACION.md): se ejecutaron **30 pruebas sin fallos ni errores**, se generó el JAR y se comprobó su arranque con solicitudes HTTP reales.
