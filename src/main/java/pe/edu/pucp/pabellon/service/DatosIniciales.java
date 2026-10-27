package pe.edu.pucp.pabellon.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pe.edu.pucp.pabellon.repository.*;

@Component
public class DatosIniciales implements ApplicationRunner {
  private final ImportacionService importacion;
  private final AmbienteRepository ambientes;
  private final ActualizacionRepository actualizaciones;

  public DatosIniciales(
      ImportacionService importacion,
      AmbienteRepository ambientes,
      ActualizacionRepository actualizaciones) {
    this.importacion = importacion;
    this.ambientes = ambientes;
    this.actualizaciones = actualizaciones;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (ambientes.count() == 0 && actualizaciones.count() == 0) {
      try (var in =
          new ClassPathResource("static/ejemplos/horarios-ficticios.csv").getInputStream()) {
        importacion.importar(in.readAllBytes(), "ejemplo.csv", true);
      }
    }
  }
}
