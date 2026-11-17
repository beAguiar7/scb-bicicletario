package br.com.vadebicicleta.scb.aluguel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

/**
 * Uma taxa extra por aluguel. O horario e o da devolucao gravada no servidor.
 */
@Entity
@Table(name = "taxa_extra")
public class TaxaExtra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "aluguel_id", nullable = false, unique = true)
	private Aluguel aluguel;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false)
	private LocalDateTime dataHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SituacaoTaxaExtra situacao;

	/**
	 * Horario da ultima tentativa de reprocessamento. Nulo ate o ciclista tentar pagar de novo.
	 */
	private LocalDateTime tentativaEm;

	/**
	 * Resultado dessa tentativa. Nao guarda cartao.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private SituacaoTaxaExtra resultadoTentativa;

	public Long getId() {
		return this.id;
	}

	public Aluguel getAluguel() {
		return this.aluguel;
	}

	public void setAluguel(Aluguel aluguel) {
		this.aluguel = aluguel;
	}

	public BigDecimal getValor() {
		return this.valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public LocalDateTime getDataHora() {
		return this.dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public SituacaoTaxaExtra getSituacao() {
		return this.situacao;
	}

	public void setSituacao(SituacaoTaxaExtra situacao) {
		this.situacao = situacao;
	}

	public LocalDateTime getTentativaEm() {
		return this.tentativaEm;
	}

	public void setTentativaEm(LocalDateTime tentativaEm) {
		this.tentativaEm = tentativaEm;
	}

	public SituacaoTaxaExtra getResultadoTentativa() {
		return this.resultadoTentativa;
	}

	public void setResultadoTentativa(SituacaoTaxaExtra resultadoTentativa) {
		this.resultadoTentativa = resultadoTentativa;
	}

}
