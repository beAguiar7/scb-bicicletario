package br.com.vadebicicleta.scb.erro;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorDeErros {

	private final RegistroDeFalhas registro;

	public TratadorDeErros(RegistroDeFalhas registro) {
		this.registro = registro;
	}

	@ExceptionHandler(ErroDeNegocio.class)
	public ResponseEntity<ErroResposta> negocio(ErroDeNegocio erro, HttpServletRequest request) {
		this.registro.registrar(erro.getCodigo(), local(request), erro.getMessage(), null);
		return resposta(erro.getStatus(), erro.getCodigo(), erro.getMessage(), erro.getErros());
	}

	@ExceptionHandler({ BindException.class, MethodArgumentNotValidException.class })
	public ResponseEntity<ErroResposta> validacao(BindException erro, HttpServletRequest request) {
		List<ErroResposta.ErroCampo> erros = new ArrayList<>();
		for (FieldError campo : erro.getFieldErrors()) {
			erros.add(new ErroResposta.ErroCampo(campo.getField(), mensagem(campo)));
		}
		this.registro.registrar("DADOS_INVALIDOS", local(request), "Campos invalidos: " + nomes(erros), null);
		return resposta(400, "DADOS_INVALIDOS", "Revise os campos informados.", erros);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErroResposta> tipoInvalido(MethodArgumentTypeMismatchException erro, HttpServletRequest request) {
		this.registro.registrar("DADOS_INVALIDOS", local(request), "Tipo invalido no campo " + erro.getName(), null);
		String texto = "nacionalidade".equals(erro.getName())
				? "Informe se a nacionalidade é brasileira ou estrangeira."
				: "Valor inválido.";
		return resposta(400, "DADOS_INVALIDOS", "Revise os campos informados.",
				List.of(new ErroResposta.ErroCampo(erro.getName(), texto)));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErroResposta> restricoes(ConstraintViolationException erro, HttpServletRequest request) {
		this.registro.registrar("DADOS_INVALIDOS", local(request), "Restricao de validacao", null);
		return resposta(400, "DADOS_INVALIDOS", "Revise os campos informados.", List.of());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException erro, HttpServletRequest request) {
		this.registro.registrar("DADOS_INVALIDOS", local(request), "Corpo da requisicao ilegivel", null);
		return resposta(400, "DADOS_INVALIDOS", "Não foi possível entender os dados enviados.", List.of());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErroResposta> arquivoGrande(MaxUploadSizeExceededException erro, HttpServletRequest request) {
		this.registro.registrar("ARQUIVO_MUITO_GRANDE", local(request), "Upload acima do limite", null);
		return resposta(413, ErroDeNegocio.arquivoMuitoGrande());
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ErroResposta> multipart(MultipartException erro, HttpServletRequest request) {
		if (causaDeTamanho(erro)) {
			return arquivoGrande(null, request);
		}
		this.registro.registrar("DADOS_INVALIDOS", local(request), "Falha ao receber o formulario", null);
		return resposta(400, "DADOS_INVALIDOS", "Não foi possível receber a foto.", List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException erro, HttpServletRequest request) {
		String causa = erro.getMostSpecificCause().getMessage();
		if (causa != null && causa.toLowerCase(java.util.Locale.ROOT).contains("uk_ciclista_email")) {
			this.registro.registrar("EMAIL_JA_CADASTRADO", local(request), "Unicidade de e-mail", null);
			return resposta(409, ErroDeNegocio.emailJaCadastrado());
		}
		this.registro.registrar("ERRO_INTERNO", local(request), erro.getClass().getSimpleName(), null);
		return erroInterno();
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResposta> inesperado(Exception erro, HttpServletRequest request) {
		this.registro.registrar("ERRO_INTERNO", local(request), erro.getClass().getSimpleName(), erro);
		return erroInterno();
	}

	private static ResponseEntity<ErroResposta> resposta(int status, ErroDeNegocio erro) {
		return resposta(status, erro.getCodigo(), erro.getMessage(), erro.getErros());
	}

	private static ResponseEntity<ErroResposta> resposta(int status, String codigo, String mensagem,
			List<ErroResposta.ErroCampo> erros) {
		return ResponseEntity.status(status)
				.header("Cache-Control", "no-store")
				.body(new ErroResposta(codigo, mensagem, erros));
	}

	private static ResponseEntity<ErroResposta> erroInterno() {
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ERRO_INTERNO",
				"Não foi possível concluir a operação. Tente novamente.", List.of());
	}

	private static String mensagem(FieldError erro) {
		if ("nacionalidade".equals(erro.getField()) && erro.getCode() != null && erro.getCode().startsWith("typeMismatch")) {
			return "Informe se a nacionalidade é brasileira ou estrangeira.";
		}
		if (erro.getDefaultMessage() == null || erro.getDefaultMessage().isBlank()) {
			return "Valor inválido.";
		}
		return erro.getDefaultMessage();
	}

	private static String nomes(List<ErroResposta.ErroCampo> erros) {
		List<String> nomes = new ArrayList<>();
		for (ErroResposta.ErroCampo erro : erros) {
			nomes.add(erro.campo());
		}
		return String.join(",", nomes);
	}

	private static String local(HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}

	private static boolean causaDeTamanho(Throwable erro) {
		Throwable atual = erro;
		while (atual != null) {
			if (atual instanceof MaxUploadSizeExceededException) {
				return true;
			}
			atual = atual.getCause();
		}
		return false;
	}

}
