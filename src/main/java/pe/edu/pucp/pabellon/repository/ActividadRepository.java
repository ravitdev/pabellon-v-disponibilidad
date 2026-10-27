package pe.edu.pucp.pabellon.repository;

import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.pucp.pabellon.model.Actividad;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
  List<Actividad> findByAmbienteCodigoOrderByDiaAscInicioAsc(String codigo);

  @Query(
      "select distinct a.ambiente.codigo from Actividad a where a.dia=:dia and a.inicio < :fin and"
          + " a.fin > :inicio")
  List<String> codigosConCruce(int dia, LocalTime inicio, LocalTime fin);
}
