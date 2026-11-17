package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import br.com.vadebicicleta.scb.aluguel.Aluguel;
import br.com.vadebicicleta.scb.aluguel.AluguelRepository;
import br.com.vadebicicleta.scb.aluguel.BicicletaRepository;
import br.com.vadebicicleta.scb.aluguel.CobrancaRepository;
import br.com.vadebicicleta.scb.aluguel.MensagemRepository;
import br.com.vadebicicleta.scb.aluguel.ResultadoPagamentoTaxa;
import br.com.vadebicicleta.scb.aluguel.SituacaoTaxaExtra;
import br.com.vadebicicleta.scb.aluguel.StatusAluguel;
import br.com.vadebicicleta.scb.aluguel.StatusBicicleta;
import br.com.vadebicicleta.scb.aluguel.TaxaExtra;
import br.com.vadebicicleta.scb.aluguel.TaxaExtraRepository;
import br.com.vadebicicleta.scb.aluguel.TaxaExtraService;
import br.com.vadebicicleta.scb.aluguel.Tranca;
import br.com.vadebicicleta.scb.aluguel.TrancaRepository;
import br.com.vadebicicleta.scb.cartao.CartaoRepository;
import br.com.vadebicicleta.scb.cartao.CartaoService;

@SpringBootTest
@AutoConfigureMockMvc
@Execution(ExecutionMode.SAME_THREAD)
class ReprocessamentoTaxaTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TaxaExtraService pagamentos;

	@Autowired
	private CartaoService cartoes;

	@Autowired
	private CartaoRepository cartoesRepositorio;

	@Autowired
	private TrancaRepository trancas;

	@Autowired
	private BicicletaRepository bicicletas;

	@Autowired
	private AluguelRepository alugueis;

	@Autowired
	private CobrancaRepository cobrancas;

	@Autowired
	private TaxaExtraRepository taxas;

	@Autowired
	private MensagemRepository mensagens;

	@Test
	void trocaOCartaoEPagaAMesmaTaxaSemChamarDeNovoQuandoJaEstaPaga() throws Exception {
		Pendencia pendencia = gerarPendente("Helena Dias");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, pendencia.email());
		this.mockMvc.perform(get("/api/taxas-extras").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(pendencia.taxaId()))
				.andExpect(jsonPath("$[0].valor").value("5.00"))
				.andExpect(jsonPath("$[0].situacao").value("PENDENTE"));
		trocarCartao(sessao, "DEMO-APROVADO");

		String corpo = this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento")
				.session(sessao)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pendencia.taxaId()))
				.andExpect(jsonPath("$.valor").value("5.00"))
				.andExpect(jsonPath("$.situacao").value("PAGA"))
				.andExpect(jsonPath("$.horario").value(pendencia.dataHora().toString()))
				.andExpect(jsonPath("$.jaPaga").value(false))
				.andExpect(jsonPath("$.simulacao").value(true))
				.andExpect(jsonPath("$.mensagem", containsString("paga")))
				.andReturn().getResponse().getContentAsString();
		assertFalseContem(corpo, pendencia.email(), "Helena Dias", "DEMO", "1111");

		TaxaExtra paga = this.taxas.findById(pendencia.taxaId()).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PAGA, paga.getSituacao());
		assertEquals(SituacaoTaxaExtra.PAGA, paga.getResultadoTentativa());
		assertNotNull(paga.getTentativaEm());
		assertEquals(0, new BigDecimal("5.00").compareTo(paga.getValor()));
		assertEquals(pendencia.dataHora(), paga.getDataHora());
		conferirDevolucao(pendencia);
		assertEquals(1, this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(pendencia.email()).size());
		this.mockMvc.perform(get("/api/taxas-extras").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		trocarCartao(sessao, "DEMO-RECUSADO");
		this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento")
				.session(sessao)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacao").value("PAGA"))
				.andExpect(jsonPath("$.valor").value("5.00"))
				.andExpect(jsonPath("$.jaPaga").value(true))
				.andExpect(jsonPath("$.tentativa").value(paga.getTentativaEm().toString()))
				.andExpect(jsonPath("$.simulacao").value(true));
		TaxaExtra deNovo = this.taxas.findById(pendencia.taxaId()).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PAGA, deNovo.getSituacao());
		assertEquals(SituacaoTaxaExtra.PAGA, deNovo.getResultadoTentativa());
		assertEquals(paga.getTentativaEm(), deNovo.getTentativaEm());
		assertEquals(0, new BigDecimal("5.00").compareTo(deNovo.getValor()));
		conferirDevolucao(pendencia);
	}

	@Test
	void recusaMantemAPendenciaEOValorOriginal() throws Exception {
		Pendencia pendencia = gerarPendente("Marina Costa");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, pendencia.email());

		String corpo = this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento")
				.session(sessao)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(pendencia.taxaId()))
				.andExpect(jsonPath("$.valor").value("5.00"))
				.andExpect(jsonPath("$.situacao").value("PENDENTE"))
				.andExpect(jsonPath("$.jaPaga").value(false))
				.andExpect(jsonPath("$.simulacao").value(true))
				.andExpect(jsonPath("$.mensagem", containsString("pendente")))
				.andReturn().getResponse().getContentAsString();
		assertFalseContem(corpo, pendencia.email(), "Marina Costa", "DEMO-RECUSADO", "0002");

		TaxaExtra taxa = this.taxas.findById(pendencia.taxaId()).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PENDENTE, taxa.getSituacao());
		assertEquals(SituacaoTaxaExtra.PENDENTE, taxa.getResultadoTentativa());
		assertNotNull(taxa.getTentativaEm());
		assertEquals(pendencia.dataHora(), taxa.getDataHora());
		assertEquals(0, new BigDecimal("5.00").compareTo(taxa.getValor()));
		conferirDevolucao(pendencia);
	}

	@Test
	void semCartaoMantemAPendencia() throws Exception {
		Pendencia pendencia = gerarPendente("Livia Nunes");
		this.cartoesRepositorio.delete(this.cartoesRepositorio.findByCiclista_Email(pendencia.email()).orElseThrow());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, pendencia.email());

		this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento")
				.session(sessao)
				.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SEM_CARTAO"));
		TaxaExtra taxa = this.taxas.findById(pendencia.taxaId()).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PENDENTE, taxa.getSituacao());
		assertNull(taxa.getTentativaEm());
		assertNull(taxa.getResultadoTentativa());
		assertEquals(0, new BigDecimal("5.00").compareTo(taxa.getValor()));
		this.mockMvc.perform(get("/api/taxas-extras").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(pendencia.taxaId()))
				.andExpect(jsonPath("$[0].situacao").value("PENDENTE"));
		conferirDevolucao(pendencia);
	}

	@Test
	void duasRequisicoesSimultaneasNaoPagamDuasVezes() throws Exception {
		Pendencia pendencia = gerarPendente("Rita Gomes");
		this.cartoes.salvar(pendencia.email(), "DEMO-APROVADO");

		List<ResultadoPagamentoTaxa> resultados = disputar(pendencia.email(), pendencia.taxaId());
		assertEquals(1, resultados.stream().filter(item -> !item.jaPaga()).count());
		assertEquals(1, resultados.stream().filter(ResultadoPagamentoTaxa::jaPaga).count());
		assertEquals(1, resultados.stream().map(ResultadoPagamentoTaxa::tentativa).distinct().count());
		assertTrue(resultados.stream().allMatch(item -> "5.00".equals(item.valor()) && "PAGA".equals(item.situacao())
				&& item.simulacao()));

		TaxaExtra taxa = this.taxas.findById(pendencia.taxaId()).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PAGA, taxa.getSituacao());
		assertEquals(0, new BigDecimal("5.00").compareTo(taxa.getValor()));
		assertEquals(pendencia.dataHora(), taxa.getDataHora());
		assertEquals(1, this.taxas.countByAluguel_Id(pendencia.aluguelId()));
		conferirDevolucao(pendencia);
	}

	@Test
	void outroCiclistaNaoConsultaNemReprocessa() throws Exception {
		Pendencia pendencia = gerarPendente("Ana Souza");
		String outroEmail = ApoioApi.email("outro-taxa");
		ApoioApi.cadastrar(this.mockMvc, outroEmail, "Bruno Lima", ApoioApi.documento());
		MockHttpSession outro = ApoioApi.entrar(this.mockMvc, outroEmail);
		this.mockMvc.perform(get("/api/taxas-extras").session(outro))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento")
				.session(outro)
				.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("TAXA_INEXISTENTE"));

		MockHttpSession dono = ApoioApi.entrar(this.mockMvc, pendencia.email());
		this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento").session(dono))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
		this.mockMvc.perform(get("/api/taxas-extras")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post("/api/taxas-extras/" + pendencia.taxaId() + "/pagamento").with(csrf()))
				.andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/taxas.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("taxas.js")));
		this.mockMvc.perform(get("/taxas.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tentar pagar novamente")));

		TaxaExtra taxa = this.taxas.findById(pendencia.taxaId()).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PENDENTE, taxa.getSituacao());
		assertNull(taxa.getTentativaEm());
		assertEquals(0, new BigDecimal("5.00").compareTo(taxa.getValor()));
		conferirDevolucao(pendencia);
	}

	private Pendencia gerarPendente(String nome) throws Exception {
		String email = ApoioApi.email("reprocessa");
		String documento = ApoioApi.documento();
		ApoioApi.cadastrar(this.mockMvc, email, nome, documento);
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		trocarCartao(sessao, "DEMO-APROVADO");
		List<Tranca> livres = trancasLivres(2);
		String rfid = this.bicicletas.findByTrancaAndStatus(livres.get(0), StatusBicicleta.DISPONIVEL).orElseThrow()
				.getRfid();
		String destino = livres.get(1).getNumero();
		String outro = ApoioApi.email("abre-destino");
		ApoioApi.cadastrar(this.mockMvc, outro, "Carlos Dias", ApoioApi.documento());
		MockHttpSession sessaoOutro = ApoioApi.entrar(this.mockMvc, outro);
		trocarCartao(sessaoOutro, "DEMO-APROVADO");
		alugar(sessaoOutro, destino);
		alugar(sessao, livres.get(0).getNumero());
		trocarCartao(sessao, "DEMO-RECUSADO");
		Aluguel ativo = this.alugueis.findByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO).orElseThrow();
		ativo.setInicio(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofHours(2).plusMinutes(40)));
		this.alugueis.saveAndFlush(ativo);
		this.mockMvc.perform(post("/api/logout").session(sessao).with(csrf())).andExpect(status().isNoContent());
		this.mockMvc.perform(post("/api/devolucoes")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rfid\":\"" + rfid + "\",\"numeroTranca\":\"" + destino + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.taxaExtra.valor").value("5.00"))
				.andExpect(jsonPath("$.taxaExtra.situacao").value("PENDENTE"));

		Aluguel encerrado = this.alugueis.findByCiclista_EmailAndStatus(email, StatusAluguel.ENCERRADO).orElseThrow();
		TaxaExtra taxa = this.taxas.findByAluguel(encerrado).orElseThrow();
		assertEquals(SituacaoTaxaExtra.PENDENTE, taxa.getSituacao());
		assertEquals(0, new BigDecimal("5.00").compareTo(taxa.getValor()));
		return new Pendencia(email, nome, taxa.getId(), taxa.getValor(), taxa.getDataHora(), encerrado.getFim(), rfid,
				destino, encerrado.getId());
	}

	private void conferirDevolucao(Pendencia pendencia) {
		Aluguel aluguel = this.alugueis.findById(pendencia.aluguelId()).orElseThrow();
		assertEquals(StatusAluguel.ENCERRADO, aluguel.getStatus());
		assertEquals(pendencia.fim(), aluguel.getFim());
		assertEquals(pendencia.destino(), aluguel.getTrancaDevolucao().getNumero());
		assertNull(aluguel.getBicicletaAtivaId());
		var bicicleta = this.bicicletas.findByRfid(pendencia.rfid()).orElseThrow();
		assertEquals(StatusBicicleta.DISPONIVEL, bicicleta.getStatus());
		assertEquals(pendencia.destino(), bicicleta.getTranca().getNumero());
		assertEquals(1, this.taxas.countByAluguel_Id(pendencia.aluguelId()));
		assertEquals(1, this.cobrancas.countByCiclista_Email(pendencia.email()));
	}

	private void trocarCartao(MockHttpSession sessao, String referencia) throws Exception {
		this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"" + referencia + "\"}"))
				.andExpect(status().isOk());
	}

	private void alugar(MockHttpSession sessao, String numero) throws Exception {
		this.mockMvc.perform(post("/api/alugueis")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"chave\":\"" + UUID.randomUUID() + "\",\"numeroTranca\":\"" + numero + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resultado").value("APROVADO"));
	}

	private List<Tranca> trancasLivres(int quantidade) {
		List<Tranca> livres = new ArrayList<>();
		for (Tranca tranca : this.trancas.findAll()) {
			if (this.bicicletas.existsByTrancaAndStatus(tranca, StatusBicicleta.DISPONIVEL)) {
				livres.add(tranca);
			}
			if (livres.size() == quantidade) {
				break;
			}
		}
		if (livres.size() < quantidade) {
			throw new IllegalStateException("Faltam trancas disponiveis para o teste.");
		}
		return livres;
	}

	private List<ResultadoPagamentoTaxa> disputar(String email, long id) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch prontos = new CountDownLatch(2);
		CountDownLatch inicio = new CountDownLatch(1);
		try {
			Future<ResultadoPagamentoTaxa> uma = pool.submit(() -> executar(prontos, inicio, email, id));
			Future<ResultadoPagamentoTaxa> outra = pool.submit(() -> executar(prontos, inicio, email, id));
			assertTrue(prontos.await(5, TimeUnit.SECONDS));
			inicio.countDown();
			return List.of(uma.get(20, TimeUnit.SECONDS), outra.get(20, TimeUnit.SECONDS));
		}
		finally {
			pool.shutdownNow();
		}
	}

	private ResultadoPagamentoTaxa executar(CountDownLatch prontos, CountDownLatch inicio, String email, long id)
			throws InterruptedException {
		prontos.countDown();
		if (!inicio.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("A disputa nao comecou.");
		}
		return this.pagamentos.pagar(email, id);
	}

	private static void assertFalseContem(String corpo, String... trechos) {
		for (String trecho : trechos) {
			assertFalse(corpo.contains(trecho), trecho);
		}
	}

	private record Pendencia(String email, String nome, long taxaId, BigDecimal valor, LocalDateTime dataHora,
			LocalDateTime fim, String rfid, String destino, long aluguelId) {
	}

}
