package br.com.vadebicicleta.scb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import br.com.vadebicicleta.scb.aluguel.AluguelRepository;
import br.com.vadebicicleta.scb.aluguel.AluguelService;
import br.com.vadebicicleta.scb.aluguel.BicicletaRepository;
import br.com.vadebicicleta.scb.aluguel.PedidoAluguel;
import br.com.vadebicicleta.scb.aluguel.ResultadoAluguel;
import br.com.vadebicicleta.scb.aluguel.StatusAluguel;
import br.com.vadebicicleta.scb.aluguel.StatusBicicleta;
import br.com.vadebicicleta.scb.aluguel.Tranca;
import br.com.vadebicicleta.scb.aluguel.TrancaRepository;
import br.com.vadebicicleta.scb.cartao.CartaoService;
import br.com.vadebicicleta.scb.ciclista.CadastroCiclistaForm;
import br.com.vadebicicleta.scb.ciclista.CiclistaService;
import br.com.vadebicicleta.scb.ciclista.Nacionalidade;
import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

@SpringBootTest
class ConcorrenciaAluguelTest {

	@Autowired
	private AluguelService aluguelService;

	@Autowired
	private CiclistaService ciclistaService;

	@Autowired
	private CartaoService cartaoService;

	@Autowired
	private TrancaRepository trancas;

	@Autowired
	private BicicletaRepository bicicletas;

	@Autowired
	private AluguelRepository alugueis;

	@Test
	void doisCiclistasNaoRetiramAMesmaBicicleta() throws Exception {
		String primeiro = criarCiclista();
		String segundo = criarCiclista();
		String numero = trancasLivres(1).get(0);

		List<Desfecho> desfechos = disputar(
				new Tentativa(primeiro, numero),
				new Tentativa(segundo, numero));

		long aprovados = desfechos.stream().filter(Desfecho::aprovado).count();
		long recusas = desfechos.stream().filter(item -> item.erro != null).count();
		assertEquals(1, aprovados);
		assertEquals(1, recusas);
		assertTrue(desfechos.stream().filter(item -> item.erro != null)
				.allMatch(item -> "TRANCA_SEM_BICICLETA".equals(item.erro.getCodigo())
						|| "CONFLITO".equals(item.erro.getCodigo())));
		assertEquals(1, this.alugueis.countByBicicleta_RfidAndStatus("RFID-" + numero, StatusAluguel.ATIVO));
	}

	@Test
	void oMesmoCiclistaNaoCriaDoisAlugueis() throws Exception {
		String email = criarCiclista();
		List<String> livres = trancasLivres(2);

		List<Desfecho> desfechos = disputar(
				new Tentativa(email, livres.get(0)),
				new Tentativa(email, livres.get(1)));

		assertEquals(1, desfechos.stream().filter(Desfecho::aprovado).count());
		assertEquals(1, desfechos.stream().filter(item -> item.erro != null).count());
		assertTrue(desfechos.stream().filter(item -> item.erro != null)
				.allMatch(item -> "ALUGUEL_ATIVO".equals(item.erro.getCodigo())
						|| "CONFLITO".equals(item.erro.getCodigo())));
		assertEquals(1, this.alugueis.countByCiclista_EmailAndStatus(email, StatusAluguel.ATIVO));
	}

	private List<Desfecho> disputar(Tentativa primeira, Tentativa segunda) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch prontos = new CountDownLatch(2);
		CountDownLatch inicio = new CountDownLatch(1);
		try {
			Future<Desfecho> um = pool.submit(() -> executar(prontos, inicio, primeira));
			Future<Desfecho> outro = pool.submit(() -> executar(prontos, inicio, segunda));
			assertTrue(prontos.await(5, TimeUnit.SECONDS));
			inicio.countDown();
			return List.of(um.get(20, TimeUnit.SECONDS), outro.get(20, TimeUnit.SECONDS));
		}
		finally {
			pool.shutdownNow();
		}
	}

	private Desfecho executar(CountDownLatch prontos, CountDownLatch inicio, Tentativa tentativa) throws InterruptedException {
		prontos.countDown();
		if (!inicio.await(5, TimeUnit.SECONDS)) {
			throw new IllegalStateException("A disputa nao comecou.");
		}
		try {
			ResultadoAluguel resultado = this.aluguelService.confirmar(tentativa.email(),
					new PedidoAluguel(UUID.randomUUID().toString(), tentativa.numero()));
			return new Desfecho(resultado, null);
		}
		catch (ErroDeNegocio erro) {
			return new Desfecho(null, erro);
		}
	}

	private String criarCiclista() throws Exception {
		String email = ApoioApi.email("concorrencia");
		CadastroCiclistaForm form = new CadastroCiclistaForm();
		form.setNome("Ciclista Teste");
		form.setNacionalidade(Nacionalidade.BRASILEIRO);
		form.setNumeroDocumento(ApoioApi.documento());
		form.setEmail(email);
		form.setSenha(ApoioApi.SENHA);
		form.setConfirmacaoSenha(ApoioApi.SENHA);
		this.ciclistaService.cadastrar(form, new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()));
		this.cartaoService.salvar(email, "DEMO-APROVADO");
		return email;
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

	private record Tentativa(String email, String numero) {
	}

	private record Desfecho(ResultadoAluguel resultado, ErroDeNegocio erro) {

		boolean aprovado() {
			return this.resultado != null && "APROVADO".equals(this.resultado.resultado());
		}

	}

}
