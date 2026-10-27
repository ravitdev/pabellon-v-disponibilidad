
package pe.edu.pucp.pabellon.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.pucp.pabellon.model.ActividadExtra.Frecuencia;
import pe.edu.pucp.pabellon.repository.ActualizacionRepository;
import pe.edu.pucp.pabellon.service.*;

@Controller
public class ActividadExtraController {

    private final ActividadExtraService actividades;
    private final DisponibilidadService disponibilidad;
    private final ActualizacionRepository actualizaciones;

    public ActividadExtraController(
            ActividadExtraService actividades,
            DisponibilidadService disponibilidad,
            ActualizacionRepository actualizaciones) {
        this.actividades = actividades;
        this.disponibilidad = disponibilidad;
        this.actualizaciones = actualizaciones;
    }

    @GetMapping("/actividades/nueva")
    public String formulario(
            @RequestParam(required = false) String ambiente,
            Model model) {

        model.addAttribute("actualizacion",
            actualizaciones.findById(1L).orElse(null));
        model.addAttribute("ambientes", disponibilidad.listar());
        model.addAttribute("seleccionado",
            ambiente == null ? "" : ambiente);
        model.addAttribute("fechaSugerida",
            LocalDate.now(ZoneId.of("America/Lima")));

        return "nueva-actividad";
    }

    @PostMapping("/actividades")
    public String crear(
            @RequestParam(required = false) String ambiente,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin,
            @RequestParam(defaultValue = "PUNTUAL") String frecuencia,
            RedirectAttributes flash) {

        String codigo = ambiente == null ? "" :
            ambiente.trim().toUpperCase(Locale.ROOT);

        try {
            LocalDate dia = LocalDate.parse(fecha);
            LocalTime desde = LocalTime.parse(inicio);
            LocalTime hasta = LocalTime.parse(fin);
            Frecuencia tipo = Frecuencia.valueOf(frecuencia);

            actividades.registrar(
                codigo, descripcion, dia, desde, hasta, tipo
            );

            flash.addFlashAttribute("exito",
                "Actividad agregada correctamente.");

            return "redirect:/ambientes/" + codigo
                + "?semana=" + dia.with(DayOfWeek.MONDAY);

        } catch (RuntimeException e) {
            String mensaje = e instanceof DateTimeException ||
                             e instanceof NullPointerException
                ? "Completa una fecha y horas válidas."
                : e.getMessage();

            flash.addFlashAttribute("error",
                mensaje == null ? "No se pudo registrar." : mensaje);

            return "redirect:/actividades/nueva?ambiente="
                + URLEncoder.encode(codigo, StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/api/ambientes/{codigo}/actividades")
    @ResponseBody
    public List<ActividadExtraService.EventoSemana> semana(
            @PathVariable String codigo,
            @RequestParam String semana) {

        LocalDate lunes = LocalDate.parse(semana);

        if (lunes.getDayOfWeek() != DayOfWeek.MONDAY)
            throw new IllegalArgumentException(
                "La semana debe comenzar en lunes.");

        return actividades.paraSemana(codigo, lunes);
    }
}
