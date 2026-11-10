package br.com.vadebicicleta.scb;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;
import br.com.vadebicicleta.scb.ciclista.RegrasCadastro;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CadastroCiclistaTest {

	private static final String SENHA = "segredo12";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CiclistaRepository repositorio;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void cadastraBrasileiroComJpegENaoDevolveSegredos() throws Exception {
		String email = email("brasileiro");

		this.mockMvc.perform(cadastro(email, SENHA, "Ana Souza", "BRASILEIRO", "12345678901", ImagensTeste.jpeg(),
				"documento.jpg", "image/jpeg"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.nacionalidade").value("BRASILEIRO"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist())
				.andExpect(jsonPath("$.fotoDocumento").doesNotExist())
				.andExpect(jsonPath("$.numeroDocumento").doesNotExist())
				.andExpect(content().string(not(containsString(SENHA))))
				.andExpect(content().string(not(containsString("$2a$"))))
				.andExpect(content().string(not(containsString("$2b$"))));

		Ciclista salvo = this.repositorio.findByEmail(email).orElseThrow();
		assertNotEquals(SENHA, salvo.getSenhaHash());
		assertTrue(this.passwordEncoder.matches(SENHA, salvo.getSenhaHash()));
		assertTrue(salvo.getFotoDocumento().length > 0);
	}

	@Test
	void cadastraEstrangeiroComDocumentoAlfanumerico() throws Exception {
		String email = email("estrangeiro");

		this.mockMvc.perform(cadastro(email, SENHA, "Maria Lopez", "ESTRANGEIRO", "AB12CD3456", ImagensTeste.png(),
				"documento.png", "image/png"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nacionalidade").value("ESTRANGEIRO"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(content().string(not(containsString("AB12CD3456"))));
	}

	@Test
	void normalizaEmailERecusaDuplicata() throws Exception {
		String id = UUID.randomUUID().toString();
		String informado = "  Pessoa." + id + "@Exemplo.TEST ";
		String normalizado = "pessoa." + id + "@exemplo.test";

		this.mockMvc.perform(cadastro(informado, SENHA, "Ana Souza", "BRASILEIRO", "12345678901", ImagensTeste.jpeg(),
				"documento.jpg", "image/jpeg"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(normalizado));

		this.mockMvc.perform(cadastro(normalizado.toUpperCase(), SENHA, "Outra Pessoa", "ESTRANGEIRO", "ZX987654",
				ImagensTeste.png(), "documento.png", "image/png"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
				.andExpect(jsonPath("$.mensagem").value("Este e-mail já está cadastrado."))
				.andExpect(content().string(not(containsString(SENHA))));
	}

	@Test
	void recusaCamposObrigatoriosEmailInvalidoESenhaCurta() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/ciclistas")
				.param("nome", " ")
				.param("numeroDocumento", "12")
				.param("email", "sem-arroba")
				.param("senha", "1234567")
				.param("confirmacaoSenha", "1234567")
				.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
				.andExpect(jsonPath("$.mensagem").value("Revise os campos informados."))
				.andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem", hasItem(containsString("Informe o nome."))))
				.andExpect(jsonPath("$.erros[?(@.campo=='nacionalidade')].mensagem", hasItem(containsString("Informe a nacionalidade."))))
				.andExpect(jsonPath("$.erros[?(@.campo=='email')].mensagem", hasItem(containsString("Informe um e-mail válido."))))
				.andExpect(jsonPath("$.erros[?(@.campo=='senha')].mensagem", hasItem(containsString("entre 8 e 72"))))
				.andExpect(content().string(not(containsString("Exception"))));

		assertTrue(RegrasCadastro.SENHA_MINIMA == 8);
	}

	@Test
	void recusaNacionalidadeDesconhecida() throws Exception {
		this.mockMvc.perform(cadastro(email("nacionalidade"), SENHA, "Ana Souza", "MARCIANO", "12345678901",
				ImagensTeste.jpeg(), "documento.jpg", "image/jpeg"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='nacionalidade')].mensagem",
						hasItem(containsString("brasileira ou estrangeira"))));
	}

	@Test
	void recusaConfirmacaoDiferenteDaSenha() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/ciclistas")
				.file(new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()))
				.param("nome", "Ana Souza")
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", "12345678901")
				.param("email", email("confirmacao"))
				.param("senha", SENHA)
				.param("confirmacaoSenha", "outrasenha")
				.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='confirmacaoSenha')].mensagem",
						hasItem(containsString("igual à senha"))));
	}

	@Test
	void recusaFotoAusenteInvalidaEAcimaDoLimite() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/ciclistas")
				.param("nome", "Ana Souza")
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", "12345678901")
				.param("email", email("sem-foto"))
				.param("senha", SENHA)
				.param("confirmacaoSenha", SENHA)
				.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='foto')].mensagem", hasItem(containsString("Envie a foto"))));

		this.mockMvc.perform(cadastro(email("gif"), SENHA, "Ana Souza", "BRASILEIRO", "12345678901",
				"GIF89a nao e foto".getBytes(), "documento.jpg", "image/jpeg"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[?(@.campo=='foto')].mensagem", hasItem(containsString("JPEG ou PNG"))));

		byte[] grande = new byte[(int) RegrasCadastro.FOTO_MAXIMA_BYTES + 1];
		this.mockMvc.perform(cadastro(email("grande"), SENHA, "Ana Souza", "BRASILEIRO", "12345678901", grande,
				"documento.jpg", "image/jpeg"))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.codigo").value("ARQUIVO_MUITO_GRANDE"))
				.andExpect(jsonPath("$.mensagem").value("A foto deve ter no máximo 5 MB."));
	}

	@Test
	void cadastroSemCsrfERecusado() throws Exception {
		String email = email("csrf");
		this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/ciclistas")
				.file(new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()))
				.param("nome", "Ana Souza")
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", "12345678901")
				.param("email", email)
				.param("senha", SENHA)
				.param("confirmacaoSenha", SENHA))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"))
				.andExpect(content().string(not(containsString(SENHA))));
		assertFalse(this.repositorio.existsByEmail(email));
	}

	private static MockMultipartHttpServletRequestBuilder cadastro(String email,
			String senha, String nome, String nacionalidade, String documento, byte[] foto, String arquivo, String tipo) {
		return MockMvcRequestBuilders.multipart("/api/ciclistas")
				.file(new MockMultipartFile("foto", arquivo, tipo, foto))
				.param("nome", nome)
				.param("nacionalidade", nacionalidade)
				.param("numeroDocumento", documento)
				.param("email", email)
				.param("senha", senha)
				.param("confirmacaoSenha", senha)
				.with(csrf());
	}

	private static String email(String prefixo) {
		return prefixo + "-" + UUID.randomUUID() + "@exemplo.test";
	}

}
