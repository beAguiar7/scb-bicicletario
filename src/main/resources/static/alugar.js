const taxa = document.getElementById("taxa");
const resumo = document.getElementById("resumo-confirmacao");
const totens = document.getElementById("totens");
const formulario = document.getElementById("formulario-aluguel");
const mensagem = document.getElementById("mensagem-formulario");
const resultado = document.getElementById("resultado");
const botao = document.getElementById("botao-confirmar");
const campoTranca = document.getElementById("numeroTranca");
const escolha = document.getElementById("tranca-escolhida");

let chave = criarChave();

formulario.hidden = true;

function criarChave() {
	if (window.crypto && typeof window.crypto.randomUUID === "function") {
		return window.crypto.randomUUID();
	}
	return "aluguel-" + Date.now().toString(36) + "-" + Math.random().toString(36).slice(2, 12);
}

function preencherTaxa(dados) {
	taxa.replaceChildren();
	resumo.replaceChildren();
	const valor = document.createElement("strong");
	valor.textContent = dados.texto ? "Taxa básica: " + dados.texto : "Taxa básica indisponível";
	const aviso = document.createElement("p");
	aviso.textContent = dados.aviso || "";
	taxa.append(valor, aviso);
	const antes = document.createElement("strong");
	antes.textContent = "Confira antes de confirmar";
	const detalhe = document.createElement("p");
	const partes = [];
	if (dados.texto) {
		partes.push(dados.texto);
	}
	if (dados.duracaoHoras !== undefined && dados.duracaoHoras !== null) {
		partes.push("Período básico: " + dados.duracaoHoras + " horas");
	}
	detalhe.textContent = partes.join(". ");
	resumo.append(antes, detalhe);
}

function selecionarTranca(numero, botaoEscolhido) {
	campoTranca.value = numero;
	escolha.textContent = "Tranca selecionada: " + numero;
	totens.querySelectorAll("button").forEach(function (item) {
		item.setAttribute("aria-pressed", "false");
	});
	if (botaoEscolhido) {
		botaoEscolhido.setAttribute("aria-pressed", "true");
	}
	campoTranca.focus();
}

function mostrarTotens(lista) {
	totens.replaceChildren();
	(lista || []).forEach(function (totem) {
		const bloco = document.createElement("section");
		bloco.className = "totem";
		const topo = document.createElement("div");
		topo.className = "totem-topo";
		const titulo = document.createElement("h2");
		titulo.textContent = totem.nome || "Totem";
		const quantidade = (totem.trancas || []).length;
		const disponibilidade = document.createElement("p");
		disponibilidade.className = "disponibilidade";
		if (typeof totem.capacidade === "number") {
			disponibilidade.textContent = quantidade + " de " + totem.capacidade + " disponíveis";
		}
		else {
			disponibilidade.textContent = quantidade + " disponíveis";
		}
		topo.append(titulo, disponibilidade);
		bloco.appendChild(topo);
		if (!totem.trancas || totem.trancas.length === 0) {
			const vazio = document.createElement("p");
			vazio.textContent = "Nenhuma tranca disponível neste totem.";
			bloco.appendChild(vazio);
		}
		const grade = document.createElement("div");
		grade.className = "trancas";
		(totem.trancas || []).forEach(function (numero) {
			const atalho = document.createElement("button");
			atalho.type = "button";
			atalho.textContent = numero;
			atalho.setAttribute("aria-pressed", campoTranca.value === numero ? "true" : "false");
			atalho.addEventListener("click", function () {
				selecionarTranca(numero, atalho);
			});
			grade.appendChild(atalho);
		});
		bloco.appendChild(grade);
		totens.appendChild(bloco);
	});
}

