package pe.edu.pucp.pabellon.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"ambiente_codigo", "dia", "inicio", "fin"}))
public class Actividad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Ambiente ambiente;

  private int dia;

  @Column(nullable = false)
  private LocalTime inicio;

  @Column(nullable = false)
  private LocalTime fin;

  protected Actividad() {}

  public Actividad(Ambiente ambiente, int dia, LocalTime inicio, LocalTime fin) {
    this.ambiente = ambiente;
    this.dia = dia;
    this.inicio = inicio;
    this.fin = fin;
  }

  public Ambiente getAmbiente() {
    return ambiente;
  }

  public int getDia() {
    return dia;
  }

  public LocalTime getInicio() {
    return inicio;
  }

  public LocalTime getFin() {
    return fin;
  }

  public String getNombreDia() {
    return java.time.DayOfWeek.of(dia)
        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es"));
  }
}
