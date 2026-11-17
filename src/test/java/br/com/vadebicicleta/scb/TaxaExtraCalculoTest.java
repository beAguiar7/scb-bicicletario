package br.com.vadebicicleta.scb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.vadebicicleta.scb.aluguel.CalculadoraTaxaExtra;

class TaxaExtraCalculoTest {

	private static final BigDecimal TARIFA = new BigDecimal("2.50");

	private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 8, 0, 0);

	@Test
	void duasHorasNaoCobraEOPrimeiroInstanteSeguinteCobraUmBloco() {
		LocalDateTime limite = INICIO.plusHours(2);
		assertEquals(new BigDecimal("0.00"), calcular(INICIO, limite));
		assertEquals(new BigDecimal("2.50"), calcular(INICIO, limite.plusNanos(1)));
		assertEquals(new BigDecimal("2.50"), calcular(INICIO, limite.plusSeconds(1)));
	}

	@Test
	void blocosIniciadosIncluemAFracao() {
		assertEquals(new BigDecimal("2.50"), calcular(INICIO, INICIO.plusHours(2).plusMinutes(1)));
		assertEquals(new BigDecimal("2.50"), calcular(INICIO, INICIO.plusHours(2).plusMinutes(30)));
		assertEquals(new BigDecimal("5.00"), calcular(INICIO, INICIO.plusHours(2).plusMinutes(31)));
	}

	@Test
	void tarifaDoBlocoEConfiguravel() {
		assertEquals(new BigDecimal("6.00"), CalculadoraTaxaExtra.calcular(INICIO, INICIO.plusHours(2).plusMinutes(31), 2,
				30, new BigDecimal("3.00")));
	}

	private static BigDecimal calcular(LocalDateTime inicio, LocalDateTime fim) {
		return CalculadoraTaxaExtra.calcular(inicio, fim, 2, 30, TARIFA);
	}

}
