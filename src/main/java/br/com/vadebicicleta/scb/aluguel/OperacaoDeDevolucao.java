package br.com.vadebicicleta.scb.aluguel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vadebicicleta.scb.cartao.CartaoDemonstracao;
import br.com.vadebicicleta.scb.cartao.CartaoRepository;
import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

/**
 * A tranca de destino e a bicicleta ficam bloqueadas durante a devolucao.
 * A ordem e a mesma do aluguel: primeiro a tranca, depois a bicicleta.
 * A taxa extra entra na mesma transacao. Recusa nao desfaz a devolucao.
 * Reprocessar pendencia e enviar e-mail ficam fora desta etapa.
 */
@Service
public class OperacaoDeDevolucao {

	private static final Logger log = LoggerFactory.getLogger(OperacaoDeDevolucao.class);

	private final TrancaRepository trancas;

	private final BicicletaRepository bicicletas;

	private final AluguelRepository alugueis;

	private final TaxaExtraRepository taxas;

	private final CartaoRepository cartoes;

	private final ConfiguracaoAluguel configuracao;

	public OperacaoDeDevolucao(TrancaRepository trancas, BicicletaRepository bicicletas, AluguelRepository alugueis,
			TaxaExtraRepository taxas, CartaoRepository cartoes, ConfiguracaoAluguel configuracao) {
		this.trancas = trancas;
		this.bicicletas = bicicletas;
		this.alugueis = alugueis;
		this.taxas = taxas;
		this.cartoes = cartoes;
		this.configuracao = configuracao;
	}

	@Transactional
	public ResultadoDevolucao executar(String rfid, String numeroTranca) {
		Tranca destino = this.trancas.findByNumeroComBloqueio(numeroTranca)
				.orElseThrow(() -> new ErroDeNegocio(404, "TRANCA_INEXISTENTE", "Não encontramos essa tranca.", List.of()));
		Bicicleta bicicleta = this.bicicletas.findByRfidComBloqueio(rfid).orElseThrow(OperacaoDeDevolucao::semAluguel);

		Bicicleta ocupante = this.bicicletas.findByTranca(destino).orElse(null);
		if (ocupante != null) {
			if (ocupante.getId().equals(bicicleta.getId())) {
				return confirmacaoOriginal(bicicleta, destino);
			}
			throw ocupada();
		}

		Aluguel aluguel = this.alugueis.findByBicicletaAndStatusComBloqueio(bicicleta, StatusAluguel.ATIVO)
				.orElseThrow(OperacaoDeDevolucao::semAluguel);
		LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		aluguel.setStatus(StatusAluguel.ENCERRADO);
		aluguel.setFim(agora);
		aluguel.setCiclistaAtivoId(null);
		aluguel.setBicicletaAtivaId(null);
		aluguel.setTrancaDevolucao(destino);
		aluguel.setTotemDevolucao(destino.getTotem());
		bicicleta.setStatus(StatusBicicleta.DISPONIVEL);
		bicicleta.setTranca(destino);
		this.alugueis.saveAndFlush(aluguel);
		this.bicicletas.saveAndFlush(bicicleta);
		TaxaExtra taxa = registrarTaxa(aluguel, agora);
		log.info("devolucao registrada aluguel={} tranca={}", aluguel.getId(), destino.getNumero());
		return montar(aluguel, "Bicicleta devolvida. A tranca foi fechada na simulação.", false, taxa);
	}

	@Transactional(readOnly = true)
	public Optional<ResultadoDevolucao> repetirSeJaDevolvida(String rfid, String numeroTranca) {
		Tranca destino = this.trancas.findByNumero(numeroTranca).orElse(null);
		Bicicleta bicicleta = this.bicicletas.findByRfid(rfid).orElse(null);
		if (destino == null || bicicleta == null || bicicleta.getTranca() == null
				|| !bicicleta.getTranca().getId().equals(destino.getId())) {
			return Optional.empty();
		}
		if (this.alugueis.existsByBicicletaAndStatus(bicicleta, StatusAluguel.ATIVO)) {
			return Optional.empty();
		}
		return this.alugueis
				.findFirstByBicicletaAndStatusAndTrancaDevolucaoOrderByFimDesc(bicicleta, StatusAluguel.ENCERRADO, destino)
				.map(aluguel -> montar(aluguel, "Esta devolução já foi registrada.", true, taxaGuardada(aluguel)));
	}

