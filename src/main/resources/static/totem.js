const formulario = document.getElementById("formulario-devolucao");
const botao = document.getElementById("botao-devolver");
const resultado = document.getElementById("resultado");
const aviso = document.getElementById("mensagem-formulario");

formulario.addEventListener("submit", function (evento) {
	evento.preventDefault();
	limparErros(["rfid", "numeroTranca"]);
	mostrarAviso(aviso, "", "");
	resultado.hidden = true;

	const rfid = document.getElementById("rfid").value.trim();
	const numeroTranca = document.getElementById("numeroTranca").value.trim();
	let valido = true;
	if (!rfid) {
		definirErro("rfid", "Informe o RFID da bicicleta.");
		valido = false;
	}
	if (!numeroTranca) {
		definirErro("numeroTranca", "Informe o número da tranca.");
		valido = false;
	}
	if (!valido) {
		return;
	}

	botao.disabled = true;
	const textoBotao = botao.textContent;
	botao.textContent = "Registrando...";
	fetch("/api/devolucoes", {
		method: "POST",
		credentials: "same-origin",
		headers: cabecalhos({ "Content-Type": "application/json" }),
		body: JSON.stringify({ rfid: rfid, numeroTranca: numeroTranca })
	})
		.then(lerCorpo)
		.then(function (dados) {
			if (dados.horario && dados.numeroTranca) {
				mostrarResultado("sucesso", dados.repetida ? "Devolução já registrada" : "Devolvida", dados.mensagem, dados);
				return;
			}
			mostrarFalha(dados);
		})
		.catch(function () {
			mostrarAviso(aviso, mensagemIndisponivel(), "erro");
		})
		.finally(function () {
			botao.disabled = false;
			botao.textContent = textoBotao;
		});
});

function mostrarFalha(dados) {
	const codigo = dados && dados.codigo;
	if (codigo === "TRANCA_INEXISTENTE") {
		mostrarResultado("indisponivel", "Tranca inexistente", dados.mensagem, null);
		return;
	}
	if (codigo === "TRANCA_OCUPADA") {
		mostrarResultado("indisponivel", "Tranca ocupada", dados.mensagem, null);
		return;
	}
	if (codigo === "BICICLETA_SEM_ALUGUEL") {
		mostrarResultado("indisponivel", "Sem aluguel ativo", dados.mensagem, null);
		return;
	}
	aplicarErrosDoServidor(dados);
	mostrarAviso(aviso, (dados && dados.mensagem) || "Não foi possível registrar a devolução.", "erro");
}

function mostrarResultado(tipo, rotulo, detalhe, dados) {
	resultado.hidden = false;
	resultado.className = "resultado " + tipo;
	resultado.replaceChildren();
	const titulo = document.createElement("p");
	titulo.className = "resultado-rotulo";
	titulo.textContent = rotulo;
	const texto = document.createElement("p");
	texto.textContent = detalhe || "";
	resultado.append(titulo, texto);
	if (!dados) {
		return;
	}
	const lista = document.createElement("dl");
	adicionarLinha(lista, "Bicicleta", dados.rfid);
	adicionarLinha(lista, "Tranca", dados.numeroTranca);
	adicionarLinha(lista, "Horário", dados.horario ? String(dados.horario).replace("T", " ") : "");
	if (dados.taxaExtra) {
		adicionarLinha(lista, "Taxa extra", formatarReais(dados.taxaExtra.valor));
		adicionarLinha(lista, "Situação", textoSituacao(dados.taxaExtra.situacao));
	}
	resultado.appendChild(lista);
	if (!dados.taxaExtra) {
		return;
	}
	const selo = document.createElement("p");
	selo.className = "selo";
	selo.textContent = "Pagamento simulado";
	resultado.appendChild(selo);
	if (dados.taxaExtra.situacao === "PENDENTE") {
		const nota = document.createElement("p");
		nota.textContent = "A devolução foi concluída. A taxa extra ficou pendente.";
		resultado.appendChild(nota);
	}
}

function formatarReais(valor) {
	return "R$ " + String(valor).replace(".", ",");
}

function textoSituacao(situacao) {
	if (situacao === "PAGA") {
		return "Paga";
	}
	if (situacao === "PENDENTE") {
		return "Pendente";
	}
	return situacao || "";
}

prepararCsrf().catch(function () {
	mostrarAviso(aviso, mensagemIndisponivel(), "erro");
	botao.disabled = true;
});
