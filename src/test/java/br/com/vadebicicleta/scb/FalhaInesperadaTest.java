package br.com.vadebicicleta.scb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class FalhaInesperadaTest {

	@MockitoBean
	private CiclistaRepository repositorio;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void falhaInesperadaNaoExpoeDetalheInterno() throws Exception {
		when(this.repositorio.existsByEmail(any())).thenReturn(false);
		when(this.repositorio.save(any(Ciclista.class)))
				.thenThrow(new IllegalStateException("SQLException select * from ciclista senha=segredo-interno"));

		this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/ciclistas")
				.file(new MockMultipartFile("foto", "documento.jpg", "image/jpeg", ImagensTeste.jpeg()))
				.param("nome", "Ana Souza")
				.param("nacionalidade", "BRASILEIRO")
				.param("numeroDocumento", "12345678901")
				.param("email", "falha@exemplo.test")
				.param("senha", "segredo12")
				.param("confirmacaoSenha", "segredo12")
				.with(csrf()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
				.andExpect(jsonPath("$.mensagem").value("Não foi possível concluir a operação. Tente novamente."))
				.andExpect(content().string(not(containsString("segredo-interno"))))
				.andExpect(content().string(not(containsString("SQLException"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

}
