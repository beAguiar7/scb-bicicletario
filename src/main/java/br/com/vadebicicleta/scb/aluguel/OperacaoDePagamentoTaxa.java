package br.com.vadebicicleta.scb.aluguel;

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
 * A linha da taxa fica bloqueada durante a tentativa.
 * A devolucao e a bicicleta nao sao alteradas. O e-mail fica fora desta etapa.
 */
@Service
public class OperacaoDePagamentoTaxa {

	private static final Logger log = LoggerFactory.getLogger(OperacaoDePagamentoTaxa.class);

	private final TaxaExtraRepository taxas;

	private final CartaoRepository cartoes;

	public OperacaoDePagamentoTaxa(TaxaExtraRepository taxas, CartaoRepository cartoes) {
		this.taxas = taxas;
		this.cartoes = cartoes;
	}

	@Transactional
	public ResultadoPagamentoTaxa pagar(String email, Long id) {
		TaxaExtra taxa = this.taxas.findByIdComBloqueio(id).orElseThrow(OperacaoDePagamentoTaxa::inexistente);
		exigirDono(email, taxa);
		if (taxa.getSituacao() == SituacaoTaxaExtra.PAGA) {
			return montar(taxa, "Esta taxa extra já está paga.", true);
		}
		CartaoDemonstracao cartao = this.cartoes.findByCiclista(taxa.getAluguel().getCiclista()).orElse(null);
		if (cartao == null) {
			throw new ErroDeNegocio(409, "SEM_CARTAO",
					"Cadastre um cartão de demonstração para pagar a taxa extra.", List.of());
		}
		LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		if (cartao.isCobrancaSimuladaAprovada()) {
			taxa.setSituacao(SituacaoTaxaExtra.PAGA);
			taxa.setResultadoTentativa(SituacaoTaxaExtra.PAGA);
		}
		else {
			taxa.setSituacao(SituacaoTaxaExtra.PENDENTE);
			taxa.setResultadoTentativa(SituacaoTaxaExtra.PENDENTE);
		}
		taxa.setTentativaEm(agora);
		this.taxas.saveAndFlush(taxa);
		log.info("taxa extra reprocessada id={} situacao={}", taxa.getId(), taxa.getSituacao());
		String mensagem = taxa.getSituacao() == SituacaoTaxaExtra.PAGA
				? "Pagamento simulado aprovado. A taxa extra foi paga."
				: "Pagamento simulado recusado. A taxa extra continua pendente.";
		return montar(taxa, mensagem, false);
	}

	@Transactional(readOnly = true)
	public Optional<ResultadoPagamentoTaxa> estadoSeJaPaga(String email, Long id) {
		TaxaExtra taxa = this.taxas.findById(id).orElse(null);
		if (taxa == null || !taxa.getAluguel().getCiclista().getEmail().equals(email)
				|| taxa.getSituacao() != SituacaoTaxaExtra.PAGA) {
			return Optional.empty();
		}
		return Optional.of(montar(taxa, "Esta taxa extra já está paga.", true));
	}

	private static void exigirDono(String email, TaxaExtra taxa) {
		if (!taxa.getAluguel().getCiclista().getEmail().equals(email)) {
			throw inexistente();
		}
	}

	private static ResultadoPagamentoTaxa montar(TaxaExtra taxa, String mensagem, boolean jaPaga) {
		String tentativa = taxa.getTentativaEm() == null ? null : taxa.getTentativaEm().toString();
		return new ResultadoPagamentoTaxa(taxa.getId(),
				taxa.getValor().setScale(2, RoundingMode.HALF_UP).toPlainString(), taxa.getSituacao().name(),
				taxa.getDataHora().toString(), tentativa, mensagem, true, jaPaga);
	}

	private static ErroDeNegocio inexistente() {
		return new ErroDeNegocio(404, "TAXA_INEXISTENTE", "Não encontramos essa taxa extra.", List.of());
	}

}
