package br.com.vadebicicleta.scb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.vadebicicleta.scb.aluguel.AluguelRepository;
import br.com.vadebicicleta.scb.aluguel.BicicletaRepository;
import br.com.vadebicicleta.scb.aluguel.MensagemRepository;
import br.com.vadebicicleta.scb.aluguel.MensagemSimulada;
import br.com.vadebicicleta.scb.aluguel.StatusAluguel;
import br.com.vadebicicleta.scb.aluguel.StatusBicicleta;
import br.com.vadebicicleta.scb.aluguel.Tranca;
import br.com.vadebicicleta.scb.aluguel.TrancaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class FalhaNotificacaoTest {

	@MockitoBean
	private MensagemRepository mensagens;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AluguelRepository alugueis;

	@Autowired
	private TrancaRepository trancas;

	@Autowired
	private BicicletaRepository bicicletas;

	@BeforeEach
	void falhaAoGravarAMensagem() {
		when(this.mensagens.save(any(MensagemSimulada.class))).thenThrow(new IllegalStateException("falha-de-teste"));
		when(this.mensagens.existsByAluguelId(any())).thenReturn(false);
	}

	@Test
	void falhaDaMensagemNaoDesfazOAluguelENaoRepeteAConfirmacao() throws Exception {
		String email = ApoioApi.email("aviso");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"DEMO-APROVADO\"}"))
				.andExpect(status().isOk());

		String numero = this.trancas.findAll().stream()
				.filter(tranca -> this.bicicletas.existsByTrancaAndStatus(tranca, StatusBicicleta.DISPONIVEL))
				.map(Tranca::getNumero)
				.findFirst()
				.orElseThrow();
		String chave = UUID.randomUUID().toString();

		alugar(sessao, chave, numero)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("APROVADO"));

		Assertions.assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));

		alugar(sessao, chave, numero)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagem", org.hamcrest.Matchers.containsString("já foi processada")));

		verify(this.mensagens, times(1)).save(any(MensagemSimulada.class));
	}

	private org.springframework.test.web.servlet.ResultActions alugar(MockHttpSession sessao, String chave, String numero)
			throws Exception {
		return this.mockMvc.perform(post("/api/alugueis")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"chave\":\"" + chave + "\",\"numeroTranca\":\"" + numero + "\"}"));
	}

}
