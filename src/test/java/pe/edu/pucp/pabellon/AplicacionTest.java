package pe.edu.pucp.pabellon;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.pabellon.repository.*;
import pe.edu.pucp.pabellon.service.*;

@SpringBootTest
@AutoConfigureMockMvc
class AplicacionTest {
  static final String CAB = "ambiente,dia_semana,hora_inicio,hora_fin\n";
  static final Path DB;

  static {
    try {
      DB = Files.createTempDirectory("pabellon-test-").resolve("datos");
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> "jdbc:h2:file:" + DB);
  }

  @Autowired ImportacionService importacion;
  @Autowired DisponibilidadService disponibilidad;
  @Autowired AmbienteRepository ambientes;
  @Autowired ActividadRepository actividades;
  @Autowired ActualizacionRepository actualizaciones;
  @Autowired MockMvc mvc;

  @BeforeEach
  void cargar() {
    importar(CAB + "V-201,Lunes,08:00,10:00\nV-201,Lunes,14:00,16:00\nV-202,,,\n");
  }

  void importar(String csv) {
    importacion.importar(csv.getBytes(StandardCharsets.UTF_8), "horarios.csv", true);
  }

  @Test
  void listaAmbientesOrdenadosYTipos() {
    assertThat(disponibilidad.listar())
        .extracting(a -> a.getCodigo())
        .containsExactly("V-201", "V-202");
    assertThat(disponibilidad.listar()).allMatch(a -> a.getTipo().equals("Laboratorio"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "07:00/08:00/true",
        "10:00/11:00/true",
        "09:00/11:00/false",
        "08:00/10:00/false",
        "07:00/17:00/false",
        "08:30/09:00/false",
        "13:00/15:00/false"
      })
  void calculaCrucesYExtremos(String caso) {
    String[] p = caso.split("/");
    var r =
        disponibilidad.consultar(
            LocalDate.of(2026, 10, 26), LocalTime.parse(p[0]), LocalTime.parse(p[1]));
    assertThat(r.getFirst().disponible()).isEqualTo(Boolean.parseBoolean(p[2]));
    assertThat(r.get(1).disponible()).isTrue();
  }

  @Test
  void fechaDeterminaDiaSemanal() {
    assertThat(
            disponibilidad.consultar(
                LocalDate.of(2026, 10, 27), LocalTime.of(8, 0), LocalTime.of(10, 0)))
        .allMatch(DisponibilidadService.Resultado::disponible);
  }

  @Test
  void rechazaIntervaloInvertidoIgualONulo() {
    for (var fin : java.util.List.of(LocalTime.of(8, 0), LocalTime.of(7, 0)))
      assertThatIllegalArgumentException()
          .isThrownBy(() -> disponibilidad.consultar(LocalDate.now(), LocalTime.of(8, 0), fin));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> disponibilidad.consultar(null, LocalTime.of(8, 0), LocalTime.of(10, 0)));
  }

