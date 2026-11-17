package br.com.vadebicicleta.scb.aluguel;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

/**
 * A transacao fica em OperacaoDeDevolucao. A leitura repetida e a disputa pela mesma
 * tranca sao resolvidas depois que a transacao ja terminou.
 */
@Service
public class DevolucaoService {

	private final OperacaoDeDevolucao operacao;

	public DevolucaoService(OperacaoDeDevolucao operacao) {
		this.operacao = operacao;
	}

	public ResultadoDevolucao devolver(PedidoDevolucao pedido) {
		String rfid = pedido.rfid().trim().toUpperCase(Locale.ROOT);
		String numero = pedido.numeroTranca().trim().toUpperCase(Locale.ROOT);
		try {
			return this.operacao.executar(rfid, numero);
		}
		catch (DataIntegrityViolationException | PessimisticLockingFailureException ex) {
			return this.operacao.repetirSeJaDevolvida(rfid, numero).orElseThrow(() -> conflito(rfid, numero));
		}
	}

	private ErroDeNegocio conflito(String rfid, String numero) {
		if (this.operacao.trancaOcupadaPorOutra(rfid, numero)) {
			return new ErroDeNegocio(409, "TRANCA_OCUPADA", "Esta tranca já está ocupada.", List.of());
		}
		return new ErroDeNegocio(409, "CONFLITO", "Não foi possível concluir a devolução. Tente novamente.", List.of());
	}

}
