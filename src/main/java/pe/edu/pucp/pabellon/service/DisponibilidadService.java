
package pe.edu.pucp.pabellon.service;

import java.time.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.pucp.pabellon.model.*;
import pe.edu.pucp.pabellon.repository.*;

@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    private final AmbienteRepository ambientes;
    private final ActividadRepository actividades;
    private final ActividadExtraRepository extras;

    // Constructor para mantener compatibilidad con pruebas existentes
    public DisponibilidadService(
            AmbienteRepository ambientes,
            ActividadRepository actividades) {

        this(ambientes, actividades, null);
    }

    // Constructor utilizado por Spring Boot
    @Autowired
    public DisponibilidadService(
            AmbienteRepository ambientes,
            ActividadRepository actividades,
            ActividadExtraRepository extras) {

        this.ambientes = ambientes;
        this.actividades = actividades;
        this.extras = extras;
    }

    public record Resultado(
            Ambiente ambiente,
            boolean disponible) {}

    // Listar todos los ambientes
    public List<Ambiente> listar() {
        return ambientes.findAllByOrderByCodigoAsc();
    }

    // Consultar disponibilidad de ambientes
    public List<Resultado> consultar(
            LocalDate fecha,
            LocalTime inicio,
            LocalTime fin) {

        if (fecha == null || inicio == null ||
            fin == null || !inicio.isBefore(fin)) {

            throw new IllegalArgumentException(
                "Selecciona una fecha y un intervalo " +
                "con inicio anterior al fin.");
        }

        // 1. Ambientes ocupados por actividades del CSV
        var ocupados = new HashSet<>(
            actividades.codigosConCruce(
                fecha.getDayOfWeek().getValue(),
                inicio,
                fin
            )
        );

        // 2. Ambientes ocupados por actividades adicionales
        if (extras != null) {

            ocupados.addAll(
                extras.codigosConCruce(
                    ActividadExtra.Frecuencia.PUNTUAL,
                    ActividadExtra.Frecuencia.SEMANAL,
                    fecha,
                    fecha.getDayOfWeek().getValue(),
                    inicio,
                    fin
                )
            );
        }

        // 3. Determinar qué ambientes están disponibles
        return listar().stream()
            .map(a -> new Resultado(
                a,
                !ocupados.contains(a.getCodigo())
            ))
            .toList();
    }

    // Buscar un ambiente por su código
    public Ambiente buscar(String codigo) {

        return ambientes.findById(codigo)
            .orElseThrow(() ->
                new NoSuchElementException(
                    "Ambiente no registrado."));
    }

    // Obtener actividades registradas mediante CSV
    public List<Actividad> horario(String codigo) {

        return actividades
            .findByAmbienteCodigoOrderByDiaAscInicioAsc(codigo);
    }
}
