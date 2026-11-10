package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.com.vadebicicleta.scb.aluguel.AluguelRepository;
import br.com.vadebicicleta.scb.aluguel.Bicicleta;
import br.com.vadebicicleta.scb.aluguel.BicicletaRepository;
import br.com.vadebicicleta.scb.aluguel.Cobranca;
import br.com.vadebicicleta.scb.aluguel.CobrancaRepository;
import br.com.vadebicicleta.scb.aluguel.DadosIniciais;
import br.com.vadebicicleta.scb.aluguel.MensagemRepository;
import br.com.vadebicicleta.scb.aluguel.MensagemSimulada;
import br.com.vadebicicleta.scb.aluguel.NotificacaoSimulada;
import br.com.vadebicicleta.scb.aluguel.ResultadoCobranca;
import br.com.vadebicicleta.scb.aluguel.StatusAluguel;
import br.com.vadebicicleta.scb.aluguel.StatusBicicleta;
import br.com.vadebicicleta.scb.aluguel.Totem;
import br.com.vadebicicleta.scb.aluguel.TotemRepository;
import br.com.vadebicicleta.scb.aluguel.Tranca;
import br.com.vadebicicleta.scb.aluguel.TrancaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AluguelTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TotemRepository totens;

	@Autowired
	private TrancaRepository trancas;

	@Autowired
	private BicicletaRepository bicicletas;

	@Autowired
	private AluguelRepository alugueis;

	@Autowired
	private CobrancaRepository cobrancas;

	@Autowired
	private MensagemRepository mensagens;

	@Autowired
	private DadosIniciais dadosIniciais;

	@Test
	void dadosIniciaisNaoSeDuplicamNemDevolvemBicicleta() throws Exception {
		assertEquals(2, this.totens.count());
		assertEquals(40, this.trancas.count());
		assertEquals(40, this.bicicletas.count());
		assertEquals(20, this.trancas.countByTotem_Nome("Totem Praça Central"));
		assertEquals(20, this.trancas.countByTotem_Nome("Totem Campus Norte"));

		Set<String> numeros = new HashSet<>();
		Set<String> rfids = new HashSet<>();
		for (Tranca tranca : this.trancas.findAll()) {
			numeros.add(tranca.getNumero());
		}
		for (Bicicleta bicicleta : this.bicicletas.findAll()) {
			rfids.add(bicicleta.getRfid());
		}
		assertEquals(40, numeros.size());
		assertEquals(40, rfids.size());

		Bicicleta bicicleta = this.bicicletas.findAll().get(0);
		bicicleta.setStatus(StatusBicicleta.EM_USO);
		bicicleta.setTranca(null);
		this.bicicletas.save(bicicleta);

		this.dadosIniciais.run(null);

		assertEquals(40, this.bicicletas.count());
		assertEquals(StatusBicicleta.EM_USO, this.bicicletas.findById(bicicleta.getId()).orElseThrow().getStatus());
		assertNull(this.bicicletas.findById(bicicleta.getId()).orElseThrow().getTranca());
	}

	@Test
	void nomeDoTotemNaoPodeRepetir() {
		Totem repetido = new Totem();
		repetido.setNome("Totem Praça Central");
		repetido.setCapacidade(20);
		assertThrows(DataIntegrityViolationException.class, () -> this.totens.saveAndFlush(repetido));
	}

	@Test
	void duasExecucoesSimultaneasNaoDuplicamTotens() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch prontos = new CountDownLatch(2);
		CountDownLatch seguir = new CountDownLatch(1);
		try {
			Future<?> uma = pool.submit(() -> executar(prontos, seguir));
			Future<?> outra = pool.submit(() -> executar(prontos, seguir));
			assertTrue(prontos.await(5, TimeUnit.SECONDS));
			seguir.countDown();
			uma.get(5, TimeUnit.SECONDS);
			outra.get(5, TimeUnit.SECONDS);
		}
		finally {
			pool.shutdownNow();
		}
		assertEquals(2, this.totens.count());
		assertEquals(40, this.trancas.count());
	}

	private void executar(CountDownLatch prontos, CountDownLatch seguir) {
		prontos.countDown();
		try {
			if (!seguir.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("A outra execucao nao iniciou.");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("A execucao foi interrompida.", ex);
		}
		this.dadosIniciais.run(null);
	}

	@Test
	void mostraATaxaAntesDoAluguel() throws Exception {
		String email = ApoioApi.email("taxa");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		this.mockMvc.perform(get("/api/aluguel/taxa").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valor").value("10.00"))
				.andExpect(jsonPath("$.duracaoHoras").value(2))
				.andExpect(jsonPath("$.simulacao").value(true))
				.andExpect(jsonPath("$.aviso", containsString("nenhuma cobrança real")));

		this.mockMvc.perform(get("/api/totens").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].capacidade").value(20))
				.andExpect(content().string(not(containsString("RFID-"))));
	}

	@Test
	void aluguelSemCartaoNaoCobra() throws Exception {
		String email = ApoioApi.email("semcartao");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		String numero = trancaLivre();

		alugar(sessao, chave(), numero)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SEM_CARTAO"));

		assertEquals(0, this.cobrancas.countByCiclista_Email(email));
		assertTrue(this.bicicletas.existsByTrancaAndStatus(this.trancas.findByNumero(numero).orElseThrow(),
				StatusBicicleta.DISPONIVEL));
	}

	@Test
	void trancaInexistenteNaoCobra() throws Exception {
		String email = ciclistaComCartao("inexistente", "DEMO-APROVADO");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		alugar(sessao, chave(), "ZZ-99")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("TRANCA_INEXISTENTE"));

		assertEquals(0, this.cobrancas.countByCiclista_Email(email));
	}

	@Test
	void pagamentoAprovadoAbreATrancaEGeraUmaConfirmacao() throws Exception {
		String email = ciclistaComCartao("aprovado", "DEMO-APROVADO");
		String outro = ciclistaComCartao("vizinho", "DEMO-APROVADO");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		MockHttpSession sessaoOutro = ApoioApi.entrar(this.mockMvc, outro);
		String numero = trancaLivre();
		String codigo = chave();

		MvcResult resultado = alugar(sessao, codigo, numero.toLowerCase())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("APROVADO"))
				.andExpect(jsonPath("$.simulacao").value(true))
				.andExpect(jsonPath("$.valor").value("10.00"))
				.andExpect(jsonPath("$.mensagem", containsString("aprovado")))
				.andExpect(jsonPath("$.aluguel.numeroTranca").value(numero))
				.andExpect(jsonPath("$.aluguel.duracaoHoras").value(2))
				.andExpect(jsonPath("$.aluguel.valor").value("10.00"))
				.andReturn();

		String corpo = resultado.getResponse().getContentAsString();
		assertTrue(corpo.contains("RFID-" + numero));

		Bicicleta bicicleta = this.bicicletas.findAll().stream()
				.filter(item -> ("RFID-" + numero).equals(item.getRfid()))
				.findFirst()
				.orElseThrow();
		assertEquals(StatusBicicleta.EM_USO, bicicleta.getStatus());
		assertNull(bicicleta.getTranca());

		Cobranca cobranca = this.cobrancas.findByChave(codigo).orElseThrow();
		assertEquals(ResultadoCobranca.APROVADO, cobranca.getResultado());
		assertEquals(0, cobranca.getValor().compareTo(new BigDecimal("10.00")));
		assertNotNull(cobranca.getDataHora());
		assertTrue(Duration.between(cobranca.getDataHora(), LocalDateTime.now()).abs().toSeconds() < 120);
		assertNotNull(cobranca.getAluguel());
		assertEquals(cobranca.getAluguel().getInicio(), cobranca.getDataHora());
		assertEquals(1, this.alugueis.countByChave(codigo));

		List<MensagemSimulada> mensagensDoCiclista = this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(email);
		assertEquals(1, mensagensDoCiclista.size());
		String texto = mensagensDoCiclista.get(0).getTexto();
		assertTrue(texto.contains(NotificacaoSimulada.AVISO));
		assertTrue(texto.contains("RFID-" + numero));
		assertTrue(texto.contains(numero));
		assertTrue(texto.contains("10"));
		assertTrue(texto.contains("2 horas"));

		this.mockMvc.perform(get("/api/aluguel").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.aluguel.rfid").value("RFID-" + numero))
				.andExpect(jsonPath("$.aluguel.duracaoHoras").value(2));

		this.mockMvc.perform(get("/api/mensagens").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].texto", containsString(NotificacaoSimulada.AVISO)));

		this.mockMvc.perform(get("/api/mensagens").session(sessaoOutro))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("RFID-" + numero))));

		alugar(sessaoOutro, chave(), numero)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANCA_SEM_BICICLETA"));
		assertEquals(0, this.cobrancas.countByCiclista_Email(outro));
	}

	@Test
	void pagamentoRecusadoNaoMoveABicicleta() throws Exception {
		String email = ciclistaComCartao("recusado", "DEMO-RECUSADO");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		String numero = trancaLivre();
		String codigo = chave();

		alugar(sessao, codigo, numero)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("RECUSADO"))
				.andExpect(jsonPath("$.mensagem", containsString("recusado")))
				.andExpect(jsonPath("$.aluguel").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.valor").value("10.00"));

		Bicicleta bicicleta = this.bicicletas.findByTrancaAndStatus(this.trancas.findByNumero(numero).orElseThrow(),
				StatusBicicleta.DISPONIVEL).orElseThrow();
		assertEquals(StatusBicicleta.DISPONIVEL, bicicleta.getStatus());
		assertEquals(ResultadoCobranca.RECUSADO, this.cobrancas.findByChave(codigo).orElseThrow().getResultado());
		assertNull(this.cobrancas.findByChave(codigo).orElseThrow().getAluguel());
		assertEquals(0, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertEquals(0, this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(email).size());

		this.mockMvc.perform(get("/api/aluguel").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	void segundoAluguelDoMesmoCiclistaEBloqueado() throws Exception {
		String email = ciclistaComCartao("segundo", "DEMO-APROVADO");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		List<String> livres = trancasLivres(2);

		alugar(sessao, chave(), livres.get(0)).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("APROVADO"));

		alugar(sessao, chave(), livres.get(1))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("ALUGUEL_ATIVO"));

		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertTrue(this.bicicletas.existsByTrancaAndStatus(this.trancas.findByNumero(livres.get(1)).orElseThrow(),
				StatusBicicleta.DISPONIVEL));
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
	}

	@Test
	void repetirAMesmaConfirmacaoNaoDuplicaCobrancaNemMensagem() throws Exception {
		String email = ciclistaComCartao("repete", "DEMO-APROVADO");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		String numero = trancaLivre();
		String codigo = chave();

		MvcResult primeira = alugar(sessao, codigo, numero).andExpect(status().isOk()).andReturn();
		MvcResult segunda = alugar(sessao, codigo, numero)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("APROVADO"))
				.andExpect(jsonPath("$.mensagem", containsString("já foi processada")))
				.andReturn();

		String idPrimeira = primeira.getResponse().getContentAsString();
		String idSegunda = segunda.getResponse().getContentAsString();
		assertTrue(idPrimeira.contains("\"cobrancaId\""));
		assertEquals(extrair(idPrimeira, "cobrancaId"), extrair(idSegunda, "cobrancaId"));
		assertEquals(1, this.cobrancas.countByChave(codigo));
		assertEquals(1, this.alugueis.countByChave(codigo));
		Long aluguelId = this.cobrancas.findByChave(codigo).orElseThrow().getAluguel().getId();
		assertEquals(1, this.mensagens.countByAluguelId(aluguelId));
	}

	@Test
	void aluguelSemSessaoOuSemCsrfERecusado() throws Exception {
		this.mockMvc.perform(get("/api/totens")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/aluguel")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/mensagens")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post("/api/alugueis")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"chave\":\"chave-segura-1\",\"numeroTranca\":\"PC-01\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
	}

	private String ciclistaComCartao(String prefixo, String referencia) throws Exception {
		String email = ApoioApi.email(prefixo);
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"" + referencia + "\"}"))
				.andExpect(status().isOk());
		return email;
	}

	private org.springframework.test.web.servlet.ResultActions alugar(MockHttpSession sessao, String chave, String numero)
			throws Exception {
		return this.mockMvc.perform(post("/api/alugueis")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"chave\":\"" + chave + "\",\"numeroTranca\":\"" + numero + "\"}"));
	}

	private String trancaLivre() {
		return trancasLivres(1).get(0);
	}

	private List<String> trancasLivres(int quantidade) {
		List<String> livres = this.trancas.findAll().stream()
				.filter(tranca -> this.bicicletas.existsByTrancaAndStatus(tranca, StatusBicicleta.DISPONIVEL))
				.map(Tranca::getNumero)
				.limit(quantidade)
				.toList();
		if (livres.size() < quantidade) {
			throw new IllegalStateException("Faltam trancas disponiveis para o teste.");
		}
		return livres;
	}

	private static String chave() {
		return UUID.randomUUID().toString();
	}

	private static String extrair(String json, String campo) {
		String marca = "\"" + campo + "\":";
		int inicio = json.indexOf(marca);
		int fim = json.indexOf(",", inicio);
		return json.substring(inicio, fim);
	}

}
