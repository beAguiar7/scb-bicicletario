package br.com.vadebicicleta.scb.seguranca;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;

@Service
public class CiclistaUserDetailsService implements UserDetailsService {

	private final CiclistaRepository repositorio;

	public CiclistaUserDetailsService(CiclistaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		Ciclista ciclista = this.repositorio.findByEmail(normalizado)
				.orElseThrow(() -> new UsernameNotFoundException("usuario nao encontrado"));
		return new User(ciclista.getEmail(), ciclista.getSenhaHash(), List.of());
	}

}
