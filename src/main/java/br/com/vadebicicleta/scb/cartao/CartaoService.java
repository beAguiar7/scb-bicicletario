package br.com.vadebicicleta.scb.cartao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vadebicicleta.scb.ciclista.Ciclista;
import br.com.vadebicicleta.scb.ciclista.CiclistaRepository;
import br.com.vadebicicleta.scb.erro.ErroDeNegocio;

@Service
public class CartaoService {

	private final CartaoRepository cartoes;

	private final CiclistaRepository ciclistas;

	public CartaoService(CartaoRepository cartoes, CiclistaRepository ciclistas) {
		this.cartoes = cartoes;
		this.ciclistas = ciclistas;
	}

	@Transactional(readOnly = true)
	public List<CartaoOpcao> listarOpcoes() {
		List<CartaoOpcao> opcoes = new ArrayList<>();
		for (CartaoFicticio cartao : CartaoFicticio.todos()) {
			opcoes.add(CartaoOpcao.de(cartao));
		}
		return opcoes;
	}

	@Transactional(readOnly = true)
	public CartaoResposta consultar(String email) {
		return this.cartoes.findByCiclista_Email(email).map(CartaoResposta::de).orElseGet(CartaoResposta::vazio);
	}

	/**
	 * A referencia e conferida antes de alterar o cartao.
	 * Se a troca falhar, a transacao desfaz a mudanca e o cartao anterior permanece.
	 */
	@Transactional
	public CartaoResposta salvar(String email, String referencia) {
		CartaoFicticio escolhido = CartaoFicticio.porReferencia(referencia);
		Ciclista ciclista = this.ciclistas.findByEmail(email)
				.orElseThrow(() -> new ErroDeNegocio(401, "NAO_AUTENTICADO", "Faça login para continuar.", List.of()));
		CartaoDemonstracao cartao = this.cartoes.findByCiclista(ciclista).orElseGet(CartaoDemonstracao::new);
		cartao.setCiclista(ciclista);
		cartao.setReferencia(escolhido.getReferencia());
		cartao.setMascara(escolhido.getMascara());
		cartao.setTitular(escolhido.getTitular());
		cartao.setCobrancaSimuladaAprovada(escolhido.isCobrancaAprovada());
		return CartaoResposta.de(this.cartoes.save(cartao));
	}

}
