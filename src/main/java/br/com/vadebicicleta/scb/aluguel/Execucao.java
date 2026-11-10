package br.com.vadebicicleta.scb.aluguel;

/**
 * Diz se a confirmacao acabou de ser aprovada e ainda precisa da mensagem simulada.
 */
record Execucao(ResultadoAluguel resultado, boolean notificar) {

}
