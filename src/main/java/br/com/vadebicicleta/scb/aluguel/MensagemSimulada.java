package br.com.vadebicicleta.scb.aluguel;

import java.time.LocalDateTime;

import br.com.vadebicicleta.scb.ciclista.Ciclista;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Caixa de saida local. Nenhuma mensagem sai do sistema.
 */
@Entity
@Table(name = "mensagem_simulada")
public class MensagemSimulada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "ciclista_id", nullable = false)
	private Ciclista destinatario;

	@Column(name = "aluguel_id", nullable = false, unique = true)
	private Long aluguelId;

	@Column(nullable = false, length = 120)
	private String assunto;

	@Column(nullable = false, length = 2000)
	private String texto;

	@Column(nullable = false)
	private LocalDateTime criadaEm;

	public Long getId() {
		return this.id;
	}

	public Ciclista getDestinatario() {
		return this.destinatario;
	}

	public void setDestinatario(Ciclista destinatario) {
		this.destinatario = destinatario;
	}

	public Long getAluguelId() {
		return this.aluguelId;
	}

	public void setAluguelId(Long aluguelId) {
		this.aluguelId = aluguelId;
	}

	public String getAssunto() {
		return this.assunto;
	}

	public void setAssunto(String assunto) {
		this.assunto = assunto;
	}

	public String getTexto() {
		return this.texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public LocalDateTime getCriadaEm() {
		return this.criadaEm;
	}

	public void setCriadaEm(LocalDateTime criadaEm) {
		this.criadaEm = criadaEm;
	}

}
