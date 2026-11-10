package br.com.vadebicicleta.scb.aluguel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

	Optional<Cobranca> findByChave(String chave);

	long countByCiclista_Email(String email);

	long countByChave(String chave);

}
