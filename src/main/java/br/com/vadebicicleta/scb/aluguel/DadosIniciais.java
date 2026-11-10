package br.com.vadebicicleta.scb.aluguel;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria dois totens somente quando ainda nao existem.
 * Reiniciar a aplicacao nao duplica dados nem devolve bicicletas alugadas.
 * Duas instancias ao mesmo tempo esbarram na unicidade do nome e uma delas desiste.
 */
@Component
public class DadosIniciais implements ApplicationRunner {

	private final TotemRepository totens;

	private final InicializacaoDosTotens gravacao;

	public DadosIniciais(TotemRepository totens, InicializacaoDosTotens gravacao) {
		this.totens = totens;
		this.gravacao = gravacao;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			this.gravacao.criarSeAusente();
		}
		catch (DataIntegrityViolationException ex) {
			if (this.totens.count() == 0) {
				throw ex;
			}
		}
	}

}

@Service
class InicializacaoDosTotens {

	private static final int CAPACIDADE = 20;

	private final TotemRepository totens;

	private final TrancaRepository trancas;

	private final BicicletaRepository bicicletas;

	InicializacaoDosTotens(TotemRepository totens, TrancaRepository trancas, BicicletaRepository bicicletas) {
		this.totens = totens;
		this.trancas = trancas;
		this.bicicletas = bicicletas;
	}

	@Transactional
	public void criarSeAusente() {
		if (this.totens.count() > 0) {
			return;
		}
		criar("Totem Praça Central", "PC");
		criar("Totem Campus Norte", "CN");
		this.totens.flush();
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
