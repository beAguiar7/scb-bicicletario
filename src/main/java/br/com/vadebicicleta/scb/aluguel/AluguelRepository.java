package br.com.vadebicicleta.scb.aluguel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vadebicicleta.scb.ciclista.Ciclista;

public interface AluguelRepository extends JpaRepository<Aluguel, Long> {

	boolean existsByCiclistaAndStatus(Ciclista ciclista, StatusAluguel status);

	Optional<Aluguel> findByCiclista_EmailAndStatus(String email, StatusAluguel status);

	long countByCiclista_EmailAndStatus(String email, StatusAluguel status);

	long countByBicicleta_RfidAndStatus(String rfid, StatusAluguel status);

	long countByChave(String chave);

}
