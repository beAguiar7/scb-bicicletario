package br.com.vadebicicleta.scb.aluguel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TaxaExtraRepository extends JpaRepository<TaxaExtra, Long> {

	Optional<TaxaExtra> findByAluguel(Aluguel aluguel);

	long countByAluguel_Id(Long aluguelId);

	long countByAluguel_Ciclista_Email(String email);

	List<TaxaExtra> findByAluguel_Ciclista_EmailAndSituacaoOrderByDataHoraAsc(String email, SituacaoTaxaExtra situacao);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from TaxaExtra t where t.id = :id")
	Optional<TaxaExtra> findByIdComBloqueio(@Param("id") Long id);

}
