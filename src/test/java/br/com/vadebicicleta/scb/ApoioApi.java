package br.com.vadebicicleta.scb;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro e login usados pelos testes. Os dados sao ficticios.
 */
final class ApoioApi {

	static final String SENHA = "segredo12";

	private ApoioApi() {
	}

	static String email(String prefixo) {
		return prefixo + "-" + UUID.randomUUID() + "@exemplo.test";
	}

	static String documento() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	static void cadastrar(MockMvc mockMvc, String email, String nome, String documento) throws Exception {
		mockMvc.perform(multipart("/api/ciclistas")
				.file(new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()))
				.param("nome", nome)
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", documento)
				.param("email", email)
				.param("senha", SENHA)
				.param("confirmacaoSenha", SENHA)
				.with(csrf()))
				.andExpect(status().isCreated());
	}

	static MockHttpSession entrar(MockMvc mockMvc, String email) throws Exception {
		MockHttpSession sessao = new MockHttpSession();
		mockMvc.perform(post("/api/login")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isOk());
		return sessao;
	}

}
