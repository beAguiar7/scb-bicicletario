package br.com.vadebicicleta.scb.aluguel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.vadebicicleta.scb.ciclista.Ciclista;

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
@Table(name = "cobranca")
public class Cobranca {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "ciclista_id", nullable = false)
	private Ciclista ciclista;

	@ManyToOne
	@JoinColumn(name = "aluguel_id")
	private Aluguel aluguel;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false)
	private LocalDateTime dataHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ResultadoCobranca resultado;

	/**
	 * Identifica a confirmacao. Repetir a mesma chave nao gera outra cobranca.
	 */
	@Column(nullable = false, unique = true, length = 80)
	private String chave;

	@Column(nullable = false, length = 40)
	private String referenciaCartao;

	public Long getId() {
		return this.id;
	}

	public Ciclista getCiclista() {
		return this.ciclista;
	}

	public void setCiclista(Ciclista ciclista) {
		this.ciclista = ciclista;
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

	public ResultadoCobranca getResultado() {
		return this.resultado;
	}

	public void setResultado(ResultadoCobranca resultado) {
		this.resultado = resultado;
	}

	public String getChave() {
		return this.chave;
	}

	public void setChave(String chave) {
		this.chave = chave;
	}

	public String getReferenciaCartao() {
		return this.referenciaCartao;
	}

	public void setReferenciaCartao(String referenciaCartao) {
		this.referenciaCartao = referenciaCartao;
	}

}
