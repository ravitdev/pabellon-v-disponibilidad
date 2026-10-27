
package pe.edu.pucp.pabellon.controller;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.edu.pucp.pabellon.repository.ActualizacionRepository;
import pe.edu.pucp.pabellon.service.*;

@Controller
public class GestionActividadesController {

    private final GestionActividadesService gestion;
    private final DisponibilidadService disponibilidad;
    private final ActualizacionRepository actualizaciones;

    public GestionActividadesController(
            GestionActividadesService gestion,
            DisponibilidadService disponibilidad,
            ActualizacionRepository actualizaciones) {

        this.gestion = gestion;
        this.disponibilidad = disponibilidad;
        this.actualizaciones = actualizaciones;
    }

    // Mostrar todas las actividades del ambiente
    @GetMapping("/ambientes/{codigo}/gestionar")
    public String mostrar(
            @PathVariable String codigo,
            Model model) {

        model.addAttribute(
            "ambiente",
            disponibilidad.buscar(codigo)
        );

        model.addAttribute(
            "registros",
            gestion.listar(codigo)
        );

        model.addAttribute(
            "actualizacion",
            actualizaciones.findById(1L).orElse(null)
        );

        return "gestionar-actividades";
    }

    // Eliminar una actividad
    @PostMapping(
        "/ambientes/{codigo}/actividades/{tipo}/{id}/eliminar"
    )
    public String eliminar(
            @PathVariable String codigo,
            @PathVariable String tipo,
            @PathVariable Long id,
            RedirectAttributes flash) {

        try {
            gestion.eliminar(codigo, tipo, id);

            flash.addFlashAttribute(
                "exito",
                "La actividad se eliminó correctamente."
            );

        } catch (IllegalArgumentException |
                 NoSuchElementException e) {

            flash.addFlashAttribute(
                "error",
                e.getMessage()
            );
        }

        return "redirect:/ambientes/" + codigo + "/gestionar";
    }
}
