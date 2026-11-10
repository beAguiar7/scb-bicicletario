package br.com.vadebicicleta.scb.seguranca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class PrincipalDaSessaoTest {

	@Test
	void principalDaSessaoSerializaSemASenha() throws Exception {
		String credencial = "credencial-de-teste";
		UserDetails usuario = new User("ana@exemplo.test", credencial, List.of());
		Authentication original = new UsernamePasswordAuthenticationToken(usuario, credencial, usuario.getAuthorities());
		Authentication sessao = AutenticacaoService.paraSessao(original);

		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(sessao);

		byte[] bytes;
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				ObjectOutputStream saida = new ObjectOutputStream(buffer)) {
			saida.writeObject(contexto);
			bytes = buffer.toByteArray();
		}

		assertFalse(new String(bytes, StandardCharsets.ISO_8859_1).contains(credencial));

		try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			SecurityContext lido = (SecurityContext) entrada.readObject();
			assertEquals("ana@exemplo.test", lido.getAuthentication().getName());
			assertInstanceOf(CiclistaAutenticado.class, lido.getAuthentication().getPrincipal());
			UserDetails principal = (UserDetails) lido.getAuthentication().getPrincipal();
			assertEquals("", principal.getPassword());
		}
	}

}
