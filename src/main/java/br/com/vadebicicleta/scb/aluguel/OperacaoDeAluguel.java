package br.com.vadebicicleta.scb.aluguel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vadebicicleta.scb.cartao.CartaoDemonstracao;
import br.com.vadebicicleta.scb.cartao.CartaoRepository;
import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;
import br.com.vadebicicleta.scb.erro.ErroDeNegocio;
import br.com.vadebicicleta.scb.erro.RegistroDeFalhas;

/**
 * Uma confirmacao por vez, com a linha do ciclista e a tranca bloqueadas.
 * A cobranca e o aluguel entram no banco somente dentro desta transacao.
 */
@Service
public class OperacaoDeAluguel {

	private static final Logger log = LoggerFactory.getLogger(OperacaoDeAluguel.class);

	private final CiclistaRepository ciclistas;

	private final CartaoRepository cartoes;

	private final TrancaRepository trancas;

	private final BicicletaRepository bicicletas;

	private final AluguelRepository alugueis;

	private final CobrancaRepository cobrancas;

	private final ConfiguracaoAluguel configuracao;

	private final RegistroDeFalhas registro;

	public OperacaoDeAluguel(CiclistaRepository ciclistas, CartaoRepository cartoes, TrancaRepository trancas,
			BicicletaRepository bicicletas, AluguelRepository alugueis, CobrancaRepository cobrancas,
			ConfiguracaoAluguel configuracao, RegistroDeFalhas registro) {
		this.ciclistas = ciclistas;
		this.cartoes = cartoes;
		this.trancas = trancas;
		this.bicicletas = bicicletas;
		this.alugueis = alugueis;
		this.cobrancas = cobrancas;
		this.configuracao = configuracao;
		this.registro = registro;
	}

	@Transactional
	public Execucao executar(String email, String chave, String numeroTranca) {
		Ciclista ciclista = this.ciclistas.findByEmailComBloqueio(email)
				.orElseThrow(() -> new ErroDeNegocio(401, "NAO_AUTENTICADO", "Faça login para continuar.", List.of()));
		Cobranca existente = this.cobrancas.findByChave(chave).orElse(null);
		if (existente != null) {
			return new Execucao(montar(exigirDono(email, existente), "Esta confirmação já foi processada."), false);
		}

		CartaoDemonstracao cartao = this.cartoes.findByCiclista(ciclista)
				.orElseThrow(() -> new ErroDeNegocio(409, "SEM_CARTAO",
						"Cadastre um cartão de demonstração antes de alugar.", List.of()));
		if (this.alugueis.existsByCiclistaAndStatus(ciclista, StatusAluguel.ATIVO)) {
			throw new ErroDeNegocio(409, "ALUGUEL_ATIVO", "Você já tem um aluguel em andamento.", List.of());
		}

		String numero = numeroTranca.trim().toUpperCase(Locale.ROOT);
		Tranca tranca = this.trancas.findByNumeroComBloqueio(numero)
				.orElseThrow(() -> new ErroDeNegocio(404, "TRANCA_INEXISTENTE", "Não encontramos essa tranca.", List.of()));
		Bicicleta bicicleta = this.bicicletas.findByTrancaAndStatusComBloqueio(tranca, StatusBicicleta.DISPONIVEL)
				.orElseThrow(() -> new ErroDeNegocio(409, "TRANCA_SEM_BICICLETA",
						"Esta tranca não tem bicicleta disponível.", List.of()));

		BigDecimal valor = this.configuracao.getValor();
		LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		Cobranca cobranca = novaCobranca(ciclista, valor, agora, chave, cartao.getReferencia());

		if (!cartao.isCobrancaSimuladaAprovada()) {
			cobranca.setResultado(ResultadoCobranca.RECUSADO);
			this.cobrancas.save(cobranca);
			this.registro.registrar("COBRANCA_RECUSADA", "POST /api/alugueis", "resultado=RECUSADO", null);
			return new Execucao(montar(cobranca, "Pagamento simulado recusado. A bicicleta continua na tranca."), false);
		}

		bicicleta.setStatus(StatusBicicleta.EM_USO);
		bicicleta.setTranca(null);
		this.bicicletas.save(bicicleta);

		Aluguel aluguel = new Aluguel();
		aluguel.setCiclista(ciclista);
		aluguel.setBicicleta(bicicleta);
		aluguel.setTranca(tranca);
		aluguel.setTotem(tranca.getTotem());
		aluguel.setInicio(agora);
		aluguel.setValor(valor);
		aluguel.setDuracaoHoras(this.configuracao.getDuracaoHoras());
		aluguel.setStatus(StatusAluguel.ATIVO);
		aluguel.setCiclistaAtivoId(ciclista.getId());
		aluguel.setBicicletaAtivaId(bicicleta.getId());
		aluguel.setChave(chave);
		this.alugueis.saveAndFlush(aluguel);

		cobranca.setResultado(ResultadoCobranca.APROVADO);
		cobranca.setAluguel(aluguel);
		this.cobrancas.save(cobranca);
		log.info("aluguel iniciado id={}", aluguel.getId());
		return new Execucao(montar(cobranca, "Pagamento simulado aprovado. A tranca foi aberta na simulação."), true);
	}

	@Transactional(readOnly = true)
	public ResultadoAluguel repetir(String email, String chave) {
		Cobranca cobranca = this.cobrancas.findByChave(chave)
				.orElseThrow(() -> new ErroDeNegocio(409, "CONFLITO",
						"Não foi possível confirmar o aluguel. Tente novamente.", List.of()));
		return montar(exigirDono(email, cobranca), "Esta confirmação já foi processada.");
	}

	private Cobranca exigirDono(String email, Cobranca cobranca) {
		if (!cobranca.getCiclista().getEmail().equals(email)) {
			throw new ErroDeNegocio(409, "CONFLITO", "Não foi possível confirmar o aluguel. Tente novamente.", List.of());
		}
		return cobranca;
	}

	private static Cobranca novaCobranca(Ciclista ciclista, BigDecimal valor, LocalDateTime agora, String chave,
			String referencia) {
		Cobranca cobranca = new Cobranca();
		cobranca.setCiclista(ciclista);
		cobranca.setValor(valor);
		cobranca.setDataHora(agora);
		cobranca.setChave(chave);
		cobranca.setReferenciaCartao(referencia);
		return cobranca;
	}

	private static ResultadoAluguel montar(Cobranca cobranca, String mensagem) {
		AluguelResumo resumo = cobranca.getAluguel() == null ? null : resumo(cobranca.getAluguel());
		return new ResultadoAluguel(cobranca.getResultado().name(), mensagem, cobranca.getId(),
				cobranca.getValor().setScale(2, RoundingMode.HALF_UP).toPlainString(), cobranca.getDataHora().toString(),
				true, resumo);
	}

	static AluguelResumo resumo(Aluguel aluguel) {
		return new AluguelResumo(aluguel.getId(), aluguel.getBicicleta().getRfid(), aluguel.getTotem().getNome(),
				aluguel.getTranca().getNumero(), aluguel.getInicio().toString(),
				aluguel.getValor().setScale(2, RoundingMode.HALF_UP).toPlainString(), aluguel.getDuracaoHoras());
	}

}
