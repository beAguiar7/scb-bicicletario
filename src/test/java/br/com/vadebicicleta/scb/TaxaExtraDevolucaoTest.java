package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.com.vadebicicleta.scb.aluguel.Aluguel;
import br.com.vadebicicleta.scb.aluguel.AluguelRepository;
import br.com.vadebicicleta.scb.aluguel.AluguelService;
import br.com.vadebicicleta.scb.aluguel.BicicletaRepository;
import br.com.vadebicicleta.scb.aluguel.CalculadoraTaxaExtra;
import br.com.vadebicicleta.scb.aluguel.CobrancaRepository;
import br.com.vadebicicleta.scb.aluguel.ConfiguracaoAluguel;
import br.com.vadebicicleta.scb.aluguel.DevolucaoService;
import br.com.vadebicicleta.scb.aluguel.MensagemRepository;
import br.com.vadebicicleta.scb.aluguel.PedidoAluguel;
import br.com.vadebicicleta.scb.aluguel.PedidoDevolucao;
import br.com.vadebicicleta.scb.aluguel.ResultadoDevolucao;
import br.com.vadebicicleta.scb.aluguel.StatusAluguel;
import br.com.vadebicicleta.scb.aluguel.StatusBicicleta;
import br.com.vadebicicleta.scb.aluguel.TaxaExtra;
import br.com.vadebicicleta.scb.aluguel.TaxaExtraRepository;
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
class TaxaExtraDevolucaoTest {

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

	@Autowired
	private ConfiguracaoAluguel configuracao;

	@Test
	void aprovadaRegistraUmaTaxaERepetirNaoRecalculaNemCobraDeNovo() throws Exception {
		String email = ApoioApi.email("taxa-paga");
		String documento = ApoioApi.documento();
		DevolucaoPreparada preparada = preparar(email, "Helena Dias", documento, "DEMO-APROVADO");
		envelhecer(email, Duration.ofHours(2).plusMinutes(45));

		MvcResult primeira = devolver(preparada.rfid(), preparada.destino())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.repetida").value(false))
				.andExpect(jsonPath("$.mensagem", containsString("devolvida")))
				.andExpect(jsonPath("$.taxaExtra.valor").value("5.00"))
				.andExpect(jsonPath("$.taxaExtra.situacao").value("PAGA"))
				.andExpect(jsonPath("$.taxaExtra.simulacao").value(true))
				.andReturn();
		String corpo = primeira.getResponse().getContentAsString();
		assertFalse(corpo.contains(email));
		assertFalse(corpo.contains(documento));
		assertFalse(corpo.contains("Helena Dias"));
		assertFalse(corpo.contains("DEMO"));
		assertFalse(corpo.contains("1111"));
		assertFalse(corpo.contains("ciclista"));

		Aluguel encerrado = this.alugueis.findByCiclista_EmailAndStatus(email, StatusAluguel.ENCERRADO).orElseThrow();
		TaxaExtra taxa = this.taxas.findByAluguel(encerrado).orElseThrow();
		assertEquals(encerrado.getFim(), taxa.getDataHora());
		assertEquals(new BigDecimal("5.00"), taxa.getValor());
		assertEquals(calculado(encerrado).toPlainString(), taxa.getValor().setScale(2, RoundingMode.HALF_UP).toPlainString());
		assertEquals(StatusBicicleta.DISPONIVEL, this.bicicletas.findByRfid(preparada.rfid()).orElseThrow().getStatus());
		assertEquals(preparada.destino(), this.bicicletas.findByRfid(preparada.rfid()).orElseThrow().getTranca().getNumero());
		assertEquals(0, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertEquals(1, this.taxas.countByAluguel_Ciclista_Email(email));
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
		assertEquals(1, this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(email).size());

		this.cartoes.salvar(email, "DEMO-RECUSADO");
		encerrado.setInicio(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(10));
		this.alugueis.saveAndFlush(encerrado);

		devolver(preparada.rfid(), preparada.destino())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.repetida").value(true))
				.andExpect(jsonPath("$.horario").value(encerrado.getFim().toString()))
				.andExpect(jsonPath("$.taxaExtra.valor").value("5.00"))
				.andExpect(jsonPath("$.taxaExtra.situacao").value("PAGA"))
				.andExpect(jsonPath("$.taxaExtra.simulacao").value(true));
		assertEquals(1, this.taxas.countByAluguel_Id(encerrado.getId()));
		assertEquals(taxa.getId(), this.taxas.findByAluguel(encerrado).orElseThrow().getId());
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
	}

	@Test
	void recusadaConcluiADevolucaoENaoDuplicaNaLeituraSeguinte() throws Exception {
		String email = ApoioApi.email("taxa-pendente");
		String documento = ApoioApi.documento();
		DevolucaoPreparada preparada = preparar(email, "Marina Costa", documento, "DEMO-APROVADO");
		this.cartoes.salvar(email, "DEMO-RECUSADO");
		envelhecer(email, Duration.ofHours(2).plusMinutes(10));

		MvcResult primeira = devolver(preparada.rfid(), preparada.destino())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagem", containsString("devolvida")))
				.andExpect(jsonPath("$.taxaExtra.valor").value("2.50"))
				.andExpect(jsonPath("$.taxaExtra.situacao").value("PENDENTE"))
				.andExpect(jsonPath("$.taxaExtra.simulacao").value(true))
				.andReturn();
		String corpo = primeira.getResponse().getContentAsString();
		assertFalse(corpo.contains(email));
		assertFalse(corpo.contains(documento));
		assertFalse(corpo.contains("Marina Costa"));
		assertFalse(corpo.contains("DEMO-RECUSADO"));
		assertFalse(corpo.contains("0002"));

		assertEquals(StatusBicicleta.DISPONIVEL, this.bicicletas.findByRfid(preparada.rfid()).orElseThrow().getStatus());
		assertEquals(preparada.destino(), this.bicicletas.findByRfid(preparada.rfid()).orElseThrow().getTranca().getNumero());
		assertEquals(0, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ENCERRADO));
		Aluguel encerrado = this.alugueis.findByCiclista_EmailAndStatus(email, StatusAluguel.ENCERRADO).orElseThrow();
		assertNotNull(encerrado.getFim());
		assertNull(encerrado.getBicicletaAtivaId());
		assertEquals(1, this.taxas.countByAluguel_Id(encerrado.getId()));
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
		assertEquals(1, this.mensagens.findByDestinatario_EmailOrderByCriadaEmDesc(email).size());

		devolver(preparada.rfid(), preparada.destino())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.repetida").value(true))
				.andExpect(jsonPath("$.horario").value(encerrado.getFim().toString()))
				.andExpect(jsonPath("$.taxaExtra.valor").value("2.50"))
				.andExpect(jsonPath("$.taxaExtra.situacao").value("PENDENTE"));
		assertEquals(1, this.taxas.countByAluguel_Id(encerrado.getId()));
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
	}

