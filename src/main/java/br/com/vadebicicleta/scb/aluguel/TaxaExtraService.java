package br.com.vadebicicleta.scb.aluguel;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

/**
 * A transacao fica em OperacaoDePagamentoTaxa. A falha de bloqueio e resolvida aqui,
 * depois que a transacao ja terminou.
 */
@Service
public class TaxaExtraService {

	private final TaxaExtraRepository taxas;

	private final OperacaoDePagamentoTaxa operacao;

	public TaxaExtraService(TaxaExtraRepository taxas, OperacaoDePagamentoTaxa operacao) {
		this.taxas = taxas;
		this.operacao = operacao;
	}

	@Transactional(readOnly = true)
	public List<TaxaPendenteResposta> listarPendentes(String email) {
		List<TaxaPendenteResposta> resposta = new ArrayList<>();
		for (TaxaExtra taxa : this.taxas.findByAluguel_Ciclista_EmailAndSituacaoOrderByDataHoraAsc(email,
				SituacaoTaxaExtra.PENDENTE)) {
			resposta.add(new TaxaPendenteResposta(taxa.getId(),
					taxa.getValor().setScale(2, RoundingMode.HALF_UP).toPlainString(), taxa.getSituacao().name(),
					taxa.getDataHora().toString()));
		}
		return resposta;
	}

	public ResultadoPagamentoTaxa pagar(String email, Long id) {
		try {
			return this.operacao.pagar(email, id);
		}
		catch (PessimisticLockingFailureException ex) {
			Optional<ResultadoPagamentoTaxa> paga = this.operacao.estadoSeJaPaga(email, id);
			if (paga.isPresent()) {
				return paga.get();
			}
			throw new ErroDeNegocio(409, "CONFLITO", "Não foi possível concluir o pagamento. Tente novamente.", List.of());
		}
	}

}
