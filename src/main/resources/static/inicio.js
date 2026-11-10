const mensagem = document.getElementById("mensagem-formulario");
const nome = document.getElementById("nome-ciclista");
const aplicacao = document.getElementById("aplicacao");
const resumoAluguel = document.getElementById("resumo-aluguel");
const resumoCartao = document.getElementById("resumo-cartao");

function irParaLogin() {
	window.location.replace("index.html");
}

function consultar(url) {
	return fetch(url, { credentials: "same-origin", cache: "no-store" }).then(function (resposta) {
		if (resposta.status === 401) {
			irParaLogin();
			return null;
		}
		return lerCorpo(resposta).then(function (corpo) {
			return { ok: resposta.ok, corpo: corpo };
		});
	});
}

function mostrarAluguel(resultado) {
	resumoAluguel.replaceChildren();
	if (!resultado) {
		return;
	}
	if (!resultado.ok) {
		const aviso = document.createElement("p");
		aviso.className = "aviso erro";
		aviso.textContent = "Não foi possível consultar o aluguel.";
		resumoAluguel.appendChild(aviso);
		return;
	}
	const dados = resultado.corpo || {};
	if (!dados.ativo || !dados.aluguel) {
		const caixa = document.createElement("div");
		caixa.className = "vazio";
		const texto = document.createElement("p");
		texto.textContent = "Nenhum aluguel em andamento.";
		const link = document.createElement("a");
		link.href = "alugar.html";
		link.textContent = "Alugar uma bicicleta";
		caixa.append(texto, link);
		resumoAluguel.appendChild(caixa);
		return;
	}
	const aluguel = dados.aluguel;
	const caixa = document.createElement("article");
	caixa.className = "aluguel-destaque";
	const titulo = document.createElement("h2");
	titulo.textContent = "Aluguel em andamento";
	const lista = document.createElement("dl");
	adicionarLinha(lista, "Bicicleta", aluguel.rfid);
	adicionarLinha(lista, "Retirada", aluguel.inicio ? String(aluguel.inicio).replace("T", " ") : "");
	if (aluguel.duracaoHoras !== undefined && aluguel.duracaoHoras !== null) {
		adicionarLinha(lista, "Período básico", aluguel.duracaoHoras + " horas");
	}
	adicionarLinha(lista, "Tranca", aluguel.numeroTranca);
	adicionarLinha(lista, "Totem", aluguel.totem);
	const link = document.createElement("a");
	link.href = "aluguel.html";
	link.textContent = "Ver aluguel";
	caixa.append(titulo, lista, link);
	resumoAluguel.appendChild(caixa);
}

function mostrarCartao(resultado) {
	resumoCartao.replaceChildren();
	if (!resultado || !resultado.ok || !resultado.corpo) {
		const aviso = document.createElement("p");
		aviso.className = "aviso erro";
		aviso.textContent = "Não foi possível consultar o cartão.";
		resumoCartao.appendChild(aviso);
		return;
	}
	const dados = resultado.corpo;
	const caixa = document.createElement("article");
	caixa.className = "resultado";
	const titulo = document.createElement("h2");
	titulo.textContent = "Cartão de demonstração";
	const texto = document.createElement("p");
	if (dados.cadastrado) {
		texto.textContent = (dados.titular || "Cartão") + " " + (dados.mascara || "") + ". " + (dados.aviso || "");
	}
	else {
		texto.textContent = dados.aviso || "Nenhum cartão de demonstração cadastrado.";
	}
	caixa.append(titulo, texto);
	resumoCartao.appendChild(caixa);
}

fetch("/api/ciclista", { credentials: "same-origin", cache: "no-store" })
	.then(function (resposta) {
		if (resposta.status === 401) {
			irParaLogin();
			return null;
		}
		if (!resposta.ok) {
			throw new Error("resposta");
		}
		return resposta.json();
	})
	.then(function (dados) {
		if (!dados) {
			return null;
		}
		nome.textContent = dados.nome || "";
		mostrarAviso(mensagem, "", "");
		aplicacao.hidden = false;
		return prepararCsrf().then(function () {
			return Promise.all([consultar("/api/aluguel"), consultar("/api/cartao")]);
		});
	})
	.then(function (resumos) {
		if (!resumos) {
			return;
		}
		mostrarAluguel(resumos[0]);
		mostrarCartao(resumos[1]);
	})
	.catch(function () {
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});
