package br.com.vadebicicleta.scb.aluguel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TrancaRepository extends JpaRepository<Tranca, Long> {

	Optional<Tranca> findByNumero(String numero);

	List<Tranca> findByTotemOrderByNumeroAsc(Totem totem);

	long countByTotem_Nome(String nome);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Tranca t where t.numero = :numero")
	Optional<Tranca> findByNumeroComBloqueio(@Param("numero") String numero);

}
