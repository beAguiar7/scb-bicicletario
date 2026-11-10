package br.com.vadebicicleta.scb.aluguel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemRepository extends JpaRepository<MensagemSimulada, Long> {

	boolean existsByAluguelId(Long aluguelId);

	long countByAluguelId(Long aluguelId);

	List<MensagemSimulada> findByDestinatario_EmailOrderByCriadaEmDesc(String email);

}
