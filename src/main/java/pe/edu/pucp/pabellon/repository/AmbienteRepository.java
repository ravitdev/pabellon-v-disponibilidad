package pe.edu.pucp.pabellon.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.pabellon.model.Ambiente;

public interface AmbienteRepository extends JpaRepository<Ambiente, String> {
  List<Ambiente> findAllByOrderByCodigoAsc();
}
