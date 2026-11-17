package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.com.vadebicicleta.scb.aluguel.AluguelRepository;
import br.com.vadebicicleta.scb.aluguel.AluguelService;
import br.com.vadebicicleta.scb.aluguel.Bicicleta;
import br.com.vadebicicleta.scb.aluguel.BicicletaRepository;
import br.com.vadebicicleta.scb.aluguel.CobrancaRepository;
import br.com.vadebicicleta.scb.aluguel.DevolucaoService;
import br.com.vadebicicleta.scb.aluguel.MensagemRepository;
import br.com.vadebicicleta.scb.aluguel.PedidoAluguel;
import br.com.vadebicicleta.scb.aluguel.PedidoDevolucao;
import br.com.vadebicicleta.scb.aluguel.ResultadoDevolucao;
import br.com.vadebicicleta.scb.aluguel.StatusAluguel;
import br.com.vadebicicleta.scb.aluguel.TaxaExtraRepository;
import br.com.vadebicicleta.scb.aluguel.StatusBicicleta;
import br.com.vadebicicleta.scb.aluguel.Tranca;
import br.com.vadebicicleta.scb.aluguel.TrancaRepository;
import br.com.vadebicicleta.scb.cartao.CartaoService;
import br.com.vadebicicleta.scb.ciclista.CadastroCiclistaForm;
import br.com.vadebicicleta.scb.ciclista.CiclistaService;
import br.com.vadebicicleta.scb.ciclista.Nacionalidade;
import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

