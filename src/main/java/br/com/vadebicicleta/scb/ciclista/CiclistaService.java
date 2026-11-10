package br.com.vadebicicleta.scb.ciclista;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

@Service
public class CiclistaService {

	private final CiclistaRepository repositorio;

	private final PasswordEncoder passwordEncoder;

	public CiclistaService(CiclistaRepository repositorio, PasswordEncoder passwordEncoder) {
		this.repositorio = repositorio;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CiclistaResposta cadastrar(CadastroCiclistaForm form, MultipartFile foto) {
		if (!form.getSenha().equals(form.getConfirmacaoSenha())) {
			throw ErroDeNegocio.campo("confirmacaoSenha", "A confirmação deve ser igual à senha.");
		}
		byte[] conteudo = validarFoto(foto);
		if (this.repositorio.existsByEmail(form.getEmail())) {
			throw ErroDeNegocio.emailJaCadastrado();
		}

		Ciclista ciclista = new Ciclista();
		ciclista.setNome(form.getNome());
		ciclista.setNacionalidade(form.getNacionalidade());
		ciclista.setNumeroDocumento(form.getNumeroDocumento());
		ciclista.setEmail(form.getEmail());
		ciclista.setSenhaHash(this.passwordEncoder.encode(form.getSenha()));
		ciclista.setFotoDocumento(conteudo);
		return CiclistaResposta.de(this.repositorio.save(ciclista));
	}

	@Transactional(readOnly = true)
	public CiclistaResposta consultarPorEmail(String email) {
		return CiclistaResposta.de(buscar(email));
	}

	@Transactional(readOnly = true)
	public PerfilCiclista consultarPerfil(String email) {
		return PerfilCiclista.de(buscar(email));
	}

	/**
	 * Altera somente o ciclista da sessao. Nao troca a senha.
	 * Se o e-mail mudar, o controller encerra a sessao.
	 */
	@Transactional
	public AtualizacaoResposta atualizar(String emailDaSessao, AtualizacaoCiclistaForm form, MultipartFile foto) {
		Ciclista ciclista = buscar(emailDaSessao);
		boolean emailAlterado = !ciclista.getEmail().equals(form.getEmail());
		if (emailAlterado && this.repositorio.existsByEmail(form.getEmail())) {
			throw ErroDeNegocio.emailJaCadastrado();
		}
		byte[] novaFoto = validarFotoSeEnviada(foto);
		ciclista.setNome(form.getNome());
		ciclista.setNacionalidade(form.getNacionalidade());
		ciclista.setNumeroDocumento(form.getNumeroDocumento());
		ciclista.setEmail(form.getEmail());
		if (novaFoto != null) {
			ciclista.setFotoDocumento(novaFoto);
		}
		String mensagem = emailAlterado
				? "E-mail alterado. Entre novamente com o novo e-mail."
				: "Cadastro atualizado.";
		return new AtualizacaoResposta(emailAlterado, mensagem, PerfilCiclista.de(ciclista));
	}

	private Ciclista buscar(String email) {
		return this.repositorio.findByEmail(email)
				.orElseThrow(() -> new ErroDeNegocio(401, "NAO_AUTENTICADO", "Faça login para continuar.", java.util.List.of()));
	}

	private byte[] validarFotoSeEnviada(MultipartFile foto) {
		if (foto == null || foto.isEmpty()) {
			return null;
		}
		return validarFoto(foto);
	}

	/**
	 * A extensao do arquivo nao basta: a foto precisa ser um JPEG ou PNG de ate 5 MB.
	 */
	private byte[] validarFoto(MultipartFile foto) {
		if (foto == null || foto.isEmpty()) {
			throw ErroDeNegocio.campo("foto", "Envie a foto do documento.");
		}
		if (foto.getSize() > RegrasCadastro.FOTO_MAXIMA_BYTES) {
			throw ErroDeNegocio.arquivoMuitoGrande();
		}
		byte[] conteudo;
		try {
			conteudo = foto.getBytes();
		}
		catch (IOException ex) {
			throw ErroDeNegocio.campo("foto", "Não foi possível ler a foto.");
		}
		if (!ehJpeg(conteudo) && !ehPng(conteudo)) {
			throw ErroDeNegocio.campo("foto", "A foto deve ser uma imagem JPEG ou PNG válida.");
		}
		try (ByteArrayInputStream entrada = new ByteArrayInputStream(conteudo)) {
			BufferedImage imagem = ImageIO.read(entrada);
			if (imagem == null) {
				throw ErroDeNegocio.campo("foto", "A foto deve ser uma imagem JPEG ou PNG válida.");
			}
		}
		catch (IOException ex) {
			throw ErroDeNegocio.campo("foto", "A foto deve ser uma imagem JPEG ou PNG válida.");
		}
		return conteudo;
	}

	private static boolean ehJpeg(byte[] conteudo) {
		return conteudo.length > 3
				&& (conteudo[0] & 0xFF) == 0xFF
				&& (conteudo[1] & 0xFF) == 0xD8
				&& (conteudo[2] & 0xFF) == 0xFF;
	}

	private static boolean ehPng(byte[] conteudo) {
		byte[] assinatura = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
		if (conteudo.length < assinatura.length) {
			return false;
		}
		for (int i = 0; i < assinatura.length; i++) {
			if (conteudo[i] != assinatura[i]) {
				return false;
			}
		}
		return true;
	}

}
