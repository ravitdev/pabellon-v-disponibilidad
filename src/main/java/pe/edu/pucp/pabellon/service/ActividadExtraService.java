
package pe.edu.pucp.pabellon.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.pabellon.model.*;
import pe.edu.pucp.pabellon.model.ActividadExtra.Frecuencia;
import pe.edu.pucp.pabellon.repository.*;

@Service
public class ActividadExtraService {

    private final AmbienteRepository ambientes;
    private final ActividadRepository horariosCsv;
    private final ActividadExtraRepository extras;
    private final ActualizacionRepository actualizaciones;

    public ActividadExtraService(
            AmbienteRepository ambientes,
            ActividadRepository horariosCsv,
            ActividadExtraRepository extras,
            ActualizacionRepository actualizaciones) {
        this.ambientes = ambientes;
        this.horariosCsv = horariosCsv;
        this.extras = extras;
        this.actualizaciones = actualizaciones;
    }

    @Transactional
    public void registrar(String codigo, String descripcion,
                          LocalDate fecha, LocalTime inicio,
                          LocalTime fin, Frecuencia frecuencia) {

        if (codigo == null || codigo.isBlank())
            throw new IllegalArgumentException("Selecciona un ambiente.");

        Ambiente ambiente = ambientes.findById(
            codigo.trim().toUpperCase(Locale.ROOT)
        ).orElseThrow(() ->
            new IllegalArgumentException("El ambiente no existe.")
        );

        if (descripcion == null ||
            descripcion.trim().length() < 2 ||
            descripcion.trim().length() > 100)
            throw new IllegalArgumentException(
                "La descripción debe tener entre 2 y 100 caracteres.");

        if (fecha == null || frecuencia == null)
            throw new IllegalArgumentException(
                "Selecciona fecha y frecuencia.");

        if (inicio == null || fin == null || !inicio.isBefore(fin))
            throw new IllegalArgumentException(
                "La hora de inicio debe ser anterior a la de fin.");

        int dia = fecha.getDayOfWeek().getValue();

        // Revisar conflictos con los horarios del CSV.
        boolean conflictoCsv = horariosCsv
            .findByAmbienteCodigoOrderByDiaAscInicioAsc(ambiente.getCodigo())
            .stream()
            .anyMatch(a -> a.getDia() == dia &&
                cruza(a.getInicio(), a.getFin(), inicio, fin));

        if (conflictoCsv)
            throw new IllegalArgumentException(
                "El ambiente ya tiene una clase en ese horario.");

        // Revisar conflictos con actividades añadidas.
        boolean conflictoExtra = extras
            .findByAmbienteCodigo(ambiente.getCodigo())
            .stream()
            .anyMatch(a -> {
                if (!cruza(a.getInicio(), a.getFin(), inicio, fin))
                    return false;

                if (a.getFrecuencia() == Frecuencia.SEMANAL)
                    return a.getDiaSemana() == dia;

                if (frecuencia == Frecuencia.SEMANAL)
                    return a.getFecha().getDayOfWeek().getValue() == dia;

                return a.getFecha().equals(fecha);
            });

        if (conflictoExtra)
            throw new IllegalArgumentException(
                "Ya existe una actividad adicional en ese horario.");

        extras.save(new ActividadExtra(
            ambiente, descripcion.trim(), frecuencia,
            fecha, inicio, fin
        ));

        Actualizacion anterior =
            actualizaciones.findById(1L).orElse(null);

        actualizaciones.save(new Actualizacion(
            Instant.now(),
            anterior != null && anterior.isEjemplo()
        ));
    }

    private boolean cruza(LocalTime i1, LocalTime f1,
                          LocalTime i2, LocalTime f2) {
        return i1.isBefore(f2) && f1.isAfter(i2);
    }

    public record EventoSemana(
        int dia, String inicio, String fin,
        String titulo, String tipo, String fecha
    ) {}

    @Transactional(readOnly = true)
    public List<EventoSemana> paraSemana(String codigo, LocalDate lunes) {
        ambientes.findById(codigo).orElseThrow(() ->
            new NoSuchElementException("Ambiente no registrado."));

        LocalDate domingo = lunes.plusDays(6);
        List<EventoSemana> eventos = new ArrayList<>();

        for (ActividadExtra a : extras.findByAmbienteCodigo(codigo)) {
            if (a.getFrecuencia() == Frecuencia.PUNTUAL) {
                if (a.getFecha().isBefore(lunes) ||
                    a.getFecha().isAfter(domingo))
                    continue;

                eventos.add(new EventoSemana(
                    a.getFecha().getDayOfWeek().getValue(),
                    a.getInicio().toString(),
                    a.getFin().toString(),
                    a.getDescripcion(),
                    "PUNTUAL",
                    a.getFecha().toString()
                ));
            } else {
                eventos.add(new EventoSemana(
                    a.getDiaSemana(),
                    a.getInicio().toString(),
                    a.getFin().toString(),
                    a.getDescripcion(),
                    "SEMANAL",
                    null
                ));
            }
        }
        return eventos;
    }
}
