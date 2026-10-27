
package pe.edu.pucp.pabellon.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "actividad_extra")
public class ActividadExtra {

    public enum Frecuencia {
        PUNTUAL, SEMANAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ambiente_codigo", nullable = false)
    private Ambiente ambiente;

    @Column(nullable = false, length = 100)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frecuencia frecuencia;

    private LocalDate fecha;
    private Integer diaSemana;

    @Column(nullable = false)
    private LocalTime inicio;

    @Column(nullable = false)
    private LocalTime fin;

    protected ActividadExtra() {}

    public ActividadExtra(Ambiente ambiente, String descripcion,
                          Frecuencia frecuencia, LocalDate fecha,
                          LocalTime inicio, LocalTime fin) {
        this.ambiente = ambiente;
        this.descripcion = descripcion;
        this.frecuencia = frecuencia;
        this.fecha = frecuencia == Frecuencia.PUNTUAL ? fecha : null;
        this.diaSemana = frecuencia == Frecuencia.SEMANAL
                ? fecha.getDayOfWeek().getValue() : null;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getId() { return id; }
    public Ambiente getAmbiente() { return ambiente; }
    public String getDescripcion() { return descripcion; }
    public Frecuencia getFrecuencia() { return frecuencia; }
    public LocalDate getFecha() { return fecha; }
    public Integer getDiaSemana() { return diaSemana; }
    public LocalTime getInicio() { return inicio; }
    public LocalTime getFin() { return fin; }
}
