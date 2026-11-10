package br.com.vadebicicleta.scb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScbApplication {

	static {
		// No perfil prod o banco e o log nao ficam em arquivo.
		if (!perfilProd()) {
			criarPasta("dados");
			criarPasta("logs");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(ScbApplication.class, args);
	}

	private static boolean perfilProd() {
		String perfis = System.getenv("SPRING_PROFILES_ACTIVE");
		if (perfis == null) {
			perfis = System.getProperty("spring.profiles.active");
		}
		return perfis != null && perfis.contains("prod");
	}

	/**
	 * O H2 grava o banco em ./dados e o log local fica em ./logs.
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
