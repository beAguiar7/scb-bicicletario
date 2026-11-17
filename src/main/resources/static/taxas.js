const mensagem = document.getElementById("mensagem-formulario");
const lista = document.getElementById("lista");
const vazio = document.getElementById("vazio");

function situacaoTexto(situacao) {
	if (situacao === "PAGA") {
		return "Paga";
	}
	if (situacao === "PENDENTE") {
		return "Pendente";
	}
	return situacao || "";
}

function formatarReais(valor) {
	return "R$ " + String(valor).replace(".", ",");
}

function montarTaxa(item) {
	const artigo = document.createElement("article");
	artigo.className = "resultado";
	const titulo = document.createElement("h2");
	titulo.textContent = "Taxa extra";
	const dados = document.createElement("dl");
	adicionarLinha(dados, "Valor", formatarReais(item.valor));
	adicionarLinha(dados, "Situação", situacaoTexto(item.situacao));
	adicionarLinha(dados, "Horário", item.horario ? String(item.horario).replace("T", " ") : "");
	const botao = document.createElement("button");
	botao.type = "button";
	botao.className = "botao-pagar";
	botao.textContent = "Tentar pagar novamente";
	botao.addEventListener("click", function () {
		pagar(botao, item.id, artigo);
	});
	artigo.append(titulo, dados, botao);
	return artigo;
}

function mostrarResultado(artigo, tipo, texto, pedirCartao) {
	let caixa = artigo.querySelector(".resultado-pagamento");
	if (!caixa) {
		caixa = document.createElement("div");
		caixa.className = "resultado resultado-pagamento";
		artigo.appendChild(caixa);
	}
	caixa.className = "resultado resultado-pagamento " + tipo;
	caixa.replaceChildren();
	const paragrafo = document.createElement("p");
	paragrafo.textContent = texto || "";
	caixa.appendChild(paragrafo);
	const selo = document.createElement("p");
	selo.className = "selo";
	selo.textContent = "Pagamento simulado";
	caixa.appendChild(selo);
	if (pedirCartao) {
		const link = document.createElement("a");
		link.href = "cartao.html";
		link.textContent = "Cadastrar cartão";
		caixa.appendChild(link);
	}
}

function marcarSituacao(artigo, texto) {
	const termos = artigo.querySelectorAll("dt");
	termos.forEach(function (termo) {
		if (termo.textContent === "Situação" && termo.nextElementSibling) {
			termo.nextElementSibling.textContent = texto;
		}
	});
}

function pagar(botao, id, artigo) {
	botao.disabled = true;
	const textoBotao = botao.textContent;
	botao.textContent = "Pagando...";
	fetch("/api/taxas-extras/" + id + "/pagamento", {
		method: "POST",
		credentials: "same-origin",
		headers: cabecalhos()
	})
		.then(function (resposta) {
			if (resposta.status === 401) {
				window.location.replace("index.html");
				return null;
			}
			return lerCorpo(resposta);
		})
		.then(function (dados) {
			if (!dados) {
				return;
			}
			if (dados.codigo === "SEM_CARTAO") {
				mostrarResultado(artigo, "indisponivel", dados.mensagem, true);
				botao.disabled = false;
				botao.textContent = textoBotao;
				return;
			}
			if (dados.situacao === "PAGA") {
				marcarSituacao(artigo, "Paga");
				mostrarResultado(artigo, "sucesso", dados.mensagem, false);
				botao.disabled = true;
				botao.textContent = "Paga";
				return;
			}
			if (dados.situacao === "PENDENTE") {
				marcarSituacao(artigo, "Pendente");
				mostrarResultado(artigo, "recusa", dados.mensagem, false);
				botao.disabled = false;
				botao.textContent = textoBotao;
				return;
			}
			mostrarResultado(artigo, "indisponivel", dados.mensagem || "Não foi possível pagar a taxa extra.", false);
			botao.disabled = false;
			botao.textContent = textoBotao;
		})
		.catch(function () {
			mostrarResultado(artigo, "indisponivel", mensagemIndisponivel(), false);
			botao.disabled = false;
			botao.textContent = textoBotao;
		});
}

exigirSessao()
	.then(function (perfil) {
		if (!perfil) {
			return null;
		}
		return prepararCsrf().then(function () {
			return fetch("/api/taxas-extras", { credentials: "same-origin", cache: "no-store" });
		});
	})
	.then(function (resposta) {
		if (!resposta) {
			return null;
		}
		if (resposta.status === 401) {
			window.location.replace("index.html");
			return null;
		}
		if (!resposta.ok) {
			throw new Error("taxas");
		}
		return resposta.json();
	})
	.then(function (taxas) {
		if (!taxas) {
			return;
		}
		lista.replaceChildren();
		if (!Array.isArray(taxas) || taxas.length === 0) {
			mostrarAviso(mensagem, "", "");
			vazio.hidden = false;
			return;
		}
		mostrarAviso(mensagem, "", "");
		taxas.forEach(function (item) {
			lista.appendChild(montarTaxa(item));
		});
	})
	.catch(function () {
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});
