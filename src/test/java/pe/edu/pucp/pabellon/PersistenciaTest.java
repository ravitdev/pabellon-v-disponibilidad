package pe.edu.pucp.pabellon;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import pe.edu.pucp.pabellon.repository.*;
import pe.edu.pucp.pabellon.service.*;

class PersistenciaTest {
  @TempDir Path temporal;

  ConfigurableApplicationContext abrir() {
    return new SpringApplicationBuilder(PabellonApplication.class)
        .web(WebApplicationType.NONE)
        .run(
            "--spring.datasource.url=jdbc:h2:file:" + temporal.resolve("persistencia"),
            "--spring.main.banner-mode=off");
  }

  @Test
  void inicializaUnaVezYConservaImportacionTrasReabrirH2() {
    Instant fecha;
    try (var app = abrir()) {
      var ambientes = app.getBean(AmbienteRepository.class);
      assertThat(ambientes.count()).isEqualTo(19);
      assertThat(ambientes.findAll().stream().filter(a -> a.getPiso().equals("1")).count())
          .isEqualTo(8);
      assertThat(ambientes.findAll().stream().filter(a -> a.getPiso().equals("2")).count())
          .isEqualTo(8);
      assertThat(ambientes.findAll().stream().filter(a -> a.getPiso().equals("3")).count())
          .isEqualTo(3);
      app.getBean(ImportacionService.class)
          .importar(
              "ambiente,dia_semana,hora_inicio,hora_fin\nV-301,Lunes,08:00,10:00\n"
                  .getBytes(StandardCharsets.UTF_8),
              "datos.csv",
              false);
      fecha = app.getBean(ActualizacionRepository.class).findById(1L).orElseThrow().getFecha();
    }
    try (var app = abrir()) {
      assertThat(app.getBean(AmbienteRepository.class).count()).isEqualTo(1);
      assertThat(app.getBean(ActividadRepository.class).count()).isEqualTo(1);
      var actualizacion = app.getBean(ActualizacionRepository.class).findById(1L).orElseThrow();
      assertThat(actualizacion.getFecha()).isEqualTo(fecha);
      assertThat(actualizacion.isEjemplo()).isFalse();
    }
  }
}
