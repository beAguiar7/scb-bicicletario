package br.com.vadebicicleta.scb.aluguel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bicicleta")
public class Bicicleta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Identificacao ficticia, reservada para uso futuro. Nao e um codigo real de equipamento.
	 */
	@Column(nullable = false, unique = true, length = 40)
	private String rfid;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusBicicleta status;

	@ManyToOne
	@JoinColumn(name = "tranca_id")
	private Tranca tranca;

	public Long getId() {
		return this.id;
	}

	public String getRfid() {
		return this.rfid;
	}

	public void setRfid(String rfid) {
		this.rfid = rfid;
	}

	public StatusBicicleta getStatus() {
		return this.status;
	}

	public void setStatus(StatusBicicleta status) {
		this.status = status;
	}

	public Tranca getTranca() {
		return this.tranca;
	}

	public void setTranca(Tranca tranca) {
		this.tranca = tranca;
	}

}