function mostrarResultado(tipo, rotulo, detalhe, aluguel) {
	resultado.hidden = false;
	resultado.className = "resultado " + tipo;
	resultado.replaceChildren();
	const titulo = document.createElement("p");
	titulo.className = "resultado-rotulo";
	titulo.textContent = rotulo;
	const texto = document.createElement("p");
	texto.textContent = detalhe || "";
	resultado.append(titulo, texto);
	if (!aluguel) {
		return;
	}
	const lista = document.createElement("dl");
	adicionarLinha(lista, "Bicicleta", aluguel.rfid);
	adicionarLinha(lista, "Tranca", aluguel.numeroTranca);
	adicionarLinha(lista, "Totem", aluguel.totem);
	adicionarLinha(lista, "Início", aluguel.inicio ? String(aluguel.inicio).replace("T", " ") : "");
	if (aluguel.duracaoHoras !== undefined && aluguel.duracaoHoras !== null) {
		adicionarLinha(lista, "Período básico", aluguel.duracaoHoras + " horas");
	}
	resultado.appendChild(lista);
}

exigirSessao()
	.then(function (perfil) {
		if (!perfil) {
			return null;
		}
		return prepararCsrf();
	})
	.then(function () {
		if (!csrf) {
			return null;
		}
		return Promise.all([
			fetch("/api/aluguel/taxa", { credentials: "same-origin", cache: "no-store" }).then(lerCorpo),
			fetch("/api/totens", { credentials: "same-origin", cache: "no-store" }).then(lerCorpo)
		]);
	})
	.then(function (respostas) {
		if (!respostas) {
			return;
		}
		const dadosTaxa = respostas[0];
		const lista = respostas[1];
		if (!dadosTaxa || !dadosTaxa.valor || !Array.isArray(lista)) {
			taxa.textContent = (dadosTaxa && dadosTaxa.mensagem) || "Não foi possível carregar a taxa e os totens.";
			mostrarAviso(mensagem, (dadosTaxa && dadosTaxa.mensagem) || "Não foi possível carregar a taxa e os totens.", "erro");
			return;
		}
		preencherTaxa(dadosTaxa);
		mostrarTotens(lista);
		formulario.hidden = false;
	})
	.catch(function () {
		taxa.textContent = "";
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});

formulario.addEventListener("submit", function (evento) {
	evento.preventDefault();
	if (botao.disabled) {
		return;
	}
	definirErro("numeroTranca", "");
	mostrarAviso(mensagem, "", "");
	resultado.hidden = true;
	const numero = campoTranca.value.trim();
	if (!numero) {
		definirErro("numeroTranca", "Informe o número da tranca.");
		return;
	}
	const textoOriginal = botao.textContent;
	botao.disabled = true;
	botao.setAttribute("aria-busy", "true");
	botao.textContent = "Confirmando...";
	fetch("/api/alugueis", {
		method: "POST",
		credentials: "same-origin",
		headers: cabecalhos({ "Content-Type": "application/json" }),
		body: JSON.stringify({ chave: chave, numeroTranca: numero })
	})
		.then(function (resposta) {
			return lerCorpo(resposta).then(function (corpo) {
				return { ok: resposta.ok, status: resposta.status, corpo: corpo };
			});
		})
		.then(function (envio) {
			if (envio.status === 401) {
				window.location.replace("index.html");
				return;
			}
			if (!envio.ok) {
				const codigo = envio.corpo.codigo || "";
				const indisponivel = codigo === "TRANCA_INEXISTENTE" || codigo === "TRANCA_SEM_BICICLETA";
				mostrarResultado(
					indisponivel ? "indisponivel" : "recusa",
					indisponivel ? "Indisponível" : "Não concluído",
					envio.corpo.mensagem || "Não foi possível concluir o aluguel."
				);
				aplicarErrosDoServidor(envio.corpo);
				botao.disabled = false;
				return;
			}
			const corpo = envio.corpo;
			if (corpo.resultado === "RECUSADO") {
				mostrarResultado("recusa", "Recusado", corpo.mensagem);
			}
			else {
				mostrarResultado("sucesso", "Aprovado", corpo.mensagem, corpo.aluguel);
			}
			return fetch("/api/totens", { credentials: "same-origin", cache: "no-store" })
				.then(lerCorpo)
				.then(function (lista) {
					if (Array.isArray(lista)) {
						mostrarTotens(lista);
					}
				});
		})
		.catch(function () {
			if (resultado.hidden) {
				mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
				botao.disabled = false;
			}
		})
		.finally(function () {
			botao.removeAttribute("aria-busy");
			botao.textContent = textoOriginal;
		});
});
