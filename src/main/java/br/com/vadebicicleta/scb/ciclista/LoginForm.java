package br.com.vadebicicleta.scb.ciclista;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginForm {

	@NotBlank(message = "Informe o e-mail.")
	@Email(message = "Informe um e-mail válido.")
	private String email;

	@NotBlank(message = "Informe a senha.")
	private String senha;

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	public String getSenha() {
		return this.senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

}