  @Test
  void horarioOrdenado() {
    assertThat(disponibilidad.horario("V-201"))
        .extracting(h -> h.getInicio())
        .containsExactly(LocalTime.of(8, 0), LocalTime.of(14, 0));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "V-201,Lunes,10:00,08:00",
        "V-201,Lunes,08:00,08:00",
        "V-201,Lunes,25:00,26:00",
        "V-201,Lunes,8:00,10:00",
        "V-201,Feriado,08:00,10:00",
        ",Lunes,08:00,10:00",
        "V-401,Lunes,08:00,10:00",
        "V-201,,08:00,10:00",
        "V-201,Lunes,,",
        "V-201,Lunes,08:00,10:00,extra",
        "V-201,Lunes,08:00,10:00\nV-201,lunes,08:00,10:00",
        "V-201,,,\nV-201,,,",
        "\"V-201,Lunes,08:00,10:00"
      })
  void csvInvalidoNoModificaInformacion(String fila) {
    var anterior = actualizaciones.findById(1L).orElseThrow().getFecha();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> importar(CAB + "V-301,Martes,11:00,12:00\n" + fila + "\n"));
    assertThat(ambientes.count()).isEqualTo(2);
    assertThat(actividades.count()).isEqualTo(2);
    assertThat(ambientes.existsById("V-301")).isFalse();
    assertThat(actualizaciones.findById(1L).orElseThrow().getFecha()).isEqualTo(anterior);
  }

  @Test
  void validaCabeceraContenidoExtensionYUtf8() {
    for (String csv :
        java.util.List.of("", CAB, "ambiente,dia,inicio,fin\nV-201,Lunes,08:00,10:00"))
      assertThatIllegalArgumentException().isThrownBy(() -> importar(csv));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> importacion.importar(CAB.getBytes(StandardCharsets.UTF_8), "archivo.txt", false));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> importacion.importar(new byte[] {(byte) 0xc3, 0x28}, "archivo.csv", false));
  }

  @Test
  void reemplazoCompletoTildesBomYTipos() {
    var anterior = actualizaciones.findById(1L).orElseThrow().getFecha();
    importar(
        "\ufeffambiente,dia_semana,hora_inicio,hora_fin,tipo\n"
            + "\"V-301\",Miércoles,09:00,11:00,Aula\n"
            + "V-302,,,,Laboratorio\n");
    assertThat(ambientes.existsById("V-201")).isFalse();
    assertThat(disponibilidad.buscar("V-301").getTipo()).isEqualTo("Aula");
    assertThat(disponibilidad.horario("V-301").getFirst().getDia()).isEqualTo(3);
    assertThat(disponibilidad.horario("V-302")).isEmpty();
    assertThat(actualizaciones.findById(1L).orElseThrow().getFecha()).isAfterOrEqualTo(anterior);
  }

  @Test
  void rechazaTiposContradictorios() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                importar(
                    CAB.stripTrailing()
                        + ",tipo\n"
                        + "V-201,Lunes,08:00,10:00,Aula\n"
                        + "V-201,Martes,08:00,10:00,Laboratorio\n"));
  }

  @Test
  void vistasYValidacionHttp() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Datos ficticios")));
    mvc.perform(
            get("/").param("fecha", "2026-10-26").param("inicio", "09:00").param("fin", "11:00"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("libres", 1L));
    mvc.perform(
            get("/")
                .param("fecha", "2026-10-26")
                .param("inicio", "09:00")
                .param("fin", "11:00")
                .param("soloDisponibles", "true"))
        .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
    mvc.perform(get("/").param("fecha", "incorrecta"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("error"));
    mvc.perform(get("/ambientes/V-201"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("lunes")));
    mvc.perform(get("/ambientes/V-999")).andExpect(status().isNotFound());
    mvc.perform(get("/importar")).andExpect(status().isOk());
  }

  @Test
  void cargaHttpValidaEInvalida() throws Exception {
    var valido =
        new MockMultipartFile(
            "archivo",
            "datos.csv",
            "text/csv",
            (CAB + "V-301,,,\n").getBytes(StandardCharsets.UTF_8));
    mvc.perform(multipart("/importar").file(valido).param("ejemplo", "true"))
        .andExpect(redirectedUrl("/importar"))
        .andExpect(flash().attributeExists("exito"));
    assertThat(ambientes.existsById("V-301")).isTrue();
    var invalido =
        new MockMultipartFile(
            "archivo",
            "datos.csv",
            "text/csv",
            (CAB + "V-101,Lunes,12:00,10:00").getBytes(StandardCharsets.UTF_8));
    mvc.perform(multipart("/importar").file(invalido)).andExpect(flash().attributeExists("error"));
    assertThat(ambientes.count()).isEqualTo(1);
    assertThat(ambientes.existsById("V-301")).isTrue();
  }
}
