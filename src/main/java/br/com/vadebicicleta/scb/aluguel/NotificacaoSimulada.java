package br.com.vadebicicleta.scb.aluguel;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;
import br.com.vadebicicleta.scb.erro.RegistroDeFalhas;

/**
 * Grava a confirmacao na caixa de saida do H2.
 * Uma falha aqui nao desfaz o aluguel, e repetir a operacao nao cria outra mensagem.
 */
@Service
public class NotificacaoSimulada {

	public static final String AVISO = "Envio de e-mail simulado — nenhuma mensagem externa foi enviada.";

	private final MensagemRepository mensagens;

	private final AluguelRepository alugueis;

	private final CiclistaRepository ciclistas;

	private final ConfiguracaoAluguel configuracao;

	private final RegistroDeFalhas registro;

	private final TransactionTemplate transacaoNova;

	public NotificacaoSimulada(MensagemRepository mensagens, AluguelRepository alugueis, CiclistaRepository ciclistas,
			ConfiguracaoAluguel configuracao, RegistroDeFalhas registro, PlatformTransactionManager transacoes) {
		this.mensagens = mensagens;
		this.alugueis = alugueis;
		this.ciclistas = ciclistas;
		this.configuracao = configuracao;
		this.registro = registro;
		this.transacaoNova = new TransactionTemplate(transacoes);
		this.transacaoNova.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void registrar(Long aluguelId) {
		try {
			this.transacaoNova.executeWithoutResult(status -> gravar(aluguelId));
		}
		catch (RuntimeException ex) {
			this.registro.registrar("NOTIFICACAO_NAO_ENVIADA", "aluguel " + aluguelId, ex.getClass().getSimpleName(), null);
		}
	}

	private void gravar(Long aluguelId) {
		if (this.mensagens.existsByAluguelId(aluguelId)) {
			return;
		}
		Aluguel aluguel = this.alugueis.findById(aluguelId).orElseThrow();
		Ciclista destinatario = this.ciclistas.getReferenceById(aluguel.getCiclista().getId());
		MensagemSimulada mensagem = new MensagemSimulada();
		mensagem.setDestinatario(destinatario);
		mensagem.setAluguelId(aluguelId);
		mensagem.setAssunto("Confirmação do empréstimo " + aluguelId);
		mensagem.setTexto(montarTexto(aluguel));
		mensagem.setCriadaEm(aluguel.getInicio());
		this.mensagens.save(mensagem);
	}

	private String montarTexto(Aluguel aluguel) {
		String horario = aluguel.getInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
		return AVISO + "\n\n"
				+ "Confirmação do empréstimo " + aluguel.getId() + "\n"
				+ "Bicicleta: " + aluguel.getBicicleta().getRfid() + "\n"
				+ "Totem: " + aluguel.getTotem().getNome() + "\n"
				+ "Tranca: " + aluguel.getTranca().getNumero() + "\n"
				+ "Horário: " + horario + "\n"
				+ "Taxa: " + this.configuracao.formatar(aluguel.getValor()) + "\n"
				+ "Duração inicial: " + aluguel.getDuracaoHoras() + " horas\n";
	}

}
