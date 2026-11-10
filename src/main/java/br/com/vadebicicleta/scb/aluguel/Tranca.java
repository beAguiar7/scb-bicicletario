package br.com.vadebicicleta.scb.aluguel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tranca")
public class Tranca {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String numero;

	@ManyToOne(optional = false)
	@JoinColumn(name = "totem_id", nullable = false)
	private Totem totem;

	public Long getId() {
		return this.id;
	}

	public String getNumero() {
		return this.numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Totem getTotem() {
		return this.totem;
	}

	public void setTotem(Totem totem) {
		this.totem = totem;
	}

}
