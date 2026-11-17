package br.com.vadebicicleta.scb.aluguel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Ate o periodo incluido, a taxa extra e zero. Depois disso, cada bloco iniciado conta por inteiro.
 */
public final class CalculadoraTaxaExtra {

	private CalculadoraTaxaExtra() {
	}

	public static BigDecimal calcular(LocalDateTime inicio, LocalDateTime fim, int horasIncluidas, int blocoMinutos,
			BigDecimal tarifa) {
		if (inicio == null || fim == null || tarifa == null || blocoMinutos < 1 || horasIncluidas < 0) {
			throw new IllegalArgumentException("Tarifa extra incompleta.");
		}
		if (!fim.isAfter(inicio)) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		Duration excesso = Duration.between(inicio, fim).minus(Duration.ofHours(horasIncluidas));
		if (excesso.isZero() || excesso.isNegative()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		long segundos = excesso.getSeconds();
		if (excesso.getNano() > 0) {
			segundos++;
		}
		long tamanho = blocoMinutos * 60L;
		long blocos = (segundos + tamanho - 1) / tamanho;
		return tarifa.multiply(BigDecimal.valueOf(blocos)).setScale(2, RoundingMode.HALF_UP);
	}

}
