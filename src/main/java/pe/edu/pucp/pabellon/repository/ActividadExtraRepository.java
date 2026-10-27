
package pe.edu.pucp.pabellon.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.pucp.pabellon.model.ActividadExtra;
import pe.edu.pucp.pabellon.model.ActividadExtra.Frecuencia;

public interface ActividadExtraRepository
        extends JpaRepository<ActividadExtra, Long> {

    List<ActividadExtra> findByAmbienteCodigo(String codigo);

    @Query("""
        SELECT DISTINCT e.ambiente.codigo
        FROM ActividadExtra e
        WHERE (
            (e.frecuencia = :puntual AND e.fecha = :fecha)
            OR
            (e.frecuencia = :semanal AND e.diaSemana = :dia)
        )
        AND e.inicio < :fin
        AND e.fin > :inicio
        """)
    List<String> codigosConCruce(
        Frecuencia puntual,
        Frecuencia semanal,
        LocalDate fecha,
        int dia,
        LocalTime inicio,
        LocalTime fin
    );
}
