package br.com.vadebicicleta.scb.ciclista;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CiclistaRepository extends JpaRepository<Ciclista, Long> {

	Optional<Ciclista> findByEmail(String email);

	boolean existsByEmail(String email);

	/**
	 * Impede dois alugueis simultaneos do mesmo ciclista.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Ciclista c where c.email = :email")
	Optional<Ciclista> findByEmailComBloqueio(@Param("email") String email);

}
