package br.com.vadebicicleta.scb.aluguel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

/**
 * A transacao fica em OperacaoDeAluguel. Aqui a chave repetida e a falha de concorrencia
 * sao resolvidas depois que a transacao ja terminou.
 */
@Service
public class AluguelService {

	private final OperacaoDeAluguel operacao;

	private final CobrancaRepository cobrancas;

	private final AluguelRepository alugueis;

	private final TotemRepository totens;

	private final TrancaRepository trancas;

	private final BicicletaRepository bicicletas;

	private final MensagemRepository mensagens;

	private final NotificacaoSimulada notificacao;

	private final ConfiguracaoAluguel configuracao;

	public AluguelService(OperacaoDeAluguel operacao, CobrancaRepository cobrancas, AluguelRepository alugueis,
			TotemRepository totens, TrancaRepository trancas, BicicletaRepository bicicletas,
			MensagemRepository mensagens, ConfiguracaoAluguel configuracao, NotificacaoSimulada notificacao) {
		this.operacao = operacao;
		this.cobrancas = cobrancas;
		this.alugueis = alugueis;
		this.totens = totens;
		this.trancas = trancas;
		this.bicicletas = bicicletas;
		this.mensagens = mensagens;
		this.configuracao = configuracao;
		this.notificacao = notificacao;
	}

	public ResultadoAluguel confirmar(String email, PedidoAluguel pedido) {
		String chave = pedido.chave().trim();
		try {
			if (this.cobrancas.findByChave(chave).isPresent()) {
				return this.operacao.repetir(email, chave);
			}
			Execucao execucao = this.operacao.executar(email, chave, pedido.numeroTranca());
			if (execucao.notificar()) {
				this.notificacao.registrar(execucao.resultado().aluguel().id());
			}
			return execucao.resultado();
		}
		catch (DataIntegrityViolationException | PessimisticLockingFailureException ex) {
			if (this.cobrancas.findByChave(chave).isPresent()) {
				return this.operacao.repetir(email, chave);
			}
			throw new ErroDeNegocio(409, "CONFLITO", "Não foi possível concluir o aluguel. Tente novamente.", List.of());
		}
	}

	@Transactional(readOnly = true)
	public TaxaResposta taxa() {
		return new TaxaResposta(this.configuracao.valorTexto(), this.configuracao.getDuracaoHoras(),
				this.configuracao.texto(), "Simulação: nenhuma cobrança real é feita.", true);
	}

	@Transactional(readOnly = true)
	public List<TotemDisponivel> listarTotens() {
		List<TotemDisponivel> resposta = new ArrayList<>();
		for (Totem totem : this.totens.findAll()) {
			List<String> numeros = new ArrayList<>();
			for (Tranca tranca : this.trancas.findByTotemOrderByNumeroAsc(totem)) {
				if (this.bicicletas.existsByTrancaAndStatus(tranca, StatusBicicleta.DISPONIVEL)) {
					numeros.add(tranca.getNumero());
				}
			}
			resposta.add(new TotemDisponivel(totem.getNome(), totem.getCapacidade(), List.copyOf(numeros)));
		}
		return resposta;
	}

	@Transactional(readOnly = true)
	public AluguelAtual consultarAtivo(String email) {
		return this.alugueis.findByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO)
				.map(aluguel -> new AluguelAtual(true, OperacaoDeAluguel.resumo(aluguel)))
				.orElseGet(() -> new AluguelAtual(false, null));
	}

	@Transactional(readOnly = true)
	public List<MensagemResposta> listarMensagens(String email) {
		List<MensagemResposta> resposta = new ArrayList<>();
		for (MensagemSimulada mensagem : this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(email)) {
			resposta.add(MensagemResposta.de(mensagem));
		}
		return resposta;
	}

}