	@Transactional(readOnly = true)
	public boolean trancaOcupadaPorOutra(String rfid, String numeroTranca) {
		Tranca destino = this.trancas.findByNumero(numeroTranca).orElse(null);
		Bicicleta ocupante = destino == null ? null : this.bicicletas.findByTranca(destino).orElse(null);
		return ocupante != null && !ocupante.getRfid().equals(rfid);
	}

	private TaxaExtra registrarTaxa(Aluguel aluguel, LocalDateTime fim) {
		BigDecimal valor = CalculadoraTaxaExtra.calcular(aluguel.getInicio(), fim, this.configuracao.getDuracaoHoras(),
				this.configuracao.getBlocoMinutos(), this.configuracao.getTaxaExtra());
		if (valor.signum() == 0) {
			return null;
		}
		TaxaExtra taxa = new TaxaExtra();
		taxa.setAluguel(aluguel);
		taxa.setValor(valor);
		taxa.setDataHora(fim);
		taxa.setSituacao(decidir(aluguel));
		this.taxas.saveAndFlush(taxa);
		log.info("taxa extra registrada aluguel={} situacao={}", aluguel.getId(), taxa.getSituacao());
		return taxa;
	}

	private SituacaoTaxaExtra decidir(Aluguel aluguel) {
		CartaoDemonstracao cartao = this.cartoes.findByCiclista(aluguel.getCiclista()).orElse(null);
		if (cartao != null && cartao.isCobrancaSimuladaAprovada()) {
			return SituacaoTaxaExtra.PAGA;
		}
		return SituacaoTaxaExtra.PENDENTE;
	}

	private ResultadoDevolucao confirmacaoOriginal(Bicicleta bicicleta, Tranca destino) {
		if (this.alugueis.findByBicicletaAndStatusComBloqueio(bicicleta, StatusAluguel.ATIVO).isPresent()) {
			throw semAluguel();
		}
		Aluguel aluguel = this.alugueis
				.findFirstByBicicletaAndStatusAndTrancaDevolucaoOrderByFimDesc(bicicleta, StatusAluguel.ENCERRADO, destino)
				.orElseThrow(OperacaoDeDevolucao::semAluguel);
		return montar(aluguel, "Esta devolução já foi registrada.", true, taxaGuardada(aluguel));
	}

	private TaxaExtra taxaGuardada(Aluguel aluguel) {
		return this.taxas.findByAluguel(aluguel).orElse(null);
	}

	private static ResultadoDevolucao montar(Aluguel aluguel, String mensagem, boolean repetida, TaxaExtra taxa) {
		TaxaExtraResposta resposta = null;
		if (taxa != null) {
			resposta = new TaxaExtraResposta(taxa.getValor().setScale(2, RoundingMode.HALF_UP).toPlainString(),
					taxa.getSituacao().name(), true);
		}
		return new ResultadoDevolucao(mensagem, aluguel.getBicicleta().getRfid(),
				aluguel.getTrancaDevolucao().getNumero(), aluguel.getFim().toString(), repetida, resposta);
	}

	private static ErroDeNegocio semAluguel() {
		return new ErroDeNegocio(409, "BICICLETA_SEM_ALUGUEL", "Não há aluguel ativo para esta bicicleta.", List.of());
	}

	private static ErroDeNegocio ocupada() {
		return new ErroDeNegocio(409, "TRANCA_OCUPADA", "Esta tranca já está ocupada.", List.of());
	}

}
