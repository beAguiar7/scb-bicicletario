package br.com.vadebicicleta.scb.cartao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vadebicicleta.scb.ciclista.Ciclista;

public interface CartaoRepository extends JpaRepository<CartaoDemonstracao, Long> {

	Optional<CartaoDemonstracao> findByCiclista(Ciclista ciclista);

	Optional<CartaoDemonstracao> findByCiclista_Email(String email);

}
