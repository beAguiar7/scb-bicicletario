package br.com.vadebicicleta.scb.aluguel;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria dois totens somente quando ainda nao existem.
 * Reiniciar a aplicacao nao duplica dados nem devolve bicicletas alugadas.
 */
@Component
public class DadosIniciais implements ApplicationRunner {

	private static final int CAPACIDADE = 20;

	private final TotemRepository totens;

	private final TrancaRepository trancas;

	private final BicicletaRepository bicicletas;

	public DadosIniciais(TotemRepository totens, TrancaRepository trancas, BicicletaRepository bicicletas) {
		this.totens = totens;
		this.trancas = trancas;
		this.bicicletas = bicicletas;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (this.totens.count() > 0) {
			return;
		}
		criar("Totem Praça Central", "PC");
		criar("Totem Campus Norte", "CN");
	}

	private void criar(String nome, String prefixo) {
		Totem totem = new Totem();
		totem.setNome(nome);
		totem.setCapacidade(CAPACIDADE);
		this.totens.save(totem);
		for (int numero = 1; numero <= CAPACIDADE; numero++) {
			String codigo = prefixo + "-" + String.format("%02d", numero);
			Tranca tranca = new Tranca();
			tranca.setNumero(codigo);
			tranca.setTotem(totem);
			this.trancas.save(tranca);

			Bicicleta bicicleta = new Bicicleta();
			bicicleta.setRfid("RFID-" + codigo);
			bicicleta.setStatus(StatusBicicleta.DISPONIVEL);
			bicicleta.setTranca(tranca);
			this.bicicletas.save(bicicleta);
		}
	}

}
