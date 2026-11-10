package br.com.vadebicicleta.scb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScbApplication {

	static {
		criarPasta("dados");
		criarPasta("logs");
	}

	public static void main(String[] args) {
		SpringApplication.run(ScbApplication.class, args);
	}

	/**
	 * O H2 grava o banco em ./dados e o log fica em ./logs.
	 * As pastas precisam existir antes da conexao e do primeiro registro.
	 */
	private static void criarPasta(String nome) {
		try {
			Files.createDirectories(Path.of(nome));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Nao foi possivel criar a pasta " + nome + ".", ex);
		}
	}

}