@SpringBootTest
@AutoConfigureMockMvc
@Execution(ExecutionMode.SAME_THREAD)
class DevolucaoTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DevolucaoService devolucoes;

	@Autowired
	private AluguelService alugueisServico;

	@Autowired
	private CiclistaService ciclistas;

	@Autowired
	private CartaoService cartoes;

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
	void devolveEmOutroTotemDepoisDeSairERepeteOMesmoHorario() throws Exception {
		String email = ApoioApi.email("devolve");
		String documento = ApoioApi.documento();
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", documento);
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"DEMO-APROVADO\"}"))
				.andExpect(status().isOk());

		Tranca origem = trancaLivre();
		Tranca destino = trancaLivreDeOutroTotem(origem);
		String rfid = this.bicicletas.findByTrancaAndStatus(origem, StatusBicicleta.DISPONIVEL).orElseThrow().getRfid();
		String outro = ApoioApi.email("abre-tranca");
		ApoioApi.cadastrar(this.mockMvc, outro, "Bruno Lima", ApoioApi.documento());
		MockHttpSession sessaoOutro = ApoioApi.entrar(this.mockMvc, outro);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessaoOutro)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"DEMO-APROVADO\"}"))
				.andExpect(status().isOk());
		alugar(sessaoOutro, destino.getNumero());
		alugar(sessao, origem.getNumero());

		this.mockMvc.perform(post("/api/logout").session(sessao).with(csrf())).andExpect(status().isNoContent());
		long encerradosAntes = this.alugueis.countByBicicleta_RfidAndStatus(rfid, StatusAluguel.ENCERRADO);

		MvcResult primeira = devolver(rfid.toLowerCase(), destino.getNumero().toLowerCase())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rfid").value(rfid))
				.andExpect(jsonPath("$.numeroTranca").value(destino.getNumero()))
				.andExpect(jsonPath("$.repetida").value(false))
				.andExpect(jsonPath("$.taxaExtra").value(nullValue()))
				.andExpect(jsonPath("$.mensagem", containsString("devolvida")))
				.andReturn();
		String corpo = primeira.getResponse().getContentAsString();
		assertFalse(corpo.contains(email));
		assertFalse(corpo.contains(documento));
		assertFalse(corpo.contains("Ana Souza"));
		assertFalse(corpo.contains("ciclista"));

		Bicicleta bicicleta = this.bicicletas.findByRfid(rfid).orElseThrow();
		assertEquals(StatusBicicleta.DISPONIVEL, bicicleta.getStatus());
		assertEquals(destino.getNumero(), bicicleta.getTranca().getNumero());
		assertEquals(0, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ENCERRADO));
		assertEquals(encerradosAntes + 1, this.alugueis.countByBicicleta_RfidAndStatus(bicicleta.getRfid(), StatusAluguel.ENCERRADO));
		var encerrado = this.alugueis
				.findFirstByBicicletaAndStatusAndTrancaDevolucaoOrderByFimDesc(bicicleta, StatusAluguel.ENCERRADO, destino)
				.orElseThrow();
		assertNotNull(encerrado.getFim());
		assertNull(encerrado.getCiclistaAtivoId());
		assertNull(encerrado.getBicicletaAtivaId());
		assertEquals(origem.getNumero(), encerrado.getTranca().getNumero());
		assertEquals(0, this.taxas.countByAluguel_Id(encerrado.getId()));
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
		assertEquals(1, this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(email).size());

		MockHttpSession nova = ApoioApi.entrar(this.mockMvc, email);
		this.mockMvc.perform(get("/api/aluguel").session(nova))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(false));

		MvcResult segunda = devolver(rfid, destino.getNumero())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.repetida").value(true))
				.andExpect(jsonPath("$.taxaExtra").value(nullValue()))
				.andExpect(jsonPath("$.horario").value(encerrado.getFim().toString()))
				.andReturn();
		assertEquals(extrair(primeira, "horario"), extrair(segunda, "horario"));
		assertEquals(0, this.taxas.countByAluguel_Id(encerrado.getId()));
		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ENCERRADO));
		assertEquals(encerradosAntes + 1, this.alugueis.countByBicicleta_RfidAndStatus(bicicleta.getRfid(), StatusAluguel.ENCERRADO));
		assertEquals(destino.getNumero(), this.bicicletas.findByRfid(bicicleta.getRfid()).orElseThrow().getTranca().getNumero());
	}

	@Test
	void trancaOcupadaNaoRecebeABicicleta() throws Exception {
		String email = ciclistaComCartao("ocupada");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		List<Tranca> livres = trancasLivres(2);

		String rfid = this.bicicletas.findByTrancaAndStatus(livres.get(0), StatusBicicleta.DISPONIVEL).orElseThrow()
				.getRfid();
		alugar(sessao, livres.get(0).getNumero());
		devolver(rfid, livres.get(1).getNumero())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANCA_OCUPADA"))
				.andExpect(jsonPath("$.mensagem", containsString("ocupada")));

		Bicicleta bicicleta = this.bicicletas.findByRfid(rfid).orElseThrow();
		assertEquals(StatusBicicleta.EM_USO, bicicleta.getStatus());
		assertNull(bicicleta.getTranca());
		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertTrue(this.bicicletas.existsByTrancaAndStatus(livres.get(1), StatusBicicleta.DISPONIVEL));
	}

	@Test
	void trancaInexistenteNaoEncerraOAluguel() throws Exception {
		String email = ciclistaComCartao("sem-tranca");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		Tranca origem = trancaLivre();
		String rfid = this.bicicletas.findByTrancaAndStatus(origem, StatusBicicleta.DISPONIVEL).orElseThrow().getRfid();
		alugar(sessao, origem.getNumero());

		devolver(rfid, "ZZ-99")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("TRANCA_INEXISTENTE"));

		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertEquals(StatusBicicleta.EM_USO, this.bicicletas.findByRfid(rfid).orElseThrow().getStatus());
	}

	@Test
	void bicicletaSemAluguelAtivoNaoMudaDeTranca() throws Exception {
		String email = ciclistaComCartao("sem-aluguel");
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		List<Tranca> livres = trancasLivres(2);
		String rfid = this.bicicletas.findByTrancaAndStatus(livres.get(0), StatusBicicleta.DISPONIVEL).orElseThrow()
				.getRfid();
		alugar(sessao, livres.get(1).getNumero());

		devolver(rfid, livres.get(1).getNumero())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("BICICLETA_SEM_ALUGUEL"))
				.andExpect(content().string(not(containsString("@"))))
				.andExpect(jsonPath("$.mensagem", containsString("aluguel ativo")));

		Bicicleta bicicleta = this.bicicletas.findByRfid(rfid).orElseThrow();
		assertEquals(StatusBicicleta.DISPONIVEL, bicicleta.getStatus());
		assertEquals(livres.get(0).getNumero(), bicicleta.getTranca().getNumero());
	}

	@Test
	void devolucaoExigeCsrfEAsDemaisRotasContinuamProtegidas() throws Exception {
		this.mockMvc.perform(get("/totem.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("botao-devolver")));
		this.mockMvc.perform(post("/api/devolucoes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rfid\":\"RFID-PC-01\",\"numeroTranca\":\"CN-01\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
		this.mockMvc.perform(post("/api/alugueis")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"chave\":\"chave-segura-1\",\"numeroTranca\":\"PC-01\"}"))
				.andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/aluguel")).andExpect(status().isUnauthorized());
	}

	@Test
	void duasDevolucoesNaoOcupamAMesmaTranca() throws Exception {
		String primeiro = criarCiclista();
		String segundo = criarCiclista();
		String terceiro = criarCiclista();
		List<Tranca> livres = trancasLivres(3);
		String primeiraBicicleta = this.bicicletas.findByTrancaAndStatus(livres.get(0), StatusBicicleta.DISPONIVEL)
				.orElseThrow().getRfid();
		String segundaBicicleta = this.bicicletas.findByTrancaAndStatus(livres.get(1), StatusBicicleta.DISPONIVEL)
				.orElseThrow().getRfid();
		this.alugueisServico.confirmar(terceiro, new PedidoAluguel(UUID.randomUUID().toString(), livres.get(2).getNumero()));
		this.alugueisServico.confirmar(primeiro, new PedidoAluguel(UUID.randomUUID().toString(), livres.get(0).getNumero()));
		this.alugueisServico.confirmar(segundo, new PedidoAluguel(UUID.randomUUID().toString(), livres.get(1).getNumero()));
		String destino = livres.get(2).getNumero();

		List<Desfecho> desfechos = disputar(
				new PedidoDevolucao(primeiraBicicleta, destino),
				new PedidoDevolucao(segundaBicicleta, destino));

		assertEquals(1, desfechos.stream().filter(item -> item.resultado != null && !item.resultado.repetida()).count());
		assertEquals(1, desfechos.stream().filter(item -> item.erro != null).count());
		assertTrue(desfechos.stream().filter(item -> item.erro != null)
				.allMatch(item -> "TRANCA_OCUPADA".equals(item.erro.getCodigo())
						|| "CONFLITO".equals(item.erro.getCodigo())));

		Bicicleta ocupante = this.bicicletas.findByTranca(this.trancas.findByNumero(destino).orElseThrow()).orElseThrow();
		assertEquals(StatusBicicleta.DISPONIVEL, ocupante.getStatus());
		assertTrue(ocupante.getRfid().equals(primeiraBicicleta) || ocupante.getRfid().equals(segundaBicicleta));
		String perdedora = ocupante.getRfid().equals(primeiraBicicleta) ? segundaBicicleta : primeiraBicicleta;
		assertEquals(StatusBicicleta.EM_USO, this.bicicletas.findByRfid(perdedora).orElseThrow().getStatus());
		assertNull(this.bicicletas.findByRfid(perdedora).orElseThrow().getTranca());
	}

	private List<Desfecho> disputar(PedidoDevolucao primeira, PedidoDevolucao segunda) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch prontos = new CountDownLatch(2);
		CountDownLatch inicio = new CountDownLatch(1);
		try {
			Future<Desfecho> uma = pool.submit(() -> executar(prontos, inicio, primeira));
			Future<Desfecho> outra = pool.submit(() -> executar(prontos, inicio, segunda));
			assertTrue(prontos.await(5, TimeUnit.SECONDS));
			inicio.countDown();
			return List.of(uma.get(20, TimeUnit.SECONDS), outra.get(20, TimeUnit.SECONDS));
		}
		finally {
			pool.shutdownNow();
		}
	}

	private Desfecho executar(CountDownLatch prontos, CountDownLatch inicio, PedidoDevolucao pedido)
			throws InterruptedException {
		prontos.countDown();
		if (!inicio.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("A disputa nao comecou.");
		}
		try {
			return new Desfecho(this.devolucoes.devolver(pedido), null);
		}
		catch (ErroDeNegocio erro) {
			return new Desfecho(null, erro);
		}
	}

	private String criarCiclista() throws Exception {
		String email = ApoioApi.email("disputa-devolucao");
		CadastroCiclistaForm form = new CadastroCiclistaForm();
		form.setNome("Ciclista Teste");
		form.setNacionalidade(Nacionalidade.BRASILEIRO);
		form.setNumeroDocumento(ApoioApi.documento());
		form.setEmail(email);
		form.setSenha(ApoioApi.SENHA);
		form.setConfirmacaoSenha(ApoioApi.SENHA);
		this.ciclistas.cadastrar(form, new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()));
		this.cartoes.salvar(email, "DEMO-APROVADO");
		return email;
	}

	private String ciclistaComCartao(String prefixo) throws Exception {
		String email = ApoioApi.email(prefixo);
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"DEMO-APROVADO\"}"))
				.andExpect(status().isOk());
		return email;
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

	private org.springframework.test.web.servlet.ResultActions devolver(String rfid, String numero) throws Exception {
		return this.mockMvc.perform(post("/api/devolucoes")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rfid\":\"" + rfid + "\",\"numeroTranca\":\"" + numero + "\"}"));
	}

	private Tranca trancaLivre() {
		return trancasLivres(1).get(0);
	}

	private Tranca trancaLivreDeOutroTotem(Tranca origem) {
		return this.trancas.findAll().stream()
				.filter(tranca -> this.bicicletas.existsByTrancaAndStatus(tranca, StatusBicicleta.DISPONIVEL))
				.filter(tranca -> !tranca.getTotem().getId().equals(origem.getTotem().getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Falta tranca livre em outro totem."));
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

	private static String extrair(MvcResult resultado, String campo) throws Exception {
		String json = resultado.getResponse().getContentAsString();
		String marca = "\"" + campo + "\":\"";
		int inicio = json.indexOf(marca);
		int fim = json.indexOf("\"", inicio + marca.length());
		return json.substring(inicio + marca.length(), fim);
	}

	private record Desfecho(ResultadoDevolucao resultado, ErroDeNegocio erro) {
	}

}