	@Test
	void duasDevolucoesJuntasGeramUmaTaxa() throws Exception {
		String email = criarCiclista();
		String outro = criarCiclista();
		List<Tranca> livres = trancasLivres(2);
		String rfid = this.bicicletas.findByTrancaAndStatus(livres.get(0), StatusBicicleta.DISPONIVEL).orElseThrow().getRfid();
		String destino = livres.get(1).getNumero();
		this.alugueisServico.confirmar(outro, new PedidoAluguel(UUID.randomUUID().toString(), destino));
		this.alugueisServico.confirmar(email, new PedidoAluguel(UUID.randomUUID().toString(), livres.get(0).getNumero()));
		envelhecer(email, Duration.ofHours(2).plusMinutes(20));

		List<Desfecho> desfechos = disputar(new PedidoDevolucao(rfid, destino), new PedidoDevolucao(rfid, destino));
		assertEquals(1, desfechos.stream().filter(item -> item.resultado != null && !item.resultado.repetida()).count());
		assertEquals(1, desfechos.stream().filter(item -> item.resultado != null && item.resultado.repetida()).count());
		assertTrue(desfechos.stream().allMatch(item -> item.erro == null));

		ResultadoDevolucao nova = desfechos.stream().filter(item -> !item.resultado.repetida()).findFirst().orElseThrow().resultado;
		ResultadoDevolucao repetida = desfechos.stream().filter(item -> item.resultado.repetida()).findFirst().orElseThrow().resultado;
		assertEquals(nova.horario(), repetida.horario());
		assertEquals(nova.taxaExtra(), repetida.taxaExtra());
		assertNotNull(nova.taxaExtra());
		assertEquals(1, this.taxas.countByAluguel_Ciclista_Email(email));
		assertEquals(1, this.cobrancas.countByCiclista_Email(email));
		assertEquals(StatusBicicleta.DISPONIVEL, this.bicicletas.findByRfid(rfid).orElseThrow().getStatus());
		assertEquals(destino, this.bicicletas.findByRfid(rfid).orElseThrow().getTranca().getNumero());
	}

	private BigDecimal calculado(Aluguel aluguel) {
		return CalculadoraTaxaExtra.calcular(aluguel.getInicio(), aluguel.getFim(), this.configuracao.getDuracaoHoras(),
				this.configuracao.getBlocoMinutos(), this.configuracao.getTaxaExtra());
	}

	private DevolucaoPreparada preparar(String email, String nome, String documento, String cartao) throws Exception {
		ApoioApi.cadastrar(this.mockMvc, email, nome, documento);
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"" + cartao + "\"}"))
				.andExpect(status().isOk());
		List<Tranca> livres = trancasLivres(2);
		String rfid = this.bicicletas.findByTrancaAndStatus(livres.get(0), StatusBicicleta.DISPONIVEL).orElseThrow().getRfid();
		String destino = livres.get(1).getNumero();
		String outro = ApoioApi.email("abre-destino");
		ApoioApi.cadastrar(this.mockMvc, outro, "Bruno Lima", ApoioApi.documento());
		MockHttpSession sessaoOutro = ApoioApi.entrar(this.mockMvc, outro);
		this.mockMvc.perform(post("/api/cartao")
				.session(sessaoOutro)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"referencia\":\"DEMO-APROVADO\"}"))
				.andExpect(status().isOk());
		alugar(sessaoOutro, destino);
		alugar(sessao, livres.get(0).getNumero());
		this.mockMvc.perform(post("/api/logout").session(sessao).with(csrf())).andExpect(status().isNoContent());
		return new DevolucaoPreparada(rfid, destino);
	}

	private void envelhecer(String email, Duration decorrido) {
		Aluguel aluguel = this.alugueis.findByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO).orElseThrow();
		aluguel.setInicio(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minus(decorrido));
		this.alugueis.saveAndFlush(aluguel);
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

	private String criarCiclista() throws Exception {
		String email = ApoioApi.email("disputa-taxa");
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

	private record DevolucaoPreparada(String rfid, String destino) {
	}

	private record Desfecho(ResultadoDevolucao resultado, ErroDeNegocio erro) {
	}

}
