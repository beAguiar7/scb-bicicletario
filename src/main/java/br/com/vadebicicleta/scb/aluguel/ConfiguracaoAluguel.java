package br.com.vadebicicleta.scb.aluguel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Valores ficticios. O PDF do trabalho nao definiu a taxa do aluguel nem a taxa extra.
 */
@Component
@ConfigurationProperties(prefix = "scb.aluguel")
public class ConfiguracaoAluguel {

	private BigDecimal valor = new BigDecimal("10.00");

	private int duracaoHoras = 2;

	private BigDecimal taxaExtra = new BigDecimal("2.50");

	private int blocoMinutos = 30;

	public BigDecimal getValor() {
		return this.valor.setScale(2, RoundingMode.HALF_UP);
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

	public BigDecimal getTaxaExtra() {
		return this.taxaExtra.setScale(2, RoundingMode.HALF_UP);
	}

	public void setTaxaExtra(BigDecimal taxaExtra) {
		this.taxaExtra = taxaExtra;
	}

	public int getBlocoMinutos() {
		return this.blocoMinutos;
	}

	public void setBlocoMinutos(int blocoMinutos) {
		this.blocoMinutos = blocoMinutos;
	}

	public String valorTexto() {
		return getValor().toPlainString();
	}

	public String formatar(BigDecimal quantia) {
		NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		return formato.format(quantia).replace('\u00A0', ' ');
	}

	public String texto() {
		String unidade = this.duracaoHoras == 1 ? "hora" : "horas";
		return formatar(getValor()) + " por " + this.duracaoHoras + " " + unidade;
	}

}
