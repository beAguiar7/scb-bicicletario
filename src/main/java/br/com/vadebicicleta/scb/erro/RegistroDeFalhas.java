package br.com.vadebicicleta.scb.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra codigo, local e descricao tecnica. A data e o usuario entram no padrao do log.
 * Senha, documento, corpo do cadastro e cookies nao sao gravados.
 */
@Component
public class RegistroDeFalhas {

	private static final Logger log = LoggerFactory.getLogger(RegistroDeFalhas.class);

	public void registrar(String codigo, String local, String descricao, Throwable erro) {
		if (erro == null) {
			log.warn("codigo={} local={} descricao={}", codigo, local, descricao);
			return;
		}
		log.error("codigo={} local={} descricao={}", codigo, local, descricao, erro);
	}

}
