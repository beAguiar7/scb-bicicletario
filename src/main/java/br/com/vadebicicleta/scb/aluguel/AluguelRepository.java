package br.com.vadebicicleta.scb.aluguel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.vadebicicleta.scb.ciclista.Ciclista;

import jakarta.persistence.LockModeType;

public interface AluguelRepository extends JpaRepository<Aluguel, Long> {

	boolean existsByCiclistaAndStatus(Ciclista ciclista, StatusAluguel status);

	boolean existsByBicicletaAndStatus(Bicicleta bicicleta, StatusAluguel status);

	Optional<Aluguel> findByCiclista_EmailAndStatus(String email, StatusAluguel status);

	long countByCiclista_EmailAndStatus(String email, StatusAluguel status);

	long countByBicicleta_RfidAndStatus(String rfid, StatusAluguel status);

	long countByChave(String chave);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Aluguel a where a.bicicleta = :bicicleta and a.status = :status")
	Optional<Aluguel> findByBicicletaAndStatusComBloqueio(@Param("bicicleta") Bicicleta bicicleta,
			@Param("status") StatusAluguel status);

	Optional<Aluguel> findFirstByBicicletaAndStatusAndTrancaDevolucaoOrderByFimDesc(Bicicleta bicicleta,
			StatusAluguel status, Tranca trancaDevolucao);

}
