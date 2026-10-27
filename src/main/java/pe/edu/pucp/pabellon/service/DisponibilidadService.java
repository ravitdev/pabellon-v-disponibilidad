package pe.edu.pucp.pabellon.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.pabellon.model.*;
import pe.edu.pucp.pabellon.repository.*;

@Service
@Transactional(readOnly = true)
public class DisponibilidadService {
  private final AmbienteRepository ambientes;
  private final ActividadRepository actividades;

  public DisponibilidadService(AmbienteRepository ambientes, ActividadRepository actividades) {
    this.ambientes = ambientes;
    this.actividades = actividades;
  }

  public record Resultado(Ambiente ambiente, boolean disponible) {}

  public List<Ambiente> listar() {
    return ambientes.findAllByOrderByCodigoAsc();
  }

  public List<Resultado> consultar(LocalDate fecha, LocalTime inicio, LocalTime fin) {
    if (fecha == null || inicio == null || fin == null || !inicio.isBefore(fin))
      throw new IllegalArgumentException(
          "Selecciona una fecha y un intervalo con inicio anterior al fin.");
    var ocupados =
        new HashSet<>(actividades.codigosConCruce(fecha.getDayOfWeek().getValue(), inicio, fin));
    return listar().stream().map(a -> new Resultado(a, !ocupados.contains(a.getCodigo()))).toList();
  }

  public Ambiente buscar(String codigo) {
    return ambientes
        .findById(codigo)
        .orElseThrow(() -> new NoSuchElementException("Ambiente no registrado."));
  }

  public List<Actividad> horario(String codigo) {
    return actividades.findByAmbienteCodigoOrderByDiaAscInicioAsc(codigo);
  }
}
