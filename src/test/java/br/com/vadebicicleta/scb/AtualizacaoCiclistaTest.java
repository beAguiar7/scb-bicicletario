package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AtualizacaoCiclistaTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CiclistaRepository ciclistas;

	@Test
	void atualizaDadosSemTrocarSenhaNemFoto() throws Exception {
		String email = ApoioApi.email("editar");
		String documento = ApoioApi.documento();
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", documento);
		byte[] fotoAntes = this.ciclistas.findByEmail(email).orElseThrow().getFotoDocumento();
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		this.mockMvc.perform(get("/api/ciclista").session(sessao).param("id", "999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.numeroDocumento").value(documento))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.fotoDocumento").doesNotExist());

		String novoDocumento = ApoioApi.documento();
		atualizar(sessao, "Ana Lima", "ESTRANGEIRO", novoDocumento, email, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailAlterado").value(false))
				.andExpect(jsonPath("$.mensagem").value("Cadastro atualizado."))
				.andExpect(jsonPath("$.perfil.nome").value("Ana Lima"))
				.andExpect(jsonPath("$.perfil.nacionalidade").value("ESTRANGEIRO"))
				.andExpect(jsonPath("$.perfil.numeroDocumento").value(novoDocumento))
				.andExpect(jsonPath("$.perfil.email").value(email))
				.andExpect(jsonPath("$.perfil.senha").doesNotExist())
				.andExpect(jsonPath("$.perfil.fotoDocumento").doesNotExist())
				.andExpect(content().string(not(containsString(ApoioApi.SENHA))));

		Ciclista gravado = this.ciclistas.findByEmail(email).orElseThrow();
		assertArrayEquals(fotoAntes, gravado.getFotoDocumento());
		assertEquals("Ana Lima", gravado.getNome());

		this.mockMvc.perform(post("/api/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + ApoioApi.SENHA + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroDocumento").doesNotExist());
	}

	@Test
	void substituiAFoto() throws Exception {
		String email = ApoioApi.email("foto");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		byte[] antes = this.ciclistas.findByEmail(email).orElseThrow().getFotoDocumento();
		byte[] png = ImagensTeste.png();
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		atualizar(sessao, "Ana Souza", "BRASILEIRO", ApoioApi.documento(), email, png).andExpect(status().isOk());

		byte[] depois = this.ciclistas.findByEmail(email).orElseThrow().getFotoDocumento();
		assertArrayEquals(png, depois);
		assertFalse(java.util.Arrays.equals(antes, depois));
	}

	@Test
	void fotoInvalidaNaoApagaAAtual() throws Exception {
		String email = ApoioApi.email("fotomau");
		String documento = ApoioApi.documento();
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", documento);
		byte[] antes = this.ciclistas.findByEmail(email).orElseThrow().getFotoDocumento();
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		atualizar(sessao, "Ana Souza", "BRASILEIRO", documento, email, "nao-e-imagem".getBytes())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));

		assertArrayEquals(antes, this.ciclistas.findByEmail(email).orElseThrow().getFotoDocumento());
	}

	@Test
	void dadosInvalidosNaoAlteramOCadastro() throws Exception {
		String email = ApoioApi.email("invalido");
		ApoioApi.cadastrar(this.mockMvc, email, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, email);

		atualizar(sessao, "A", "BRASILEIRO", "12", email, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));

		this.mockMvc.perform(get("/api/ciclista").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza"));
	}

	@Test
	void emailDuplicadoERecusado() throws Exception {
		String primeiro = ApoioApi.email("um");
		String segundo = ApoioApi.email("dois");
		ApoioApi.cadastrar(this.mockMvc, primeiro, "Ana Souza", ApoioApi.documento());
		ApoioApi.cadastrar(this.mockMvc, segundo, "Bruno Lima", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, primeiro);

		atualizar(sessao, "Ana Souza", "BRASILEIRO", ApoioApi.documento(), segundo.toUpperCase(), null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
				.andExpect(jsonPath("$.mensagem").value("Este e-mail já está cadastrado."));

		assertEquals(primeiro, this.ciclistas.findByEmail(primeiro).orElseThrow().getEmail());
		this.mockMvc.perform(get("/api/ciclista").session(sessao))
				.andExpect(jsonPath("$.email").value(primeiro));
	}

	@Test
	void emailAlteradoEncerraASessaoEPedeNovoLogin() throws Exception {
		String antigo = ApoioApi.email("antigo");
		String novo = ApoioApi.email("novo");
		ApoioApi.cadastrar(this.mockMvc, antigo, "Ana Souza", ApoioApi.documento());
		MockHttpSession sessao = ApoioApi.entrar(this.mockMvc, antigo);

		atualizar(sessao, "Ana Souza", "BRASILEIRO", ApoioApi.documento(), "  " + novo.toUpperCase() + "  ", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailAlterado").value(true))
				.andExpect(jsonPath("$.mensagem").value("E-mail alterado. Entre novamente com o novo e-mail."))
				.andExpect(jsonPath("$.perfil.email").value(novo));

		this.mockMvc.perform(get("/api/ciclista").session(sessao)).andExpect(status().isUnauthorized());

		this.mockMvc.perform(post("/api/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + novo + "\",\"senha\":\"" + ApoioApi.SENHA + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(novo));

		this.mockMvc.perform(post("/api/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + antigo + "\",\"senha\":\"" + ApoioApi.SENHA + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("Login ou senha inválidos"));
	}

	@Test
	void atualizacaoSemSessaoOuSemCsrfERecusada() throws Exception {
		this.mockMvc.perform(get("/api/ciclista")).andExpect(status().isUnauthorized());

		MockMultipartHttpServletRequestBuilder pedido = multipart("/api/ciclista")
				.param("nome", "Ana Souza")
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", ApoioApi.documento())
				.param("email", ApoioApi.email("csrf"));
		pedido.with(requisicao -> {
			requisicao.setMethod("PUT");
			return requisicao;
		});
		this.mockMvc.perform(pedido).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
	}

	private org.springframework.test.web.servlet.ResultActions atualizar(MockHttpSession sessao, String nome,
			String nacionalidade, String documento, String email, byte[] foto) throws Exception {
		MockMultipartHttpServletRequestBuilder pedido = multipart("/api/ciclista")
				.param("nome", nome)
				.param("nacionalidade", nacionalidade)
				.param("numeroDocumento", documento)
				.param("email", email)
				.param("senha", "outra-senha-que-deve-ser-ignorada");
		if (foto != null) {
			pedido.file(new MockMultipartFile("foto", "documento.png", "image/png", foto));
		}
		pedido.session(sessao).with(csrf());
		pedido.with(requisicao -> {
			requisicao.setMethod("PUT");
			return requisicao;
		});
		return this.mockMvc.perform(pedido);
	}

}
