
package pe.edu.pucp.pabellon.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.pabellon.model.*;
import pe.edu.pucp.pabellon.repository.*;

@Service
public class GestionActividadesService {

    private final ActividadRepository actividades;
    private final ActividadExtraRepository extras;
    private final AmbienteRepository ambientes;
    private final ActualizacionRepository actualizaciones;

    public GestionActividadesService(
            ActividadRepository actividades,
            ActividadExtraRepository extras,
            AmbienteRepository ambientes,
            ActualizacionRepository actualizaciones) {

        this.actividades = actividades;
        this.extras = extras;
        this.ambientes = ambientes;
        this.actualizaciones = actualizaciones;
    }

    // Datos que mostraremos en la tabla de gestión
    public record Fila(
            Long id,
            String tipo,
            String titulo,
            String cuando,
            String horario,
            String detalle
    ) {}

    @Transactional(readOnly = true)
    public List<Fila> listar(String codigo) {

        if (!ambientes.existsById(codigo)) {
            throw new NoSuchElementException(
                "El ambiente no existe.");
        }

        List<Fila> filas = new ArrayList<>();

        // 1. Actividades provenientes del CSV
        for (Actividad a : actividades
                .findByAmbienteCodigoOrderByDiaAscInicioAsc(codigo)) {

            filas.add(new Fila(
                a.getId(),
                "CSV",
                "Clase programada",
                a.getNombreDia(),
                a.getInicio() + " – " + a.getFin(),
                "Programación semanal del CSV"
            ));
        }

        // 2. Actividades añadidas individualmente
        Locale espanol = Locale.forLanguageTag("es");

        DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (ActividadExtra a :
                extras.findByAmbienteCodigo(codigo)) {

            boolean puntual =
                a.getFrecuencia() ==
                ActividadExtra.Frecuencia.PUNTUAL;

            String cuando = puntual
                ? a.getFecha().format(formatoFecha)
                : DayOfWeek.of(a.getDiaSemana())
                    .getDisplayName(TextStyle.FULL, espanol);

            String detalle = puntual
                ? "Actividad de una sola fecha"
                : "Actividad semanal recurrente";

            filas.add(new Fila(
                a.getId(),
                "EXTRA",
                a.getDescripcion(),
                cuando,
                a.getInicio() + " – " + a.getFin(),
                detalle
            ));
        }

        return filas;
    }

    // Eliminar una única actividad
    @Transactional
    public void eliminar(String codigo, String tipo, Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                "Identificador inválido.");
        }

        if (!ambientes.existsById(codigo)) {
            throw new NoSuchElementException(
                "El ambiente no existe.");
        }

        if ("CSV".equals(tipo)) {

            Actividad actividad = actividades.findById(id)
                .orElseThrow(() ->
                    new NoSuchElementException(
                        "La actividad no existe."));

            if (!actividad.getAmbiente()
                    .getCodigo().equals(codigo)) {
                throw new IllegalArgumentException(
                    "La actividad no pertenece a este ambiente.");
            }

            actividades.delete(actividad);

        } else if ("EXTRA".equals(tipo)) {

            ActividadExtra actividad = extras.findById(id)
                .orElseThrow(() ->
                    new NoSuchElementException(
                        "La actividad no existe."));

            if (!actividad.getAmbiente()
                    .getCodigo().equals(codigo)) {
                throw new IllegalArgumentException(
                    "La actividad no pertenece a este ambiente.");
            }

            extras.delete(actividad);

        } else {
            throw new IllegalArgumentException(
                "Tipo de actividad inválido.");
        }

        // Registrar la última modificación
        Actualizacion anterior =
            actualizaciones.findById(1L).orElse(null);

        boolean ejemplo = anterior != null &&
                          anterior.isEjemplo();

        actualizaciones.save(
            new Actualizacion(Instant.now(), ejemplo)
        );
    }
}
