package pe.edu.pucp.pabellon.model;

import jakarta.persistence.*;

@Entity
public class Ambiente {
  @Id
  @Column(length = 20)
  private String codigo;

  @Column(nullable = false)
  private String tipo;

  protected Ambiente() {}

  public Ambiente(String codigo, String tipo) {
    this.codigo = codigo;
    this.tipo = tipo;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getTipo() {
    return tipo;
  }

  public String getPiso() {
    return codigo.substring(2, 3);
  }
}
