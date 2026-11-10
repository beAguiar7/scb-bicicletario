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
@Table(name = "aluguel")
public class Aluguel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "ciclista_id", nullable = false)
	private Ciclista ciclista;

	@ManyToOne(optional = false)
	@JoinColumn(name = "bicicleta_id", nullable = false)
	private Bicicleta bicicleta;

	@ManyToOne(optional = false)
	@JoinColumn(name = "tranca_id", nullable = false)
	private Tranca tranca;

	@ManyToOne(optional = false)
	@JoinColumn(name = "totem_id", nullable = false)
	private Totem totem;

	@Column(nullable = false)
	private LocalDateTime inicio;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false)
	private int duracaoHoras;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusAluguel status;

	/**
	 * Preenchido enquanto o aluguel esta ativo. A unicidade impede dois alugueis ativos do mesmo ciclista.
	 */
	@Column(name = "ciclista_ativo_id", unique = true)
	private Long ciclistaAtivoId;

	/**
	 * Impede dois alugueis ativos da mesma bicicleta.
	 */
	@Column(name = "bicicleta_ativa_id", unique = true)
	private Long bicicletaAtivaId;

	@Column(nullable = false, unique = true, length = 80)
	private String chave;

	public Long getId() {
		return this.id;
	}

	public Ciclista getCiclista() {
		return this.ciclista;
	}

	public void setCiclista(Ciclista ciclista) {
		this.ciclista = ciclista;
	}

	public Bicicleta getBicicleta() {
		return this.bicicleta;
	}

	public void setBicicleta(Bicicleta bicicleta) {
		this.bicicleta = bicicleta;
	}

	public Tranca getTranca() {
		return this.tranca;
	}

	public void setTranca(Tranca tranca) {
		this.tranca = tranca;
	}

	public Totem getTotem() {
		return this.totem;
	}

	public void setTotem(Totem totem) {
		this.totem = totem;
	}

	public LocalDateTime getInicio() {
		return this.inicio;
	}

	public void setInicio(LocalDateTime inicio) {
		this.inicio = inicio;
	}

	public BigDecimal getValor() {
		return this.valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public int getDuracaoHoras() {
		return this.duracaoHoras;
	}

	public void setDuracaoHoras(int duracaoHoras) {
		this.duracaoHoras = duracaoHoras;
	}

	public StatusAluguel getStatus() {
		return this.status;
	}

	public void setStatus(StatusAluguel status) {
		this.status = status;
	}

	public Long getCiclistaAtivoId() {
		return this.ciclistaAtivoId;
	}

	public void setCiclistaAtivoId(Long ciclistaAtivoId) {
		this.ciclistaAtivoId = ciclistaAtivoId;
	}

	public Long getBicicletaAtivaId() {
		return this.bicicletaAtivaId;
	}

	public void setBicicletaAtivaId(Long bicicletaAtivaId) {
		this.bicicletaAtivaId = bicicletaAtivaId;
	}

	public String getChave() {
		return this.chave;
	}

	public void setChave(String chave) {
		this.chave = chave;
	}

}
