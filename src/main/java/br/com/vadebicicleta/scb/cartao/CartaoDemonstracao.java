package br.com.vadebicicleta.scb.cartao;

import br.com.vadebicicleta.scb.ciclista.Ciclista;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * So guarda a referencia ficticia, a mascara e o resultado usado na simulacao.
 */
@Entity
@Table(name = "cartao_demonstracao")
public class CartaoDemonstracao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "ciclista_id", nullable = false, unique = true)
	private Ciclista ciclista;

	@Column(nullable = false, length = 40)
	private String referencia;

	@Column(nullable = false, length = 30)
	private String mascara;

	@Column(nullable = false, length = 80)
	private String titular;

	@Column(nullable = false)
	private boolean cobrancaSimuladaAprovada;

	public Long getId() {
		return this.id;
	}

	public Ciclista getCiclista() {
		return this.ciclista;
	}

	public void setCiclista(Ciclista ciclista) {
		this.ciclista = ciclista;
	}

	public String getReferencia() {
		return this.referencia;
	}

	public void setReferencia(String referencia) {
		this.referencia = referencia;
	}

	public String getMascara() {
		return this.mascara;
	}

	public void setMascara(String mascara) {
		this.mascara = mascara;
	}

	public String getTitular() {
		return this.titular;
	}

	public void setTitular(String titular) {
		this.titular = titular;
	}

	public boolean isCobrancaSimuladaAprovada() {
		return this.cobrancaSimuladaAprovada;
	}

	public void setCobrancaSimuladaAprovada(boolean cobrancaSimuladaAprovada) {
		this.cobrancaSimuladaAprovada = cobrancaSimuladaAprovada;
	}

}
