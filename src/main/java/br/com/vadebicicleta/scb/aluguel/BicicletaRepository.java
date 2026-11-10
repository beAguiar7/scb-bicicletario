package br.com.vadebicicleta.scb.aluguel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {

	boolean existsByTrancaAndStatus(Tranca tranca, StatusBicicleta status);

	Optional<Bicicleta> findByTrancaAndStatus(Tranca tranca, StatusBicicleta status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Bicicleta b where b.tranca = :tranca and b.status = :status")
	Optional<Bicicleta> findByTrancaAndStatusComBloqueio(@Param("tranca") Tranca tranca,
			@Param("status") StatusBicicleta status);

}
