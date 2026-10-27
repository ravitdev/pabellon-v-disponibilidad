package pe.edu.pucp.pabellon.controller;

import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.pabellon.repository.ActualizacionRepository;
import pe.edu.pucp.pabellon.service.*;

@Controller
public class WebController {
  private final DisponibilidadService disponibilidad;
  private final ImportacionService importacion;
  private final ActualizacionRepository actualizaciones;

  public WebController(
      DisponibilidadService disponibilidad,
      ImportacionService importacion,
      ActualizacionRepository actualizaciones) {
    this.disponibilidad = disponibilidad;
    this.importacion = importacion;
    this.actualizaciones = actualizaciones;
  }

  @ModelAttribute
  public void comunes(Model model) {
    model.addAttribute("actualizacion", actualizaciones.findById(1L).orElse(null));
  }

  @GetMapping("/")
  public String inicio(
      @RequestParam(required = false) String fecha,
      @RequestParam(required = false) String inicio,
      @RequestParam(required = false) String fin,
      @RequestParam(defaultValue = "false") boolean soloDisponibles,
      Model model) {
    model.addAttribute(
        "fecha", fecha == null ? LocalDate.now(ZoneId.of("America/Lima")).toString() : fecha);
    model.addAttribute("inicio", inicio == null ? "08:00" : inicio);
    model.addAttribute("fin", fin == null ? "10:00" : fin);
    model.addAttribute("soloDisponibles", soloDisponibles);
    if (fecha != null || inicio != null || fin != null) {
      try {
        var resultados =
            disponibilidad.consultar(
                LocalDate.parse(fecha == null ? "" : fecha),
                LocalTime.parse(inicio == null ? "" : inicio),
                LocalTime.parse(fin == null ? "" : fin));
        model.addAttribute(
            "libres",
            resultados.stream().filter(DisponibilidadService.Resultado::disponible).count());
        model.addAttribute(
            "resultados",
            resultados.stream().filter(r -> !soloDisponibles || r.disponible()).toList());
        model.addAttribute("consultado", true);
      } catch (IllegalArgumentException | DateTimeException e) {
        model.addAttribute(
            "error", "Selecciona una fecha válida y un intervalo con inicio anterior al fin.");
      }
    }
    model.addAttribute("ambientes", disponibilidad.listar());
    return "index";
  }

  @GetMapping("/ambientes/{codigo}")
  public String horario(@PathVariable String codigo, Model model) {
    model.addAttribute("ambiente", disponibilidad.buscar(codigo));
    model.addAttribute("horarios", disponibilidad.horario(codigo));
    return "horario";
  }

  @GetMapping("/importar")
  public String carga() {
    return "importar";
  }

  @PostMapping("/importar")
  public String importar(
      @RequestParam("archivo") MultipartFile archivo,
      @RequestParam(defaultValue = "false") boolean ejemplo,
      RedirectAttributes flash) {
    try {
      int total = importacion.importar(archivo.getBytes(), archivo.getOriginalFilename(), ejemplo);
      flash.addFlashAttribute(
          "exito",
          "Importación completada: "
              + total
              + " ambientes. Se reemplazaron los horarios anteriores.");
    } catch (IllegalArgumentException e) {
      flash.addFlashAttribute("error", e.getMessage() + " No se modificaron los datos anteriores.");
    } catch (IOException e) {
      flash.addFlashAttribute(
          "error", "No se pudo leer el archivo. Los datos anteriores se conservan.");
    }
    return "redirect:/importar";
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String archivoGrande(RedirectAttributes flash) {
    flash.addFlashAttribute(
        "error", "El archivo supera el límite de 2 MB. No se modificaron los datos.");
    return "redirect:/importar";
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String noEncontrado() {
    return "no-encontrado";
  }
}
