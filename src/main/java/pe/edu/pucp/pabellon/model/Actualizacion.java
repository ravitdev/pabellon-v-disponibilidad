package pe.edu.pucp.pabellon.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Actualizacion {
  @Id private Long id = 1L;

  @Column(nullable = false)
  private Instant fecha;

  private boolean ejemplo;

  protected Actualizacion() {}

  public Actualizacion(Instant fecha, boolean ejemplo) {
    this.fecha = fecha;
    this.ejemplo = ejemplo;
  }

  public Instant getFecha() {
    return fecha;
  }

  public boolean isEjemplo() {
    return ejemplo;
  }

  public String getFechaLocal() {
    return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        .withZone(java.time.ZoneId.of("America/Lima"))
        .format(fecha);
  }
}
