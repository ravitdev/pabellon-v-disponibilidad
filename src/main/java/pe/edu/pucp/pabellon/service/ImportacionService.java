package pe.edu.pucp.pabellon.service;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.pabellon.model.*;
import pe.edu.pucp.pabellon.repository.*;

@Service
public class ImportacionService {
  private final AmbienteRepository ambientes;
  private final ActividadRepository actividades;
  private final ActualizacionRepository actualizaciones;

  public ImportacionService(
      AmbienteRepository ambientes,
      ActividadRepository actividades,
      ActualizacionRepository actualizaciones) {
    this.ambientes = ambientes;
    this.actividades = actividades;
    this.actualizaciones = actualizaciones;
  }

  private record Fila(String codigo, String tipo, Integer dia, LocalTime inicio, LocalTime fin) {}

  private static final List<String> DIAS =
      List.of("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo");

  private static String normalizar(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .trim();
  }

  @Transactional
  public int importar(byte[] contenido, String nombre, boolean ejemplo) {
    if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".csv"))
      throw new IllegalArgumentException("Selecciona un archivo .csv.");
    if (contenido.length == 0 || contenido.length > 2 * 1024 * 1024)
      throw new IllegalArgumentException("El CSV debe contener datos y pesar como máximo 2 MB.");
    List<Fila> filas = validar(contenido);
    // La validación completa precede a cualquier escritura; todas las escrituras comparten una
    // transacción.
    actividades.deleteAllInBatch();
    ambientes.deleteAllInBatch();
    Map<String, Ambiente> nuevos = new LinkedHashMap<>();
    for (var f : filas) nuevos.computeIfAbsent(f.codigo(), c -> new Ambiente(c, f.tipo()));
    ambientes.saveAll(nuevos.values());
    for (var f : filas)
      if (f.dia() != null)
        actividades.save(new Actividad(nuevos.get(f.codigo()), f.dia(), f.inicio(), f.fin()));
    actualizaciones.save(new Actualizacion(Instant.now(), ejemplo));
    return nuevos.size();
  }

  private List<Fila> validar(byte[] contenido) {
    String texto;
    try {
      texto =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(contenido))
              .toString();
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("El archivo debe estar codificado en UTF-8.");
    }
    if (texto.startsWith("\ufeff")) texto = texto.substring(1);
    var formato =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();
    List<Fila> filas = new ArrayList<>();
    Set<String> unicos = new HashSet<>();
    Map<String, String> tipos = new HashMap<>();
    try (var parser = CSVParser.parse(texto, formato)) {
      var cabecera = parser.getHeaderNames();
      if (!cabecera.equals(List.of("ambiente", "dia_semana", "hora_inicio", "hora_fin"))
          && !cabecera.equals(List.of("ambiente", "dia_semana", "hora_inicio", "hora_fin", "tipo")))
        throw new IllegalArgumentException(
            "Cabecera esperada: ambiente,dia_semana,hora_inicio,hora_fin (opcional: ,tipo).");
      for (CSVRecord r : parser) {
        try {
          if (!r.isConsistent())
            throw new IllegalArgumentException("Cantidad de columnas incorrecta.");
          String codigo = r.get("ambiente").toUpperCase(Locale.ROOT);
          if (!codigo.matches("V-[123][0-9]{2}"))
            throw new IllegalArgumentException(
                "Código requerido con formato V-100 a V-399 (pisos 1 a 3).");
          String tipo = cabecera.contains("tipo") ? normalizar(r.get("tipo")) : "laboratorio";
          if (!List.of("aula", "laboratorio").contains(tipo))
            throw new IllegalArgumentException("Tipo válido: Aula o Laboratorio.");
          tipo = tipo.equals("aula") ? "Aula" : "Laboratorio";
          if (tipos.putIfAbsent(codigo, tipo) != null && !tipos.get(codigo).equals(tipo))
            throw new IllegalArgumentException("Tipos contradictorios para el mismo ambiente.");
          String dia = r.get("dia_semana"), inicio = r.get("hora_inicio"), fin = r.get("hora_fin");
          Fila fila;
          if (dia.isEmpty() && inicio.isEmpty() && fin.isEmpty())
            fila = new Fila(codigo, tipo, null, null, null);
          else {
            int d = DIAS.indexOf(normalizar(dia)) + 1;
            if (d == 0) throw new IllegalArgumentException("Día de la semana no válido.");
            if (!inicio.matches("([01][0-9]|2[0-3]):[0-5][0-9]")
                || !fin.matches("([01][0-9]|2[0-3]):[0-5][0-9]"))
              throw new IllegalArgumentException("Usa horas en formato HH:mm, de 00:00 a 23:59.");
            var i = LocalTime.parse(inicio);
            var f = LocalTime.parse(fin);
            if (!i.isBefore(f))
              throw new IllegalArgumentException(
                  "La hora de inicio debe ser anterior a la de fin.");
            fila = new Fila(codigo, tipo, d, i, f);
          }
          String clave = fila.codigo() + "/" + fila.dia() + "/" + fila.inicio() + "/" + fila.fin();
          if (!unicos.add(clave)) throw new IllegalArgumentException("Registro duplicado.");
          filas.add(fila);
          if (filas.size() > 10000)
            throw new IllegalArgumentException("Máximo 10 000 filas por archivo.");
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
              "Fila " + (r.getRecordNumber() + 1) + ": " + e.getMessage(), e);
        }
      }
    } catch (IOException | UncheckedIOException e) {
      throw new IllegalArgumentException("CSV mal formado: revisa las comillas y columnas.", e);
    }
    if (filas.isEmpty())
      throw new IllegalArgumentException("El CSV debe contener al menos un ambiente.");
    return filas;
  }
}
