package br.com.vadebicicleta.scb.seguranca;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal gravado na sessao. So leva o e-mail, para a sessao JDBC conseguir serializar
 * sem guardar a senha nem o hash.
 */
public class CiclistaAutenticado implements UserDetails, Serializable {

	private static final long serialVersionUID = 1L;

	private final String email;

	public CiclistaAutenticado(String email) {
		this.email = email;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return this.email;
	}

}
